"""Release automation regressions, using local fixtures only; no publication or live API calls."""
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import struct
import tempfile
import unittest
from unittest.mock import patch
import zipfile

spec = importlib.util.spec_from_file_location("release", Path(__file__).with_name("release.py"))
release = importlib.util.module_from_spec(spec)
spec.loader.exec_module(release)


class ReleaseTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for name in ["pom.xml", ".github/release-version", ".github/modrinth.json", "RELEASE-NOTES.md", "src/main/resources/plugin.yml"]:
            dest = self.root / name
            dest.parent.mkdir(parents=True, exist_ok=True)
            dest.write_bytes((release.ROOT / name).read_bytes())
        self.version = release.project_version(self.root)

    def jar(self, *, version=None, descriptor=None, major=65, raw_dependency=False):
        version = version or self.version
        artifact = self.root / "artifact"
        artifact.mkdir(exist_ok=True)
        path = artifact / f"PluginUpdateWatch-{self.version}.jar"
        with zipfile.ZipFile(path, "w") as out:
            source = (self.root / "src/main/resources/plugin.yml").read_text(encoding="utf-8")
            out.writestr("plugin.yml", descriptor or source.replace("${project.version}", version))
            out.writestr("build.properties", "version=" + version + "\n")
            out.writestr("META-INF/maven/dev.updatewatch/PluginUpdateWatch/pom.properties", "version=" + version + "\n")
            for name in ["UpdateWatch", "DownloadManager", "DownloadQueue", "BulkProgress", "DownloadCache", "MessageLog", "ScanMessages"]:
                out.writestr(f"dev/updatewatch/{name}.class", b"\xca\xfe\xba\xbe\0\0" + struct.pack(">H", major))
            for name in ["dev/updatewatch/lib/bstats/MetricsBase.class", "dev/updatewatch/lib/gson/Gson.class", "dev/updatewatch/lib/jsoup/Jsoup.class", "META-INF/LICENSE", "META-INF/BSTATS-LICENSE.txt"]:
                out.writestr(name, "fixture")
            if raw_dependency:
                out.writestr("org/bstats/MetricsBase.class", "unrelocated")
        (artifact / "version.txt").write_text(self.version + "\n")
        digest = hashlib.sha256(path.read_bytes()).hexdigest()
        (artifact / (path.name + ".sha256")).write_text(f"{digest}  {path.name}\n")
        return artifact, path

    def test_version_marker_tag_and_notes_must_agree(self):
        self.assertEqual(self.version, release.validate(root=self.root)[0])
        for bad in ["1.5.0", "1.5.1-SNAPSHOT", "1.5.1; exit 0"]:
            with self.assertRaises(ValueError):
                release.validate(bad, self.root)
        (self.root / ".github/release-version").write_text("0.0.0")
        with self.assertRaises(ValueError):
            release.validate(self.version, self.root)

    def test_notes_and_unsupported_loaders_versions_are_rejected(self):
        original = release.current_notes(self.root)
        (self.root / "RELEASE-NOTES.md").write_text("# Different release\n")
        with self.assertRaises(ValueError):
            release.validate(root=self.root)
        (self.root / "RELEASE-NOTES.md").write_text(original, encoding="utf-8")
        for config in [{"loaders": ["spigot"], "game_versions": ["1.21.11", "26.3"]},
                       {"loaders": ["paper"], "game_versions": ["1.21.11", "26.3", "27.1"]}]:
            (self.root / ".github/modrinth.json").write_text(json.dumps(config))
            with self.assertRaises(ValueError):
                release.validate(root=self.root)

    def test_tested_artifact_is_copied_exactly_with_verifiable_alias_checksums(self):
        artifact, jar = self.jar()
        output = self.root / "release"
        release.prepare(self.version, artifact, output, "dev-dyly/PluginUpdateWatch", self.root)
        for name in [jar.name, "PluginUpdateWatch.jar"]:
            self.assertEqual(jar.read_bytes(), (output / name).read_bytes())
            expected = hashlib.sha256((output / name).read_bytes()).hexdigest()
            self.assertEqual([expected, name], (output / (name + ".sha256")).read_text(encoding="utf-8").split())
        self.assertNotIn("# PluginUpdateWatch 1.4.1", (output / "RELEASE-NOTES.md").read_text(encoding="utf-8"))

    def test_tampered_artifact_or_wrong_tested_version_is_rejected(self):
        artifact, jar = self.jar()
        (artifact / (jar.name + ".sha256")).write_text("0" * 64 + "  " + jar.name)
        with self.assertRaises(ValueError):
            release.prepare(self.version, artifact, self.root / "release", "dev-dyly/PluginUpdateWatch", self.root)
        (artifact / "version.txt").write_text("0.0.0")
        with self.assertRaises(ValueError):
            release.prepare(self.version, artifact, self.root / "release", "dev-dyly/PluginUpdateWatch", self.root)

    def test_wrong_jar_version_or_changed_permissions_is_rejected(self):
        _, jar = self.jar(version="0.0.0")
        with self.assertRaises(ValueError):
            release.verify_jar(jar, self.version, self.root)
        descriptor = (self.root / "src/main/resources/plugin.yml").read_text(encoding="utf-8").replace("${project.version}", self.version).replace("default: op", "default: true")
        _, jar = self.jar(descriptor=descriptor)
        with self.assertRaises(ValueError):
            release.verify_jar(jar, self.version, self.root)

    def test_java_compatibility_and_dependency_relocation_are_required(self):
        for options in [{"major": 69}, {"raw_dependency": True}]:
            _, jar = self.jar(**options)
            with self.assertRaises(ValueError):
                release.verify_jar(jar, self.version, self.root)

    def test_bbcode_uses_valid_links_lists_and_no_markdown_link_fragments(self):
        notes = "# PluginUpdateWatch " + self.version + "\n\n- See [the release](https://github.com/dev-dyly/PluginUpdateWatch/releases/latest).\n- Keep **checksums** and `config.yml`.\n\nDone.\n"
        text = release.spigot_notes(notes, self.version, "a" * 64, "dev-dyly/PluginUpdateWatch")
        self.assertIn("[LIST]\n[*]", text)
        self.assertEqual(1, text.count("[/LIST]"))
        self.assertIn("[URL=https://github.com/dev-dyly/PluginUpdateWatch/releases/latest]the release[/URL]", text)
        self.assertIn(f"[URL=https://github.com/dev-dyly/PluginUpdateWatch/releases/tag/v{self.version}]", text)
        self.assertIn("[URL=https://dev-dyly.github.io/PluginUpdateWatch/]", text)
        self.assertNotIn("](https", text)
        self.assertNotIn("[URL='[", text)

    def test_modrinth_without_credentials_skips_without_network_and_half_configuration_fails(self):
        with patch.dict(os.environ, {"MODRINTH_TOKEN": "", "MODRINTH_PROJECT_ID": ""}), patch.object(release.urllib.request, "urlopen", side_effect=AssertionError("No network expected")):
            self.assertFalse(release.modrinth_preflight(self.root))
        with patch.dict(os.environ, {"MODRINTH_TOKEN": "fixture-only", "MODRINTH_PROJECT_ID": ""}):
            with self.assertRaises(ValueError):
                release.modrinth_preflight(self.root)

    def test_modrinth_v2_paper_plugin_project_and_official_tags_pass_read_only_preflight(self):
        import io
        config = json.loads((self.root / ".github/modrinth.json").read_text(encoding="utf-8"))
        responses = [io.BytesIO(json.dumps(value).encode()) for value in [
            {"id": "fixture-user"}, {"project_type": "mod", "loaders": ["paper"]},
            [{"version": version, "version_type": "release"} for version in config["game_versions"]],
            [{"name": "paper"}]]]
        with patch.dict(os.environ, {"MODRINTH_TOKEN": "fixture-only", "MODRINTH_PROJECT_ID": "fixture-project"}), patch.object(release.urllib.request, "urlopen", side_effect=responses) as calls:
            self.assertTrue(release.modrinth_preflight(self.root))
            self.assertEqual(4, calls.call_count)
            self.assertEqual("https://api.modrinth.com/v2/user", calls.call_args_list[0].args[0].full_url)

    def test_modrinth_existing_incompatible_project_is_not_published(self):
        import io
        responses = [io.BytesIO(b'{"id":"fixture-user"}'), io.BytesIO(b'{"project_type":"mod","loaders":["fabric"]}')]
        with patch.dict(os.environ, {"MODRINTH_TOKEN": "fixture-only", "MODRINTH_PROJECT_ID": "fixture-project"}), patch.object(release.urllib.request, "urlopen", side_effect=responses):
            with self.assertRaises(ValueError):
                release.modrinth_preflight(self.root)


if __name__ == "__main__":
    unittest.main()
