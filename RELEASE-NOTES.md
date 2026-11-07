# PluginUpdateWatch 1.5.0

**Stable release — Bulk Update Downloads**

The new `/pu downloadall` command downloads all **confirmed, eligible plugin updates** from the most recent successful scan or check. The server owner verified the feature on a live Paper server: five updates were detected, ViaVersion downloaded and passed checksum validation, while two manual-only releases and two checksum-blocked releases were correctly skipped.

- **Download all:** Run `/pu check` (or `/pu scan`) followed by `/pu downloadall`. Downloads are queued sequentially without blocking the main server thread.
- **Safety preserved:** Confirmed newer releases only. Files without direct download links, files blocked by the configured checksum policy, and downloads already in progress are skipped. Unknown/unconfirmed versions and provider errors are not silently downloaded.
- **Actionable results:** The command reports the number queued, reasons for skipped updates, each download's result, and a final count of saved, already-installed/not-newer, and failed downloads.
- **Existing integrity checks:** Downloads still enforce publisher checksums when required, HTTPS and filename restrictions, archive/plugin identity checks, and protection against non-newer replacement files.
- **No automatic installation:** Downloaded JARs are saved to `plugins/PluginUpdateWatch/downloads/`. Stop the server and replace installed JARs manually after reviewing plugin and Minecraft-version compatibility.
- **Compatibility:** Existing `config.yml` files and single-plugin `/pu download <plugin>` behaviour are preserved. No configuration migration is required.
- **Build:** Targets Paper 1.21.11–26.3, Java 21 bytecode. Release CI verifies the packaged JAR on Java 21 and 25 before publishing.

**Install:** Download `PluginUpdateWatch-1.5.0.jar` from Assets. Stop the server, remove the previous PluginUpdateWatch JAR, add the new one, keep your existing `plugins/PluginUpdateWatch/` folder, then restart.

---

# PluginUpdateWatch 1.4.1

Download PluginUpdateWatch-1.4.1.jar from Assets. Stop the server, replace the old PluginUpdateWatch JAR, keep the configuration folder, then restart. No config migration is required. The tested preview passed the server owner's live Paper 26.3 test; stable verification passed 166 checks on Java 21 and Java 25.

- compact console/RCON reports show confirmed updates and actionable problems; `/pu list all` exposes current/unconfirmed rows without falsely classifying uncertain versions as current.
- consolidate download warnings, link specific troubleshooting topics, and mark an identical-file download CURRENT in its cached report until the next check.

- Compare provider checksums against original installed JARs before advertising an update. Identical files are CURRENT even when release labels differ, fixing the reported BedrockEssentials beta.1/beta.2 false alert.
- Read Geyser/Floodgate build metadata and SHA-256 checksums for official latest Spigot links. Pin downloads to the checked build, suppress identical files and avoid offering older core versions over installed development builds.
- Report duplicate/non-newer downloads as informational outcomes with no replacement saved, rather than provider errors with retry instructions.
- Accept Spigot resource titles with encoded Unicode emoji/punctuation while still rejecting encoded ASCII path separators, traversal and invalid UTF-8.
- Resolve the official Geyser/Floodgate project download pages to their Spigot download endpoints instead of unrelated GitHub footer links.
- Compare explicit numeric snapshot/beta/alpha/RC counters within a channel and numeric core changes for recognized development versions. Unnumbered/custom build labels remain uncertain. Warn for GitHub development tags even when the publisher does not mark them prereleases.
- Enabled plugins are checked against the newest listed Modrinth Paper/Spigot/Bukkit release even without a matching Minecraft-version label. Equal/newer installed versions remain CURRENT; newer releases carry a compatibility warning when needed. Disabled plugins retain strict game-version filtering. Failed checks remain errors.
- Scans and cleanup saves separate plugin config entries with blank lines. Run `/pu scan` to format an existing config; settings are preserved.
- Preserve the existing order of fields within config entries across repeated scans.
- Restore dependency relocation to fix bStats startup in the packaged JAR. Add a final-artifact regression test.
- No configuration migration is required. Enabled status proves startup only, not full functionality or compatibility of a new release. Download validation is unchanged.

---

# PluginUpdateWatch 1.4.0

Download **PluginUpdateWatch-1.4.0.jar** from Assets. Stop the server, replace the older PluginUpdateWatch JAR, keep the configuration folder, then restart.

- Finalize the 1.4 preview as a stable release with explicit HTTPS web sources, quiet console output and prerelease selection support.
- Preserve config compatibility for existing `jar/source` entries, legacy `plugins:` entries and older checksum settings while keeping stricter safety checks for modern downloads.
- Keep Paper 1.21.11–26.3 compatibility and Java 21/25 verification as part of the standard release validation.
- No migration is required from 1.3.x, but administrators should review the config if they rely on older defaults or explicit web-source settings.

Validation: Maven `clean verify` passed on Java 21 and Java 25, and the exact verified release artifact is published with checksum verification and generated release notes.

---

# PluginUpdateWatch 1.3.3

Download **PluginUpdateWatch-1.3.3.jar** from Assets. Stop the server, replace the older PluginUpdateWatch JAR, keep the configuration folder, then restart.

- Add the standard bStats client for PluginUpdateWatch (ID 34400), bundled and relocated to prevent dependency conflicts.
- Usage reporting defaults to enabled, including for older configurations. Set `metrics.enabled: false` in PluginUpdateWatch's config and run `/pu reload` to opt out. The shared bStats global opt-o[...]
- Report only standard bStats data; no installed-plugin list, source links, update results or custom charts are submitted.
- Validate `metrics.enabled` as a YAML boolean. Invalid values reject configuration loading rather than silently enabling reporting; use unquoted `true` or `false`.
- Avoid duplicate metrics clients on reload and stop the reporting client when disabled. Metrics initialization failures are logged without disabling update checks.
- Document the public bStats dashboard/API, reported data, opt-out controls and reporting delay. Active reporting servers are not lifetime installations.

[Usage dashboard](https://bstats.org/plugin/bukkit/PluginUpdateWatch/34400) · [Metrics and opt-out documentation](https://dylantanderson92-lang.github.io/PluginUpdateWatch/metrics/)

The first dashboard data point can take 3–36 minutes after server startup. `/pu stats` continues to show local update-check diagnostics. Existing configurations remain compatible; no migration i[...]

Validation: **95 unit tests**, with exact published-JAR live checks on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. See [BUILD-REPORT.md](https://github.com/dylantanderson92-lang/PluginUpdateWatch/blob/main/BUILD-REPORT.md)

---
