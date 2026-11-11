"""Offline release checks and deterministic release assets; never creates a tag or publishes."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import struct
import urllib.error
import urllib.request
import xml.etree.ElementTree as ET
import zipfile

ROOT = Path(__file__).resolve().parents[2]
NS = {"m": "http://maven.apache.org/POM/4.0.0"}


def project_version(root=ROOT):
    return ET.parse(root / "pom.xml").getroot().findtext("m:version", namespaces=NS)


def current_notes(root=ROOT):
    return (root / "RELEASE-NOTES.md").read_text(encoding="utf-8").split("\n---\n", 1)[0].rstrip() + "\n"


def validate(version=None, root=ROOT):
    version = version or project_version(root)
    if not re.fullmatch(r"\d+\.\d+\.\d+", version):
        raise ValueError("A stable release requires a numeric major.minor.patch version")
    if project_version(root) != version or (root / ".github/release-version").read_text().strip() != version:
        raise ValueError("Tag, pom.xml and .github/release-version must agree")
    if current_notes(root).splitlines()[0] != f"# PluginUpdateWatch {version}":
        raise ValueError("The current release-note heading must match the version")
    descriptor = (root / "src/main/resources/plugin.yml").read_text(encoding="utf-8")
    if not re.search(r"(?m)^version: \$\{project.version\}$", descriptor):
        raise ValueError("plugin.yml must use the Maven-filtered project version")
    config = json.loads((root / ".github/modrinth.json").read_text())
    if config["loaders"] != ["paper"]:
        raise ValueError("Publish only the documented Paper loader; Spigot/Folia are not supported")
    versions = config["game_versions"]
    if len(versions) != len(set(versions)) or "1.21.11" not in versions or "26.3" not in versions:
        raise ValueError("Supported Minecraft versions must be distinct and include both policy endpoints")
    if any(v != "1.21.11" and not re.fullmatch(r"26\.[123](?:\.\d+)?", v) for v in versions):
        raise ValueError("A Minecraft version is outside the existing compatibility policy")
    return version, config


def properties(text):
    return dict(line.split("=", 1) for line in text.splitlines() if "=" in line and not line.startswith("#"))


def verify_jar(path, version, root=ROOT):
    with zipfile.ZipFile(path) as jar:
        descriptor = jar.read("plugin.yml").decode("utf-8")
        source = (root / "src/main/resources/plugin.yml").read_text(encoding="utf-8")
        expected = source.replace("${project.version}", version)
        if descriptor.replace("\r\n", "\n") != expected.replace("\r\n", "\n"):
            raise ValueError("Packaged descriptor differs from source commands/permissions/version")
        build = properties(jar.read("build.properties").decode())
        pom = properties(jar.read("META-INF/maven/dev.updatewatch/PluginUpdateWatch/pom.properties").decode())
        if build.get("version") != version or pom.get("version") != version:
            raise ValueError("JAR build/Maven version metadata does not match")
        for name in ["UpdateWatch", "DownloadManager", "DownloadQueue", "BulkProgress", "DownloadCache", "MessageLog", "ScanMessages"]:
            data = jar.read(f"dev/updatewatch/{name}.class")
            if data[:4] != b"\xca\xfe\xba\xbe" or struct.unpack(">H", data[6:8])[0] > 65:
                raise ValueError("Plugin class is invalid or requires a newer runtime than Java 21")
        for name in ["dev/updatewatch/lib/bstats/MetricsBase.class", "dev/updatewatch/lib/gson/Gson.class",
                     "dev/updatewatch/lib/jsoup/Jsoup.class", "META-INF/LICENSE", "META-INF/BSTATS-LICENSE.txt"]:
            jar.getinfo(name)
        if any(name.startswith(("org/bstats/", "com/google/gson/", "org/jsoup/", "org/bukkit/")) and name.endswith(".class") for name in jar.namelist()):
            raise ValueError("Unrelocated dependencies or a bundled server API were found")
    return hashlib.sha256(Path(path).read_bytes()).hexdigest()


def inline_bbcode(text):
    text = re.sub(r"\[([^\]]+)\]\((https://[^\s)]+)\)", r"[URL=\2]\1[/URL]", text)
    text = re.sub(r"\*\*(.+?)\*\*", r"[B]\1[/B]", text)
    return re.sub(r"`([^`]+)`", r"[FONT=Courier New]\1[/FONT]", text)


def spigot_notes(notes, version, digest, repository):
    if not re.fullmatch(r"[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+", repository):
        raise ValueError("Invalid repository name")
    output, in_list = [], False
    for line in notes.splitlines():
        bullet = line.startswith("- ")
        if bullet and not in_list:
            output.append("[LIST]")
        elif in_list and not bullet:
            output.append("[/LIST]")
        in_list = bullet
        if bullet:
            output.append("[*]" + inline_bbcode(line[2:]))
        elif line.startswith("#"):
            output.append("[B]" + inline_bbcode(line.lstrip("# ")) + "[/B]")
        else:
            output.append(inline_bbcode(line))
    if in_list:
        output.append("[/LIST]")
    output += ["", f"[B]SHA-256:[/B] [FONT=Courier New]{digest}[/FONT]",
               f"[URL=https://github.com/{repository}/releases/tag/v{version}]GitHub release and checksum[/URL] | "
               "[URL=https://dev-dyly.github.io/PluginUpdateWatch/]Documentation & Wiki[/URL]"]
    return "\n".join(output) + "\n"


def prepare(version, artifact, output, repository, root=ROOT):
    validate(version, root)
    name = f"PluginUpdateWatch-{version}.jar"
    if (artifact / "version.txt").read_text().strip() != version:
        raise ValueError("The tested artifact was built for a different version")
    digest = verify_jar(artifact / name, version, root)
    checksum = (artifact / (name + ".sha256")).read_text().split()
    if checksum != [digest, name]:
        raise ValueError("The tested artifact checksum/filename does not match")
    output.mkdir(parents=True, exist_ok=True)
    for target in [name, "PluginUpdateWatch.jar"]:
        shutil.copyfile(artifact / name, output / target)
        (output / (target + ".sha256")).write_text(f"{digest}  {target}\n", encoding="utf-8")
    notes = current_notes(root)
    (output / "RELEASE-NOTES.md").write_text(notes, encoding="utf-8")
    (output / "SPIGOT-UPDATE.bbcode").write_text(spigot_notes(notes, version, digest, repository), encoding="utf-8")
    return digest


def github_output(name, value):
    if os.environ.get("GITHUB_OUTPUT"):
        with open(os.environ["GITHUB_OUTPUT"], "a", encoding="utf-8") as stream:
            stream.write(f"{name}<<RELEASE_VALUE\n{value}\nRELEASE_VALUE\n")


def modrinth_preflight(root=ROOT):
    """Read-only checks, before any GitHub/Modrinth publication. Never print credentials."""
    token = os.environ.get("MODRINTH_TOKEN", "")
    project = os.environ.get("MODRINTH_PROJECT_ID", "")
    if bool(token) != bool(project):
        raise ValueError("Configure both MODRINTH_TOKEN and MODRINTH_PROJECT_ID, or neither")
    if not token:
        print("::notice::Modrinth publishing is not configured; GitHub release only. No Modrinth upload will be attempted.")
        return False
    if not re.fullmatch(r"[A-Za-z0-9_-]+", project):
        raise ValueError("MODRINTH_PROJECT_ID must be a project ID or slug")
    _, config = validate(root=root)
    headers = {"User-Agent": "PluginUpdateWatch-Release/" + project_version(root), "Authorization": token}
    def read(endpoint):
        request = urllib.request.Request("https://api.modrinth.com/v2/" + endpoint, headers=headers)
        with urllib.request.urlopen(request, timeout=30) as response:
            return json.load(response)
    read("user")
    info = read("project/" + project)
    if info.get("project_type") not in {"plugin", "mod"}:
        raise ValueError("The configured Modrinth project cannot publish plugins")
    # Modrinth v2 reports Bukkit/Paper plugins as project_type=mod.
    if info.get("loaders") and "paper" not in info["loaders"]:
        raise ValueError("The existing Modrinth project does not list Paper support")
    valid_versions = {v["version"] for v in read("tag/game_version") if v["version_type"] == "release"}
    if not set(config["game_versions"]) <= valid_versions:
        raise ValueError("A configured Minecraft version is not a published Modrinth release tag")
    if not set(config["loaders"]) <= {v["name"] for v in read("tag/loader")}:
        raise ValueError("A configured Modrinth loader is unknown")
    print("Modrinth account, plugin project and release tags passed read-only preflight.")
    return True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=["validate", "verify-jar", "prepare", "modrinth-preflight"])
    parser.add_argument("version", nargs="?")
    parser.add_argument("--artifact", type=Path, default=Path("artifact"))
    parser.add_argument("--output", type=Path, default=Path("release"))
    parser.add_argument("--jar", type=Path)
    parser.add_argument("--repository", default="dev-dyly/PluginUpdateWatch")
    args = parser.parse_args()
    version, config = validate(args.version)
    if args.command == "validate":
        github_output("version", version)
        github_output("loaders", "\n".join(config["loaders"]))
        github_output("game-versions", "\n".join(config["game_versions"]))
        print("Release metadata matches " + version)
    elif args.command == "verify-jar":
        print("Verified JAR SHA-256: " + verify_jar(args.jar or Path(f"target/PluginUpdateWatch-{version}.jar"), version))
    elif args.command == "prepare":
        digest = prepare(version, args.artifact, args.output, args.repository)
        github_output("sha256", digest)
        print("Prepared exact verified release artifact: " + digest)
    else:
        github_output("enabled", str(modrinth_preflight()).lower())


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, KeyError, zipfile.BadZipFile) as error:
        # urllib errors may carry request context: report their type, never credential-bearing bodies.
        message = type(error).__name__ if isinstance(error, urllib.error.URLError) else str(error)
        raise SystemExit("Release verification failed: " + message)
