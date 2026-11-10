#!/usr/bin/env python3
"""Validate the final remapped Fabric mod JAR and emit SHA-256 and build-info metadata."""

from __future__ import annotations

import argparse
import datetime as dt
import hashlib
import json
import subprocess
import sys
import zipfile
from pathlib import Path

REQUIRED_JAR_ENTRIES = (
    "fabric.mod.json",
    "assets/customizablehud/icon.png",
    "assets/customizablehud/lang/en_us.json",
    "assets/customizablehud/lang/es_es.json",
    "dev/customizablehud/client/CustomizableHudClient.class",
    "dev/customizablehud/config/HudConfig.class",
    "dev/customizablehud/layout/HudLayoutEngine.class",
)


def parse_gradle_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        props[key.strip()] = value.strip()
    return props


def resolve_git_commit(repo_root: Path) -> tuple[str, str]:
    try:
        sha = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=repo_root, text=True).strip()
    except Exception:
        sha = "unknown"
    try:
        branch = subprocess.check_output(["git", "branch", "--show-current"], cwd=repo_root, text=True).strip() or "detached"
    except Exception:
        branch = "unknown"
    return sha, branch


def resolve_expected_jar(repo_root: Path, props: dict[str, str]) -> Path:
    base_name = props.get("archives_base_name", "").strip()
    mod_version = props.get("mod_version", "").strip()
    if not base_name or not mod_version:
        raise RuntimeError("archives_base_name and mod_version must be set in gradle.properties")

    libs_dir = repo_root / "build" / "libs"
    if not libs_dir.is_dir():
        raise RuntimeError(f"Expected output directory does not exist: {libs_dir}")

    expected_name = f"{base_name}-{mod_version}.jar"
    expected_jar = libs_dir / expected_name

    if expected_name.endswith(("-dev.jar", "-sources.jar", "-sources-dev.jar", "-javadoc.jar")):
        raise RuntimeError(f"Resolved unexpected auxiliary JAR name: {expected_name}")

    if not expected_jar.is_file():
        found = sorted(p.name for p in libs_dir.iterdir())
        raise RuntimeError(f"Expected remapped mod JAR '{expected_name}' not found in {libs_dir}. Found: {found}")

    return expected_jar


def validate_jar(jar_path: Path, props: dict[str, str]) -> tuple[int, str]:
    with zipfile.ZipFile(jar_path, "r") as zf:
        bad_entry = zf.testzip()
        if bad_entry is not None:
            raise RuntimeError(f"Corrupt entry inside JAR {jar_path.name}: {bad_entry}")

        names = set(zf.namelist())
        missing = [req for req in REQUIRED_JAR_ENTRIES if req not in names]
        if missing:
            raise RuntimeError(f"Missing required entries in {jar_path.name}: {missing}")

        if not any(n.startswith("LICENSE") for n in names):
            raise RuntimeError(f"Missing LICENSE file inside {jar_path.name}")

        java_sources_inside = [n for n in names if n.endswith(".java")]
        if java_sources_inside:
            raise RuntimeError(
                f"{jar_path.name} contains Java source files ({java_sources_inside[:5]}); "
                "this looks like a sources JAR instead of the compiled mod JAR"
            )

        if any(n.startswith("com/terraformersmc/modmenu/") for n in names):
            raise RuntimeError(f"{jar_path.name} must not bundle Mod Menu classes inside the mod JAR")

        fmod_raw = zf.read("fabric.mod.json").decode("utf-8")
        fmod = json.loads(fmod_raw)

        if fmod.get("id") != "customizablehud":
            raise RuntimeError(f"Expected mod id 'customizablehud' in JAR metadata, got {fmod.get('id')!r}")
        if fmod.get("environment") != "client":
            raise RuntimeError(f"Expected client environment in JAR metadata, got {fmod.get('environment')!r}")

        expected_version = props.get("mod_version", "")
        if fmod.get("version") != expected_version or "${" in str(fmod.get("version", "")):
            raise RuntimeError(f"Unexpected version in packaged fabric.mod.json: {fmod.get('version')!r}")

        depends = fmod.get("depends") or {}
        if "modmenu" in depends:
            raise RuntimeError("Mod Menu must NOT be declared under 'depends' in packaged fabric.mod.json")

        en_lang = json.loads(zf.read("assets/customizablehud/lang/en_us.json").decode("utf-8"))
        es_lang = json.loads(zf.read("assets/customizablehud/lang/es_es.json").decode("utf-8"))
        if set(en_lang.keys()) != set(es_lang.keys()):
            raise RuntimeError("Language key mismatch inside packaged JAR")

    sha256 = hashlib.sha256(jar_path.read_bytes()).hexdigest()
    return len(names), sha256


def main() -> int:
    parser = argparse.ArgumentParser(description="Validate compiled Customizable HUD mod JAR and write checksums.")
    parser.add_argument(
        "--meta-dir",
        default="build/release-meta",
        help="Directory to write <jar>.sha256 and build-info.txt (default: build/release-meta)",
    )
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent
    props = parse_gradle_properties(repo_root / "gradle.properties")
    jar_path = resolve_expected_jar(repo_root, props)
    entry_count, sha256 = validate_jar(jar_path, props)
    size_bytes = jar_path.stat().st_size

    meta_dir = (repo_root / args.meta_dir).resolve()
    meta_dir.mkdir(parents=True, exist_ok=True)

    sha_file = meta_dir / f"{jar_path.name}.sha256"
    sha_file.write_text(f"{sha256}  {jar_path.name}\n", encoding="utf-8")

    commit_sha, branch = resolve_git_commit(repo_root)
    utc_now = dt.datetime.now(dt.timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ")

    info_file = meta_dir / "build-info.txt"
    info_lines = [
        f"artifact_name={jar_path.name}",
        f"artifact_path={jar_path.relative_to(repo_root)}",
        f"artifact_size_bytes={size_bytes}",
        f"artifact_sha256={sha256}",
        f"mod_id=customizablehud",
        f"mod_version={props.get('mod_version', 'unknown')}",
        f"minecraft_version={props.get('minecraft_version', 'unknown')}",
        f"fabric_loader_version={props.get('loader_version', 'unknown')}",
        f"fabric_api_version={props.get('fabric_api_version', 'unknown')}",
        f"loom_version={props.get('loom_version', 'unknown')}",
        f"modmenu_compile_version={props.get('modmenu_version', 'unknown')}",
        f"java_target=21",
        f"git_commit={commit_sha}",
        f"git_branch={branch}",
        f"built_utc={utc_now}",
    ]
    info_file.write_text("\n".join(info_lines) + "\n", encoding="utf-8")

    print(f"[OK] Validated remapped JAR: {jar_path.relative_to(repo_root)}")
    print(f"     Entries : {entry_count}")
    print(f"     Size    : {size_bytes} bytes")
    print(f"     SHA-256 : {sha256}")
    print(f"     Written : {sha_file.relative_to(repo_root)}, {info_file.relative_to(repo_root)}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
