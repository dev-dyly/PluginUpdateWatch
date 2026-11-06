# PluginUpdateWatch

**PluginUpdateWatch** is a Paper plugin that detects installed plugins, checks supported update sources, notifies administrators when updates are available, and downloads updates on request.

Supported update sources:

- **Modrinth**
- **Spigot / Spiget**
- **GitHub Releases**
- **[HTTPS web pages and direct downloads](sources/web.md)**

## Current version

**PluginUpdateWatch 1.4.1**

[Download PluginUpdateWatch](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/latest){ .md-button .md-button--primary }

[Download the 1.4.1 JAR](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/download/v1.4.1/PluginUpdateWatch-1.4.1.jar) or view the latest release above.

## Quick start

1. Stop your Paper server.
2. Place `PluginUpdateWatch-1.4.1.jar` in the server's `plugins` folder.
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
- Single-plugin and bulk downloads with /pu downloadall
- Download checksum verification
- Download archive validation
- Admin join notifications
- Update statistics and diagnostics

## Compatibility

PluginUpdateWatch targets Paper **1.21.11Ã¢â‚¬â€œ26.3** and uses Java 21 bytecode.

See [Compatibility](compatibility.md) for the exact verification scope.
