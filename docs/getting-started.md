# Quick Start

After PluginUpdateWatch is installed, it scans the server's original plugin JARs and tries to identify update sources.

## 1. Check the current status

Run:

```text
/pu list
```

This shows:

- available updates
- unresolved plugins
- configuration problems
- duplicate or ambiguous JARs
- version mismatches
- disabled plugins as informational entries

## 2. Retry automatic discovery

```text
/pu scan
```

A scan:

1. Detects installed plugin JARs.
2. Tries to discover a source.
3. Saves detected sources.
4. Checks for updates.

## 3. Add unresolved plugins manually

Open:

```text
plugins/PluginUpdateWatch/config.yml
```

Add the exact JAR filename and the real project link:

```yaml
updates:
  - jar: "ExamplePlugin-2.0.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

Then run:

```text
/pu reload
```

## 4. Download available updates

For a single plugin:

```text
/pu download <plugin>
```

To download **all confirmed, eligible updates** in v1.5.0:

```text
/pu check
/pu downloadall
```

Bulk downloads skip manual-only, checksum-blocked, already-running, and unconfirmed updates. See the [DownloadAll guide](downloads.md#download-all-confirmed-updates).

Downloaded files are saved to:

```text
plugins/PluginUpdateWatch/downloads/
```

Review the downloaded file, stop the server, replace the old plugin JAR, and restart.

!!! important
    PluginUpdateWatch downloads updates on request. It does not hot-swap or automatically install plugins into a running server.
