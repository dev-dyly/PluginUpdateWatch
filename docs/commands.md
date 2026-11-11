# Commands

All commands use `/pu`, which is an alias of `/pluginupdates`.

| Command | Description |
| --- | --- |
| `/pu list all` | Show the full report, including current and unconfirmed versions (1.4.1) |
| `/pu list` | Show cached update results, unresolved plugins and configuration problems |
| `/pu scan` | Scan installed JARs, discover sources, save config and check updates |
| `/pu check` | Check already-known sources without retrying automatic discovery |
| `/pu download <plugin>` | Download an available update |
| `/pu downloadall` | Queue all confirmed, directly downloadable updates from the last successful check (since v1.5.0) |
| `/pu cleanup` | Preview config entries whose configured JAR files are missing |
| `/pu cleanup confirm` | Back up the config and remove exactly the previewed missing-file entries |
| `/pu stats` | Show the latest check duration, update count and provider success/failure counts |
| `/pu reload` | Reload the configuration and schedule a fresh scan/check |

## Permission

All administrative commands use:

```text
pluginupdatewatch.admin
```

The permission defaults to server operators.

## Console

Console commands omit the slash:

```text
pu list
pu scan
pu check
```

## Download tab completion

For `/pu download`, tab completion supplies plugin names when an available update can be downloaded. `/pu downloadall` is also available in subcommand completion.

## Bulk downloads (v1.5.1)

See the [full DownloadAll guide](downloads.md#download-all-confirmed-updates) for eligibility, skipped items, checksum policy and examples.

Run `/pu scan` or `/pu check` to refresh the update report, then run `/pu downloadall`. The command downloads only confirmed newer versions with direct URLs from the latest successful check. It skips unconfirmed versions, unavailable direct downloads, files blocked by checksum settings, and downloads already in progress. A running/failed check must be resolved first.

Each accepted item uses the same HTTPS, checksum-policy, file-size, plugin-identity, archive-validation, and freshness protections as `/pu download <plugin>`. The downloads are sequentially processed on the existing I/O worker, with individual results and a final saved/unchanged/failed summary. Files are saved in `plugins/PluginUpdateWatch/downloads/`, **never installed or activated automatically**. Preview and manually replace plugin JARs while the server is stopped.

## Console output in 1.4.0+

Console and RCON hide successful up-to-date plugin rows and intentionally disabled informational entries. Since 1.4.1, confirmed updates, errors and configuration warnings remain visible; unconfirmed comparisons are counted in the summary. Use `/pu list all` for full console details. In-game `/pu list` keeps the full report. Alpha/beta/prerelease/development or unknown release types warn before downloading.

## Compact console reports in the 1.4.1

Default console/RCON reports show confirmed updates and actionable problems, with specific troubleshooting links. Unconfirmed versions are counted rather than printed individually; `/pu list all` retains full details. In-game reports remain complete. Download warnings are consolidated, and identical-file downloads mark the current cached result CURRENT until the next check.

Version 1.5.1 preserves all commands and permissions. Downloads require a successful current check after reload; queued transfers recheck the configuration before connecting. RCON receives immediate acknowledgements; delayed results are logged to console and can be inspected with `/pu list` or `/pu stats`.
