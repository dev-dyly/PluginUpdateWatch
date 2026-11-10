# Live integration verification — PR #15

Tested **11 November 2026 (Pacific/Auckland)** on an actual, isolated Paper server. Native-provider and deterministic-provider phases below are distinct. No mocked Bukkit server was used. Historical reports are retained after this section.

## Environment and artifact identity

| Item | Actual value |
| --- | --- |
| Paper | **26.3 build 246**, `b31b4b5`, API `26.3.build.246-stable` |
| Java | Amazon Corretto **25.0.4.1+8-LTS**, Windows 11 amd64 |
| Runtime | `-Xms384M -Xmx1536M`, fresh flat world, view/simulation distance 2, whitelist, online mode |
| Dedicated directory | `C:\Users\dtand\Documents\ChatGPT\PluginUpdateWatch-review\.work\live-pr15-20261111\server` |
| Bindings | Minecraft `127.0.0.1:25595`; RCON `127.0.0.1:25596`; controlled fixture HTTP `127.0.0.1:25597` |
| Credentials/data | Random test-only RCON credential in ignored local files; no production credentials/configuration/world/player data copied. LuckPerms generated a new local H2 database. Metrics disabled. |
| EULA | Operator explicitly authorized reuse of the existing accepted EULA file for isolated testing in this chat. |
| Original reviewed source | `0d0b836e6690d477cd8a57b1590eea132b640f86`, initial JAR SHA-256 `e40266686dc45570183a470261cc772c2784d2a4ac6d8f72b19e1e7fd2b58268` |
| Recovery fix source | **`da7388b98231d630cec0843a30320bc0e9ffb88a`**, version remains 1.5.0 |
| Fixed controlled-suite JAR | SHA-256 `b7e2f603e5e0230f083aa37ae1dbfa261d0c80d0460bd5da74c74d8b177ff367` |
| Final Maven-built JAR, retested with native HTTPS | SHA-256 `fe167e5f1fc95c2ecab2089fe046165e2d0743438b1dfcbe959e69d068024c14`; identical committed source, different packaging timestamps |

The existing production Paper launcher and vanilla cache were copied read-only. Paper identified itself as build 246 in each test process. Its launcher SHA-256 is `d158542561e440c4026be7ccb32f551ee88313864792f38ea0ce5d1b88f1b27c`. Generic dependency libraries came from the previous isolated test directory. Only test directories were written. The production DylyCraft server was not started, stopped, restarted or reconfigured; no production plugin files were changed. All test Paper processes and the fixture listener were stopped after testing.

## What was executed

1. **Minimal native Paper boot:** PluginUpdateWatch alone generated its default config, enabled successfully, ran the actual scheduled startup scan and stopped cleanly.
2. **Native HTTPS/provider phase:** Real LuckPerms 5.5.53 plus four small metadata fixture plugins loaded in real Paper. Native Modrinth hash discovery identified LuckPerms; real Spiget and GitHub endpoints were queried. LuckPerms GitHub returned no published releases and remained unknown, not CURRENT. Actual HTTPS downloaded LuckPerms 5.5.71, independently matched its published SHA-512, and left the installed 5.5.53 JAR unchanged.
3. **Controlled-provider phase:** Eighteen harmless installed fixture plugins ran in actual Paper. A test-JVM-only URL handler forwarded designated fixture provider requests to a loopback HTTP server. The distributable PluginUpdateWatch JAR was unchanged by that handler. Deterministic GitHub/Modrinth/Spiget/Geyser/web responses exercised real commands, scheduler callbacks, worker tasks, retries, streaming socket reads, checksums, archive validation, file moves, reload and shutdown. **Remote provider responses/TLS were simulated in this phase; it does not verify third-party outages, real HTTPS cancellation or authenticated players.** All captured updater fixture requests originated on `PluginUpdateWatch-IO`.
4. **After the recovery fix:** Rebuilt and reran the full controlled suite, recovered an actual residual partial file on live startup, then rebuilt again and reran native scan/check/downloadall/duplicate/shutdown against real Modrinth with the final artifact above.

Commands actually issued through local console input and/or isolated authenticated RCON:

```text
pu scan                         # repeated local-console scans
pu check                        # repeated console/RCON checks, including failure/recovery
pu list
pu stats
pu downloadall                  # normal, duplicate, concurrent, stale-config, cancellation cases
pu download LiveNoHash
pu download LiveWeb
pu download LiveSlow
pu reload                       # valid, invalid YAML, invalid source schema, active downloads
list                            # main-thread heartbeat during socket timeouts
tps
mspt
minecraft:tick query
minecraft:tick sprint 18120      # advances a real scheduled 15-minute interval; test process only
stop                            # includes stop with active/queued downloads
```

Only test configuration files were edited for stale/invalid cases. Test downloads were never installed as plugins.

## Observed test matrix

| Scenario | Actual result | Status |
| --- | --- | --- |
| Minimal startup/default config/scheduled scan/disable | Version 1.5.0 enabled, config generated, scheduled summary emitted, normal shutdown | PASS — native Paper |
| Installed identity and source discovery | Real LuckPerms name/version matched; Modrinth project `Vebnzrzj` persisted; GitHub/Spigot website fallback and blank unresolved sources observed | PASS — native Paper/APIs |
| Repeated explicit scan/check | Commands completed consistently; explicit console requests returned full requested details | PASS — native + controlled |
| Repeated automatic report | Actual Paper tick sprint crossed the scheduled interval; one summary, no repeated unchanged missing-source/update detail | PASS — controlled live Paper |
| Provider detection | Native Modrinth/Spiget queried; native GitHub empty-release condition handled. Deterministic success paths additionally covered GitHub/Modrinth/Spiget/Geyser/web | PASS — scopes as stated |
| Multiple updates/current/manual/no-checksum | 12 confirmed updates: 1 manual-only + 2 checksum-blocked; 9 eligible. CURRENT and UNKNOWN fixtures excluded | PASS — controlled live Paper |
| Mixed successful and failed bulk queue | **9/9 processed: 5 saved, 4 failed**. Failure types: HTTP 503, invalid JAR, wrong checksum, conflicting filename. Remaining jobs continued | PASS — controlled live Paper |
| Existing saved artifacts/repeated bulk | **9/9 processed: 0 saved, 5 already saved/installed/not newer, 4 failed**. Saved file hashes unchanged | PASS — controlled live Paper |
| Filename conflict | `shared.jar` retained CollisionB's verified bytes; CollisionA could not overwrite it | PASS — controlled live Paper |
| Missing required checksum | Direct attempt emitted WARNING and remained blocked; bulk skipped two blocked releases | PASS — controlled live Paper |
| Explicit web inspection | UNKNOWN web source excluded from bulk; explicit download accepted a valid newer plugin JAR with checksum warnings | PASS — controlled live Paper |
| Native successful download | Final artifact downloaded real LuckPerms; **1/1 saved**, independent SHA-512 match; repeat reported ALREADY_DOWNLOADED without replacement | PASS — native HTTPS |
| Concurrent bulk/single command | During an active slow transfer, second bulk reported 3 already running and queued none; single reported already running | PASS — controlled live Paper |
| Reload cancellation | 3/3 accounted once: 0 saved, 0 failed, 3 cancellation requests. Temporary file gone before subsequent checks; checks remained usable | PASS — controlled live Paper |
| Shutdown cancellation | Actual stop with active/queued work accounted all 3 once; no server-thread wait. Pre-fix run exposed an orphan partial file; see fix below | PASS for accounting; recovery required for leftovers |
| Recovery of real shutdown residual | Next live startup removed the observed 2 KiB partial; retained saved sentinel JAR and unrelated `.tmp`. Final fixed controlled stop left no transfer temp files | PASS — live startup and regression tests |
| Edited config without reload | Candidates could be queued from cache, but every work item rejected the config snapshot **before any new provider/download request**; no file accepted | PASS — controlled live Paper |
| Invalid YAML/rejected reload | ERROR response, cached downloads blocked; restoring/reloading/checking restored successful duplicate-safe downloads | PASS — controlled live Paper |
| Failed source-schema check | Valid YAML with invalid `updates` shape failed the actual async scan/check; downloads remained blocked | PASS — controlled live Paper |
| HTTP 429/cooldown | One fixture GitHub request returned 429/Retry-After; later same-host source used cooldown without a second request; Modrinth still worked; failed status never became CURRENT | PASS — controlled HTTP in live Paper |
| Socket failure/retry/debug | Fixture header stall caused bounded socket read retries and NETWORK_ERROR; debug stack identified worker thread; simultaneous `list` response remained responsive | PASS — controlled HTTP in live Paper |
| Console severity/duplication | INFO saves/summaries; WARN missing checksum/cancellation; ERROR invalid config/HTTP/archive/hash/identity. One local audit line per download outcome and one bulk completion per request | PASS — live console logs |
| RCON sender delivery | Real TCP authentication and synchronous list/stats/warnings/queue acknowledgements observed | PASS — isolated real RCON |
| RCON delayed completion payload | Paper's response window ends when the command returns; delayed completions were observed in console, not as unsolicited RCON packets. Retrieve later with list/stats | LIMITATION — not claimed as delivered |
| Administrator player/chat/clicks/permissions | No authenticated Minecraft test account was supplied or used | NOT RUN |
| Installed files/manual installation | SHA-256 snapshot of all installed test plugin JARs unchanged throughout controlled commands; downloaded updates remained under the data-folder downloads directory | PASS — live filesystem checks |
| Production 63-plugin setup/player load | Deliberately not run or modified | NOT RUN |

## Confirmed defect and fix

A repeated **graceful** shutdown with a throttled socket read left `.download-7300461657494971338.tmp` containing 2,048 bytes. Cancellation summaries were correct, but `shutdownNow()` only interrupts the daemon worker: the JVM can exit before the stream returns and the download's `finally` removes the temp file. Immediate removal would require waiting for I/O during server shutdown.

Commit `da7388b` adds asynchronous startup recovery **as the first job on the same I/O worker**, before any new transfer. It removes only regular files matching the reserved `.download-<number>.tmp` pattern, skips directories/symbolic-link entries, retains saved JARs/unrelated files and reports cleanup errors. No security policy or installation behaviour changed. `DownloadRecoveryTest` adds two regressions covering ownership/retention/idempotence and a fresh installation without a downloads directory.

The exact residual file was left in place, the fixed build started in actual Paper, and its recovery message was observed. The full controlled suite then passed with the fixed build; the final native rebuild also passed real download/duplicate/shutdown tests. **Immediate cancellation cleanup is still best effort; recovery happens on the next normal startup if the JVM exits first.**

Modified follow-up files: `DownloadManager.java`, `UpdateWatch.java`, new `DownloadRecoveryTest.java`, download/troubleshooting documentation, and the earlier review's link to this follow-up. Report/evidence files add no runtime dependencies.

## Performance observations

- Native initial representative boot: Paper's `Done` reported 11.900 s. Final native boot completed successfully; actual process-to-ready measurement was 7.39 s on the warmed isolated runtime.
- Final native TPS: **20.0 / 20.0 / 20.0**. Tick query: **0.3 ms average**, P50 0.1 ms, P95 0.6 ms, P99 4.9 ms (100 samples). MSPT last 5 s: avg/min/max **0.4 / 0.1 / 4.9 ms**. This is an empty test-world sample, not a load benchmark.
- During controlled timeout/download activity, RCON heartbeat requests completed in **0–16 ms** on the final run. A thread dump showed `PluginUpdateWatch-IO` reading a socket while `Server thread` was parked waiting for its next tick, not blocked on updater network I/O. See the attached thread sample.
- Fixed-run G1 heap snapshot: **373 MiB used, 445 MiB committed**, within the 1,536 MiB cap (`jcmd GC.heap_info`, retained locally). No player-load or long-duration leak guarantee is made.
- Tick sprint deliberately inflates rolling TPS above 20 in that controlled phase; those rolling TPS values are not presented as normal performance. The earlier long idle gap while this chat awaited continuation is not claimed as a monitored soak test.

## Exceptions, warnings and harness corrections

Expected updater ERROR/WARN output was deliberately induced for bad configuration, 429/503 responses, socket timeouts, invalid archives, digest mismatch, filename conflict, unverified web downloads and cancellation. No unexpected PluginUpdateWatch initialization/shutdown exception was observed.

The host's **OSHI/Windows Perflib 009** error/warning appeared before plugin enable, including native runs. Paper also reported its installed build was two builds behind and used spark's Java profiler fallback on Windows. These were not attributed to PluginUpdateWatch or suppressed.

An early test URL handler did not implement the proxy overload used by Mojang authlib, producing `UnsupportedOperationException`/authentication-discovery warnings in the initial controlled run. The harness proxy forwarding was corrected and the full controlled phase rerun; those authlib warnings were absent in final controlled/native logs. An initial harness import attempted to relocate test JARs already open by Paper and was corrected. An assertion also incorrectly expected an unchanged network error's full detail to repeat, despite the intended summary-only behaviour; it was corrected after confirming the initial ERROR remained available. These harness failures are not represented as plugin defects or successful tests.

The first baseline launcher used closed stdin, causing terminal prompt repetition in its console capture; later runs kept a real console pipe open. This was a launcher issue, not plugin logging duplication. Complete raw console output and `latest.log` captures remain in the ignored test directory; only focused sanitized evidence is committed. No RCON credentials or server configs are committed.

## Automated verification after the fix

- `mvn --batch-mode --no-transfer-progress clean verify` on Java 25: **PASS — 191 unit tests + 1 packaged-JAR integration test, zero failures/errors/skips**. Run both before the controlled retest and again before the final native retest.
- `mkdocs build --strict --site-dir .work/site`: PASS.
- `git diff --check`: PASS.
- GitHub Actions Java 21/25 results are visible on PR #15 and were checked after pushing this follow-up.

Automated tests remain separate from the actual Paper runs above. Maven dependency resolution uses the network; unit tests use local fixtures/injected transports.

## Sanitized evidence and retained raw captures

- [Native provider discovery/download log](test-evidence/pr15/native-providers.log)
- [Fixed controlled-provider commands and outcomes](test-evidence/pr15/controlled-providers.log)
- [Shutdown residual and actual startup recovery](test-evidence/pr15/shutdown-recovery.log)
- [Final native artifact smoke/download/duplicate/shutdown](test-evidence/pr15/final-native.log)
- [Live worker/server thread sample during a transfer](test-evidence/pr15/worker-thread-sample.txt)

Full raw captures are local, under `.work/live-pr15-20261111/`: phase-specific `*-console.log`, `*-latest.log`, JSON observations, HTTP fixture request records, thread/heap samples, and exact tested artifacts. These are ignored rather than shipped as server files. Raw final `controlled-latest.log` SHA-256: `b63e883fca89002f27880d8cd568142f6d1eb9aaed2139e67d65def73c93d500`; raw `final-native-latest.log`: `0bb33c56a45536589b34655ce3bccae4fa45f22ac2706eed6a421244e41370c1`.

Final real LuckPerms downloaded artifact SHA-512 (independently matched against Modrinth):

```text
188a91f0a543d23bfda32385fca6db63d61e49c8a422bd452a260bd9cbc6a7d7fe45071199e9fca8f3ce43c2b41ee84fd315bd15464577028ff3951a7d4fab27
```

PR #15 remains open and unmerged. No production update or release was installed.

---

# Exact published JAR verification — 1.3.3

Checked 2 October 2026 (Pacific/Auckland).

The GitHub release was published from commit `5f2e8c4ff6b9a1812c339dc36f6ad60cd0687ee0` before the documentation PR was merged. [Release CI](https://github.com/dylantanderson92-lang/PluginUpdateWatch/actions/runs/36838508545) passed Java 21 and Java 25 builds and published the exact verified Java 21 JAR.

The downloaded GitHub asset, its checksum file, the JAR downloaded from Spigot, and the stable JAR installed in DylyCraft all match SHA-256:

`67bc4735a2880190f1e4071e9446e749dbd3f460729cf886551880152b80ed78`

The **exact published JAR** passed all isolated live checks on Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25: startup, scan/check/list/reload, real Modrinth download with independent SHA-512 verification, missing-checksum rejection, cleanup preview/confirmation/backup, and disabled-plugin informational reporting. Both servers also passed the bStats global/local opt-out, single-client reload, opt-out thread termination and active-client shutdown checks. Both server processes exited with code 0. Test metrics were disabled or stopped before submission.

The stable descriptor, relocated bStats classes and bundled license were verified inside the published archive. DylyCraft was stopped when its installed JAR was checked; these tests did not start the production server.

The release notes now accurately describe malformed metrics settings: version 1.3.3 rejects an invalid YAML boolean. It does not implement a warning-only fallback. Earlier notes claiming that fallback were incorrect; no release artifact or tag was replaced.

---

# Live verification — 1.3.3

Checked 1 October 2026 (Pacific/Auckland), using the final local stable JAR.

| Server | Runtime | Result |
| --- | --- | --- |
| Paper 1.21.11 build 132 | Java 21 | Passed |
| Paper 26.3 build 41 alpha | Java 25 | Passed |

Both servers passed startup, `pu scan`, `pu check`, `pu list`, `pu reload`, and a real `pu download LuckPerms` from Modrinth with independent SHA-512 verification. Missing-checksum downloads were rejected without changing output files. Cleanup preview, confirmation and exact config backup passed; deliberately disabled plugins appeared once as informational entries.

The metrics-specific run verified global opt-out with the local setting absent, local opt-out while globally enabled, exactly one reporting thread after enabling and repeated reloads, thread termination after opting out, update checks after opt-out, and normal shutdown with the client active. Both processes exited with code 0.

These checks used isolated localhost-only servers and dedicated test worlds. Public bStats submissions were disabled or the server was stopped before the initial reporting delay, avoiding artificial usage reports.

Local stable JAR SHA-256: `10f2d24dee99b1230ef51618f997c6d054cc4ff98a0bad98f0f67c0cda2c2751`.

Separately, the user's existing snapshot server console recorded a successful bStats submission after the empty-server pause. Public dashboard/API availability was not used as a release gate, and dashboard counts were not independently confirmed.

---

# Live verification — bStats development build

Checked 1 October 2026 (Pacific/Auckland), using the final local `PluginUpdateWatch-1.3.3-SNAPSHOT.jar`.

Paper 1.21.11 build 132 / Java 21 and Paper 26.3 build 41 alpha / Java 25 both passed:

- Plugin startup with bundled, relocated bStats 3.2.1.
- Shared bStats opt-out: no reporting thread when globally disabled, even with the new local setting absent.
- Plugin-specific opt-out: no reporting thread when `metrics.enabled: false`, even when globally enabled.
- Enabling metrics: exactly one bStats reporting thread.
- Repeated `/pu reload`: still exactly one reporting thread.
- Disabling metrics through config and `/pu reload`: reporting thread terminates.
- `/pu check` still completes after metrics opt-out.
- Normal server stop with the metrics client active: both server processes exit with code 0.

The checks used the existing isolated verification servers under `.work/live`, not the user's production server. The test JAR and metrics/config files were restored afterward. Tests finished before bStats' first scheduled submission; no public dashboard ingestion or installation count is claimed.

Artifact SHA-256: `170aadf346fb206510fdfd471512f3640acd2afbfaaf5b4f08b6874834e3c849`

---

# Live verification — 1.3.2

Completed 30 September 2026 (Pacific/Auckland), using isolated localhost-only Paper servers and dedicated test worlds.

| Runtime | Startup and commands | Exact publisher filename + SHA-512 | Cleanup preview/backup/confirmation | Disabled status and single console report | Shutdown |
| --- | --- | --- | --- | --- | --- |
| Paper 1.21.11 build 132 / Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Paper 26.3 build 41 alpha / Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |

The final local 1.3.2 JAR was tested on both servers. Commands covered startup discovery, `pu scan`, `pu check`, `pu list`, `pu download LuckPerms`, `pu cleanup`, `pu cleanup confirm` and `pu reload`.

- A real Modrinth download retained exactly `LuckPerms-Bukkit-5.5.71.jar`. Independent SHA-512 verification matched Modrinth metadata; the installed LuckPerms 5.5.53 JAR was unchanged.
- A seeded missing-file entry was reported once. Cleanup preview left config unchanged; confirmation removed only that entry and saved the complete original config in a backup before a successful fresh scan.
- A real Spigot check still reported an update, while downloading without a checksum remained blocked without creating/changing output files.
- With LuckPerms deliberately disabled, the report showed zero unresolved plugins and one intentionally disabled plugin. The disabled message appeared once with no warning prefix.
- The supplied server's EssentialsX and mcMMO JARs separately passed read-only discovery and full archive validation using the larger descriptor bound. No live-server config cleanup or installed-plugin replacement was performed on the user's server.

Final local 1.3.2 JAR SHA-256:

```text
a4b08fca648b07203051378d2cfa31196d424ba05e96122ab4aeb03788d37341
```

CI rebuilds the reviewed source and publishes its exact Java 21 artifact with its own checksum; the local test hash can differ due to build timestamps/toolchains. The 26.3 runtime is an alpha build. Console coverage does not establish in-game chat interaction, long-duration performance or compatibility with every intervening Paper build. Cleanup deliberately does not infer which new JAR an old missing filename belonged to.

---

# Live verification — 1.3.0 and 1.3.1

Completed 27 September 2026 (Pacific/Auckland). The exact published 1.3.0 JAR and the final locally packaged 1.3.1 JAR were each exercised on two real, isolated Paper servers with dedicated test worlds and localhost-only ports. Version 1.3.1 includes the additional filename-collision safeguard. The 26.3 runtime was copied from the supplied DylyCraft installation; the user's server and worlds were not modified or stopped.

| Plugin artifact | Runtime | Startup | Scan/check/list/reload | Modrinth download + SHA-512 | Missing checksum rejection | Graceful stop |
| --- | --- | --- | --- | --- | --- | --- |
| Published 1.3.0 | Paper 1.21.11 build 132 (`c5eb079`), Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Published 1.3.0 | Paper 26.3 build 41 alpha (`a15fed9`), Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |
| Final local 1.3.1 | Paper 1.21.11 build 132 (`c5eb079`), Java 21.0.12 | Passed | Passed | Passed | Passed | Exit 0 |
| Final local 1.3.1 | Paper 26.3 build 41 alpha (`a15fed9`), Java 25.0.4.1 | Passed | Passed | Passed | Passed | Exit 0 |

## Checks performed for each artifact and server

1. Started Paper with the indicated PluginUpdateWatch version and the original, unmodified LuckPerms 5.5.53 JAR. Confirmed both plugins enabled successfully.
2. Used an older-style configuration without `downloads.require-checksum`. Automatic discovery identified LuckPerms by its published Modrinth file hash and persisted the correct source link.
3. Ran `pu scan`, `pu check` and `pu list` through the server console. Confirmed the compatible stable release `v5.5.71-bukkit` was shown. Because that publisher label contains a suffix, the plugin correctly displays `NOT_CURRENT` with a request to verify version ordering rather than asserting a numeric comparison.
4. Ran `pu download LuckPerms`. The real Modrinth download completed with the plugin's `Checksum verified` message. Independently computed SHA-512 of the downloaded file and compared it with the [Modrinth version API](https://api.modrinth.com/v2/version/b0mk8uS6).
5. Confirmed the installed LuckPerms JAR was unchanged; the downloaded update was stored only inside PluginUpdateWatch's downloads folder.
6. Changed LuckPerms' source to [Spigot resource 28140](https://www.spigotmc.org/resources/luckperms.28140/) and ran `pu reload`. The subsequent list still showed the update but explained that its missing checksum blocked automatic download.
7. Ran `pu download LuckPerms` again. Confirmed the actionable `PROVIDER_ERROR` / `Download blocked` message, no downloaded file added or changed, and no leftover temporary file. This also verified the strict default with the checksum setting absent.
8. Stopped each test server through its console and confirmed exit status 0.

## Artifact evidence

Published [PluginUpdateWatch 1.3.0 JAR](https://github.com/dylantanderson92-lang/PluginUpdateWatch/releases/download/v1.3.0/PluginUpdateWatch-1.3.0.jar), from release commit `4410b43a3aff6b904c9906cc75a97a2b6cc836dc`, SHA-256:

```text
040847286752850f842df9966563742ebe3c5a36bcf10146c937a5f31516ae18
```

Final locally built 1.3.1 JAR SHA-256:

```text
26a2253deb19a654412f2dc71beab6ac4284a8a0b600ba54180c16313d0e00ac
```

Downloaded LuckPerms 5.5.71 JAR SHA-512, equal in all four runs and equal to the provider's hash:

```text
188a91f0a543d23bfda32385fca6db63d61e49c8a422bd452a260bd9cbc6a7d7fe45071199e9fca8f3ce43c2b41ee84fd315bd15464577028ff3951a7d4fab27
```

The complete procedure was run separately for each artifact. Release automation builds the reviewed 1.3.1 source again and publishes that run's exact Java 21 JAR and checksum. Toolchains and build timestamps can make its hash differ from the local pre-release JAR recorded here.

## Scope

These are live server-console and real provider checks, not an in-game player/chat-click test, a performance soak, or a claim covering every intermediate Paper build. The Paper 26.3 build tested is an alpha. No downloaded update was installed into the user's server. Injected checksum mismatch, HTTP 429/5xx retries, malformed archives and discovery ambiguity are covered by automated tests; these live runs did not alter a remote provider or deliberately corrupt its production files. The filename-collision fix is covered by three new regression tests. See [BUILD-REPORT.md](BUILD-REPORT.md) for the 1.3.0 72-test suite and 1.3.1 75-test suite, and [COMPATIBILITY.md](COMPATIBILITY.md) for the supported range.
