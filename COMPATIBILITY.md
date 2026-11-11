# Compatibility policy

PluginUpdateWatch targets Paper **1.21.11–26.3**. `plugin.yml` deliberately declares **the minimum API version**, `1.21.11`; it is not an upper bound. Paper performs its own API compatibility check before enabling the plugin. `Compatibility.java` also classifies server versions at startup: known versions below the floor are rejected, and versions outside the target receive a warning instead of a speculative upper-version rejection. No claim is made for Folia or standalone Spigot.

The plugin uses Java 21 bytecode. Run the Java version required by your Paper build; the live test environments use Java 21.0.12 for Paper 1.21.11 and Java 25.0.4.1 for Paper 26.3.

## 1.5.1 release scope

The release retains Paper **1.21.11–26.3** and Java 21 bytecode. Release CI verifies Java 21 and Java 25 before publishing the exact tested Java 21 artifact. Modrinth metadata advertises only **Paper** and the currently registered targeted releases: **1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3**. This is the existing compatibility policy, not a claim that every listed build has been live-tested. Standalone Spigot, Folia and unverified loaders are not advertised.

The PR #15 fixes were live-tested on Paper **26.3 build 246 / Java 25.0.4.1**, including real provider downloads and separate controlled-provider failure/cancellation cases. Authenticated-player testing and the full production plugin stack were not exercised. The v1.5.1 metadata bump is separately build-verified; see the release and live reports for artifact-specific scope.

## 1.3.2

The final local 1.3.2 JAR passed startup, scan/check/list/reload, a real Modrinth download with its exact publisher filename and verified SHA-512, missing-checksum rejection, config cleanup preview/confirmation/backup, and informational disabled-plugin reporting on Paper **1.21.11 build 132 / Java 21** and **26.3 build 41 alpha / Java 25**. The minimum API and target range are unchanged. See [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for exact scope and artifact evidence.

## Published 1.3.0

The [published release](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/tag/v1.3.0) passed all **72 tests** in Maven CI on both Java 21 and Java 25, using the declared `1.21.11-R0.1-SNAPSHOT` API. Its source also compiled locally against cached `26.3.build.6-alpha`, with local tests passing on Java 25. That alternate API check applies to the cached build, not every 26.3 build.

The exact published JAR passed live startup, `pu scan`, `pu check`, `pu list`, `pu reload`, a real Modrinth download with independent SHA-512 verification, and rejection of a download without a checksum on Paper **1.21.11 build 132 / Java 21.0.12** and **26.3 build 41 alpha / Java 25.0.4.1**. Both test servers stopped cleanly with exit status 0. [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) identifies the exact artifacts tested for each version.

## 1.3.1

Version 1.3.1 adds protection against JAR filenames that differ only by case and three regression tests. Local Maven verification passed **75 tests**. The minimum API and intended runtime range remain unchanged from 1.3.0.

The final local 1.3.1 JAR passed live startup, `pu scan`, `pu check`, `pu list`, `pu reload`, a real Modrinth download with independent SHA-512 verification, and rejection of a Spigot download without a checksum on both Paper builds above. Both isolated test servers shut down with exit status 0. [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) identifies the exact tested artifact and verification scope.

## Verification scope

Neither API compilation nor tests on these two Paper builds establish compatibility with every intermediate release, every plugin combination or future Paper builds. Historical 26.3 build 41 was an alpha; PR #15 was additionally live-tested on stable build 246. Live checks use server-console commands; in-game player chat interactions and sustained load are not covered.

Before production use, test startup, scan, reload during a scan and download on a server copy. Confirm that the configured provider lists the intended plugin and, for Modrinth, a release explicitly tagged for your Minecraft version. GitHub/Spigot checks still require manual review of the publisher's server compatibility notes.

See [BUILD-REPORT.md](BUILD-REPORT.md) for automated coverage and [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for exact artifacts and procedures.
