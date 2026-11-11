# Changelog

## 1.5.1 Stable — Download reliability and logging fixes

[Release notes](https://github.com/dev-dyly/PluginUpdateWatch/blob/main/RELEASE-NOTES.md) · [Release assets, once published](https://github.com/dev-dyly/PluginUpdateWatch/releases/tag/v1.5.1)

- Account for bulk queue completion, rejection and cancellation exactly once; continue remaining jobs after a failed transfer.
- Reject stale download configuration after edits, failed checks and reloads; validate each queued snapshot before connecting.
- Avoid duplicate saved artifacts and protect output files belonging to other plugins.
- Recover reserved partial-transfer files on startup while retaining saved JARs and unrelated files.
- Consolidate warnings, report changed scan issues and recovery, and avoid unrelated repeat administrator notifications.
- Preserve operational error severity, missing-checksum policy, cancellation classification and optional debug diagnostics.
- Identify the actual HTTP client version and reuse identical JSON requests only within one check.
- Preserve commands, permissions, configuration, validation, Java 21 bytecode and manual installation.
- Release publishing verifies Java 21/25 tests and final JAR metadata, uses the exact verified artifact and generates valid SpigotMC BBCode.

PR #15 was live-tested on isolated Paper 26.3 build 246 / Java 25. The v1.5.1 version bump is separately checked by automated builds; earlier live tests do not certify byte-for-byte testing of this new versioned artifact. See the [live report](https://github.com/dev-dyly/PluginUpdateWatch/blob/main/LIVE-TEST-REPORT.md) for limitations.


## 1.5.0 — Bulk update downloads

[Download v1.5.0](https://github.com/dev-dyly/PluginUpdateWatch/releases/tag/v1.5.0) · [DownloadAll guide](downloads.md#download-all-confirmed-updates)

- Add `/pu downloadall` to queue all confirmed, eligible plugin updates from the last successful scan or check.
- Reuse single-plugin safeguards: HTTPS, checksum policy, archive and plugin identity validation, filename restrictions, and freshness checks.
- Skip manual-only releases, checksum-blocked items, already-running downloads and unconfirmed update labels.
- Report each download and a final saved, already-installed/not-newer and failed summary; skipped items are reported separately.
- Save files to `plugins/PluginUpdateWatch/downloads/`; **no automatic installation**.
- Existing configurations remain supported without migration; targets Paper 1.21.11–26.3 with Java 21 bytecode.
- **Validation:** Java 21 and Java 25 CI passed. Live Paper test: five updates found, one checksum-verified ViaVersion JAR saved, two manual-only and two checksum-blocked skipped; zero failures.


## 1.4.1

[Release and downloads](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/tag/v1.4.1). Existing configurations remain supported. The server owner confirmed the tested preview works on live Paper 26.3.

- compact console/RCON reports show confirmed updates and actionable problems; `/pu list all` exposes current/unconfirmed rows without falsely classifying uncertain versions as current.
- consolidate download warnings, link specific troubleshooting topics, and mark an identical-file download CURRENT in its cached report until the next check.



- Suppress false updates when a provider checksum matches the original installed JAR, even if the release label differs.
- Use official Geyser/Floodgate build metadata, pinned build downloads and SHA-256 checksums for latest Spigot endpoints.
- Report identical or non-newer downloads as informational outcomes instead of provider errors.
- Accept encoded Unicode in Spigot resource titles and resolve Geyser/Floodgate project download pages to the Spigot endpoints.
- Compare numbered snapshot/prerelease versions conservatively and warn for GitHub development tags lacking a prerelease flag.
- Check enabled plugins against the newest listed Modrinth Paper/Spigot/Bukkit release when Minecraft-version labels are missing. Keep CURRENT reports quiet and warn before downloading an unlisted-compatibility release.
- Keep strict game-version filtering for disabled plugins and preserve errors for failed checks.
- Add blank lines between config entries during scans and cleanup, including existing unchanged entries.
- Preserve config entry field order across repeated scans.
- Restore bStats, Gson and jsoup relocation and test the final shaded JAR, including bStats' own relocation check.

## 1.4.0

[Release and downloads](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/tag/v1.4.0). Existing 1.3.x configurations remain supported; keep the configuration folder when upgrading.

- Fix false alerts for Plan numbered-build labels, LuckPerms' Bukkit suffix and DoubleDoors' Paper prefix.
- Notify only for confirmed newer versions; label ambiguous downloads as inspection only.
- Reject identical installed JARs and stale descriptor versions behind advertised updates.

- Suppress current and intentionally disabled plugin rows in console/RCON reports; keep full in-game listings.
- Add explicit HTTPS wiki/project pages, direct JAR URLs and extensionless download endpoints.
- Allow missing checksums for web sources with warnings and a separate strict-policy override.
- Select newest compatible Modrinth alpha/beta/release and newest published GitHub prerelease/release.
- Display release-type warnings before download and descriptor versions afterward.
- Add bounded HTML parsing, redirect/security and console filtering regression tests.

## 1.3.3

- Add optional bStats usage metrics (plugin ID 34400), with a local `metrics.enabled` opt-out and support for the shared bStats opt-out.
- Stop the metrics client on disable and avoid duplicate clients on reload.
- Document public usage reports, reporting delays and the difference between active servers and lifetime installations.


## 1.3.2


Highlights:

- Preserve publisher JAR filenames for GitHub and Modrinth downloads.
- Use `<PluginName>.jar` when a provider supplies no filename.
- Add `/pu cleanup` and `/pu cleanup confirm`.
- Back up the config before confirmed cleanup.
- Increase the plugin descriptor limit from 64 KiB to 1 MiB.
- Show deliberately disabled plugins as informational entries.
- Avoid duplicate console reporting for each check.

Validation documented for this release includes **89 Maven tests** plus live verification on the project's two documented Paper test environments.

## 1.3.1

Highlights:

- Added protection against JAR filenames differing only by letter case.
- Added discovery regression tests.
- Retained checksum enforcement and existing configuration compatibility.

## 1.3.0

Highlights:

- Added bounded network retries.
- Required provider checksums by default.
- Added configurable network and download limits.
- Improved discovery ambiguity handling.
- Improved error classification and reporting.
- Added archive validation hardening.
- Added Java 21 / 25 CI coverage.

## 1.2.0

Highlights:

- Added automatic installed-plugin detection.
- Added exact Modrinth SHA-512 lookup.
- Added simple `jar` / `source` manual configuration.
- Continued support for GitHub, Spigot and Modrinth.
