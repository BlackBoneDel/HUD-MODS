#!/usr/bin/env python3
"""Static repository checks (metadata, translations, resource presence, Java version parity, git diff --check)."""

from __future__ import annotations

import json
import re
import shutil
import subprocess
import sys
from pathlib import Path

PNG_SIGNATURE = b"\x89PNG\r\n\x1a\n"
PURE_DOMAIN_PREFIXES = (
    "src/client/java/dev/customizablehud/config/",
    "src/client/java/dev/customizablehud/format/",
    "src/client/java/dev/customizablehud/layout/",
)


def check_git_diff(repo_root: Path) -> None:
    if shutil.which("git") is None or not (repo_root / ".git").exists():
        print("[SKIP] git diff --check (git unavailable)")
        return
    proc = subprocess.run(
        ["git", "diff", "--check"],
        cwd=repo_root,
        capture_output=True,
        text=True,
        check=False,
    )
    if proc.returncode != 0:
        raise RuntimeError(f"git diff --check failed:\n{proc.stdout}\n{proc.stderr}")
    print("[OK] git diff --check passed")


def load_json(path: Path) -> dict:
    if not path.is_file():
        raise RuntimeError(f"Missing required JSON file: {path}")
    try:
        data = json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as ex:
        raise RuntimeError(f"Invalid JSON in {path}: {ex}") from ex
    if not isinstance(data, dict):
        raise RuntimeError(f"Expected JSON object in {path}")
    return data


def check_metadata_and_resources(repo_root: Path) -> dict:
    mod_json_path = repo_root / "src/main/resources/fabric.mod.json"
    mod_meta = load_json(mod_json_path)

    if mod_meta.get("id") != "customizablehud":
        raise RuntimeError(f"Expected mod id 'customizablehud', got {mod_meta.get('id')!r}")
    if mod_meta.get("name") != "Hud Utils":
        raise RuntimeError(f"Expected mod name 'Hud Utils', got {mod_meta.get('name')!r}")
    authors = mod_meta.get("authors") or []
    if "Denvdrite" not in authors:
        raise RuntimeError(f"Expected author 'Denvdrite' in fabric.mod.json, got {authors!r}")
    if mod_meta.get("environment") != "client":
        raise RuntimeError(f"Expected client environment, got {mod_meta.get('environment')!r}")

    depends = mod_meta.get("depends") or {}
    if "modmenu" in depends:
        raise RuntimeError("Mod Menu must remain optional, not listed in 'depends'")
    for required_dep in ("fabricloader", "minecraft", "java", "fabric-api"):
        if required_dep not in depends:
            raise RuntimeError(f"Missing required dependency '{required_dep}' in fabric.mod.json")

    icon_rel = mod_meta.get("icon")
    if not isinstance(icon_rel, str) or not icon_rel:
        raise RuntimeError("Missing 'icon' in fabric.mod.json")
    icon_path = repo_root / "src/main/resources" / icon_rel
    if not icon_path.is_file():
        raise RuntimeError(f"Declared icon does not exist: {icon_path}")
    if not icon_path.read_bytes().startswith(PNG_SIGNATURE):
        raise RuntimeError(f"Declared icon is not a valid PNG file: {icon_path}")

    entrypoints = mod_meta.get("entrypoints") or {}
    if "client" not in entrypoints or not entrypoints["client"]:
        raise RuntimeError("Missing 'client' entrypoint in fabric.mod.json")
    for group_name, classes in entrypoints.items():
        if not isinstance(classes, list):
            raise RuntimeError(f"Entrypoint group '{group_name}' must be a list")
        for fqcn in classes:
            if not isinstance(fqcn, str) or not fqcn:
                raise RuntimeError(f"Invalid entrypoint class in '{group_name}': {fqcn!r}")
            rel_java = fqcn.replace(".", "/") + ".java"
            found = any(
                (repo_root / src_root / rel_java).is_file()
                for src_root in ("src/main/java", "src/client/java")
            )
            if not found:
                raise RuntimeError(f"Entrypoint class source missing: {fqcn} ({rel_java})")

    print("[OK] fabric.mod.json, icon resource and entrypoints validated")
    return mod_meta


def check_java_sources_and_translations(repo_root: Path) -> None:
    en_path = repo_root / "src/main/resources/assets/customizablehud/lang/en_us.json"
    es_path = repo_root / "src/main/resources/assets/customizablehud/lang/es_es.json"
    en_lang = load_json(en_path)
    es_lang = load_json(es_path)

    for lang_name, mapping in (("en_us.json", en_lang), ("es_es.json", es_lang)):
        for key, value in mapping.items():
            if not isinstance(value, str) or not value.strip():
                raise RuntimeError(f"Translation key '{key}' in {lang_name} has empty/non-string value")

    missing_in_es = sorted(set(en_lang.keys()) - set(es_lang.keys()))
    missing_in_en = sorted(set(es_lang.keys()) - set(en_lang.keys()))
    if missing_in_es or missing_in_en:
        raise RuntimeError(
            f"Translation key mismatch: missing in es_es={missing_in_es}, missing in en_us={missing_in_en}"
        )

    java_files = sorted((repo_root / "src").rglob("*.java"))
    if not java_files:
        raise RuntimeError("No Java files found under src/")

    code_translation_keys: set[str] = set()
    translatable_pattern = re.compile(r'Component\.translatable\(\s*"([^"]+)"')
    keymapping_pattern = re.compile(r'new\s+KeyMapping\(\s*"([^"]+)"')
    prefixed_pattern = re.compile(r'"((?:customizablehud|key(?:\.category)?\.customizablehud)\.[a-z0-9_.]+)"')

    for jf in java_files:
        rel = jf.relative_to(repo_root).as_posix()
        text = jf.read_text(encoding="utf-8")

        pkg_match = re.search(r"^\s*package\s+([a-zA-Z0-9_.]+)\s*;", text, re.MULTILINE)
        if not pkg_match:
            raise RuntimeError(f"Missing package declaration in {rel}")
        expected_pkg = rel.split("/java/", 1)[1].rsplit("/", 1)[0].replace("/", ".")
        if pkg_match.group(1) != expected_pkg:
            raise RuntimeError(f"Package mismatch in {rel}: {pkg_match.group(1)} != {expected_pkg}")

        if rel.startswith(PURE_DOMAIN_PREFIXES):
            if "net.minecraft" in text or "net.fabricmc" in text:
                raise RuntimeError(f"Minecraft/Fabric dependency leaked into pure module {rel}")

        if rel.startswith("src/client/java/"):
            code_translation_keys.update(translatable_pattern.findall(text))
            code_translation_keys.update(keymapping_pattern.findall(text))
            for key in prefixed_pattern.findall(text):
                if key != "customizablehud.json":
                    code_translation_keys.add(key)

    missing_keys = sorted(code_translation_keys - set(en_lang.keys()))
    if missing_keys:
        raise RuntimeError(f"Translation keys referenced in code missing from lang files: {missing_keys}")

    # key.category.customizablehud.general is mapped by Minecraft from KeyMapping.Category.register(Identifier)
    unused_keys = sorted(set(en_lang.keys()) - code_translation_keys - {"key.category.customizablehud.general"})
    if unused_keys:
        raise RuntimeError(f"Unused translation keys found in lang files: {unused_keys}")

    print(
        f"[OK] Checked {len(java_files)} Java files; verified {len(en_lang)} en_us/es_es translation keys "
        f"({len(code_translation_keys)} direct code keys + keybind category)"
    )


def check_java_version_consistency(repo_root: Path, mod_meta: dict) -> None:
    java_ver_file = repo_root / ".java-version"
    if not java_ver_file.is_file():
        raise RuntimeError("Missing .java-version file")
    java_ver = java_ver_file.read_text(encoding="utf-8").strip()
    if java_ver != "21":
        raise RuntimeError(f"Expected .java-version to be '21', got {java_ver!r}")

    build_gradle = (repo_root / "build.gradle").read_text(encoding="utf-8")
    if "options.release = 21" not in build_gradle or "JavaVersion.VERSION_21" not in build_gradle:
        raise RuntimeError("build.gradle does not consistently enforce Java 21")

    mod_java = (mod_meta.get("depends") or {}).get("java")
    if mod_java != ">=21":
        raise RuntimeError(f"Expected depends.java='>=21' in fabric.mod.json, got {mod_java!r}")

    workflow = (repo_root / ".github/workflows/build.yml").read_text(encoding="utf-8")
    if ".java-version" not in workflow and "java-version: '21'" not in workflow:
        raise RuntimeError("CI workflow does not specify Java 21")

    print("[OK] Java 21 configuration is consistent across .java-version, build.gradle, fabric.mod.json and CI")


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    check_git_diff(repo_root)
    mod_meta = check_metadata_and_resources(repo_root)
    check_java_sources_and_translations(repo_root)
    check_java_version_consistency(repo_root, mod_meta)
    print("All static repository validations passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
