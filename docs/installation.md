# Installation

## Requirements

PluginUpdateWatch targets:

- **Paper 1.21.11â€“26.3**
- Java compatible with your Paper version
- PluginUpdateWatch itself uses **Java 21 bytecode**

Folia and standalone Spigot are not claimed as supported.

## Download

[Download PluginUpdateWatch](https://github.com/dev-dyly/PluginUpdateWatch/releases/latest){ .md-button .md-button--primary }

Choose the normal `PluginUpdateWatch-<version>.jar` from the release's **Assets** section. The `.sha256` file is an optional checksum for verifying your download; it is not a plugin.

## Install

1. Stop the Minecraft server.
2. Download [PluginUpdateWatch-1.5.0.jar](https://github.com/dev-dyly/PluginUpdateWatch/releases/download/v1.5.0/PluginUpdateWatch-1.5.0.jar).
3. Place the JAR in the server's `plugins` folder.
4. Start the server.
5. PluginUpdateWatch creates its configuration folder and begins a scan after startup.
6. Run:

```text
/pu list
```

Operators receive the `pluginupdatewatch.admin` permission by default.

## Upgrade

When upgrading PluginUpdateWatch:

1. Stop the server.
2. Remove the older PluginUpdateWatch JAR.
3. Put the new JAR in the `plugins` folder.
4. Keep the existing `plugins/PluginUpdateWatch/` configuration folder.
5. Start the server.

Existing two-field `jar` / `source` entries and older legacy `plugins:` entries remain supported.

!!! warning
    Do not install the Maven `original-` JAR. The installable artifact is the normal `PluginUpdateWatch-<version>.jar`.
