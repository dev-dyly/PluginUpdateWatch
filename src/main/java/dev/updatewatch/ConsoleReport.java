package dev.updatewatch;

import java.util.*;

/** Short console lines; full provider explanations and clickable actions remain in the detailed report. */
final class ConsoleReport {
    static String error(String name, String error) {
        String details = error.split(" Help: ", 2)[0];
        int action = details.indexOf(". ");
        if (action >= 0) details = details.substring(0, action);
        String help = error.contains(" Help: ") ? error.substring(error.lastIndexOf(" Help: ") + 7) : Troubleshooting.forText(error);
        return (name == null ? "" : clean(name) + ": ") + clean(details) + ". Help: " + help;
    }
    static String note(String name, String note) {
        if (note.contains("Source not identified"))
            return "[WARNING] " + clean(name) + ": source link missing; add jar/source in config.yml. Help: " + Troubleshooting.link("missing-source");
        if (note.contains("[ERROR]")) return error(name, note);
        String detail = note.split(";", 2)[0].replaceFirst("^\\[(?:WARNING|INFO)\\]\\s*", "");
        return "[WARNING] " + clean(name) + ": " + clean(detail) + ". Help: " + Troubleshooting.forText(note);
    }
    static String result(UpdateCheckService.Result result, boolean checksumRequired) {
        if (result.error() != null) return error(result.source().name(), result.error());
        var release = result.release();
        List<String> warnings = new ArrayList<>();
        if (release.type().warning && release.type() != ReleaseType.UNKNOWN) warnings.add(release.type().label);
        if (release.compatibilityWarning() != null) warnings.add("Minecraft compatibility unlisted");
        boolean manual = release.download() == null || checksumRequired && !release.hasChecksum();
        if (manual) warnings.add("manual download" + (release.download() != null && !release.hasChecksum() ? " (no checksum)" : ""));
        else if (!release.hasChecksum()) warnings.add("checksum unverified");
        String line = "[UPDATE] " + clean(result.source().name()) + ": " + clean(result.source().installed()) + " -> " + clean(release.version());
        if (!warnings.isEmpty()) line += " [" + String.join("; ", warnings) + "]";
        line += manual ? ". Release: " + release.page() : ". Use /pu download " + clean(result.source().name());
        if (!release.hasChecksum() && release.download() != null) line += ". Help: " + Troubleshooting.link("checksums");
        else if (release.compatibilityWarning() != null) line += ". Help: " + Troubleshooting.link("compatibility-labels");
        else if (release.type().warning && release.type() != ReleaseType.UNKNOWN) line += ". Help: " + Troubleshooting.link("prereleases");
        return (warnings.isEmpty() ? "[INFO] " : "[WARNING] ") + line;
    }
    static String summary(long updates, long untracked, long failures, long uncertain) {
        List<String> counts = new ArrayList<>();
        if (updates > 0) counts.add(updates + " update(s)");
        if (untracked > 0) counts.add(untracked + " need source/configuration");
        if (failures > 0) counts.add(failures + " failed check(s)");
        if (uncertain > 0) counts.add(uncertain + " unconfirmed");
        return "Check complete: " + (counts.isEmpty() ? "no updates or configuration problems" : String.join(", ", counts))
                + ". Full details: /pu list all." + (uncertain > 0 ? " Help: " + Troubleshooting.link("unconfirmed-versions") : "");
    }
    static String downloadStart(UpdateCheckService.Result result) {
        List<String> warnings = new ArrayList<>();
        if (result.status() != Versions.Status.UPDATE) warnings.add("inspection only; newer version unconfirmed");
        if (result.release().type().warning) warnings.add(result.release().type().label);
        if (result.release().compatibilityWarning() != null) warnings.add("Minecraft compatibility unlisted");
        if (!result.release().hasChecksum()) warnings.add("checksum unverified; HTTPS/JAR validation applies");
        return (warnings.isEmpty() ? "[INFO] " : "[WARNING] ") + "Downloading " + clean(result.source().name()) + " " + clean(result.release().version())
                + (warnings.isEmpty() ? "..." : " [" + String.join("; ", warnings) + "]. Help: " + Troubleshooting.forText(String.join("; ", warnings)));
    }
    static String downloadSaved(UpdateCheckService.Result result, java.nio.file.Path path, String version, ReleaseType type) {
        boolean newer = Versions.compare(result.source().installed(), version) == Versions.Status.UPDATE;
        List<String> details = new ArrayList<>();
        details.add(result.release().hasChecksum() ? "checksum verified" : "checksum unverified");
        if (type.warning) details.add(type.label);
        if (!newer) details.add("inspection only; newer version unconfirmed");
        if (result.release().compatibilityWarning() != null) details.add("Minecraft compatibility unlisted");
        String help = !result.release().hasChecksum() ? "checksums" : result.release().compatibilityWarning() != null ? "compatibility-labels"
                : !newer ? "unconfirmed-versions" : type.warning ? "prereleases" : "installing-updates";
        return (type.warning || !newer || !result.release().hasChecksum() || result.release().compatibilityWarning() != null ? "[WARNING] " : "[INFO] ")
                + "Saved " + path + "; version " + clean(version) + "; " + String.join("; ", details)
                + ". Stop server, replace old JAR, restart. Help: " + Troubleshooting.link(help);
    }
    private static String clean(String text) { return text.replaceAll("[\\p{Cntrl}\\u00a7]", " ").replaceAll("\\s+", " ").trim(); }
}
