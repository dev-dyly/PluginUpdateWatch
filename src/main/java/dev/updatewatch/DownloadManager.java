package dev.updatewatch;

import java.io.*;
import java.nio.file.*;

final class DownloadManager {
    interface Opener { InputStream open(String url, int seconds) throws IOException; }
    /** Recover partial transfers left when the JVM exits before an interrupted daemon can clean up. */
    static int cleanupIncomplete(Path folder) throws IOException {
        if (!Files.exists(folder, LinkOption.NOFOLLOW_LINKS)) return 0;
        int removed = 0;
        IOException failure = null;
        try (var files = Files.list(folder)) {
            for (Path path : files.filter(p -> p.getFileName().toString().matches("\\.download-[0-9]+\\.tmp")).toList()) {
                if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS)) continue;
                try { Files.delete(path); removed++; }
                catch (IOException e) {
                    if (failure == null) failure = e;
                    else failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) throw failure;
        return removed;
    }
    static Path download(Remote.Source source, Remote.Release release, Path folder, Settings settings, boolean requireChecksum, Opener opener) throws Exception {
        return download(source, release, folder, settings, requireChecksum, opener, null);
    }
    static Path download(Remote.Source source, Remote.Release release, Path folder, Settings settings, boolean requireChecksum, Opener opener, Path installed) throws Exception {
        if (requireChecksum && !release.hasChecksum()) throw Failure.problem(Failure.Kind.CHECKSUM_MISSING, source.type() + " supplies no supported checksum; use the release page for a manual download, or explicitly set downloads.require-checksum: false");
        checkCancelled();
        String filename = release.filename() == null ? source.name() + ".jar" : release.filename();
        if (!Discovery.validFilename(filename)) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Unsafe download filename; use the release page for a manual download");
        Files.createDirectories(folder);
        Path existing = existingDownload(folder, filename, source.name());
        if (existing != null && release.hasChecksum()) {
            boolean matches = false;
            try { Remote.verifyHash(existing, release, true); matches = true; }
            catch (Failure.Problem mismatch) { if (mismatch.kind != Failure.Kind.INVALID_ARTIFACT) throw mismatch; }
            if (matches) throw alreadySaved();
        }
        Path temp = Files.createTempFile(folder, ".download-", ".tmp");
        try {
            try (InputStream in = open(opener, release.download(), settings.downloadSeconds()); OutputStream out = Files.newOutputStream(temp)) {
                byte[] buffer = new byte[8192]; long total = 0; int n;
                while ((n = read(in, buffer)) != -1) {
                    checkCancelled();
                    total += n;
                    if (total > settings.downloadBytes()) throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, source.type() + " download rejected at " + total + " bytes (limit " + settings.downloadBytes() + ")");
                    out.write(buffer, 0, n);
                }
            }
            Remote.verifyHash(temp, release, requireChecksum);
            Remote.validate(temp, source.name());
            ArtifactFreshness.validate(source, release, temp, installed);
            Path target = folder.resolve(filename);
            existing = existingDownload(folder, filename, source.name());
            if (existing != null && Files.mismatch(existing, temp) == -1) throw alreadySaved();
            checkCancelled();
            try { Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING); }
            catch (AtomicMoveNotSupportedException e) { Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING); }
            return target;
        } finally { Files.deleteIfExists(temp); }
    }
    private static Failure.Problem alreadySaved() {
        return Failure.problem(Failure.Kind.ALREADY_DOWNLOADED, "The selected artifact is already saved in the downloads folder; no duplicate was saved");
    }
    private static void checkCancelled() throws InterruptedIOException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Download cancelled");
    }
    private static Path existingDownload(Path folder, String filename, String plugin) throws Exception {
        Path match = null;
        // A generic asset name such as plugin.jar must never overwrite another plugin's download.
        try (var files = Files.list(folder)) {
            for (Path existing : files.filter(p -> p.getFileName().toString().equalsIgnoreCase(filename)).toList()) {
                if (!existing.getFileName().toString().equals(filename) || !Files.isRegularFile(existing, LinkOption.NOFOLLOW_LINKS))
                    throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Download filename conflicts with an existing file; move or rename " + existing.getFileName() + " first");
                try { Remote.validate(existing, plugin); }
                catch (InterruptedIOException e) { throw e; }
                catch (Exception e) { throw Failure.problem(Failure.Kind.INVALID_ARTIFACT, "Existing " + filename + " could not be verified as the same plugin; move or rename it first", e); }
                match = existing;
            }
        }
        return match;
    }
    private static InputStream open(Opener opener, String url, int seconds) throws IOException {
        try { return opener.open(url, seconds); }
        catch (Remote.HttpError | Failure.Problem e) { throw e; }
        catch (IOException e) { throw Failure.problem(Failure.Kind.NETWORK, "Could not open the download connection", e); }
    }
    private static int read(InputStream in, byte[] buffer) throws IOException {
        try { return in.read(buffer); }
        catch (IOException e) { throw Failure.problem(Failure.Kind.NETWORK, "Download interrupted before completion; temporary file discarded", e); }
    }
}
