package dev.updatewatch;

/** Stable wiki anchors shared by console messages and detailed error output. */
final class Troubleshooting {
    static final String PAGE = "https://dev-dyly.github.io/PluginUpdateWatch/troubleshooting/";
    static String link(String topic) { return PAGE + "#" + topic; }
    static String failure(Failure.Kind kind) {
        return link(switch (kind) {
            case NETWORK, CANCELLED -> "network-errors";
            case RATE_LIMIT -> "rate-limits";
            case INVALID_CONFIG -> "config-errors";
            case CHECKSUM_MISSING -> "checksums";
            case INVALID_ARTIFACT -> "invalid-downloads";
            case ALREADY_DOWNLOADED, ALREADY_INSTALLED, NO_NEWER_ARTIFACT -> "already-installed";
            case IO -> "file-errors";
            case NO_COMPATIBLE_RELEASE -> "compatibility-labels";
            default -> "provider-errors";
        });
    }
    static String forText(String text) {
        String value = text.toLowerCase(java.util.Locale.ROOT);
        if (value.contains("already_installed") || value.contains("no_update")) return link("already-installed");
        if (value.contains("network_error") || value.contains("cancelled")) return link("network-errors");
        if (value.contains("rate limit")) return link("rate-limits");
        if (value.contains("checksum")) return link("checksums");
        if (value.contains("source not identified") || value.contains("source link missing")) return link("missing-source");
        if (value.contains("config_error") || value.contains("invalid config") || value.contains("invalid source")) return link("config-errors");
        if (value.contains("jar") || value.contains("duplicate") || value.contains("metadata")) return link("jar-discovery");
        if (value.contains("does not list minecraft") || value.contains("compatible release")) return link("compatibility-labels");
        if (value.contains("release type") || value.contains("prerelease")) return link("prereleases");
        if (value.contains("provider_error")) return link("provider-errors");
        if (value.contains("file_error")) return link("file-errors");
        return link("unconfirmed-versions");
    }
    static String help(String text) { return text.contains("Help: ") ? text : text + " Help: " + forText(text); }
}
