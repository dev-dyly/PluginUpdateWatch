# Stable release preparation — 1.5.0

Checked 7 November 2026 (Pacific/Auckland).

- The server owner successfully tested `/pu downloadall` on a live Paper server: five confirmed updates, one checksum-verified ViaVersion download saved, two manual-only releases skipped, two releases skipped by checksum policy, zero failures.
- Bulk downloads reuse the existing validated single-download pipeline, with only confirmed newer release labels eligible. The test log confirms no automatic installation of plugin JARs.
- Version metadata is updated for `1.5.0`; release workflow verifies the final artifact's version, SHA-256 and Java 21/25 Maven builds before GitHub publication.
- No configuration migration is needed.

---

# Stable release preparation - 1.4.1

Checked 9 October 2026 (Pacific/Auckland).

- The server owner reported a successful live Paper 26.3 test of the final 1.4.1 preview and authorized stable release publication.
- Stable source changes only version metadata and documentation; runtime behavior matches the tested preview. Java 21 and Java 25 each passed 166 checks (165 unit tests and one packaged-JAR integration test), with zero failures/errors/skips. Strict MkDocs verification passed.
- Stable Java 21 artifact SHA-256: `5206d95578f142a683e32f34b54ce9c2972a228f63a7c158dd7f9a9128901de9`.
- Stable downloads, command documentation and wiki troubleshooting now document 1.4.1. Prior artifact-specific verification remains recorded below.

---

# Compact console reports and troubleshooting - 1.4.1-SNAPSHOT

Checked 9 October 2026 (Pacific/Auckland).

- Java 21 and Java 25 `mvn clean verify`: **166 checks passed on each runtime** (165 unit tests plus one packaged-JAR integration test), zero failures/errors/skips.
- Six new tests cover compact missing-source messages, classified failures and wiki routing, confirmed update actions/checksum restrictions, consolidated download warnings, identical-download cache replacement and truthful summaries. Updated visibility tests ensure UNKNOWN/DIFFERENT remain available in full reports but do not masquerade as confirmed updates.
- The exact Java 21 artifact passed isolated Paper 26.3 / Java 25 startup, scan/check/list/list all/reload, confirmed update display, CURRENT and uncertain console suppression, full-report retention, troubleshooting links, current download refusal and config-save stability. Real Modrinth fixtures exercised compatibility warnings. The server exited normally; original test configs/JARs were restored. Production was not changed.
- Strict MkDocs build passed; all 14 troubleshooting anchors referenced by the new message helpers exist in the generated page. Links become available on the public wiki when these docs are merged/deployed.
- An identical download marks only its still-current cached result CURRENT; a fresh check reevaluates the source. Generic mutable URLs are not permanently considered current. Downloaded newer files are not confused with installed updates.

Snapshot SHA-256: `72b7e854a964851c3cbaf9ff180c486fc5d24a75f7a8ee8b33981d3c5c388fc0`.

---

# Identical-artifact and official build metadata verification — 1.4.1-SNAPSHOT

Checked 8 October 2026 (Pacific/Auckland), after the production download attempts at 14:36–14:38.

- Java 21 and Java 25 `mvn clean verify`: **160 checks passed on each runtime** (159 unit tests plus one packaged-JAR integration test), zero failures/errors/skips.
- Eleven new tests cover identical/different/malformed provider digests, missing/ambiguous/replaced installed JARs, SHA-256 and multi-digest matching, official build metadata, pinned URLs, default-channel downgrades, malformed responses, rate limits and endpoint scope. Existing download tests now assert informational duplicate/non-newer outcomes while still ensuring stale files are rejected and previous downloads preserved.
- Modrinth's advertised BedrockEssentials `0.1.0-beta.2` asset is named `BedrockEssentials-0.1.0-beta.1.jar` and its SHA-512 matched the original installed file. A read-only invocation of the new checker against production JARs and live APIs returned CURRENT with no error for BedrockEssentials, Geyser-Spigot and floodgate.
- Geyser's selected default endpoint reported version `2.11.3`, build `1249`, versus installed `2.12.0-SNAPSHOT`. Floodgate reported `2.2.5`, build `141`, with SHA-256 matching the installed file. Both pinned Spigot download URLs returned HTTP 200 and a ZIP signature. No production plugin was downloaded into place, executed or restarted during these diagnostics.
- The exact Java 21 JAR below passed isolated Paper 26.3 build 41 / Java 25 startup, scan/check/list/reload, current suppression/download refusal, real Modrinth compatibility warning and repeated config-save stability checks. Test metrics were globally opted out; the server exited with code 0 and its previous configs/JARs were restored. Prior live 1.21.11 results below apply to their separately identified artifacts.
- Generic web sources still cannot establish current/newer status without metadata. This change neither suppresses provider failures nor disables checksum/JAR validation. No persistent assumption is made about mutable download URLs: available provider digests are checked again on each scan/check.

Snapshot SHA-256: `4a9cca0838696e9b351f7441442a071ff2d29066cb27c9c54d146823288f02eb`.

---

# Console/config follow-up — 1.4.1-SNAPSHOT

Checked 8 October 2026 (Pacific/Auckland), after the 14:17 console report.

- Java 21 and Java 25 `mvn clean verify`: **149 checks passed on each runtime** (148 unit tests and one final-JAR integration test), zero failures/errors/skips.
- Nine added tests cover the reported encoded Spigot titles, malicious/invalid encodings, exact Geyser project page routing, unrelated URLs, numbered snapshot/beta ordering, genuinely ambiguous labels, GitHub development warnings, stale prerelease downloads and preservation of config row key order across scans.
- The configured Spigot IDs `124687` and `75097` returned HTTP 200 with versions `1.1.7` and `1.35.24`. Both official Geyser/Floodgate Spigot endpoints returned HTTP 200 and a ZIP signature. These probes did not install or execute downloaded upstream code.
- The final Java 21 follow-up JAR passed isolated Paper 26.3 build 41 / Java 25 startup, scan/check/list/reload, current download refusal, real Modrinth missing-label warnings and repeated config-save stability. The server exited with code 0 and its prior config/JARs were restored. The earlier two-version live evidence below identifies a different artifact; it is not claimed as a live test of this exact follow-up JAR on 1.21.11.
- Production config was read for diagnosis, not edited. Fixed Essentials artifact links and custom/unnumbered version labels remain inspection/uncertain results. No source is guessed for local plugins with blank links.

Follow-up snapshot SHA-256: `0f94abf71b9492b2781937c775ac468ae66c15bc5c150d57f1ab2a0ed5341b33`.

---

# Enabled-plugin compatibility and config formatting — 1.4.1-SNAPSHOT

Checked 8 October 2026 (Pacific/Auckland). Unreleased preview based on main `952b09f6624a336a283a4477fffa3f7bfeab8d5b`.

- Java 21 and Java 25 Maven `clean verify`: **140 checks passed on each runtime** (139 unit tests plus one final-JAR integration test), zero failures, errors or skipped tests.
- Eight compatibility tests cover enabled/current/newer/older versions, newest releases without Minecraft labels, loader/status filtering, disabled-plugin strict filtering, matching-label warnings, provider failures and propagation through modern/legacy config.
- Three formatting tests cover blank lines between modern and legacy plugin entries, multiline values, repeated serialization, concurrent-edit protection and cleanup backups.
- The packaged-JAR test verifies relocation of bStats, Gson and jsoup and constructs the relocated bStats client with reporting disabled and its own relocation guard enabled. This catches the missing relocation in the published 1.4.0 POM. CI now retains both unit and integration test reports.
- The exact Java 21 artifact passed isolated Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25 startup, scan/check/list/reload, config spacing and repeated-save stability checks. Enabled fixture plugins queried the real GPTitle Modrinth project: the newer result displayed its missing-26.3-label warning; the current fixture stayed out of console output and its download was refused. bStats construction succeeded with global reporting opted out. Both servers exited with code 0; their prior config/JARs were restored.
- These fixture checks verify PluginUpdateWatch behavior, not GPTitle's runtime compatibility. No new upstream artifact was installed or executed. Production DylyCraft was not modified or restarted. Full in-game warning rendering is covered by result/visibility assertions, not a connected player session.
- `mkdocs build --strict` passed. Documentation marks the changed policy as a 1.4.1 preview; stable download links remain at 1.4.0.

Tested snapshot SHA-256: `e1c6e2f7907da3efc592fac9318be888c6b8e41f30334e5d6a0bf21b3c7edd2e`.

---

# Published release and documentation verification — 1.4.0

Checked 7 October 2026 (Pacific/Auckland).

- Release commit: `0717544c8f96a6d6b9df1dd33c2365c8e4cc1606`.
- [Release workflow](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs/37477465886) passed Java 21 and Java 25 verification and published the verified artifact. The separate [main build](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs/37477167644) also passed both Java jobs.
- The GitHub release JAR, its `.sha256` file and the JAR downloaded from Spigot all match SHA-256 `26c07085c9c2e0d4d2f072e83b3213b17638e9feb4c844882a5246212ab1c5f7`. The packaged descriptor reports version `1.4.0` and API floor `1.21.11`.
- Updated the wiki's current version, download/build examples, stable feature descriptions, web checksum setting, compatibility scope and troubleshooting. `mkdocs build --strict` passed with the same MkDocs Material version (`9.7.7`) used for deployment; no plugin code or release artifact was changed.
- Live results below apply to the identified development JARs. This publication audit does not claim a new live test of the exact stable JAR.

---

# False-update regression verification — 1.4.0-SNAPSHOT

Checked 7 October 2026 (Pacific/Auckland).

- Java 21 and Java 25 Maven clean verify: **128 tests passed**. Ten additional regression tests cover the five reported plugin examples, numbered-build/platform normalization, conservative development labels, notification eligibility, identical installed files, stale advertised downloads, genuine newer builds and changed snapshots with unchanged descriptor versions.
- The production log identified three formatting-only false alerts: Plan `5.8 build 3638` versus `5.8+build.3638`; LuckPerms `5.5.71` versus `v5.5.71-bukkit`; DoubleDoors `1.4.9` versus `paper-1.4.9`. These now compare as CURRENT.
- BigDoors `0.1.8.71 → 0.1.8.72` and ResourcePackManager `2.4.5 → 2.4.6` were genuine updates in that log. Their downloaded descriptors and SHA-512 hashes match the newer Modrinth releases. Their generic filenames are unchanged between releases.
- The corrected Java 21 JAR passed isolated Paper 1.21.11 / Java 21 and Paper 26.3 / Java 25 startup, scan/check/list/reload tests. A fixture with Plan's installed build label queried its real Modrinth source: CURRENT was suppressed from console, and its download command was refused as no update. The three Geyser/Floodgate/Essentials web downloads and strict checksum override still passed. Both test servers exited with code 0 and prior config/JARs were restored.
- Update/join notifications now require a confirmed UPDATE result. Ambiguous labels and unknown web versions remain available as explicitly unconfirmed inspection downloads.
- Download freshness checks run before the file is accepted: exact installed bytes are rejected when the original JAR can be matched; numeric downgrades and same/older descriptor versions behind an advertised update are rejected. A different snapshot with an unchanged version label is not falsely claimed to be newer.
- Production files were only read for diagnosis; the running server and its configuration were not changed.

Corrected local JAR SHA-256: `5159cf2ede634be28fe9a6955e0b02fca7b06cc90f81945a2999dc0d1ef37801`.

---

# Development verification — 1.4.0-SNAPSHOT

Checked 6 October 2026 (Pacific/Auckland). This is an unreleased preview.

- Java 21 and Java 25 Maven `clean verify`: **118 tests passed**, zero failures, errors or skipped tests.
- Added 23 tests covering web/direct/extensionless sources, relative and redirected HTML links, bounded crawling, unsafe URLs/redirects, oversized pages, binary probing, provider fallbacks, checksum-policy scope, wrong-plugin/HTML/digest rejection, console filtering, and newest prerelease selection. Existing tests were updated for GitHub's release-list response and Modrinth's inclusive release policy.
- The Java 21 packaged JAR passed isolated Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25 checks: startup, scan/check/list/reload, console suppression of CURRENT/disabled rows, visible configuration warnings, real Geyser/Floodgate/Essentials downloads, preserved output filenames, release-type warnings and strict-web-checksum rejection.
- Live download checks used minimal installed fixture plugins with the expected names; downloaded upstream plugins were validated and saved, not installed or executed. This verifies PluginUpdateWatch's download flow, not those plugins' runtime compatibility. The fixture servers were stopped and their previous configs/JARs restored. Metrics submission was disabled.
- The supplied Geyser endpoint returned Geyser-Spigot 2.11.3-SNAPSHOT (build 1248), saved as `Geyser-Spigot.jar`. Floodgate returned 2.2.5-SNAPSHOT (build 141), saved as `floodgate.jar`. Essentials returned 2.22.1-dev+27-e70bdb8 and retained `EssentialsX-2.22.1-dev+27-e70bdb8.jar`.
- jsoup is bundled for HTML parsing; its MIT notice is retained at `META-INF/jsoup/LICENSE`. Network requests continue through the bounded HTTPS transport, never jsoup's network API.
- Arbitrary HTML pages cannot reliably identify newest files or Minecraft compatibility. Such files retain UNKNOWN status; scripts/login flows are not executed. A fixed artifact URL remains fixed. GitHub selection examines the 100 most recent API entries. New options default safely for existing provider configurations; the requested missing-checksum exception applies only to explicit web entries.
- Production DylyCraft was not started or modified for this verification.

---

# Published artifact verification — 1.3.3

Checked 2 October 2026 (Pacific/Auckland).

The GitHub release was published from commit `5f2e8c4ff6b9a1812c339dc36f6ad60cd0687ee0` before the documentation PR was merged. [Release CI](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs/36838508545) passed Java 21 and Java 25 builds and published the exact verified Java 21 JAR.

The downloaded GitHub asset, its checksum file, the JAR downloaded from Spigot, and the stable JAR installed in DylyCraft all match SHA-256:

`67bc4735a2880190f1e4071e9446e749dbd3f460729cf886551880152b80ed78`

The **exact published JAR** passed all isolated live checks on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25: startup, scan/check/list/reload, real Modrinth download with independent SHA-512 verification, missing-checksum rejection, cleanup preview/confirmation/backup, and disabled-plugin informational reporting. Both servers also passed the bStats global/local opt-out, single-client reload, opt-out thread termination and active-client shutdown checks. Both server processes exited with code 0. Test metrics were disabled or stopped before submission.

The stable descriptor, relocated bStats classes and bundled license were verified inside the published archive. DylyCraft was stopped when its installed JAR was checked; these tests did not start the production server.

The release notes now accurately describe malformed metrics settings: version 1.3.3 rejects an invalid YAML boolean. It does not implement a warning-only fallback. Earlier notes claiming that fallback were incorrect; no release artifact or tag was replaced.

---

# Build verification — 1.3.3

Checked 1 October 2026 (Pacific/Auckland).

- Final stable build: Java 21 Maven `clean verify` passed **95 tests**, zero failures, errors or skipped tests.
- Packaged descriptor declares version 1.3.3; bStats is bundled and relocated to `dev.updatewatch.lib.bstats`, with its MIT notice in `META-INF/BSTATS-LICENSE.txt`. No unrelocated `org/bstats/` classes are present.
- All six metrics lifecycle tests and existing provider, discovery, concurrency, integrity and cleanup tests pass.
- The final local stable JAR passed live checks on both documented Paper builds, including real Modrinth downloads, independent SHA-512 comparison, missing-checksum rejection, cleanup/backup and metrics opt-out/reload/shutdown checks.
- PR and release automation run the full suite on Java 21 and 25. The release workflow publishes its exact Java 21 artifact and matching SHA-256 file after both builds pass.
- README, wiki and release notes describe default-enabled bStats reporting, local/global opt-out, active-server count semantics and the 3–36 minute first dashboard delay.

Local stable JAR SHA-256: `10f2d24dee99b1230ef51618f997c6d054cc4ff98a0bad98f0f67c0cda2c2751`. CI packaging timestamps can produce a different archive hash; the release asset's own checksum is authoritative.

---

# Build verification — 1.3.3-SNAPSHOT (bStats)

Checked 1 October 2026 (Pacific/Auckland).

- Local Java 21 Maven `clean verify`: **95 tests, zero failures, errors or skipped tests**; shaded JAR packaging succeeded.
- Six new `MetricsControllerTest` tests cover the default for existing configs, explicit opt-out, rejection of malformed boolean values, no client creation while disabled, reload/disable/re-enable lifecycle without duplicate clients, and recovery after failed initialization.
- bStats Bukkit/base 3.2.1 are bundled and relocated to `dev.updatewatch.lib.bstats`. The packaged JAR includes the bStats MIT copyright/license notice. No separate bStats JAR is required.
- Registration was verified through the public bStats API: PluginUpdateWatch, ID 34400, owner dylyboo, Bukkit software ID 1.
- Isolated Paper 1.21.11 (Java 21) and Paper 26.3 (Java 25) checks verified the global opt-out, local opt-out, exactly one reporting thread when enabled, no additional thread on repeated reloads, and termination of the reporting thread after local opt-out. Update checks remained functional. See the live test report.
- Tests do not submit artificial usage to the public bStats dashboard. Successful public ingestion and dashboard counts are not claimed.
- This development build does not change `.github/release-version`; merging it does not automatically publish a stable release. Documentation describes the new telemetry default and opt-out, and records the required Spigot listing wording change before stable publication.

---

# Build verification — 1.3.2

Checked 30 September 2026 (Pacific/Auckland).

- Local Maven `clean verify` passed on Java 21: **89 tests, zero failures, errors or skipped tests**, followed by successful shaded JAR packaging.
- `ConfigCleanupTest` adds seven tests covering preview without mutation, exact backups, preservation of plugin files/data and settings, rejection of intervening config/file changes, case-insensitive existence checks, invalid rows/paths and unavailable directories.
- `DownloadFilenameTest` adds seven tests covering exact publisher names, plain fallback names, unsafe filename rejection, cross-plugin collision protection, safe same-plugin replacement, large descriptors and the descriptor size limit.
- Existing GitHub and Modrinth tests also assert that the selected provider filename is retained.
- Read-only verification against the supplied server's real EssentialsX descriptor (67,135 bytes) and mcMMO descriptor (110,525 bytes) passed both discovery and full JAR validation. Neither JAR was changed.
- CI runs the full Maven suite on Java 21 and Java 25. See [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for live command coverage and exact artifact evidence.

The added cleanup command only removes config rows after an explicit preview/confirmation and an exact backup. It does not infer old-to-new plugin identity, delete plugin data or automatically rewrite custom source URLs.

---

# Build verification — 1.3.1 and historical 1.3.0

Checked 27 September 2026 (Pacific/Auckland).

## Published 1.3.0

- [GitHub Release v1.3.0](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/tag/v1.3.0) identifies commit `4410b43a3aff6b904c9906cc75a97a2b6cc836dc`.
- All **72 tests** passed with Maven `clean verify` on Java 21 and Java 25 in [CI](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs/36248215330). Release automation also passed and published the verified Java 21 JAR with its SHA-256 checksum.
- Standard local Maven `clean verify` previously passed using JDK 21.0.12, including javac compilation, all 72 tests, JAR packaging and Gson shading.
- The 72-test revision also passed on Java 25.0.4.1 against the cached Paper `26.3.build.6-alpha` API; production source was separately compiled against that API with Java 21 bytecode using Eclipse ECJ.
- The exact GitHub Release JAR passed live startup, scan/check/list/reload, real Modrinth downloads with independently verified SHA-512, and missing-checksum rejection on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. Both isolated servers stopped cleanly with exit status 0. See [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for artifact-specific results.

Published JAR SHA-256:

```text
040847286752850f842df9966563742ebe3c5a36bcf10146c937a5f31516ae18
```

## 1.3.1

Version 1.3.1 adds a safeguard for JAR filenames that differ only by case. It blocks ambiguous source selection across manual entries, discovery and legacy fallback, and adds three regression tests. These changes are not present in the published 1.3.0 JAR.

- Standard Maven `clean verify` passed locally using JDK 21.0.12: **75 tests, 0 failures, 0 errors, 0 skipped**, followed by JAR packaging and Gson shading.
- The final locally packaged 1.3.1 JAR passed live startup, scan/check/list/reload, real Modrinth downloads with independently verified SHA-512, and missing-checksum rejection on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25. Both isolated servers stopped cleanly with exit status 0; see [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) for artifact evidence.
- The normal Maven dependency remains Paper `1.21.11-R0.1-SNAPSHOT`. CI is configured to run `mvn clean verify` with this dependency on Java 21 and 25.
- All prior tests remain in the suite. Local HTTP tests inject deterministic responses and waits to cover failures without depending on remote rate limits or outages.

## Test coverage

| Test class | Published 1.3.0 | 1.3.1 | Coverage |
| --- | ---: | ---: | --- |
| CheckStateTest | 2 | 2 | Reload invalidation, no overlapping checks, queued scans |
| DiscoveryTest | 15 | 18 | Configuration round trips, legacy sources, normalized matches, duplicate JAR ambiguity, mismatches, orphan files and invalid descriptors; follow-up adds case-colliding filenames across explicit/discovered/legacy sources |
| DownloadValidationTest | 5 | 5 | Paper/Bukkit descriptors, wrong plugin identity, non-plugin files and HTML |
| FailureTest | 7 | 7 | Network/provider/config categories, retry actions, malformed responses, unknown compatibility status, successful version states and redaction |
| HardeningTest | 14 | 14 | Source paths, config types/duplicates, safe filenames, limits, config preservation, GitHub digests, failed downloads and compatibility boundaries |
| HttpTransportTest | 14 | 14 | 429/all 5xx retries on each provider, 250/500 ms backoff, Retry-After delays, exhaustion, cooldowns, date rounding, overflow, cancellation and redirects |
| IntegrityPolicyTest | 6 | 6 | Required checksum defaults, explicit opt-out, missing hashes before network access, SHA-256/SHA-512 success, wrong hashes and artifact validation |
| ModrinthTest | 7 | 7 | Stable release and Minecraft/loader filtering, primary/ambiguous assets, URL parameters and checksums |
| VersionsTest | 2 | 2 | Version comparison |
| **Total** | **72** | **75** | **0 failures, 0 errors, 0 skipped in the verification runs described above** |

## Configuration and compatibility

Existing `updates` rows and legacy `plugins` entries remain supported. An existing explicit `downloads.require-checksum: false` remains supported; an omitted value requires a provider checksum in 1.3.0. This intentional safety change is documented in README and release notes. Opting out never bypasses a supplied mismatched checksum.

The API floor remains 1.21.11. Paper 1.21.11–26.3 is the target range, not a claim of live verification on every intermediate build. The live 26.3 test runtime is build 41 alpha; the earlier alternate-API compilation applies to cached build 6 alpha. Console checks do not cover player chat interactions, a long-running load test or every installed plugin combination.
