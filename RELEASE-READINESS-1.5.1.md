# PluginUpdateWatch 1.5.1 Stable — release preparation

Prepared 12 November 2026 (Pacific/Auckland) on `codex/prepare-v1.5.1`, based on latest `main` commit `b836498` containing merged PR #15. **No release or tag has been created. This document is preparation evidence, not a publication announcement.**

## Version and runtime preservation

- `pom.xml` and `.github/release-version` identify **1.5.1**; `plugin.yml` retains `${project.version}` and the minimum API `1.21.11`.
- The shaded JAR contains `version: 1.5.1`, `build.properties` version 1.5.1 and Maven version 1.5.1. Main class remains `dev.updatewatch.UpdateWatch`; class major version **65** preserves Java 21 bytecode.
- Packaged `plugin.yml` matches the source descriptor after version filtering, including every command, alias and permission. No files under `src/main/java`, `src/main/resources` or existing Java tests changed from merged PR #15. Runtime security protections and startup partial-download recovery are retained.
- Relocated bStats/Gson/jsoup classes and license files are present; unrelocated copies and a bundled Bukkit API are rejected by verification. The local distributable has **713 archive entries**.
- Production DylyCraft was neither read for this release preparation nor modified, started or stopped.

## Release notes and wiki/documentation

`RELEASE-NOTES.md` has a professional 1.5.1 maintenance-release section covering PR #15 cancellation/completion accounting, stale configuration protection, duplicate/filename safety, interrupted-transfer recovery, logging/error classification and provider request identification/reuse. It preserves the documented live-test limitations and manual installation policy. The obsolete one-line `RELEASE_NOTES.md` stub was removed so publishing has one canonical notes source.

README, install/quick-start pages, command/download guidance, changelog and compatibility pages now describe 1.5.1. The MkDocs wiki includes a reviewed release procedure and is built strictly from its versioned sources. Links state that 1.5.1 assets appear only after tag-triggered publication, rather than claiming an unpublished download already exists. Historical release notes and artifact-specific live reports remain intact.

The separate GitHub wiki clone endpoint returned repository-not-found; no existing pages could be retrieved there. The canonical MkDocs documentation/wiki sources in this repository were updated and validated for normal deployment after merge.

## Workflow fixes

1. The tag workflow validates a stable numeric tag, matching POM/marker/notes and membership of its commit in `main`.
2. Both Java 21 and Java 25 jobs run **`mvn clean verify`**, including Failsafe's packaged-JAR check. Publishing depends on both jobs. Python release-tooling regressions and final JAR metadata/contents checks also run in CI.
3. Publishing downloads the **exact tested Java 21 artifact**, checks its version and SHA-256, and copies it unchanged. It never rebuilds or selects a JAR using `find | head`.
4. Assets include the versioned JAR/checksum, an identical stable-name alias/checksum and a downloadable SpigotMC BBCode snippet. Checksum files use the standard filename format.
5. Spigot links use `[URL=https://…]label[/URL]`; current notes are converted to BBCode lists/bold/inline formatting. Regression tests reject the previous Markdown-inside-BBCode link pattern.
6. Only the current `RELEASE-NOTES.md` section is published. Generated `gh-pages` substitution/mutation was removed; the existing MkDocs source-deployment workflow owns the wiki.
7. `release.ps1` is verification-only. The former broad staging, committing and push-to-main behaviour was removed. It never tags, publishes or merges.

## Modrinth readiness

`.github/modrinth.json` advertises only **Paper**, consistent with `COMPATIBILITY.md`; standalone Spigot/Folia and unverified loaders are not advertised. Supported Minecraft tags are **1.21.11, 26.1, 26.1.1, 26.1.2, 26.2 and 26.3**. All six were confirmed against the official Modrinth release-tag API on 12 November 2026; this is a targeted-version list, not six independent live-server certifications.

The action preserves that explicit list with `game-version-filter: none`, uses stable `version-type: release`, and uploads the same verified JAR/current notes. Before any publication, read-only preflight checks configured API authentication, project metadata and official version/loader tags. Its local tests use controlled responses; **no real token's upload permissions were tested**.

Repository secret and variable names were inspected without reading secret values: no Modrinth token/project settings were configured. The similarly named production environment also exposed no secret names, and environment-only credentials are not inherited by this workflow. Public project lookup/search did not identify an existing PluginUpdateWatch project. This does not rule out private or differently named projects.

**GitHub release preparation can proceed; Modrinth publication is not configured.** To include Modrinth, configure repository secret `MODRINTH_TOKEN` and variable/secret `MODRINTH_PROJECT_ID` before tagging. Neither present means a visible GitHub-only notice and skip; only one present fails before publishing. No Modrinth project/version was created or uploaded.

## Verification results

| Check | Result |
| --- | --- |
| Java 21.0.12 `mvn --batch-mode --no-transfer-progress clean verify` | PASS: 191 unit + 1 packaged-JAR integration test, zero failures/errors/skips |
| Java 25.0.4.1 same Maven verification | PASS: 191 unit + 1 packaged-JAR integration test, zero failures/errors/skips |
| Offline release-tooling regressions | PASS: 10 Python unittest cases; no API calls or publication |
| Final JAR metadata/descriptor/contents verification | PASS on both local builds |
| Exact-artifact release packaging dry run | PASS: copied bytes/checksums/alias/current notes/BBCode checked locally; no publishing action executed |
| GitHub Actions syntax/context validation | PASS: official actionlint v1.7.12, downloaded with its published checksum verified |
| Strict MkDocs build | PASS |
| PowerShell helper syntax | PASS |
| `git diff --check` | PASS |

Local **Java 21** candidate JAR SHA-256:

```text
91ef464ae1641057e494efafe2bd4747747b274db444b2289aa4b5e1eee665ed
```

Separately verified local **Java 25** JAR SHA-256:

```text
71c3a93a50c335abefd70a04cc0240d426e3955f9e21c4be9e34c2d41bd103e1
```

CI/release rebuild timestamps may yield different hashes; official assets must use the checksum produced alongside their exact tested CI artifact. Existing Maven shade manifest/module-info overlap warnings remain visible; relocation/package checks pass.

An early release-tooling test run caught Windows-default text encoding in the new notes fixture. New notes and test text reads now use UTF-8; all final checks above pass. No runtime bug fixes or new features were introduced in this preparation.

## Publication gate and limits

This preparation does **not** claim the tag-triggered publishing actions ran, authenticated Modrinth publication worked, or the 1.5.1 JAR was live-tested. The PR #15 runtime code has prior actual Paper 26.3 build 246 / Java 25 evidence in `LIVE-TEST-REPORT.md`; the new version's metadata and archive were separately verified here. No production or new live test server was touched in this task.

After the preparation PR is merged, use a clean, current `main`, confirm both Java CI jobs pass for that exact commit, verify the candidate and the absence of a remote/local `v1.5.1` tag, then create and push only the annotated `v1.5.1` tag at that verified commit. **That step is authorized only after merge and has deliberately not been executed now.** Do not auto-merge the preparation PR. See `docs/releasing.md`.
