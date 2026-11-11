# PluginUpdateWatch 1.5.0 technical review

Reviewed 11 November 2026 from latest `origin/main` (`8dc3f5c`) on `codex/review-downloadall-logging`. Changes retain version 1.5.0, Java 21 bytecode compatibility and Java 25 runtime support. No dependencies, permissions, source-selection policies or automatic installation behaviour were changed.

## Confirmed defects and causes

| Finding | Cause and correction |
| --- | --- |
| Bulk summaries could disappear at shutdown | `shutdownNow()` discarded queued Runnables and Bukkit callbacks were skipped after disable. Track each admitted task, complete rejected/cancelled/finished work exactly once, and account for all items without waiting on the server thread. Late callbacks cannot release a newer operation's key. |
| Downloads could continue using old settings across reload | Only update checks had a generation gate; downloads held old source/security snapshots. Successful and rejected reloads now cancel outstanding downloads; fresh checks gate both commands. |
| Cached downloads could survive failed reload/check preflight | A disk-read/config mismatch returned without recording check failure; the single-download command had no cache gate. Record those failures and check configuration snapshots again on the I/O worker before each transfer. |
| Identical downloads were transferred and replaced repeatedly | The in-flight name set only prevented simultaneous attempts. Validate existing plugin identity and publisher checksum before transfer; recognize checksumless byte-identical files after validation. `ALREADY_DOWNLOADED` stays separate from `ALREADY_INSTALLED`. |
| Logging levels did not match message severity | Console `sendMessage` rendered error text at INFO; player failures used `logger.warning`, even for ERROR; RCON was treated as the local console for audit routing. Use one local logger route, WARNING for expected checksum absence, SEVERE for genuine errors, and a separate RCON response plus one audit entry. |
| Bulk warnings differed by command audience | Console bulk downloads omitted preflight warnings; player warnings were spread across several messages. Use one consolidated per-file security/release warning for every audience. |
| Scheduled scans replayed unrelated warnings and admin alerts | Any changed result/note replayed the entire console report and notified players while any update existed. Compare issues independently, retain summary counts, report recovery/recurrence, and notify admins only about newly changed confirmed updates. |
| Interruptions could appear as network errors | Explicit NETWORK wrappers took priority over their interruption cause. Cancellation now retains its classification; socket timeouts remain network failures. |
| HTTP identification was stale | The User-Agent advertised `1.4.0-SNAPSHOT` and the former repository. Filter build metadata from Maven; identify the current repository. Troubleshooting links now agree with `mkdocs.yml`'s current site URL. |
| Shared project metadata was fetched repeatedly | No exact-URL metadata reuse across plugins/discovery in one scan. Memoize success/failure per check only, return defensive JSON copies, and retain fresh requests on subsequent checks. |

Local console download warning duplication was **not** confirmed as a blanket v1.5.0 defect: existing `!console` guards already avoided many `sendMessage`/logger duplicates. Player messages and their console audit copies are distinct audiences. Confirmed problems were inconsistent severity, RCON audit omission, fragmented preflight warnings and replaying unchanged scan issues. The new route makes one local entry explicit and regression-tested.

## Update detection review

- Modrinth retains Paper/Spigot/Bukkit loader filtering, listed-release selection, exact Minecraft labels for disabled plugins and explicit compatibility warnings for enabled plugins with unlisted game versions. This is the existing documented policy, not evidence that an update works on Paper 26.3.
- GitHub retains newest published non-draft release selection among the requested 100 entries, prerelease reporting, unique safe asset selection and digest enforcement. Ambiguous/missing assets remain manual downloads.
- Spiget checks resource and latest-version metadata; premium/external resources remain manual. Missing checksums do not disable integrity policy. Provider availability/rate limits never establish CURRENT.
- Geyser endpoints retain pinned build download URLs and SHA-256 metadata. Generic snapshots with indistinguishable version labels cannot prove a newer build; explicit inspection is still available.
- Explicit web sources retain bounded crawling, HTTPS/public-host validation, UNKNOWN version reporting and their configured checksum exception. UNKNOWN/DIFFERENT results never enter the bulk plan.
- Version/build normalization and installed-file checksum comparison already prevented several false positives; no unsupported version-order changes were introduced. Provider publication dates are not proof of Minecraft compatibility or artifact freshness.

## Supplied server log

Analyzed all 1,090 lines of `latest.log`; SHA-256 `56531d288dc460b6d43ab5f8c2e8437f143493095bdb1c9e827753720427a6d3`. Raw log was not committed. It records Paper 26.3 build 246, Java 25 and 63 initialized plugins. PluginUpdateWatch loads (line 164), enables (592) and disables (979), without a directly attributed exception. There are no PluginUpdateWatch scan/download report lines and **no recorded `/pu downloadall` execution**. Shutdown starts at 12:47:09 (890). The initiating cause is not established by this log; the recorded system stop message is not proof of a plugin crash.

| External problem and evidence | Recommendation outside this PR |
| --- | --- |
| Duplicate WorldEdit/FAWE identity (26–28) | Keep one WorldEdit implementation and cold-restart. FAWE's installation guide explicitly says it replaces WorldEdit: [official guide](https://intellectualsites.gitbook.io/fastasyncworldedit/getting-started/installation). |
| WorldGuard `YAMLProcessor` NoSuchMethodError (239–240), then shutdown NPE (259–260) | Resolve the duplicate implementation first, then verify a mutually compatible WorldGuard/WorldEdit-or-FAWE set for this Paper version. Linkage mismatch is confirmed; the exact compatible replacement versions are not established. See [WorldGuard troubleshooting](https://worldguard.enginehub.org/en/latest/faq/). |
| Jobs WorldGuard class-loading failure (488–508), skript-worldguard disables (562) | Recheck integrations after WorldGuard successfully enables. These are downstream WorldGuard failures, not updater exceptions. |
| GeyserDisplayEntity `JAVA_ENTITY_TYPES` NoSuchFieldError (799–800) | Use an extension build compatible with the installed Geyser API, or disable that extension pending a matching build. Exact replacement versions require publisher verification. |
| CreativeManager cannot find `ReadWriteItemNBT` (726) | Verify its required NBTAPI dependency and classpath/load ordering. The log proves a missing class, not which dependency version is sufficient; consult its publisher and the [NBT-API dependency guidance](https://github.com/tr7zw/Item-NBT-API/blob/master/wiki/Using-Gradle.md). |
| ClearLag internal TPS/update-check version parsing fails (736–744) | Obtain a build supporting the server's version format or report the parser failure. Its “bukkit may be down” warning is not evidence of provider downtime. |
| ProtocolLib and ViaVersion compatibility warnings (100–101, 234–237) | Verify publisher support for this exact server/protocol version before relying on their integrations. |
| MyFurniture premium/config limits (422–425), corrupt `golden_sink.png` (430–431) | Correct furniture configuration to match the licensed feature set; replace the invalid PNG and revalidate the resource pack. |
| BetonQuest FancyHolograms integration policy mismatch (859), async task accesses closed JAR during shutdown (899–925) | Align supported integration versions and report shutdown task cleanup to BetonQuest. The closed-JAR error occurs after stopping starts; it is not evidence of a PluginUpdateWatch failure. |
| Other warnings | Floodgate locale fallback (104), legacy plugin API metadata (87–88), TimedFlyX existing lang file (554), BigDoors/Geyser environment (766), GravesX/PlugManX unsupported environment (640–641) require their own configuration/support review. Do not bypass compatibility protections merely to remove warnings. |

PluginUpdateWatch can report discovery ambiguity, missing sources, provider failures and unverified updates. It cannot repair external plugin dependency graphs, binary APIs, licensing, resource packs or external task shutdown. No unrelated plugin files/settings were modified.

## Downloadall verification

The plan admits only confirmed UPDATE results; manual-only, checksum-blocked and active items are counted separately. Name admission is case-insensitive and serial on the server thread. Transfers, existing-file checks, hashing and archive validation remain on one background worker; a failed item does not stop later jobs. Each accepted file retains HTTPS checks, size/deadline limits, publisher checksum enforcement, full archive CRC/path/descriptor/main-class validation, plugin identity and freshness checks. Temporary files are deleted by the download operation on controlled failure. Colliding output names cannot overwrite an unrelated validated plugin. Files remain in PluginUpdateWatch's downloads directory for manual installation.

Bulk completion includes processed/total and separate saved, already saved/installed/not-newer, failed and cancellation-requested counts. Cancellation reports requests honestly: a blocking OS/network operation may take until timeout to respond, and a final file move already in progress may finish. Abrupt JVM/OS termination cannot guarantee temporary-file cleanup. No synchronous executor join or network request was added to the server thread.

## Modified files

All Java paths below are relative to `src/main/java/dev/updatewatch/` unless stated otherwise.

| Files | Purpose |
| --- | --- |
| `UpdateWatch.java` | Integrate lifecycle tracking, cache gates, audience routing, changed-issue reports, debug diagnostics and fresh admin notifications. |
| `DownloadQueue.java`, `BulkProgress.java` (new) | Testable exactly-once admission/completion and bulk cancellation accounting. |
| `DownloadCache.java` (new), `BulkDownloadPlan.java` | Cache eligibility and duplicate-name selection. |
| `DownloadManager.java` | Verified existing downloads, filename ownership preflight, interruption checks and unchanged-file detection. |
| `Failure.java`, `Remote.java`, `Troubleshooting.java` | Missing-checksum/duplicate/cancellation classification and current help links. |
| `MessageLog.java`, `ScanMessages.java` (new), `ConsoleReport.java`, `ReportVisibility.java` | Severity, one console route, state-based issue reporting and update-only notifications. |
| `HttpTransport.java`, `src/main/resources/build.properties` (new) | Maven-derived request identification. |
| `Providers.java`, `UpdateCheckService.java` | Per-check exact-URL metadata memoization and optional provider diagnostics. |
| Tests: `BulkDownloadPlanTest`, `DownloadFilenameTest`, `HardeningTest`, `HttpTransportTest`, `IntegrityPolicyTest`, `PackagedDependenciesIT`; new `DownloadQueueTest`, `ReviewRegressionTest` | Regression coverage for selection, security, collisions/duplicates, interruption, cancellation/rejection/late callbacks, continuation, progress, stale caches, log routing/levels, warning recurrence/recovery, notification isolation, metadata reuse and packaged build identification. |
| `docs/downloads.md`, `docs/update-checking.md`, `docs/troubleshooting.md`, `REVIEW-1.5.0.md` | Describe final behaviour, external findings and verification limits. |

## Validation and remaining limits

| Validation | Result |
| --- | --- |
| `mvn --batch-mode --no-transfer-progress clean verify`, Java 25.0.4.1 | PASS: 189 unit tests plus 1 packaged-JAR integration test; no failures/errors/skips. |
| Same Maven verification, Java 21.0.12 | PASS: 189 unit tests plus 1 packaged-JAR integration test; no failures/errors/skips. |
| `mkdocs build --strict --site-dir .work/site` (Material 9.7.7) | PASS. |
| `git diff --check` | PASS. |

This adds 20 unit tests to the original 169 and extends packaged resource verification. The first development verification caught an existing integrity-test fixture reusing the same output filename for two provider cases; after duplicate avoidance was implemented, the second case correctly returned ALREADY_DOWNLOADED. The fixture now isolates providers and dedicated tests cover duplicates. All final checks pass. Automated tests use local fixtures/injected transports; none rely on live provider APIs. Maven's dependency resolution is separate from test networking.

No new Paper server session was run during the initial code review. Follow-up live Paper testing and the shutdown temporary-file recovery fix are recorded in [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md). The following limitations describe the initial review scope. Actual player delivery, asynchronous RCON command response lifetime, Paper scheduler/disable ordering, and the 63-plugin startup/shutdown interaction remain live integration checks. Existing historical live-test reports concern earlier artifacts and do not certify this patch. The pure Java queue/dispatcher tests simulate callback ordering and active/queued cancellation, not a live Bukkit scheduler.

The existing dependency relocation/package integration test remains required. Existing Maven shade overlaps (manifest and multi-release module metadata) are not Minecraft runtime exceptions. No Maven dependency upgrade was justified by the supplied log.

A separate repository maintenance concern remains: legacy `release.ps1` broadly stages the worktree and pushes `main`. It was inspected but not invoked or changed by this runtime-focused review; prefer the reviewed release workflow or narrow its staging/branch checks in a separate maintenance change. No releases, tags or installed server artifacts were changed.
