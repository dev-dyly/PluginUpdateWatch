package dev.updatewatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class HardeningTest {
    @TempDir Path temp;
    @Test void rejectsMalformedSourcePathsAndRepositories() {
        for (String link : List.of("https://github.com/o/..", "https://github.com/o/%2e%2e", "https://github.com/o/.git", "https://github.com/o//repo", "https://github.com/o/r/releases/download/v1/plugin.zip"))
            assertThrows(IllegalArgumentException.class, () -> SourceLink.parse(link));
    }
    @Test void invalidRowsCannotBeSilentlyDropped() {
        for (Object row : List.of("oops", Map.of("jar", "a.jar"), Map.of("jar", 42, "source", ""))) {
            var y = new YamlConfiguration(); y.set("updates", List.of(row));
            assertThrows(IllegalArgumentException.class, () -> ConfigSources.resolve(y, List.of(), List.of(), "26.3", true, h -> null));
        }
    }
    @Test void duplicateStaleRowsAcrossProvidersAreReported() {
        var y = new YamlConfiguration(); y.set("updates", List.of(Map.of("jar", "a.jar", "source", "https://github.com/o/r"), Map.of("jar", "A.jar", "source", "https://modrinth.com/plugin/a")));
        var r = ConfigSources.resolve(y, List.of(), List.of(), "26.3", true, h -> null);
        assertTrue(r.notes().values().stream().anyMatch(n -> n.contains("Duplicate"))); assertEquals(2, r.entries().size());
    }
    @Test void rejectsUnsafeFilenames() {
        for (String name : List.of("CON.jar", "a?.jar", "a\n.jar", ".hidden.jar", "../a.jar", "a:stream.jar")) assertFalse(Discovery.validFilename(name), name);
    }
    @Test void validatesLimitsWithoutClampingMistakes() {
        var y = new YamlConfiguration(); y.set("downloads.max-size-mib", 500);
        assertEquals(500L * 1024 * 1024, Settings.parse(y, temp).downloadBytes());
        y.set("network.attempts", 0); assertThrows(IllegalArgumentException.class, () -> Settings.parse(y, temp));
    }
    @Test void protectsUserConfigEditsAndMalformedYaml() throws Exception {
        var path = temp.resolve("config.yml"); Files.writeString(path, "user edited");
        assertThrows(IOException.class, () -> ConfigManager.save(path, "old", "new")); assertEquals("user edited", Files.readString(path));
        assertThrows(Exception.class, () -> ConfigManager.parse("updates: ["));
    }
    @Test void githubChecksumIsExtractedAndVerified() throws Exception {
        String hash = "a".repeat(64);
        var source = new Remote.Source("Example", "1", "github", "owner/repo", ".*\\.jar", "26.3");
        var release = Providers.latest(source, u -> GithubReleaseTest.list("{\"tag_name\":\"2\",\"assets\":[{\"name\":\"a.jar\",\"browser_download_url\":\"https://github.com/o/r/releases/download/2/a.jar\",\"digest\":\"sha256:" + hash + "\"}]}"));
        assertEquals(hash, release.sha256());
        assertEquals("a.jar", release.filename());
        var file = temp.resolve("a"); Files.writeString(file, "test");
        assertThrows(IOException.class, () -> Remote.verifyHash(file, hash, "SHA-256"));
        Remote.verifyHash(file, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest("test".getBytes())), "SHA-256");
    }
    @Test void malformedProviderResponseIncludesProvider() {
        for (String provider : List.of("github", "modrinth", "spigot")) {
            var s = new Remote.Source("Example", "1", provider, provider.equals("github") ? "owner/repo" : "123", ".*\\.jar", "26.3");
            var e = assertThrows(IOException.class, () -> Providers.latest(s, u -> JsonParser.parseString("{}")));
            assertTrue(e.getMessage().contains(provider));
        }
    }
    @Test void missingMainClassAndMissingMetadataAreRejected() throws Exception {
        var path = temp.resolve("incomplete.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new JarEntry("plugin.yml")); out.write("name: Example\nversion: '1'\nmain: example.Absent\n".getBytes()); out.closeEntry();
        }
        assertThrows(IOException.class, () -> JarValidation.validate(path, "Example"));
        Files.writeString(temp.resolve("html.jar"), "<html>error</html>");
        assertEquals(1, Discovery.inventory(temp).size());
    }
    @Test void unsafeArchiveEntriesAreRejected() throws Exception {
        var path = temp.resolve("unsafe.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(path))) { out.putNextEntry(new JarEntry("../escape")); out.write(1); out.closeEntry(); }
        assertThrows(IOException.class, () -> JarValidation.validate(path, "Example"));
    }
    @Test void strictChecksumPolicyRejectsBeforeNetworkOrFileCreation() {
        assertThrows(IOException.class, () -> Remote.download(new Remote.Source("A", "1", "spigot", "1", ".*", "26.3"), new Remote.Release("2", "https://api.spiget.org/file", "page"), temp.resolve("downloads"), Settings.defaults(), true));
        assertFalse(Files.exists(temp.resolve("downloads")));
    }
    @Test void compatibilityIsExplicitAndUnknownVersionsAreNotBlocked() {
        assertEquals(Compatibility.Status.BELOW_MINIMUM, Compatibility.classify("1.21.10"));
        assertEquals(Compatibility.Status.TARGETED, Compatibility.classify("1.21.11"));
        assertEquals(Compatibility.Status.TARGETED, Compatibility.classify("26.3"));
        assertEquals(Compatibility.Status.UNVERIFIED, Compatibility.classify("27.1"));
    }
    @Test void failedDownloadsLeaveNoTemporaryOrAcceptedFile() throws Exception {
        var source = new Remote.Source("A", "1", "github", "o/r", ".*", "26.3");
        var release = new Remote.Release("2", "https://github.com/file", "page");
        var limits = new Settings(1000, 1000, 10, 10, 4, 1, temp, false);
        var e = assertThrows(IOException.class, () -> DownloadManager.download(source, release, temp, limits, false,
                (u, seconds) -> new java.io.ByteArrayInputStream(new byte[5])));
        assertTrue(e.getMessage().contains("5 bytes"));
        try (var files = Files.list(temp)) { assertEquals(0, files.count()); }
        assertThrows(IOException.class, () -> DownloadManager.download(source, release, temp, Settings.defaults(), false,
                (u, seconds) -> new java.io.InputStream() { public int read() throws IOException { throw new IOException("connection lost"); } }));
        try (var files = Files.list(temp)) { assertEquals(0, files.count()); }
    }
    @Test void inventoryRejectsMissingMetadata() throws Exception {
        var path = temp.resolve("incomplete.jar");
        try (var out = new JarOutputStream(Files.newOutputStream(path))) { out.putNextEntry(new JarEntry("plugin.yml")); out.write("name: Example\n".getBytes()); out.closeEntry(); }
        assertTrue(Discovery.inventory(temp).isEmpty());
    }
    @Test void interruptionDuringDownloadDiscardsTemporaryFileAndDoesNotSaveArchive() throws Exception {
        var source = new Remote.Source("Example", "1", "web", "url", ".*", "26.3");
        var release = new Remote.Release("2", "url", "page");
        try {
            var error = assertThrows(IOException.class, () -> DownloadManager.download(source, release, temp, Settings.defaults(), false,
                    (url, seconds) -> new java.io.ByteArrayInputStream(new byte[16]) {
                        @Override public int read(byte[] b, int off, int len) {
                            Thread.currentThread().interrupt(); return super.read(b, off, len);
                        }
                    }));
            assertEquals(Failure.Kind.CANCELLED, Failure.classify(error).kind());
        } finally { Thread.interrupted(); }
        try (var files = Files.list(temp)) { assertEquals(0, files.count()); }
    }
    @Test void invalidArchiveWithAllowedMissingChecksumStillCannotBeSaved() throws Exception {
        var source = new Remote.Source("Example", "1", "web", "url", ".*", "26.3");
        var release = new Remote.Release("2", "url", "page");
        var error = assertThrows(IOException.class, () -> DownloadManager.download(source, release, temp, Settings.defaults(), false,
                (url, seconds) -> new java.io.ByteArrayInputStream("<html>failure</html>".getBytes())));
        assertEquals(Failure.Kind.INVALID_ARTIFACT, Failure.classify(error).kind());
        try (var files = Files.list(temp)) { assertEquals(0, files.count()); }
    }
}
