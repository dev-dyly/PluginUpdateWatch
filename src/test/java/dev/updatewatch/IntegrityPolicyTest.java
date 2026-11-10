package dev.updatewatch;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.bukkit.configuration.file.YamlConfiguration;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.jar.*;
import static org.junit.jupiter.api.Assertions.*;

class IntegrityPolicyTest {
    @TempDir Path temp;
    private byte[] plugin() throws Exception {
        var bytes = new ByteArrayOutputStream();
        try (var out = new JarOutputStream(bytes)) {
            out.putNextEntry(new JarEntry("plugin.yml")); out.write("name: Example\nversion: '2.0'\nmain: example.Main\n".getBytes()); out.closeEntry();
            out.putNextEntry(new JarEntry("example/Main.class"));
            try (var in = getClass().getResourceAsStream("/dev/updatewatch/IntegrityPolicyTest.class")) { in.transferTo(out); }
            out.closeEntry();
        }
        return bytes.toByteArray();
    }
    private Remote.Source source(String provider) { return new Remote.Source("Example", "1.0", provider, "123", ".*\\.jar", "26.3"); }
    private String digest(byte[] bytes, String algorithm) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance(algorithm).digest(bytes)); }
    @Test void missingOptionRequiresChecksumAndExplicitFalseRemainsSupported() {
        var yaml = new YamlConfiguration(); assertTrue(Settings.requireChecksum(yaml));
        yaml.set("downloads.require-checksum", false); assertFalse(Settings.requireChecksum(yaml));
        yaml.set("downloads.require-checksum", true); assertTrue(Settings.requireChecksum(yaml));
        yaml.set("downloads.require-checksum", "false"); assertThrows(IllegalArgumentException.class, () -> Settings.requireChecksum(yaml));
    }
    @Test void requiredMissingHashFailsInVerifierBeforeReadingTheFile() {
        assertThrows(IOException.class, () -> Remote.verifyHash(temp.resolve("absent"), null, "SHA-512", true));
        assertThrows(IOException.class, () -> Remote.verifyHash(temp.resolve("absent"), new Remote.Release("2.0", "url", "page"), true));
    }
    @Test void allProvidersAreBlockedBeforeNetworkWhenChecksumIsRequiredButMissing() {
        for (String provider : new String[]{"github", "spigot", "modrinth"}) {
            var exception = assertThrows(Failure.Problem.class, () -> DownloadManager.download(source(provider), new Remote.Release("2.0", "url", "page"), temp, Settings.defaults(), true,
                    (url, seconds) -> { fail("Missing integrity must block before connecting"); return null; }));
            assertEquals(Failure.Kind.CHECKSUM_MISSING, Failure.classify(exception).kind());
            assertTrue(Failure.classify(exception).describe(provider).startsWith("[WARNING] CHECKSUM_MISSING"));
        }
    }
    @Test void githubSha256AndModrinthSha512AreBothAcceptedUnderStrictPolicy() throws Exception {
        byte[] bytes = plugin();
        var github = new Remote.Release("2.0", "url", "page", null, digest(bytes, "SHA-256"));
        var modrinth = new Remote.Release("2.0", "url", "page", digest(bytes, "SHA-512"));
        for (var release : new Remote.Release[]{github, modrinth}) {
            Path saved = DownloadManager.download(source("github"), release, temp.resolve(release.sha512() == null ? "github" : "modrinth"), Settings.defaults(), true, (u, seconds) -> new ByteArrayInputStream(bytes));
            assertArrayEquals(bytes, Files.readAllBytes(saved));
        }
    }
    @Test void explicitlyAllowedMissingHashStillRequiresAValidPluginJar() throws Exception {
        var release = new Remote.Release("2.0", "url", "page"); byte[] bytes = plugin();
        assertTrue(Files.exists(DownloadManager.download(source("spigot"), release, temp, Settings.defaults(), false, (u, s) -> new ByteArrayInputStream(bytes))));
        assertThrows(Failure.Problem.class, () -> DownloadManager.download(source("spigot"), release, temp, Settings.defaults(), false, (u, s) -> new ByteArrayInputStream("<html>error</html>".getBytes())));
    }
    @Test void explicitOptOutNeverBypassesASuppliedWrongHash() throws Exception {
        byte[] bytes = plugin(); var release = new Remote.Release("2.0", "url", "page", null, "0".repeat(64));
        var exception = assertThrows(Failure.Problem.class, () -> DownloadManager.download(source("github"), release, temp, Settings.defaults(), false, (u, s) -> new ByteArrayInputStream(bytes)));
        assertTrue(exception.getMessage().contains("does not match"));
        try (var files = Files.list(temp)) { assertEquals(0, files.count()); }
    }
}
