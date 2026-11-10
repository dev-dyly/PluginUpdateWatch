# Troubleshooting

<a id="missing-source"></a>

## A plugin shows "Source not identified"

Add its source manually in:

```text
plugins/PluginUpdateWatch/config.yml
```

Example:

```yaml
updates:
  - jar: "ExamplePlugin.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

Then run:

```text
/pu reload
```

<a id="jar-discovery"></a>

## A JAR filename changed

Update the existing `jar:` value in the config.

PluginUpdateWatch retains unmatched old entries instead of guessing which new filename replaced them.

## Duplicate candidate JARs

If more than one JAR could represent the same installed plugin, PluginUpdateWatch reports the ambiguity rather than selecting one.

Stop the server, move the duplicate JAR out of the plugins folder, restart, then run:

```text
/pu scan
```

Renaming a duplicate to another `.jar` filename in the same folder does not resolve duplicate plugin metadata. Keep only the intended copy in the plugins folder.

## Filenames differ only by case

Files such as:

```text
Alpha.jar
alpha.jar
```

are considered ambiguous.

Stop the server and inspect the conflicting files. If they are copies of the same plugin, move the unwanted copy out of the plugins folder. If they are different plugins, give them distinct filenames that differ by more than letter case and update any corresponding `jar:` entries in the config. Restart the server, then run `/pu scan`.

<a id="checksums"></a>

## Download blocked: checksum missing

This is expected when:

```yaml
downloads:
  require-checksum: true
```

and the provider does not supply a supported checksum.

Spigot/Spiget commonly falls into this category.

You can either download manually from the publisher or explicitly opt out:

```yaml
downloads:
  require-checksum: false
```

Then run `/pu reload`.

For explicit web sources, `downloads.allow-unverified-web: true` permits a missing checksum by default. If you set this to `false` with `require-checksum: true`, a web source without a checksum is also blocked. A supplied invalid or mismatched checksum is always rejected; do not disable validation to work around a mismatch.

<a id="already-installed"></a>

## A download is identical, older, or only available for inspection

Version 1.4.0 normalizes known build/platform labels and only notifies for confirmed newer versions. Plan `5.8 build 3638` and `5.8+build.3638`, for example, are the same build.

Run `/pu scan` after installing or renaming a plugin JAR so the cached installed metadata is refreshed. If a download is rejected as identical or stale, review the source's release and the downloaded artifact it advertises. A fixed artifact URL may keep returning the same file; update it or use an appropriate project/download page. Repeating the download cannot make that file newer.

**Download for inspection** means version order is uncertain. A different snapshot with the same version label may contain changes, but the plugin does not claim it is newer. Generic filenames can also remain unchanged for genuine updates; review the descriptor version and publisher's release notes.

<a id="unconfirmed-versions"></a>

## Current plugins are missing from console output

This is intentional in 1.4.0 and later. Console/RCON reports hide current plugins and deliberately disabled informational entries. In-game `/pu list` still shows the full report. In stable 1.4.0, updates, unknown web downloads, errors and configuration warnings remain visible. In 1.4.1, default console/RCON output shows confirmed updates and actionable errors/configuration warnings; unconfirmed comparisons are counted in the summary. Use `/pu list all` for the full console report. UNKNOWN still means unconfirmed, not current. Vault build suffixes, generic ProtocolLib dev-build labels and fixed Essentials URLs cannot alone prove that an update exists. Provider checksum matches can establish CURRENT; an identical-file download also marks its cached result CURRENT until the next fresh check.

<a id="config-errors"></a>

## Config error

Correct the affected entry in `config.yml` and run:

```text
/pu reload
```

<a id="provider-errors"></a>

## Provider or network error

Retry later with:

```text
/pu check
```

Provider errors are not reported as "up to date".

## Old result marked cached

If a complete scan fails, `/pu list` can label older results as cached.

Run a new scan/check after the underlying problem is resolved.

## Need diagnostics

Run:

```text
/pu stats
```

You can also temporarily enable:

```yaml
debug: true
```

and then run `/pu reload`.

<a id="network-errors"></a>

## Network errors

Check outbound HTTPS access, DNS and server connectivity. Temporary failures are retried within bounded limits. Retry `/pu check` later, or `/pu download <plugin>` after resolving a failed download. Partial temporary files are discarded; no replacement is saved.

<a id="rate-limits"></a>

## API rate limits

Wait before checking again. Avoid repeated scans/downloads while the provider is limiting requests. Review the provider response and any retry interval in the detailed report; `/pu list all` displays full failure information.

<a id="invalid-downloads"></a>

## Invalid downloads

A checksum mismatch, invalid plugin descriptor, wrong plugin identity or malformed JAR blocks the download. Check the configured source and publisher's file. Do not bypass a supplied mismatched checksum. Use `/pu scan` after replacing an installed JAR.

<a id="compatibility-labels"></a>

## Minecraft compatibility labels

An enabled installed plugin can be checked even if a Modrinth release does not list this Minecraft version. Startup does not prove a newer release is compatible. Review publisher notes and test before installing; checking failures are never treated as current.

<a id="prereleases"></a>

## Development releases

Alpha, beta, snapshot and development builds can be selected as the latest release. Their warnings identify the release type; review publisher notes before installing and keep backups. An unknown release type does not itself prove the file is a development build.

<a id="file-errors"></a>

## File errors

Check disk space, file permissions and locks on the downloads folder. Stop the server before replacing installed plugins. Retry after correcting the underlying problem; do not delete installed plugin data to resolve a download error.

<a id="installing-updates"></a>

## Installing downloaded updates

A saved download is not installed automatically. Stop the server, replace the old plugin JAR with the validated downloaded file, then restart and run `/pu scan`. A newer downloaded file remains an update until the installed plugin changes.

## Already downloaded and cancellation

`ALREADY_DOWNLOADED` means the selected artifact is already in PluginUpdateWatch's downloads folder. It does not mean the installed plugin has been upgraded. Review and install it manually after stopping the server.

A bulk completion summary accounts for every queued item, including cancellation requested by reload/disable. Check the downloads folder after cancellation: a final move already in progress may have completed before interruption was observed. Controlled failures discard temporary files. Even a graceful stop can finish before an interrupted network read returns. The next plugin startup recovers reserved `.download-<number>.tmp` partial transfers on the I/O worker, retaining saved JARs and unrelated files. Cleanup failures remain errors with optional debug diagnostics; an abrupt JVM or operating-system termination cannot guarantee immediate cleanup.

A missing required checksum is a WARNING and remains blocked. A supplied mismatched checksum, invalid archive, wrong plugin identity or filesystem failure is still an ERROR. Scheduled scans retain unchanged issues in reports and summary counts rather than replaying their full warnings every time.
