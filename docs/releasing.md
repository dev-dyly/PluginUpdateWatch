# Preparing and publishing a stable release

## Release preparation

Prepare the version, release notes and wiki/documentation on a separate branch and open a PR to `main`. For 1.5.1, `pom.xml`, `.github/release-version`, the first heading of `RELEASE-NOTES.md` and the future `v1.5.1` tag must agree. Keep `plugin.yml` at `version: ${project.version}` so Maven filters the final descriptor; its minimum API remains `1.21.11`.

Run `mvn clean verify` on Java 21 and Java 25. The build workflow also runs the offline Python release-tooling regressions and checks the shaded JAR's descriptor, build/Maven version metadata, Java class compatibility, dependency relocation and licenses. The complete packaged descriptor must match the source descriptor with only its version filtered, preserving commands and permissions.

`release.ps1 -Version 1.5.1` is a local **verification-only** helper requiring Python and Maven on PATH. It never stages files, commits, pushes, tags or publishes. The obsolete `RELEASE_NOTES.md` stub is removed; `RELEASE-NOTES.md` is canonical, with the current section before the first horizontal separator.

## After the preparation PR is merged

Do not create the tag while the PR is still open. Once merged:

1. Fetch the latest `main` into a clean checkout, fast-forward it, and confirm the intended release commit includes the merged preparation PR.
2. Check that **both Java 21 and Java 25 GitHub Actions jobs passed for that exact main commit**. Rerun verification if the commit changes. Run `mvn clean verify` locally and verify `PluginUpdateWatch-1.5.1.jar` metadata.
3. Confirm that `v1.5.1` does not already exist locally or remotely and review optional Modrinth configuration below.
4. Create an annotated `v1.5.1` tag at the verified main commit and push **only that explicit tag**. This step is deliberately not executed by preparation scripts or PR workflows.
5. Monitor the tag-triggered release workflow. It requires the tagged commit to be on `main`, validates metadata, runs both Java test jobs, then downloads and verifies the **exact tested Java 21 artifact**. Publishing never rebuilds or searches for a possibly different JAR.
6. Verify published assets and checksums, the generated SpigotMC snippet, and any configured Modrinth upload. Nothing is automatically installed on a Minecraft server.

GitHub assets include the versioned JAR and SHA-256, an identical `PluginUpdateWatch.jar` compatibility alias and its checksum, and `SPIGOT-UPDATE.bbcode`. Checksums use the standard `sha256sum` filename format. SpigotMC links use `[URL=https://…]label[/URL]`, with BBCode list/bold formatting rather than embedded Markdown links.

The release workflow is triggered only by version-tag pushes, not preparation-branch pushes or PR merges. The existing documentation workflow builds the MkDocs wiki from reviewed `docs/` sources on `main`; the release job no longer edits generated `gh-pages` files with broad substitutions. The release-notes sync workflow can update an existing release body but does not create a release when its tag is absent.

## Optional Modrinth publishing

The reviewed policy is in `.github/modrinth.json`: loader `paper`; explicit targeted Minecraft releases from 1.21.11 through 26.3. Do not advertise standalone Spigot/Folia or add future Minecraft versions without reviewing compatibility. `game-version-filter: none` preserves this explicit list. Stable versions use `version-type: release`; the same verified JAR and current release-note section are uploaded.

Configure both:

- Repository secret `MODRINTH_TOKEN` with appropriate project/version publishing access.
- Repository variable `MODRINTH_PROJECT_ID` (preferred), or secret of the same name, containing the real Modrinth project ID/slug.

The release job reads repository-level settings. Environment-only secrets are not implicitly available. With neither value, it visibly reports **GitHub release only** and skips Modrinth; with only one, it fails before publishing anything. With both, a read-only preflight checks API authentication, project compatibility and current official release/loader tags before publication. This cannot prove all upload permissions without performing an upload; actual publication failures remain failures.

At preparation time no Modrinth repository settings were configured, and public project search did not identify a project. No project was created and no Modrinth version was uploaded. Configure the project/token before tagging if Modrinth publication is required. Never commit tokens, paste them into release notes, or send them in chat.
