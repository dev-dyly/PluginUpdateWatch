package dev.updatewatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class DownloadFilenameTest {
    @TempDir Path temp;
    byte[] jar(String name, String padding) throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new JarOutputStream(bytes)) {
            out.putNextEntry(new JarEntry("plugin.yml")); out.write(("name: " + name + "\nversion: '2.0'\nmain: example.Main\n#" + padding).getBytes()); out.closeEntry();
            out.putNextEntry(new JarEntry("example/Main.class"));
            try (var in = getClass().getResourceAsStream("/dev/updatewatch/DownloadFilenameTest.class")) { in.transferTo(out); }
            out.closeEntry();
        }
        return bytes.toByteArray();
    }
    Path download(String name, String filename, byte[] bytes) throws Exception {
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        return DownloadManager.download(new Remote.Source(name, "1", "github", "o/r", ".*", "26.3"),
                new Remote.Release("v2.0-paper", "url", "page", null, hash, filename), temp, Settings.defaults(), true,
                (url, seconds) -> new ByteArrayInputStream(bytes));
    }
    @Test void preservesPublisherFilenameIncludingSpacesAndBuildCharacters() throws Exception {
        byte[] bytes = jar("Example", "");
        Path saved = download("Example", "Example Plugin-2.0+paper.jar", bytes);
        assertEquals("Example Plugin-2.0+paper.jar", saved.getFileName().toString()); assertArrayEquals(bytes, Files.readAllBytes(saved));
    }
    @Test void providerWithoutFilenameUsesPlainPluginName() throws Exception {
        assertEquals("Example.jar", download("Example", null, jar("Example", "")).getFileName().toString());
    }
    @Test void unsafeProviderFilenamesAreRejectedBeforeNetwork() {
        for (String filename : List.of("../escape.jar", "CON.jar", "a:stream.jar", "dir/plugin.jar", "dir\\plugin.jar")) {
            var release = new Remote.Release("2", "url", "page", null, "a".repeat(64), filename);
            assertThrows(Exception.class, () -> DownloadManager.download(new Remote.Source("Example", "1", "github", "o/r", ".*", "26.3"),
                    release, temp, Settings.defaults(), true, (u, s) -> { fail("Unsafe name must fail before download"); return null; }));
        }
    }
    @Test void sameAssetNameCannotOverwriteAnotherPlugin() throws Exception {
        byte[] first = jar("First", ""); download("First", "plugin.jar", first);
        assertThrows(Exception.class, () -> download("Second", "plugin.jar", jar("Second", "")));
        assertArrayEquals(first, Files.readAllBytes(temp.resolve("plugin.jar")));
        try (var paths = Files.list(temp)) { assertEquals(1, paths.count()); }
    }
    @Test void repeatDownloadForSamePluginReplacesOnlyItsValidatedFile() throws Exception {
        download("Example", "plugin.jar", jar("Example", "first")); byte[] replacement = jar("Example", "second");
        assertArrayEquals(replacement, Files.readAllBytes(download("Example", "plugin.jar", replacement)));
    }
    @Test void acceptsLargeRealisticDescriptorsForDiscoveryAndDownloadValidation() throws Exception {
        byte[] bytes = jar("Example", "x".repeat(120000)); Path saved = download("Example", "large.jar", bytes);
        assertEquals("Example", Discovery.read(saved).name());
    }
    @Test void oversizedDescriptorsRemainRejected() throws Exception {
        Path path = temp.resolve("too-large.jar"); Files.write(path, jar("Example", "x".repeat(Discovery.MAX_DESCRIPTOR_BYTES)));
        assertThrows(Exception.class, () -> Discovery.read(path)); assertThrows(Exception.class, () -> JarValidation.validate(path, "Example"));
    }
    @Test void identicalVerifiedDownloadDoesNotOpenAnotherConnectionOrMarkInstalledCurrent() throws Exception {
        byte[] bytes = jar("Example", "same");
        download("Example", "plugin.jar", bytes);
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        var source = new Remote.Source("Example", "1", "github", "o/r", ".*", "26.3");
        var release = new Remote.Release("2.0", "url", "page", null, hash, "plugin.jar");
        var error = assertThrows(Failure.Problem.class, () -> DownloadManager.download(source, release, temp, Settings.defaults(), true,
                (url, seconds) -> { fail("Already saved verified artifact must not be downloaded again"); return null; }));
        assertEquals(Failure.Kind.ALREADY_DOWNLOADED, Failure.classify(error).kind());
        assertArrayEquals(bytes, Files.readAllBytes(temp.resolve("plugin.jar")));
        var result = new UpdateCheckService.Result(source, release, Versions.Status.UPDATE, null);
        assertSame(result, ReportVisibility.afterDownload(result, Failure.classify(error)));
    }
    @Test void checksumlessIdenticalDownloadDiscardsTemporaryFileWithoutReplacingAcceptedFile() throws Exception {
        byte[] bytes = jar("Example", "same"); download("Example", "plugin.jar", bytes);
        var release = new Remote.Release("2", "url", "page", null, null, "plugin.jar");
        var source = new Remote.Source("Example", "1", "web", "url", ".*", "26.3");
        var error = assertThrows(Failure.Problem.class, () -> DownloadManager.download(source, release, temp, Settings.defaults(), false,
                (url, seconds) -> new ByteArrayInputStream(bytes)));
        assertEquals(Failure.Kind.ALREADY_DOWNLOADED, Failure.classify(error).kind());
        try (var files = Files.list(temp)) { assertEquals(1, files.count()); }
    }
    @Test void unrelatedExistingFilenameFailsBeforeNetwork() throws Exception {
        byte[] original = jar("First", ""); download("First", "plugin.jar", original);
        var source = new Remote.Source("Second", "1", "web", "url", ".*", "26.3");
        var release = new Remote.Release("2", "url", "page", null, null, "plugin.jar");
        assertThrows(Failure.Problem.class, () -> DownloadManager.download(source, release, temp, Settings.defaults(), false,
                (url, seconds) -> { fail("Filename collision must be found before network"); return null; }));
        assertArrayEquals(original, Files.readAllBytes(temp.resolve("plugin.jar")));
    }
}
