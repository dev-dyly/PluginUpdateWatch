# Downloading Updates

PluginUpdateWatch **downloads updates for manual installation**. Download one plugin or queue all confirmed, eligible updates after a successful check.

## Download one plugin

```text
/pu download <plugin>
```

A single-plugin download may also be offered for inspection if a newer version cannot be confirmed. Verify the publisher's release notes before installing.

## Download all confirmed updates

**New in v1.5.0:**

```text
/pu scan
/pu downloadall
```

Or run `/pu check` instead of `/pu scan` when the source links are already configured.

`/pu downloadall` uses the **last successful** update check. It queues **only confirmed newer versions** with direct download URLs, processing eligible downloads sequentially on the existing background I/O worker. Unknown or unconfirmed version labels and errored provider results are excluded.

The command skips and reports:

- updates with no direct download link (manual download required);
- releases without a checksum when the configured checksum policy requires one;
- downloads that are already in progress.

If a check is running, the last check failed, or no successful check has completed yet, complete a successful `/pu check` first.

The command reports the queue count, individual download results, and a final summary counting saved, already installed/not newer, and failed downloads. **Skipped updates are not counted as download failures.**

### Example result

If five updates are confirmed but two require manual download and two lack required checksums, `/pu downloadall` queues one eligible update and reports four skipped updates.

### Allowing downloads without provider checksums

Standard sources default to `downloads.require-checksum: true`. If a provider offers no supported checksum, automatic downloading is blocked, including in bulk mode. To *deliberately* allow those files:

```yaml
downloads:
  require-checksum: false
```

Run `/pu reload`, complete a fresh `/pu check`, then run `/pu downloadall` again.

!!! warning "Reduced integrity assurance"
    Disabling checksum requirements means the publisher's file digest cannot be verified. HTTPS, archive validation, plugin identity checks, size limits and freshness checks still apply. A checksum that is supplied but incorrect is still rejected.

See [Checksums & Security](checksums.md) for provider-specific behaviour.



Downloads are saved to:

```text
plugins/PluginUpdateWatch/downloads/
```

## Filenames

For GitHub and Modrinth, PluginUpdateWatch preserves the safe publisher asset filename. Direct web downloads preserve a safe filename from the URL, including spaces, `+` and build identifiers. Extensionless web endpoints use `<PluginName>.jar`.

When a provider does not supply a filename, normally Spigot, the fallback is:

```text
<PluginName>.jar
```

PluginUpdateWatch does not automatically rename or delete previously downloaded files.

## Installing a downloaded update

1. Review the publisher's compatibility notes.
2. Stop the server.
3. Replace the old plugin JAR with the downloaded update.
4. Keep the plugin's existing data/configuration folder.
5. Restart the server.

PluginUpdateWatch does not overwrite running plugins or automatically install downloaded updates.

## Validation

Downloaded JARs must contain a valid Paper/Bukkit descriptor with:

- the expected plugin name
- a version
- a main class
- the referenced main class file

Archive validation also checks entries for size, CRC, unsafe paths and duplicates.

## Freshness and inspection downloads

Version formatting alone does not establish an update. For example, Plan's `5.8 build 3638` and `5.8+build.3638` describe the same build, while platform labels such as `paper-1.4.9` can match an installed `1.4.9`.

When the installed original JAR can be matched, an identical downloaded file is rejected. Numerically older versions and same/older descriptor versions behind an advertised update are also rejected before the file is accepted.

The **1.4.1** also compares available provider checksums with an unambiguous original installed JAR during checks. A matching SHA-512/SHA-256 makes the result CURRENT even if the publisher used a different release label. If multiple supported digests are supplied, all must match. Missing or ambiguous local JARs cannot establish file equality. Checks do not download full remote JARs just to compare them.

Duplicate or non-newer downloads report informational `ALREADY_INSTALLED` or `NO_UPDATE` outcomes, retain the installed plugin and discard the temporary file. They no longer appear as provider failures with retry instructions. Older-version outcomes include the installed and downloaded descriptor versions. Invalid archives, identity failures and checksum mismatches remain errors.

An unknown web version or ambiguous version difference is labelled **Download for inspection**; it does not establish that a newer release exists or trigger an update notification. A changed snapshot with the same descriptor version may be saved for inspection, with an explicit warning that a newer version has not been established.

Alpha, beta, prerelease, development and unknown release types carry warnings. Review the publisher's compatibility notes before installing. Generic filenames such as `BigDoors.jar` can stay unchanged across genuine releases; the filename alone is not the version.

## Download limits

Default settings:

```yaml
downloads:
  max-size-mib: 100
  timeout-seconds: 120
  require-checksum: true
  allow-unverified-web: true
```

Explicit web sources may lack checksums and are allowed with warnings by default. See [Checksums & Security](checksums.md) to require them.

Fixed archive validation limits also apply even when the configured download size is increased.
