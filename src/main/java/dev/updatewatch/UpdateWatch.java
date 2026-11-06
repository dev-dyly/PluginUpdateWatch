package dev.updatewatch;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.*;
import org.bstats.bukkit.Metrics;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import java.util.*;
import dev.updatewatch.UpdateCheckService.Result;
import java.util.concurrent.*;

public final class UpdateWatch extends JavaPlugin implements Listener, TabCompleter {
    // Mutable state below is owned exclusively by the server thread.
    private Map<String, Result> results = Map.of();
    private final CheckState checkState = new CheckState();
    private UpdateCheckService.Report lastReport;
    private Settings settings;
    private boolean requireChecksum;
    private boolean allowUnverifiedWeb;
    private boolean metricsEnabled;
    private final MetricsController metrics = new MetricsController(() -> {
        var client = new Metrics(this, 34400);
        return client::shutdown;
    });
    private String loadedConfig;
    private String lastCheckFailure;
    private final Set<String> downloads = new HashSet<>();
    private ExecutorService worker;
    private BukkitTask timer;
    private Map<String, String> discoveryNotes = Map.of();
    private ConfigCleanup.Plan cleanupPreview;
    private String cleanupOwner;

    @Override public void onEnable() {
        saveDefaultConfig();
        var compatibility = Compatibility.classify(getServer().getMinecraftVersion());
        if (compatibility == Compatibility.Status.BELOW_MINIMUM) {
            getLogger().severe("Requires Paper 1.21.11 or newer."); getServer().getPluginManager().disablePlugin(this); return;
        }
        if (compatibility == Compatibility.Status.UNVERIFIED) getLogger().warning("This Minecraft version is outside the targeted 1.21.11–26.3 range; compatibility is unverified.");
        try { loadSettings(); } catch (Exception e) {
            getLogger().severe(Failure.classify(e).describe(null)); getServer().getPluginManager().disablePlugin(this); return;
        }
        worker = Executors.newSingleThreadExecutor(r -> { Thread t = new Thread(r, "PluginUpdateWatch-IO"); t.setDaemon(true); return t; });
        getServer().getPluginManager().registerEvents(this, this);
        Objects.requireNonNull(getCommand("pluginupdates")).setTabCompleter(this);
        schedule();
        configureMetrics();
    }
    @Override public void onDisable() {
        checkState.invalidate();
        if (worker != null) worker.shutdownNow();
        metrics.close();
    }
    private void configureMetrics() {
        try { metrics.configure(metricsEnabled); }
        catch (RuntimeException | LinkageError e) {
            getLogger().warning("bStats could not start; update checks remain available. " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    private void loadSettings() throws Exception {
        String contents = java.nio.file.Files.readString(getDataFolder().toPath().resolve("config.yml"));
        var parsed = ConfigManager.parse(contents);
        var loaded = Settings.parse(parsed, getDataFolder().toPath().toAbsolutePath().getParent());
        boolean checksumPolicy = Settings.requireChecksum(parsed);
        boolean webPolicy = Settings.allowUnverifiedWeb(parsed);
        boolean metricsPolicy = MetricsController.enabled(parsed);
        reloadConfig(); settings = loaded;
        requireChecksum = checksumPolicy;
        allowUnverifiedWeb = webPolicy;
        metricsEnabled = metricsPolicy;
        loadedConfig = contents;
    }
    private void schedule() {
        if (timer != null) timer.cancel();
        long minutes = Math.clamp(getConfig().getLong("check-interval-minutes", 360), 15, 10080);
        checkState.requestScan();
        timer = getServer().getScheduler().runTaskTimer(this, () -> {
            if (!checkState.running()) check(null, checkState.takePendingScan());
        }, 100, minutes * 1200);
    }
    private void sync(Runnable action) {
        if (isEnabled()) {
            try { getServer().getScheduler().runTask(this, action); } catch (org.bukkit.plugin.IllegalPluginAccessException ignored) { }
        }
    }
    private void tell(CommandSender sender, String message) {
        if (message.contains("[WARNING]") || message.contains("[ERROR]")) message = Troubleshooting.help(message);
        NamedTextColor color = message.contains("[ERROR]") ? NamedTextColor.RED
                : message.contains("[WARNING]") ? NamedTextColor.YELLOW : NamedTextColor.GRAY;
        sender.sendMessage(Component.text("[Updates] ", NamedTextColor.AQUA).append(Component.text(message, color)));
    }
    private void check(CommandSender sender, boolean scan) {
        if (!getServer().isPrimaryThread()) throw new IllegalStateException("Check state must be accessed on the server thread");
        if (checkState.running()) { if (sender != null) tell(sender, "A check is already running."); return; }
        var installed = Arrays.stream(getServer().getPluginManager().getPlugins()).filter(p -> p != this)
                .map(p -> new Discovery.Installed(p.getName(), p.getPluginMeta().getVersion(), p.getPluginMeta().getWebsite(), p.isEnabled())).toList();
        String snapshot = getConfig().saveToString();
        var configFile = getDataFolder().toPath().resolve("config.yml");
        String diskSnapshot;
        try { diskSnapshot = java.nio.file.Files.readString(configFile); }
        catch (Exception e) { tell(sender == null ? getServer().getConsoleSender() : sender, "Cannot read config.yml. " + Failure.classify(e).describe(null)); return; }
        if (!diskSnapshot.equals(loadedConfig)) {
            tell(sender == null ? getServer().getConsoleSender() : sender, "[WARNING] CONFIG_ERROR | Config changed on disk. Run /pu reload before checking or scanning."); return;
        }
        String minecraft = getServer().getMinecraftVersion();
        Settings checkSettings = settings;
        long epoch = checkState.begin();
        if (sender != null) tell(sender, scan ? "Scanning installed JARs and checking update sources..." : "Checking configured plugins...");
        worker.execute(() -> {
            UpdateCheckService.Report report;
            try {
                // Parse the file itself so invalid YAML can never be replaced by default values.
                report = UpdateCheckService.check(diskSnapshot, installed, minecraft, scan, checkSettings);
            } catch (Exception e) {
                sync(() -> {
                    if (checkState.finish(epoch)) {
                        lastCheckFailure = "Scan/check failed: " + UpdateCheckService.message(e);
                        CommandSender recipient = sender == null ? getServer().getConsoleSender() : sender;
                        tell(recipient, console(recipient) ? ConsoleReport.error(null, lastCheckFailure) : lastCheckFailure);
                        if (!console(recipient)) getLogger().warning(lastCheckFailure);
                    }
                    resumePendingScan();
                });
                return;
            }
            var resolution = report.resolution();
            var checked = report.results();
            sync(() -> {
                if (!checkState.finish(epoch)) { resumePendingScan(); return; }
                try {
                    if (!snapshot.equals(getConfig().saveToString()) || !diskSnapshot.equals(java.nio.file.Files.readString(configFile))) {
                        lastCheckFailure = "[WARNING] CONFIG_ERROR | Config changed during scan; results discarded. Run /pu reload.";
                        tell(sender == null ? getServer().getConsoleSender() : sender, lastCheckFailure); return;
                    }
                    if (scan) {
                        var updated = ConfigManager.parse(diskSnapshot);
                        updated.set("updates", resolution.entries());
                        String formatted = ConfigManager.serialize(updated);
                        if (!formatted.equals(diskSnapshot)) {
                            ConfigManager.save(configFile, diskSnapshot, formatted);
                            loadedConfig = formatted;
                            reloadConfig();
                        }
                    }
                } catch (Exception e) {
                    lastCheckFailure = "Could not save discovery config. " + Failure.classify(e).describe(null);
                    tell(sender == null ? getServer().getConsoleSender() : sender, lastCheckFailure); return;
                }
                boolean changed = !checked.equals(results);
                changed |= !discoveryNotes.equals(resolution.notes());
                discoveryNotes = resolution.notes();
                results = checked; lastReport = report;
                lastCheckFailure = null;
                if (checkSettings.debug()) getLogger().info("Check completed in " + report.durationMillis() + " ms; " + report.counts() + "; updates=" + updateCount());
                if (sender != null) show(sender);
                if (changed) {
                    if (sender != getServer().getConsoleSender()) show(getServer().getConsoleSender());
                    if (hasUpdates()) for (var player : getServer().getOnlinePlayers())
                        if (player.hasPermission("pluginupdatewatch.admin") && player != sender) tell(player, "Newer plugin versions are available. Use /pu list.");
                }
                resumePendingScan();
            });
        });
    }
    private void resumePendingScan() {
        if (checkState.takePendingScan()) check(null, true);
    }
    private long updateCount() { return results.values().stream().filter(r -> r.error() == null && r.status() == Versions.Status.UPDATE).count(); }
    private boolean hasUpdates() { return results.values().stream().anyMatch(ReportVisibility::confirmedUpdate); }
    @EventHandler public void onJoin(PlayerJoinEvent event) {
        if (getConfig().getBoolean("notify-on-join", true) && event.getPlayer().hasPermission("pluginupdatewatch.admin") && hasUpdates())
            tell(event.getPlayer(), "Newer plugin versions are available. Use /pu list.");
    }
    private boolean checksumRequired(Result result) {
        return Settings.checksumRequired(result.source(), requireChecksum, allowUnverifiedWeb);
    }
    private void show(CommandSender sender) {
        show(sender, false);
    }
    private static boolean console(CommandSender sender) {
        return sender instanceof ConsoleCommandSender || sender instanceof RemoteConsoleCommandSender;
    }
    private void show(CommandSender sender, boolean all) {
        boolean console = console(sender) && !all;
        if (lastCheckFailure != null) {
            tell(sender, console ? ConsoleReport.error(null, lastCheckFailure) : lastCheckFailure);
            if (!results.isEmpty()) tell(sender, "[WARNING] The results below are cached from an earlier check, not a fresh update status.");
        }
        if (results.isEmpty() && discoveryNotes.isEmpty()) tell(sender, "No results yet. Use /pu scan to detect installed plugins.");
        discoveryNotes.forEach((name, note) -> { if (ReportVisibility.note(console, note)) tell(sender, console ? ConsoleReport.note(name, note)
                : (note.contains("[ERROR]") || note.startsWith("[INFO]") ? "" : "[WARNING] ") + name + ": " + note); });
        for (Result r : results.values()) {
            if (!ReportVisibility.result(console, r)) continue;
            if (console) { tell(sender, ConsoleReport.result(r, checksumRequired(r))); continue; }
            if (r.error() != null) { tell(sender, r.source().name() + ": check failed - " + r.error()); continue; }
            String label = switch (r.status()) {
                case UNKNOWN -> "[WARNING] UNKNOWN | source offers a file; no newer version or Minecraft compatibility confirmed";
                case CURRENT -> "[INFO] CURRENT | up to date (or installed version is newer)";
                case UPDATE -> "[INFO] NOT_CURRENT | update available";
                case DIFFERENT -> "[WARNING] UNKNOWN | different version label; no newer version confirmed";
            };
            NamedTextColor color = r.status() == Versions.Status.CURRENT ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
            Component line = Component.text(r.source().name() + ": " + r.source().installed() + " -> " + r.release().version() + " - " + label, color);
            if (r.release().compatibilityWarning() != null)
                line = line.append(Component.text(" | " + r.release().compatibilityWarning(), NamedTextColor.YELLOW));
            if (r.status() != Versions.Status.CURRENT) {
                line = line.append(Component.text(" | " + r.release().type().message(), r.release().type().warning ? NamedTextColor.YELLOW : NamedTextColor.GRAY));
                line = line.append(Component.text(" [Release page]", NamedTextColor.AQUA).clickEvent(ClickEvent.openUrl(r.release().page())));
                if (r.release().download() != null && (!checksumRequired(r) || r.release().hasChecksum())) line = line.append(Component.text(
                        r.status() == Versions.Status.UPDATE ? " [Download update]" : " [Download for inspection]", NamedTextColor.GREEN)
                        .clickEvent(ClickEvent.suggestCommand("/pu download " + r.source().name())));
                else line = line.append(Component.text(" (manual download required)", NamedTextColor.YELLOW));
                if (r.release().download() != null && !r.release().hasChecksum()) line = line.append(Component.text(checksumRequired(r)
                        ? " [WARNING] No checksum; automatic download blocked" : " [WARNING] Checksum unavailable; explicitly allowed without integrity verification", NamedTextColor.YELLOW));
            }
            sender.sendMessage(line);
        }
        long disabled = discoveryNotes.values().stream().filter(n -> n.equals("[INFO] Disabled in existing configuration")).count();
        long untracked = Arrays.stream(getServer().getPluginManager().getPlugins()).filter(p -> p != this && !results.containsKey(p.getName().toLowerCase(Locale.ROOT))
                && !"[INFO] Disabled in existing configuration".equals(discoveryNotes.get(p.getName()))).count();
        if (console) tell(sender, ConsoleReport.summary(updateCount(), untracked,
                results.values().stream().filter(r -> r.error() != null).count(),
                results.values().stream().filter(r -> r.error() == null && (r.status() == Versions.Status.UNKNOWN || r.status() == Versions.Status.DIFFERENT)).count()));
        else tell(sender, untracked + " plugin(s) not checked; " + disabled + " intentionally disabled. See messages above; manual entries need only jar and source link.");
    }
    private void cleanup(CommandSender sender, boolean confirm) {
        if (checkState.running()) { tell(sender, "A check is running; retry cleanup when it finishes."); return; }
        var configFile = getDataFolder().toPath().resolve("config.yml");
        try {
            String disk = java.nio.file.Files.readString(configFile);
            if (!disk.equals(loadedConfig)) { tell(sender, "Config changed on disk. Run /pu reload before cleanup."); return; }
            if (!confirm) {
                cleanupPreview = ConfigCleanup.plan(disk, settings.pluginsFolder()); cleanupOwner = sender.getName();
                if (cleanupPreview.missing().isEmpty()) { tell(sender, "No config entries reference missing JAR files."); return; }
                tell(sender, "Cleanup preview: " + cleanupPreview.missing().size() + " config entry/entries reference missing files:");
                cleanupPreview.missing().forEach(name -> tell(sender, " - " + name));
                tell(sender, "If a plugin was renamed, update its jar entry to preserve its source first. Otherwise run /pu cleanup confirm. A config backup will be saved; no plugin files or data folders are deleted.");
                return;
            }
            if (cleanupPreview == null || !sender.getName().equals(cleanupOwner) || cleanupPreview.missing().isEmpty()) {
                tell(sender, "Run /pu cleanup to preview missing-file entries first."); return;
            }
            int count = cleanupPreview.missing().size();
            var backup = ConfigCleanup.apply(configFile, settings.pluginsFolder(), cleanupPreview);
            loadSettings(); checkState.invalidate(); results = Map.of(); discoveryNotes = Map.of(); lastReport = null; lastCheckFailure = null;
            cleanupPreview = null; cleanupOwner = null; schedule();
            tell(sender, "Removed " + count + " stale config entry/entries. Backup: " + backup + ". A fresh scan will run shortly.");
        } catch (Exception e) { cleanupPreview = null; cleanupOwner = null; tell(sender, "Cleanup stopped: " + UpdateCheckService.message(e)); }
    }
    /** Each bulk request reports a single summary after its last queued download finishes. */
    private final class BulkProgress {
        private final CommandSender sender;
        private final int total;
        private int completed;
        private int saved;
        private int unchanged;
        private int failed;

        BulkProgress(CommandSender sender, int total) {
            this.sender = sender;
            this.total = total;
        }

        void completed(Failure failure) {
            completed++;
            if (failure == null) saved++;
            else if (failure.noUpdate()) unchanged++;
            else failed++;
            if (completed == total)
                tell(sender, "downloadall complete: " + saved + " saved, " + unchanged
                        + " already installed/not newer, " + failed + " failed. Review plugins/PluginUpdateWatch/downloads/ before installing.");
        }
    }

    private void downloadAll(CommandSender sender) {
        if (checkState.running()) {
            tell(sender, "An update check is running. Retry /pu downloadall when it finishes.");
            return;
        }
        if (lastCheckFailure != null) {
            tell(sender, "[WARNING] The last check failed. Run /pu check successfully before /pu downloadall.");
            return;
        }
        if (lastReport == null) {
            tell(sender, "No completed update check yet. Run /pu scan or /pu check first.");
            return;
        }
        var plan = BulkDownloadPlan.plan(results.values(), this::checksumRequired, downloads);
        if (plan.confirmedUpdates() == 0) {
            tell(sender, "No confirmed updates in the last completed check. Run /pu check to refresh.");
            return;
        }
        if (plan.manual() > 0)
            tell(sender, "[WARNING] " + plan.manual() + " update(s) require a manual download from their release pages. Use /pu list.");
        if (plan.checksumBlocked() > 0)
            tell(sender, "[WARNING] " + plan.checksumBlocked() + " update(s) skipped: checksum required but unavailable. Use /pu list for release pages and review downloads.require-checksum.");
        if (plan.alreadyRunning() > 0)
            tell(sender, plan.alreadyRunning() + " download(s) already running; not queued again.");
        if (plan.ready().isEmpty()) {
            tell(sender, "No eligible downloads to queue from " + plan.confirmedUpdates() + " confirmed update(s).");
            return;
        }
        tell(sender, "Queueing " + plan.ready().size() + " of " + plan.confirmedUpdates()
                + " confirmed update(s) from the last check. Downloads are saved for manual installation, not applied automatically.");
        var progress = new BulkProgress(sender, plan.ready().size());
        for (Result r : plan.ready()) download(sender, r.source().name(), progress);
    }

    private void download(CommandSender sender, String name) { download(sender, name, null); }

    private void download(CommandSender sender, String name, BulkProgress progress) {
        boolean console = console(sender);
        String key = name.toLowerCase(Locale.ROOT);
        Result r = results.get(key);
        if (r == null || r.error() != null || r.status() == Versions.Status.CURRENT || r.release().download() == null) {
            tell(sender, "No downloadable update cached for that plugin. Use /pu check or its release page."); return;
        }
        if (checksumRequired(r) && !r.release().hasChecksum()) {
            String message = "[ERROR] PROVIDER_ERROR | " + r.source().type() + " supplies no checksum for " + r.source().name()
                    + ". Download blocked. Use its release page, or explicitly set downloads.require-checksum: false and /pu reload to allow an unverified download.";
            if (console) message = "[ERROR] " + r.source().name() + ": checksum missing; download blocked. Release: " + r.release().page()
                    + ". Help: " + Troubleshooting.link("checksums");
            tell(sender, message); if (!console) getLogger().warning(message); return;
        }
        if (!downloads.add(key)) {
            tell(sender, "That download is already running.");
            if (progress != null) progress.completed(Failure.classify(new IllegalStateException("Download already queued")));
            return;
        }
        if (console && progress == null) tell(sender, ConsoleReport.downloadStart(r));
        if (!console) {
            if (r.status() != Versions.Status.UPDATE)
                tell(sender, "[WARNING] No newer version has been confirmed. This explicitly requested download is for inspection, not a verified upgrade.");
            if (progress == null) tell(sender, "Downloading " + r.source().name() + " " + r.release().version() + "...");
            if (progress == null || r.release().type().warning)
                tell(sender, r.source().name() + ": " + r.release().type().message());
            if (r.release().compatibilityWarning() != null) {
                tell(sender, r.release().compatibilityWarning());
                getLogger().warning(r.source().name() + ": " + r.release().compatibilityWarning());
            }
            if (r.release().type().warning) getLogger().warning(r.source().name() + ": " + r.release().type().message());
        }
        Settings downloadSettings = settings;
        boolean checksumRequired = checksumRequired(r);
        var downloadFolder = getDataFolder().toPath().resolve("downloads");
        if (!console && !r.release().hasChecksum()) {
            String warning = "[WARNING] " + r.source().name() + ": checksum unavailable; explicitly allowed by config. HTTPS and JAR structure will be checked, but integrity is unverified.";
            tell(sender, warning); getLogger().warning(warning);
        }
        worker.execute(() -> {
            String message;
            Failure outcome = null;
            try {
                var path = Remote.download(r.source(), r.release(), downloadFolder, downloadSettings, checksumRequired);
                var descriptor = Discovery.read(path);
                var inferredType = ReleaseType.fromLabel(descriptor.version());
                var actualType = inferredType == ReleaseType.UNKNOWN ? r.release().type() : inferredType;
                String freshness = Versions.compare(r.source().installed(), descriptor.version()) == Versions.Status.UPDATE
                        ? "[INFO] JAR version is newer than the installed version. "
                        : "[WARNING] JAR version does not establish a newer release; this file is for inspection only. ";
                message = (r.release().hasChecksum() ? "[INFO] Checksum verified. " : "[WARNING] Checksum unverified (explicitly allowed). ")
                        + "Saved " + path + ". Downloaded version: " + descriptor.version() + ". " + freshness + actualType.message()
                        + (r.release().compatibilityWarning() == null ? "" : " " + r.release().compatibilityWarning())
                        + " Stop the server, replace the old plugin JAR, then restart. Check the release's Minecraft compatibility first.";
                if (console) message = ConsoleReport.downloadSaved(r, path, descriptor.version(), actualType);
            } catch (Exception e) {
                var failure = Failure.classify(e);
                outcome = failure;
                message = (console ? ConsoleReport.error(r.source().name(), failure.describe(r.source().type())) : failure.describe(r.source().type())) + (failure.noUpdate() || console ? ""
                        : " Retry downloads with /pu download " + r.source().name() + " after resolving the problem.");
            }
            String finalMessage = message;
            Failure finalOutcome = outcome;
            sync(() -> {
                if (finalOutcome != null && finalOutcome.kind() == Failure.Kind.ALREADY_INSTALLED && results.get(key) == r) {
                    var updated = new LinkedHashMap<>(results);
                    updated.put(key, ReportVisibility.afterDownload(r, finalOutcome));
                    results = Collections.unmodifiableMap(updated);
                }
                downloads.remove(key); tell(sender, finalMessage);
                if (progress != null) progress.completed(finalOutcome);
                if (!console) {
                    if (finalMessage.contains("[ERROR]") || finalMessage.contains("[WARNING]")) getLogger().warning(finalMessage);
                    else getLogger().info(finalMessage);
                }
            });
        });
    }
    @Override public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("pluginupdatewatch.admin")) { tell(sender, "You do not have permission."); return true; }
        String sub = args.length == 0 ? "list" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "list" -> {
                if (args.length == 1 || args.length == 0) show(sender);
                else if (args.length == 2 && args[1].equalsIgnoreCase("all")) show(sender, true);
                else tell(sender, "Usage: /pu list [all]");
            }
            case "check" -> check(sender, false);
            case "scan" -> check(sender, true);
            case "download" -> { if (args.length == 2) download(sender, args[1]); else tell(sender, "Usage: /pu download <plugin>"); }
            case "downloadall" -> { if (args.length == 1) downloadAll(sender); else tell(sender, "Usage: /pu downloadall"); }
            case "cleanup" -> {
                if (args.length == 1 || (args.length == 2 && args[1].equalsIgnoreCase("confirm"))) cleanup(sender, args.length == 2);
                else tell(sender, "Usage: /pu cleanup [confirm]");
            }
            case "stats" -> {
                if (lastReport == null) tell(sender, "No completed check yet.");
                else tell(sender, "Last check: " + lastReport.durationMillis() + " ms; updates: " + updateCount() + "; provider success/failure: " + lastReport.counts());
            }
            case "reload" -> {
                try {
                    loadSettings(); checkState.invalidate(); results = Map.of(); discoveryNotes = Map.of(); lastReport = null; lastCheckFailure = null;
                    cleanupPreview = null; cleanupOwner = null;
                    configureMetrics(); schedule(); tell(sender, "Configuration reloaded. Automatic scan will run shortly.");
                } catch (Exception e) { tell(sender, "Reload rejected; previous settings retained. " + Failure.classify(e).describe(null)); }
            }
            default -> tell(sender, "Usage: /pu [list|scan|check|download <plugin>|downloadall|cleanup [confirm]|stats|reload]");
        }
        return true;
    }
    @Override public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("pluginupdatewatch.admin")) return List.of();
        List<String> choices = args.length == 1 ? List.of("list", "scan", "check", "download", "downloadall", "cleanup", "stats", "reload")
                : args.length == 2 && args[0].equalsIgnoreCase("cleanup") ? List.of("confirm")
                : args.length == 2 && args[0].equalsIgnoreCase("list") ? List.of("all")
                : args.length == 2 && args[0].equalsIgnoreCase("download") ? results.values().stream()
                .filter(r -> r.error() == null && r.status() != Versions.Status.CURRENT && r.release().download() != null && (!checksumRequired(r) || r.release().hasChecksum())).map(r -> r.source().name()).toList() : List.of();
        String prefix = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        return choices.stream().filter(s -> s.toLowerCase(Locale.ROOT).startsWith(prefix)).toList();
    }
}
