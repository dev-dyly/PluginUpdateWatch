package dev.updatewatch;

import com.google.gson.*;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

final class Providers {
    interface JsonFetch { JsonElement get(String url) throws IOException; }
    /** Scope to one scan; never reuse release metadata across checks or configuration reloads. */
    static JsonFetch memoized(JsonFetch fetch) {
        Map<String, JsonElement> values = new HashMap<>();
        Map<String, IOException> failures = new HashMap<>();
        return url -> {
            if (failures.containsKey(url)) throw failures.get(url);
            if (!values.containsKey(url)) {
                try { values.put(url, fetch.get(url)); }
                catch (IOException e) { failures.put(url, e); throw e; }
            }
            return values.get(url).deepCopy();
        };
    }
    static Remote.Release latest(Remote.Source s, JsonFetch fetch) throws IOException {
        try { validate(s); }
        catch (RuntimeException e) { throw Failure.problem(Failure.Kind.INVALID_CONFIG, "Invalid provider, source ID or asset pattern", e); }
        try {
            var release = switch (s.type().toLowerCase(Locale.ROOT)) {
                case "modrinth" -> Modrinth.select(fetch.get(Modrinth.url(s)).getAsJsonArray(), s);
                case "github" -> github(s, newestGithub(fetch.get("https://api.github.com/repos/" + s.id() + "/releases?per_page=100").getAsJsonArray()));
                case "spigot" -> spigot(s, fetch);
                default -> throw new IOException("Unknown provider");
            };
            if (release.version() == null || release.version().isBlank()) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Provider release has no version");
            if (release.download() != null) {
                try { HttpTransport.safeUri(release.download()); }
                catch (IOException e) { throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Provider returned an unsupported download URL; use the release page", e); }
            }
            return release;
        } catch (Remote.HttpError | Failure.Problem e) { throw e; }
        catch (IOException e) { throw Failure.problem(Failure.Kind.NETWORK, "Could not complete the provider request", e); }
        catch (RuntimeException e) { throw Failure.problem(Failure.Kind.INVALID_RESPONSE, s.type() + " response is missing valid release metadata", e); }
    }
    static void validate(Remote.Source s) {
        boolean valid = switch (s.type().toLowerCase(Locale.ROOT)) {
            case "github" -> s.id().matches("[A-Za-z0-9][A-Za-z0-9-]*/[A-Za-z0-9_.-]+") && !s.id().endsWith("/.") && !s.id().endsWith("/..");
            case "modrinth" -> s.id().matches("[A-Za-z0-9_-]+");
            case "spigot" -> s.id().matches("[1-9][0-9]*");
            default -> false;
        };
        if (!valid) throw new IllegalArgumentException("Invalid " + s.type() + " source ID");
        Pattern.compile(s.asset());
    }
    private static JsonObject newestGithub(JsonArray releases) throws IOException {
        JsonObject selected = null;
        java.time.Instant newest = java.time.Instant.MIN;
        for (JsonElement element : releases) {
            JsonObject release = element.getAsJsonObject();
            if (release.get("draft").getAsBoolean()) continue;
            java.time.Instant published = java.time.Instant.parse(release.get("published_at").getAsString());
            if (published.isAfter(newest)) { newest = published; selected = release; }
        }
        if (selected == null) throw Failure.problem(Failure.Kind.NO_COMPATIBLE_RELEASE, "No published GitHub releases found");
        return selected;
    }
    private static Remote.Release github(Remote.Source s, JsonObject v) throws IOException {
        Pattern pattern = Pattern.compile(s.asset()); List<JsonObject> matches = new ArrayList<>();
        for (JsonElement e : v.getAsJsonArray("assets")) {
            JsonObject a = e.getAsJsonObject(); String name = a.get("name").getAsString();
            if (Discovery.validFilename(name) && pattern.matcher(name).matches()) matches.add(a);
        }
        String download = null, sha256 = null, filename = null;
        if (matches.size() == 1) {
            JsonObject a = matches.getFirst(); download = a.get("browser_download_url").getAsString();
            filename = a.get("name").getAsString();
            if (a.has("digest") && !a.get("digest").isJsonNull()) {
                String digest = a.get("digest").getAsString();
                if (!digest.matches("sha256:[a-fA-F0-9]{64}")) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "GitHub asset has an unsupported or malformed digest");
                sha256 = digest.substring(7);
            }
        }
        String version = v.get("tag_name").getAsString();
        if (version.isBlank()) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "GitHub release has an empty version");
        String tag = java.net.URLEncoder.encode(version, java.nio.charset.StandardCharsets.UTF_8).replace("+", "%20");
        ReleaseType labelType = ReleaseType.fromLabel(version);
        return new Remote.Release(version, download, "https://github.com/" + s.id() + "/releases/tag/" + tag, null, sha256, filename,
                v.get("prerelease").getAsBoolean() ? ReleaseType.PRERELEASE : labelType == ReleaseType.UNKNOWN ? ReleaseType.STABLE : labelType);
    }
    private static Remote.Release spigot(Remote.Source s, JsonFetch fetch) throws IOException {
        String base = "https://api.spiget.org/v2/resources/" + s.id();
        JsonObject info = fetch.get(base).getAsJsonObject(), version = fetch.get(base + "/versions/latest").getAsJsonObject();
        boolean blocked = (info.has("premium") && info.get("premium").getAsBoolean()) || (info.has("external") && info.get("external").getAsBoolean());
        String name = version.get("name").getAsString(); long id = version.get("id").getAsLong();
        if (name.isBlank() || id <= 0) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Spigot response has no valid release version");
        return new Remote.Release(name, blocked ? null : base + "/versions/" + id + "/download", "https://www.spigotmc.org/resources/" + s.id() + "/",
                null, null, null, ReleaseType.fromLabel(name));
    }
}
