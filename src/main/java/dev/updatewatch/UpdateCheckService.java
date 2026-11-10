package dev.updatewatch;

import java.util.*;
import org.bukkit.configuration.file.YamlConfiguration;

/** Runs on the I/O executor. No access to the live plugin, scheduler or command senders. */
final class UpdateCheckService {
    record Result(Remote.Source source, Remote.Release release, Versions.Status status, String error) {}
    record Count(int success, int failure) {}
    record Report(ConfigSources.Resolution resolution, Map<String, Result> results, Map<String, Count> counts, long durationMillis) {}
    static Report check(String yaml, List<Discovery.Installed> installed, String minecraft, boolean scan, Settings settings) throws Exception {
        long started = System.nanoTime();
        var config = new YamlConfiguration(); config.loadFromString(yaml);
        var transport = new HttpTransport(settings);
        Providers.JsonFetch fetch = Providers.memoized(url -> Remote.jsonValue(url, settings, transport));
        Discovery.Lookup lookup = hash -> {
            try {
                String project = fetch.get("https://api.modrinth.com/v2/version_file/" + hash + "?algorithm=sha512").getAsJsonObject().get("project_id").getAsString();
                if (!project.matches("[A-Za-z0-9_-]+")) throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Modrinth discovery returned an invalid project ID");
                return project;
            }
            catch (Remote.HttpError e) { if (e.code == 404) return null; throw e; }
            catch (RuntimeException e) { throw Failure.problem(Failure.Kind.INVALID_RESPONSE, "Modrinth discovery response is missing valid project metadata", e); }
        };
        var inventory = Discovery.inspect(settings.pluginsFolder()).excludingPlugin("PluginUpdateWatch");
        var resolution = ConfigSources.resolve(config, installed, inventory, minecraft, scan, lookup);
        Map<String, Result> checked = new LinkedHashMap<>(); Map<String, Count> counts = new LinkedHashMap<>();
        for (var source : resolution.sources()) {
            if (Thread.currentThread().isInterrupted()) throw new java.io.InterruptedIOException("Check cancelled");
            Result result = checkSource(source, fetch, url -> SourcePage.read(url, settings, transport), inventory.jars(), settings.debug());
            checked.put(source.name().toLowerCase(Locale.ROOT), result);
            Count previous = counts.getOrDefault(source.type(), new Count(0, 0));
            counts.put(source.type(), new Count(previous.success() + (result.error() == null ? 1 : 0), previous.failure() + (result.error() != null ? 1 : 0)));
        }
        return new Report(resolution, Collections.unmodifiableMap(checked), Map.copyOf(counts), (System.nanoTime() - started) / 1_000_000);
    }
    static Result checkSource(Remote.Source source, Providers.JsonFetch fetch) {
        return checkSource(source, fetch, url -> { throw Failure.problem(Failure.Kind.INVALID_CONFIG, "Web page fetcher is unavailable"); });
    }
    static Result checkSource(Remote.Source source, Providers.JsonFetch fetch, SourcePage.Fetch pages) {
        return checkSource(source, fetch, pages, List.of());
    }
    static Result checkSource(Remote.Source source, Providers.JsonFetch fetch, SourcePage.Fetch pages, List<Discovery.Jar> jars) {
        return checkSource(source, fetch, pages, jars, false);
    }
    private static Result checkSource(Remote.Source source, Providers.JsonFetch fetch, SourcePage.Fetch pages, List<Discovery.Jar> jars, boolean debug) {
        try {
            var release = source.type().equals("web") ? SourcePage.latest(source, pages, fetch) : Providers.latest(source, fetch);
            var status = SourcePage.UNKNOWN_VERSION.equals(release.version()) ? Versions.Status.UNKNOWN : Versions.compare(source.installed(), release.version());
            if (status != Versions.Status.CURRENT && InstalledArtifact.matches(source, release, jars)) status = Versions.Status.CURRENT;
            return new Result(source, release, status, null);
        } catch (Exception e) {
            if (debug) java.util.logging.Logger.getLogger("PluginUpdateWatch").log(java.util.logging.Level.INFO,
                    "[DEBUG] Provider check failed for " + source.name(), e);
            return new Result(source, null, null, Failure.classify(e).describe(source.type()));
        }
    }
    static String message(Exception e) { return Failure.classify(e).describe(null); }
}
