package dev.updatewatch;

import com.google.gson.JsonParseException;
import java.io.*;
import java.net.*;
import java.nio.file.FileSystemException;
import java.util.*;
import java.util.zip.ZipException;
import javax.net.ssl.SSLException;
import org.bukkit.configuration.InvalidConfigurationException;

/** Safe, actionable failure summaries. Untrusted exception messages are never copied to chat. */
record Failure(Failure.Kind kind, String detail, long retryAfterSeconds) {
    enum Kind {
        NETWORK, RATE_LIMIT, PROVIDER_DENIED, PROVIDER_ERROR, INVALID_CONFIG,
        INVALID_RESPONSE, INVALID_ARTIFACT, CHECKSUM_MISSING, NO_COMPATIBLE_RELEASE, ALREADY_INSTALLED, ALREADY_DOWNLOADED, NO_NEWER_ARTIFACT, IO, CANCELLED, UNEXPECTED
    }

    /** Call sites supply a controlled description, never a remote response body or URL. */
    static final class Problem extends IOException {
        private static final long serialVersionUID = 1L;
        final Kind kind;
        private Problem(Kind kind, String safeDetail, Throwable cause) {
            super(safe(safeDetail), cause);
            this.kind = Objects.requireNonNull(kind);
        }
    }

    static Problem problem(Kind kind, String safeDetail) { return problem(kind, safeDetail, null); }
    static Problem problem(Kind kind, String safeDetail, Throwable cause) { return new Problem(kind, safeDetail, cause); }

    static Failure classify(Throwable thrown) {
        List<Throwable> causes = new ArrayList<>();
        Set<Throwable> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Throwable cause = thrown; cause != null && seen.add(cause); cause = cause.getCause()) causes.add(cause);
        for (Throwable cause : causes) {
            if (cause instanceof InterruptedException || cause instanceof java.util.concurrent.RejectedExecutionException
                    || cause instanceof InterruptedIOException && !(cause instanceof SocketTimeoutException))
                return new Failure(Kind.CANCELLED, "Operation was cancelled; review the downloads folder before retrying", 0);
        }
        // Explicit classifications preserve the operation's context (e.g. malformed downloaded YAML).
        for (Throwable cause : causes) if (cause instanceof Problem p) return new Failure(p.kind, p.getMessage(), 0);
        for (Throwable cause : causes) if (cause instanceof Remote.HttpError http) {
            if (http.code == 429 || (http.code == 403 && http.retryAfterSeconds > 0))
                return new Failure(Kind.RATE_LIMIT, "Provider rate limit reached (HTTP " + http.code + ")", http.retryAfterSeconds);
            if (http.code == 401 || http.code == 403)
                return new Failure(Kind.PROVIDER_DENIED, "Provider denied access (HTTP " + http.code + ")", 0);
            return new Failure(Kind.PROVIDER_ERROR, "Provider returned HTTP " + http.code, http.retryAfterSeconds);
        }
        for (Throwable cause : causes) {
            if (cause instanceof InvalidConfigurationException)
                return new Failure(Kind.INVALID_CONFIG, "Configuration is not valid YAML", 0);
            if (cause instanceof JsonParseException)
                return new Failure(Kind.INVALID_RESPONSE, "Provider returned malformed JSON", 0);
            if (cause instanceof ZipException)
                return new Failure(Kind.INVALID_ARTIFACT, "Downloaded archive is invalid or damaged", 0);
            if (cause instanceof SocketTimeoutException)
                return new Failure(Kind.NETWORK, "Network request timed out", 0);
            if (cause instanceof UnknownHostException)
                return new Failure(Kind.NETWORK, "Provider hostname could not be resolved", 0);
            if (cause instanceof SocketException || cause instanceof SSLException || cause instanceof EOFException)
                return new Failure(Kind.NETWORK, "Secure connection failed or ended before the response completed", 0);
            if (cause instanceof InterruptedException || cause instanceof InterruptedIOException)
                return new Failure(Kind.CANCELLED, "Operation was cancelled", 0);
            if (cause instanceof FileSystemException)
                return new Failure(Kind.IO, "Local file operation failed", 0);
        }
        for (Throwable cause : causes) if (cause instanceof IllegalArgumentException)
            return new Failure(Kind.INVALID_CONFIG, "Source or configuration value is invalid", 0);
        for (Throwable cause : causes) if (cause instanceof IOException)
            return new Failure(Kind.IO, "I/O operation failed", 0);
        return new Failure(Kind.UNEXPECTED, "Unexpected operation failure", 0);
    }

    String describe(String provider) {
        String category = switch (kind) {
            case NETWORK -> "NETWORK_ERROR";
            case INVALID_CONFIG -> "CONFIG_ERROR";
            case IO -> "FILE_ERROR";
            case CANCELLED -> "CANCELLED";
            case UNEXPECTED -> "INTERNAL_ERROR";
            case ALREADY_INSTALLED -> "ALREADY_INSTALLED";
            case ALREADY_DOWNLOADED -> "ALREADY_DOWNLOADED";
            case NO_NEWER_ARTIFACT -> "NO_UPDATE";
            case CHECKSUM_MISSING -> "CHECKSUM_MISSING";
            case NO_COMPATIBLE_RELEASE -> "NO_COMPATIBLE_RELEASE";
            default -> "PROVIDER_ERROR";
        };
        String prefix = noUpdate() ? "[INFO]" : kind == Kind.NO_COMPATIBLE_RELEASE || kind == Kind.CHECKSUM_MISSING || kind == Kind.CANCELLED ? "[WARNING]" : "[ERROR]";
        String source = providerName(provider);
        return prefix + " " + category + (source.isEmpty() ? "" : " | " + source) + ": " + safe(detail) + ". " + action()
                + " Help: " + Troubleshooting.failure(kind);
    }

    boolean noUpdate() { return kind == Kind.ALREADY_INSTALLED || kind == Kind.ALREADY_DOWNLOADED || kind == Kind.NO_NEWER_ARTIFACT; }

    private String action() {
        return switch (kind) {
            case NETWORK -> "Check the server connection and retry /pu check.";
            case RATE_LIMIT -> (retryAfterSeconds > 0 ? "Wait at least " + retryAfterSeconds + " seconds" : "Wait for the provider rate limit to reset") + " before retrying /pu check.";
            case PROVIDER_DENIED -> "Check that the source is public and accessible; inspect the source link before retrying.";
            case PROVIDER_ERROR -> retryAfterSeconds > 0 ? "Wait at least " + retryAfterSeconds + " seconds before retrying; check the provider release page if it persists."
                    : "Check the provider release page and source link; retry /pu check later if the provider is unavailable.";
            case INVALID_CONFIG -> "Correct the source/config entry in config.yml, then run /pu reload.";
            case INVALID_RESPONSE -> "Check the provider release page; retry /pu check later or report a repeated malformed response.";
            case CHECKSUM_MISSING -> "Use the release page for manual downloading, or review downloads.require-checksum before /pu reload.";
            case INVALID_ARTIFACT -> "Download was rejected. Check the publisher's release and checksum before trying again.";
            case NO_COMPATIBLE_RELEASE -> "Check the source link and supported Minecraft versions on the release page; update status is unknown.";
            case ALREADY_DOWNLOADED -> "Review the existing downloaded file for manual installation.";
            case ALREADY_INSTALLED -> "Keep the installed plugin. A different release label alone does not make this file an update.";
            case NO_NEWER_ARTIFACT -> "Keep the installed plugin. Review the selected source or release channel before downloading again.";
            case IO -> "Check disk space and server file permissions, then retry the operation.";
            case CANCELLED -> "Run /pu check after the reload or restart completes.";
            case UNEXPECTED -> "Review server diagnostics and report this failure if it repeats.";
        };
    }

    private static String providerName(String value) {
        if (value == null || value.isBlank()) return "";
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "web" -> "Web source";
            case "github" -> "GitHub";
            case "modrinth" -> "Modrinth";
            case "spigot", "spiget" -> "Spigot";
            default -> "Remote provider";
        };
    }

    private static String safe(String text) {
        if (text == null || text.isBlank()) return "Operation failed";
        String cleaned = text.replaceAll("(?i)\\b(?:[a-z][a-z0-9+.-]*://|www\\.)\\S+", "[redacted URL]")
                .replaceAll("[\\p{Cntrl}\\u00a7]", " ").replaceAll("\\s+", " ").trim();
        return cleaned.substring(0, Math.min(300, cleaned.length()));
    }
}
