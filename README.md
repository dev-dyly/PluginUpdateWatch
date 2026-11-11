# PluginUpdateWatch

Detect installed Paper plugins, find update sources, notify admins, and download updates on request. Supports **Modrinth, Spigot, GitHub and explicit HTTPS web sources**.

**PluginUpdateWatch 1.5.1 Stable** is a maintenance release for reliable bulk downloads, safe cached results and clearer console reporting. It includes the fixes and live Paper verification from PR #15; commands, permissions and configuration remain compatible.

[Version 1.5.1 release assets](https://github.com/dev-dyly/PluginUpdateWatch/releases/tag/v1.5.1) contain the verified `PluginUpdateWatch-1.5.1.jar` and SHA-256 checksums. See [release notes](RELEASE-NOTES.md), the [publication report](RELEASE-REPORT-1.5.1.md) and the [release procedure](docs/releasing.md).

## Install or upgrade

### 1.5.1

Bulk download completion and cancellation are counted once, stale configuration cannot trigger a transfer, and identical saved artifacts are reused safely. Scheduled reports show changed issues plus summaries; important failures and checksum warnings remain visible. Partial transfer files left by shutdown are recovered on the next startup. No configuration migration or automatic installation is introduced.


### 1.5.0

**New:** `/pu downloadall` queues all confirmed, eligible updates from the last successful check, using the same validation and checksum policy as single-file downloads. Manual-only, checksum-blocked, and already-running updates are skipped and reported. A successful live server test downloaded ViaVersion while correctly skipping four ineligible updates. Downloaded files require manual review and installation after stopping the server. No configuration changes are necessary.

### 1.4.1

The `1.4.1` release checks enabled plugins against the newest published Modrinth Paper/Spigot/Bukkit release even when the publisher omits the server's Minecraft version. An equal or newer installed version is CURRENT and stays out of console reports. A newer release is offered with an unlisted-compatibility warning. Being enabled confirms startup only; it does not prove every feature works or that a new release is compatible. Disabled plugins retain the exact Minecraft-version filter, and failed checks never become CURRENT.

Scans and cleanup saves add a blank line between plugin entries in `updates:` and legacy `plugins:` sections. Existing settings are preserved; run `/pu scan` to format an existing config. This release also restores bundled dependency relocation, fixing the bStats startup failure in 1.4.0.

### Stable installation

1. Stop the server.
2. Put `PluginUpdateWatch-1.5.1.jar` into the server's `plugins` folder. Remove the older PluginUpdateWatch JAR if upgrading. Keep its existing configuration folder.
3. Start the server. A scan begins automatically after startup.
4. Run `/pu list` as an operator to see updates or plugins needing a source link.

## Automatic detection

The scanner reads original JARs directly in the server's plugins folder and matches their name and version against installed plugins. It avoids Paper's remapped cache, which would produce different results. Plugin names and versions are stored directly from plugin descriptors, not from filenames or filesystem metadata.

Detected sources are saved automatically. Unresolved plugins receive an entry with the correct filename and a blank source, plus a message in `/pu list`. The scanner never selects projects by similarity; plugins without a detected source remain in config with an empty source field for manual review.

Names are trimmed and compared without case sensitivity; versions only have surrounding whitespace trimmed. Original metadata remains unchanged. Versions such as `1.0`, `01.0` and `1.0-beta` remain distinct after normalization; the displayed version preserves exact whitespace in the original descriptor.

Filenames that differ only by case (for example `Alpha.jar` and `alpha.jar` on Linux) are also ambiguous. Both files are skipped, including legacy fallback, until the collision is resolved. `/pu list` reports the conflict.

Existing links and legacy settings are preserved. If the config changes during a scan, results are discarded so edits are not overwritten. Saving can reformat YAML comments; existing setting values are not changed.

## Manual entry: just filename and link

Open `plugins/PluginUpdateWatch/config.yml` and fill in the blank source:

```yaml
updates:
  - jar: "ExamplePlugin-2.0.jar"
    source: "https://modrinth.com/plugin/example-plugin"
```

Use the exact installed filename, including `.jar`, and the real project's link. The example is a placeholder. No folder path, internal plugin name, resource ID or provider field is required. Keep only one source link per plugin.

Spigot and GitHub work the same way:

```yaml
updates:
  - jar: "SpigotPlugin.jar"
    source: "https://www.spigotmc.org/resources/example-plugin.12345/"

  - jar: "GitHubPlugin.jar"
    source: "https://github.com/owner/repository"
```

Save and run `/pu reload`. A scan/check will run shortly. Run `/pu scan` to retry discovery any time. If a plugin's JAR filename changes during an upgrade, update its `jar` value. Unmatched old entries can be removed with `/pu cleanup`.

## Cleaning obsolete config entries

When a plugin JAR changes filename, update the existing `jar` field first to keep its configured source. PluginUpdateWatch does not guess which plugin an old, missing filename belonged to.

Run `/pu cleanup` to preview entries whose files no longer exist in the configured plugins directory. Then run `/pu cleanup confirm` to remove those entries from `updates`. The original config, in full, is backed up before removal. Only missing-file entries matching the preview are removed; plugin JARs, downloaded files, plugin data and legacy settings remain untouched.

The confirmation rechecks the config and missing-file list. If either changed, run a new preview. Existing files are retained even when their plugin is disabled, their descriptor is unreadable, or their version is unparseable.

## Commands

| Command | Action |
| --- | --- |
| `/pu scan` | Detect installed JARs, find sources, save config, then check updates |
| `/pu check` | Check known sources without retrying automatic discovery |
| `/pu list` | Show cached updates and unresolved/configuration problems |
| `/pu download <plugin>` | Download an available update; tab completion supplies the plugin name |
| `/pu downloadall` | Queue every confirmed, downloadable update from the last successful check; report skipped and failed downloads |
| `/pu cleanup` | Preview config entries whose JAR files are missing |
| `/pu cleanup confirm` | Back up config and remove exactly the previewed missing-file entries |
| `/pu reload` | Reload config and schedule a new scan/check |
| `/pu stats` | Show the last check duration, update count and per-provider success/failure counts |

Permission: `pluginupdatewatch.admin`, granted to operators by default. Console commands omit `/`. The chat Download button fills the command; press Enter to submit it. Checks run every six hours by default; the interval is configurable.

## Installing downloaded updates

The 1.4 release normalizes Paper/Spigot/Bukkit wrappers and numbered-build labels such as Plan's `5.8 build 3638` and `5.8+build.3638`. These identify the same version. Only a demonstrably newer version is confirmed as an update; formatting differences alone do not trigger a download offer.

Before saving, downloads are compared with an unambiguously matched original installed JAR when available. Identical bytes are rejected. Numerically older files, and same/older descriptor versions without a matched original, are flagged for inspection rather than automatic install.

Run `/pu check` (or `/pu scan`) before `/pu downloadall` to refresh cached results. The bulk command queues **confirmed newer versions only**; unknown/different version labels and failed checks are excluded. Items without direct downloads, blocked by checksum policy, or already downloading are reported as skipped. Each file uses the existing checksum, HTTPS, plugin identity, JAR structure and version-freshness validation. Downloads are processed one by one, with individual results and a final saved/unchanged/failed summary. **No running plugin JAR is replaced automatically.**

Downloads go to `plugins/PluginUpdateWatch/downloads/`. GitHub and Modrinth downloads keep the exact safe publisher asset filename. When no filename is supplied (normally Spigot), the fallback is `<PluginName>.jar`.

## Supported sources

- **Modrinth:** newest published Paper, Spigot or Bukkit release, including beta/alpha. Enabled installed plugins can be checked without a matching Minecraft label, with a compatibility warning on downloads; disabled plugins retain exact game-version filtering. The primary matching JAR is preferred, and its SHA-512 checksum is verified.
- **Spigot:** updates are checked using Spiget, which may lag behind Spigot. Premium or external resources show a manual download link.
- **GitHub:** newest published release by publication date among the 100 most recent API entries, including prereleases and excluding drafts. Direct download works when exactly one JAR is present.

- **Web sources (1.4 release):** explicit HTTPS wiki/project pages or direct JAR/download endpoints. See [web sources](docs/sources/web.md). A page can link to an existing provider or a downloadable JAR file.

Release reports show stable, beta, alpha, prerelease, development/snapshot or unknown type. Modrinth and GitHub provide channel metadata; Spigot and direct files may only offer hints in version/filename analysis.

Downloads must contain a Paper/Bukkit descriptor with the expected plugin name, a version and a main class file. Every archive entry is checked for size, CRC, unsafe paths and duplicates. Both descriptor identity and main class are verified to match the expected plugin.

**Checksums are required by default for GitHub, Spigot and Modrinth**, including when the setting is absent from an older config. Explicit web sources have a separate missing-checksum exception, defaulting to warnings instead of rejection.

To deliberately allow a file without a checksum, set `downloads.require-checksum: false` and run `/pu reload`. An existing explicit `false` is preserved. These downloads still require HTTPS and are validated for safe paths, size, and plugin descriptor presence.

Explicit web sources default to `downloads.allow-unverified-web: true`, as they often supply no checksum. This exception warns before and after downloading while retaining HTTPS, file-size, plugin-descriptor and duplicate checks.

## Network, downloads and diagnostics (1.3.0; web additions in 1.4 release)

Existing configs can omit these options; defaults are applied. Invalid numeric limits reject startup/reload with a specific message.

```yaml
plugins-directory: "" # Empty = parent of this plugin's data folder
debug: false
network:
  connect-timeout-seconds: 10 # 1â€“60
  read-timeout-seconds: 15 # 1â€“120
  metadata-timeout-seconds: 30 # 1â€“300, elapsed request budget
  attempts: 3 # 1â€“5, includes the original attempt
downloads:
  max-size-mib: 100 # 1â€“1024
  timeout-seconds: 120 # 1â€“1800, elapsed request budget
  require-checksum: true # Standard providers
  allow-unverified-web: true # Web entries only; warn if no checksum. Set false for strict enforcement.
```

Requests retry HTTP 408, 429 and all 5xx responses, plus connection timeouts/resets, using three total attempts by default. Backoff follows `250 ms Ã— attempt`: the default two waits are 250 ms and 500 ms. Connection/read timeouts apply per-request, not cumulatively.

Connect/read timeouts are clamped to the remaining request budget; elapsed time is checked between stages and body reads. JVM/OS DNS resolution is synchronous and may outlast that budget. Up to five requests per update source are allowed within the configured timeout window.

Archive expansion is additionally capped at 1 GiB overall, 256 MiB per entry and 100,000 entries. These fixed validation ceilings apply even if the download size limit is raised. Unsafe output files with `../`, null bytes or duplicate paths are rejected.

Console and RCON reports omit successful CURRENT rows and informational discovery notes, including deliberately disabled plugins. Updates, unknown web downloads, failures and configuration problems are always shown.

`debug: true` adds one scan summary to the console. `/pu stats` reports the most recently accepted check; these local diagnostics are not uploaded to a telemetry service. Provider errors are logged with actionable messages.

Messages separate `[ERROR] NETWORK_ERROR`, `PROVIDER_ERROR` and `CONFIG_ERROR` from successful `[INFO] CURRENT` and `NOT_CURRENT` results. Rate limits include a retry delay when known; config errors do not retry.

All mutable command/result/lifecycle state is owned by the server thread. `UpdateCheckService` returns an immutable report from the I/O executor. Reload invalidates an old report and queues one fresh check.

## Existing settings and compatibility

The older `plugins:` format remains supported, including `enabled: false` and `asset-regex`. A new explicit jar/source entry takes precedence; a blank entry does not override legacy settings.

Targets Paper **1.21.11â€“26.3**, using Java 21 bytecode and no server internals. `api-version: '1.21.11'` deliberately declares the minimum Paper API, not a range or a maximum. Servers below that version are not supported.

### Tested Versions vs API Version

| Declaration or check | Meaning |
| --- | --- |
| `api-version: '1.21.11'` | Minimum Paper API; not an upper compatibility bound |
| Target Paper 1.21.11â€“26.3 | Intended runtime range; intermediate builds are not all individually tested |
| Paper 1.21.11 API + Java 21 | Source compilation and local unit tests |
| Cached Paper 26.3 build 6 alpha API + Java 25 | Source compilation and local unit tests against this specific API build |
| Published 1.3.0, Java 21 and 25 CI | 72 tests passed; see [the CI run](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs/36248215330) |
| Published 1.3.0 JAR, live Paper 1.21.11 build 132 / Java 21 and 26.3 build 41 alpha / Java 25 | Startup, scan/check/list/reload, real Modrinth download and missing-checksum rejection passed with evidence |
| 1.3.3 verification | 95 local tests passed; the final local stable JAR passed live update/download/integrity/cleanup and bStats lifecycle checks on both Paper builds above. PR/release CI runs on Java 21 and 25. |
| 1.4.1 verification | 166 checks passed (165 unit tests and one packaged-JAR integration test) on Java 21 and 25 in final development verification; the stable release also passed Java 21/25 Maven verification. Development live-test scope and published artifact evidence are recorded in [BUILD-REPORT.md](BUILD-REPORT.md). |
| 1.5.0 verification | Bulk download selection has regression tests and passed the owner's live Paper update-download trial; release builds run `mvn clean verify` on Java 21 and 25 in CI. |

Existing jar/source and legacy `plugins:` entries remain valid without migration. The deliberate safety change is that an **omitted** checksum setting now requires checksums; administrators wanting the earlier behavior should explicitly set `downloads.require-checksum: false`.

Build with JDK 21+ and Maven 3.9+: `mvn clean verify`. The installable local artifact is `target/PluginUpdateWatch-1.5.1.jar`. Gson and bStats are bundled and relocated; do not install the `original-` JAR.

Pull requests and main pushes build/test on Java 21 and 25. Actions stores the Java 21 JAR/checksum as the `plugin-java-21` artifact for 14 days, including development builds on main. To prepare a release, update the version in `pom.xml` and `.github/release-version`, then push or dispatch the release workflow.

## License

PluginUpdateWatch's project code and documentation, including version 1.2.0, are available under the [MIT License](LICENSE). Third-party dependencies retain their own licenses and notices.

## Usage metrics

Version 1.3.3 and later integrate bStats, enabled by default (including when the setting is absent from an older config). [View the PluginUpdateWatch dashboard](https://bstats.org/plugin/bukkit/PluginUpdateWatch/34400).

bStats reports active participating servers and plugin-version adoption, not lifetime installations or download counts. Offline servers, older builds without bStats, and servers that opt out are not counted.

Only standard bStats metrics are sent: plugin/server versions, online-player count, online-mode setting, Java version, OS name/version/architecture, CPU core count and a random server identifier (no IP, hostname or player names).

To disable reporting for this plugin, add this to `plugins/PluginUpdateWatch/config.yml` and run `/pu reload`:

```yaml
metrics:
  enabled: false
```

Alternatively, set `enabled: false` in the shared `plugins/bStats/config.yml` and restart the server to disable bStats across plugins. This plugin does not override that global choice. A report about your server's configuration is never sent unsolicited.

Reports start after bStats' normal delay (approximately 3â€“6 minutes) and recur about every 30 minutes. The dashboard publishes at hh:00 and hh:30, so the first data point can take 3â€“36 minutes to appear.

### 1.4.1 console reports

Console/RCON reports show confirmed updates and actionable problems with troubleshooting links. Use `/pu list all` to inspect current and unconfirmed versions. Unconfirmed labels are not treated as updates or proof of being current. In-game reports remain complete.
