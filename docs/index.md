# PluginUpdateWatch

**PluginUpdateWatch** is a Paper plugin that detects installed plugins, checks supported update sources, notifies administrators when updates are available, and downloads updates on request.

Supported update sources:

- **Modrinth**
- **Spigot / Spiget**
- **GitHub Releases**
- **[HTTPS web pages and direct downloads](sources/web.md)**

## Current version

**PluginUpdateWatch 1.5.1 Stable**

[Download PluginUpdateWatch](https://github.com/dev-dyly/PluginUpdateWatch/releases/latest){ .md-button .md-button--primary }

[Version 1.5.1 assets](https://github.com/dev-dyly/PluginUpdateWatch/releases/tag/v1.5.1) are published with the verified JAR and SHA-256 checksums. The latest-release button above opens the current stable release.

!!! tip "1.5.1 — Reliable downloads and clearer reporting"
    Run `/pu check` followed by `/pu downloadall` to queue eligible updates. Cancellation accounting, stale-config protection, duplicate avoidance and interrupted-transfer recovery are improved. Manual-only and checksum-blocked items are reported and skipped. Files are saved for review, **not installed automatically**. See the [DownloadAll guide](downloads.md#download-all-confirmed-updates).

## Quick start

1. Stop your Paper server.
2. Place `PluginUpdateWatch-1.5.1.jar` in the server's `plugins` folder.
3. Start the server.
4. Run `/pu list` as an operator.
5. Use `/pu scan` to retry automatic source discovery at any time.

!!! note
    PluginUpdateWatch does not automatically replace running plugin JARs. Downloads are placed in `plugins/PluginUpdateWatch/downloads/` for you to review and install during a restart.

## Main features

- Automatic plugin JAR discovery
- Exact Modrinth SHA-512 lookup
- Modrinth, Spigot and GitHub update checks
- Explicit HTTPS wiki/project pages and direct download sources
- Latest published releases, including prereleases, with release-type warnings
- Quiet console reports and conservative update notifications
- Version comparison and download freshness checks to reduce false alerts
- Manual source configuration using only a JAR filename and source link
- Config cleanup with backups
- Single-plugin and bulk downloads with `/pu downloadall` ([guide](downloads.md#download-all-confirmed-updates))
- Download checksum verification
- Download archive validation
- Admin join notifications
- Update statistics and diagnostics

## Compatibility

PluginUpdateWatch targets Paper **1.21.11Ã¢â‚¬â€œ26.3** and uses Java 21 bytecode.

See [Compatibility](compatibility.md) for the exact verification scope.
