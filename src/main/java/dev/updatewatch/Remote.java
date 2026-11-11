package dev.updatewatch;

import com.google.gson.*;
import java.io.*;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.*;

final class Remote {
    static final class HttpError extends IOException {
        private static final long serialVersionUID = 1L;
        final int code;
        final String host;
        final long retryAfterSeconds;
        HttpError(int code) { this(code, "remote provider", 0); }
        HttpError(int code, String host, long retry) {
            super(host + ": HTTP " + code + (code == 429 || (code == 403 && retry > 0) ? " (rate limited)" : code == 403 ? " (access denied)" : "")
                    + (retry > 0 ? "; retry after " + retry + " seconds" : ""));
            this.code = code; this.host = host; retryAfterSeconds = retry;
        }
    }
    record Source(String name, String installed, String type, String id, String asset, String minecraft, boolean enabled) {
        Source(String name, String installed, String type, String id, String asset, String minecraft) {
            this(name, installed, type, id, asset, minecraft, false);
        }
        Source withEnabled(boolean value) { return new Source(name, installed, type, id, asset, minecraft, value); }
    }
    record Release(String version, String download, String page, String sha512, String sha256, String filename, ReleaseType type,
                   String compatibilityWarning) {
        Release(String version, String download, String page, String sha512, String sha256, String filename, ReleaseType type) {
            this(version, download, page, sha512, sha256, filename, type, null);
        }
        Release(String version, String download, String page, String sha512, String sha256, String filename) {
            this(version, download, page, sha512, sha256, filename, ReleaseType.UNKNOWN);
        }
        Release(String version, String download, String page, String sha512, String sha256) { this(version, download, page, sha512, sha256, null); }
        Release(String version, String download, String page) { this(version, download, page, null, null); }
        Release(String version, String download, String page, String sha512) { this(version, download, page, sha512, null); }
        boolean hasChecksum() { return sha512 != null || sha256 != null; }
    }
    static JsonObject json(String url) throws IOException { return jsonValue(url, Settings.defaults()).getAsJsonObject(); }
    static JsonElement jsonValue(String url, Settings settings) throws IOException {
        return jsonValue(url, settings, new HttpTransport(settings));
    }
    static JsonElement jsonValue(String url, Settings settings, HttpTransport transport) throws IOException {
        try (InputStream in = transport.open(url, settings.metadataSeconds())) {
            byte[] bytes = in.readNBytes(2 * 1024 * 1024 + 1);
            if (bytes.length > 2 * 1024 * 1024) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "API response exceeded the 2 MiB limit");
            try {
                JsonElement parsed = JsonParser.parseString(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
                if (parsed.isJsonNull()) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Empty API response");
                return parsed;
            } catch (JsonParseException e) { throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Malformed JSON response", e); }
        } catch (HttpError | Failure.Problem e) { throw e; }
        catch (IOException e) { throw Failure.problem(Failure.Kind.NETWORK, "Could not complete the provider request", e); }
    }
    static Release latest(Source s, Settings settings) throws IOException {
        var transport = new HttpTransport(settings);
        Providers.JsonFetch json = url -> jsonValue(url, settings, transport);
        return s.type().equals("web") ? SourcePage.latest(s, url -> SourcePage.read(url, settings, transport), json) : Providers.latest(s, json);
    }
    static Path download(Source source, Release release, Path folder, Settings settings, boolean requireChecksum) throws Exception {
        var transport = new HttpTransport(settings);
        Path installed = null;
        if (Files.isDirectory(settings.pluginsFolder())) {
            var match = Discovery.matchReport(Discovery.inspect(settings.pluginsFolder()).jars(),
                    new Discovery.Installed(source.name(), source.installed(), null));
            if (match.jar() != null) installed = match.jar().path();
        }
        return DownloadManager.download(source, release, folder, settings, requireChecksum,
                source.type().equals("web") ? transport::openWeb : transport::open, installed);
    }
    static void verifyHash(Path path, String expected) throws Exception { verifyHash(path, expected, "SHA-512"); }
    static void verifyHash(Path path, String expected, String algorithm) throws Exception {
        verifyHash(path, expected, algorithm, false);
    }
    static void verifyHash(Path path, Release release, boolean required) throws Exception {
        if (release.sha512() != null) verifyHash(path, release.sha512(), "SHA-512", true);
        verifyHash(path, release.sha256(), "SHA-256", required && release.sha512() == null);
    }
    static void verifyHash(Path path, String expected, String algorithm, boolean required) throws Exception {
        if (expected == null) {
            if (required) throw Failure.problem(Failure.Kind.CHECKSUM_MISSING, "Required provider checksum is missing; no download was accepted");
            return;
        }
        var digest = MessageDigest.getInstance(algorithm);
        if (!expected.matches("[a-fA-F0-9]{" + digest.getDigestLength() * 2 + "}")) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Invalid " + algorithm + " checksum");
        try (InputStream in = Files.newInputStream(path)) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Checksum verification cancelled");
                digest.update(buffer, 0, n);
            }
        }
        if (!MessageDigest.isEqual(digest.digest(), HexFormat.of().parseHex(expected))) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Downloaded file checksum does not match provider (" + algorithm + ")");
    }
    static void validate(Path path, String expectedName) throws Exception {
        try { JarValidation.validate(path, expectedName); }
        catch (InterruptedIOException e) { throw e; }
        catch (Exception e) { throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "JAR structure or plugin identity validation failed; the file was rejected", e); }
    }
}
