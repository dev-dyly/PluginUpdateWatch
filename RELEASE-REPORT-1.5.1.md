# PluginUpdateWatch v1.5.1 Stable — release report

Verified 12 November 2026, Pacific/Auckland. This report records publication separately from earlier live testing. No runtime code, commands, permissions or security settings were changed during release publication.

## Source and GitHub publication

- [PR #16](https://github.com/dev-dyly/PluginUpdateWatch/pull/16) was squash-merged after Java 21 and Java 25 checks passed. The repository requires linear history; no repository rules were bypassed.
- Release source/main commit: `72a7ff7376b51abb5a4483969940a7490297aca4`.
- Annotated tag: **`v1.5.1`**, created at that verified commit after confirming no existing tag or release.
- [Official GitHub release](https://github.com/dev-dyly/PluginUpdateWatch/releases/tag/v1.5.1): **PluginUpdateWatch 1.5.1 Stable**, published, neither draft nor prerelease.
- Published at `2026-11-11T15:33:12Z` (12 November 2026, 04:33 NZDT).

Release assets:

| Asset | Verification |
| --- | --- |
| `PluginUpdateWatch-1.5.1.jar` | Official versioned JAR, 1,051,344 bytes |
| `PluginUpdateWatch-1.5.1.jar.sha256` | Matches downloaded versioned JAR |
| `PluginUpdateWatch.jar` | Byte-for-byte identical convenience alias |
| `PluginUpdateWatch.jar.sha256` | Matches downloaded alias |
| `SPIGOT-UPDATE.bbcode` | Generated release changelog with valid BBCode links |

**Official JAR SHA-256:**

```text
c0d70938427a1f6d89c7bdffcfd167e923216f94de111b47ced7647e01b05d01
```

The downloaded release JAR matches the exact `plugin-java-21` artifact from the successful tag workflow. It was not replaced with a separate local build. ZIP integrity, filtered `plugin.yml`, Maven/build metadata, version 1.5.1, class versions at most Java 21, relocated dependencies and bundled license notices passed final-artifact validation. Runtime Java sources/resources are unchanged from the merged PR #15 fixes, except release version filtering through the POM. Bulk download, checksum, archive identity, stale-configuration and filename protections remain present.

## Validation and Actions

| Check | Actual result |
| --- | --- |
| [Final main Maven matrix](https://github.com/dev-dyly/PluginUpdateWatch/actions/runs/43044426442) | Java 21 and Java 25 passed |
| Local final-main `mvn --batch-mode --no-transfer-progress clean verify`, Java 25 | Passed: 191 unit tests and 1 packaged integration test; zero failures |
| Release tooling | 10 Python tests passed; release metadata/JAR validation passed |
| Workflow lint | actionlint passed |
| Strict documentation build | Passed |
| [Tag release workflow](https://github.com/dev-dyly/PluginUpdateWatch/actions/runs/43049357763) | Validation, Java 21, Java 25 and publishing jobs all succeeded |
| Published-artifact round trip | GitHub versioned JAR, alias, SHA files and exact tag CI artifact matched |
| Earlier live Paper testing | PR #15 ran on actual isolated Paper 26.3 build 246, Java 25; see [LIVE-TEST-REPORT.md](LIVE-TEST-REPORT.md) |

The release process did not repeat live Paper testing with the exact v1.5.1 CI artifact. Earlier live runs used the explicitly identified PR #15 artifacts. Controlled-provider cases, native HTTPS cases and authenticated-player limitations remain distinguished in that report. Maven/package tests are not represented as live server tests. Existing Maven shade overlap notices did not fail verification.

## SpigotMC

**Published on the existing resource:** [PluginUpdateWatch, resource 139209](https://www.spigotmc.org/resources/pluginupdatewatch.139209/).

- Uploaded the exact verified GitHub JAR and published version **1.5.1** once.
- [Maintenance update/changelog](https://www.spigotmc.org/resources/pluginupdatewatch.139209/update?update=662907) uses the workflow-generated BBCode, including the official checksum and working release/documentation links.
- Independently observed the resource title and version panel displaying **1.5.1** and the new update in its history.
- Updated the main description, install filename, maintenance fixes, testing scope and current Modrinth link. Preserved historical introduction of `/pu downloadall` in 1.5.0. Repaired malformed list markup, unsupported `[HR]` text and invalid YAML example formatting.
- Current download points to resource version **659900**: [download](https://www.spigotmc.org/resources/pluginupdatewatch.139209/download?version=659900).

**Verification limitation:** Chrome did not return a saved file through the supported download API; an unauthenticated HTTP download returned 403. Browser policy also disallowed opening Chrome's internal downloads page. No security controls were bypassed. The uploaded file identity is verified locally and publication/version/link are verified, but a separately downloaded Spigot-served JAR checksum is **pending**. An operator can complete this last check by downloading from the listing and comparing its SHA-256 with the value above. Do not create another version update to perform this check.

## Modrinth

**Existing project retained:** [PluginUpdateWatch](https://modrinth.com/plugin/pluginupdatewatch), organization [Dev Dyl NZ](https://modrinth.com/organization/dev-dyl-nz), project ID **`1dZfjHmL`**.

- Authenticated project/moderation pages explicitly showed **Under review**, with no rejection or moderator change request observed. The moderation page states modifications do not affect the submission's queue position.
- Added exactly one **1.5.1 Stable** release to this existing submission: [version `ip2ZglA4`](https://modrinth.com/plugin/pluginupdatewatch/version/ip2ZglA4).
- Loader: **Paper**. Minecraft labels: **1.21.11 and 26.3**, matching the existing verified project metadata; no additional compatibility labels were claimed for this upload.
- Used official release notes and the exact verified JAR. Downloaded the resulting CDN artifact and independently confirmed 1,051,344 bytes and the same official SHA-256.
- Corrected source, issue tracker, wiki and license URLs to the current repository, and added a concise current-release reference to the existing description. Preserved disclosures, donation settings and the original moderation submission.
- The project remains **under review**. Adding a version does not mean moderation approval or public discovery is available. No duplicate project, withdrawal or resubmission was made.

### Future automatic publication

No `MODRINTH_TOKEN` or `MODRINTH_PROJECT_ID` repository secret/variable was available. Automatic Modrinth publication remained disabled, and the release workflow correctly completed GitHub publication without it. No token was created, read from the browser, exposed or committed.

After approval, configure a suitably scoped Actions secret through the platform's secure interface and set `MODRINTH_PROJECT_ID=1dZfjHmL` together. Revalidate project status, token access and supported version labels before enabling publication. The workflow's configured target range includes intermediate releases; the manual upload deliberately used only the two verified project labels above. Review that list before future automatic publishing. Do not add only one credential setting, because preflight rejects incomplete configuration. Do not rerun publication of the already existing 1.5.1 version.

## Documentation

[Public wiki/MkDocs](https://dev-dyly.github.io/PluginUpdateWatch/) is available. [Main documentation deployment](https://github.com/dev-dyly/PluginUpdateWatch/actions/runs/43044426358) succeeded. Home, changelog, installation, downloads, configuration, troubleshooting and compatibility pages returned HTTP 200. Current release pages identify 1.5.1; configuration and troubleshooting retain version-independent guidance. Downloadall documentation and historical release notes remain available.

This report's documentation follow-up removes release-preparation wording from the README/wiki home and fixes a remaining README local-build filename from 1.5.0 to 1.5.1. It does not change the tagged release artifact. Public deployment of that follow-up occurs after its PR is merged.

## DylyCraft safety and installation procedure

Only read-only JAR metadata/hash inspection was performed. At final inspection, production's on-disk `plugins/PluginUpdateWatch-1.5.1.jar` already identified **1.5.1** and matched the official SHA-256. This release process did **not** install it, modify configuration, issue server commands, stop or restart DylyCraft. The currently running version was not queried and cannot be inferred from the file on disk.

For an operator-managed installation or later rollback:

1. Confirm the running version using `version PluginUpdateWatch` during an authorized maintenance session. If it is already 1.5.1, no replacement is needed.
2. Before replacing anything, back up the currently installed PluginUpdateWatch JAR and the entire `plugins/PluginUpdateWatch/` directory, including `config.yml`, backups and downloaded updates. Keep backups outside the active plugins directory.
3. Obtain the official GitHub versioned JAR and compare `Get-FileHash -Algorithm SHA256` against the value above. Use the Paper-required runtime; Paper 26.3 testing used Java 25. Java 21 bytecode does not remove Paper's runtime requirements.
4. Only with separate operator authorization, stop the server normally, retain exactly one installed PluginUpdateWatch JAR, replace the previous JAR with the verified file, preserve configuration/data and restart. Do not hot-swap plugins.
5. Check initialization logs and `version PluginUpdateWatch`, then `pu scan`, `pu check`, `pu list all` and `pu stats`. Review genuine provider/configuration errors. An optional `pu downloadall` must follow a successful check; inspect results and files in the separate downloads folder before manually installing any plugin updates.

## Remaining actions

1. Complete the independent Spigot download checksum check described above.
2. Await Modrinth moderation; address any actual feedback without duplicate submissions. Configure/validate future automatic publication only after approval and secure credentials are available.
3. Merge the documentation/report follow-up through normal required checks, then let MkDocs deploy the small post-publication wording corrections. Keep the existing release tag unchanged.
4. Production runtime verification or restart remains an operator-controlled maintenance action.
