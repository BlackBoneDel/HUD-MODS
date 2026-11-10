#!/usr/bin/env python3
"""Create a reproducible source ZIP of the working tree without external dependencies."""

from __future__ import annotations

import argparse
import hashlib
import os
import stat
import sys
import zipfile
from pathlib import Path

EXCLUDED_DIRS = {
    ".git",
    ".gradle",
    "build",
    "dist",
    "out",
    "classes",
    "run",
    "bin",
    ".idea",
    ".vscode",
    ".settings",
    ".eclipse",
    ".metadata",
    "__pycache__",
    "Unselected files",
}

EXCLUDED_EXACT_NAMES = {
    ".DS_Store",
    ".netrc",
    ".npmrc",
    ".pypirc",
    ".git-credentials",
    "credentials.json",
    "id_rsa",
    "id_ed25519",
    "id_ecdsa",
    "id_dsa",
}

EXCLUDED_SUFFIXES = (
    ".iml",
    ".ipr",
    ".iws",
    ".launch",
    ".hprof",
    ".jfr",
    ".tmp",
    ".bak",
    ".swp",
    ".swo",
    "~",
    ".pem",
    ".key",
    ".p12",
    ".pfx",
    ".jks",
    ".keystore",
)

FIXED_ZIP_TIME = (2026, 1, 1, 0, 0, 0)

REQUIRED_ENTRIES = (
    "gradlew",
    "gradlew.bat",
    "gradle/wrapper/gradle-wrapper.jar",
    "gradle/wrapper/gradle-wrapper.properties",
    "src/main/resources/fabric.mod.json",
    "src/main/resources/assets/customizablehud/icon.png",
    "src/main/resources/assets/customizablehud/lang/en_us.json",
    "src/main/resources/assets/customizablehud/lang/es_es.json",
    ".github/workflows/build.yml",
)


def is_sensitive_or_excluded_file(rel_posix: str, name: str) -> bool:
    if name in EXCLUDED_EXACT_NAMES:
        return True
    if name == ".env" or (name.startswith(".env.") and name not in {".env.example", ".env.sample"}):
        return True
    if name.startswith(("hs_err_", "replay_")) and name.endswith(".log"):
        return True
    lower = name.lower()
    if lower.endswith(EXCLUDED_SUFFIXES):
        return True
    # Catch obvious local secret files without touching normal project files.
    if rel_posix == ".git/config" or rel_posix.startswith(".git/"):
        return True
    return False


def collect_source_files(repo_root: Path) -> list[tuple[Path, str]]:
    files: list[tuple[Path, str]] = []
    for current_root, dir_names, file_names in os.walk(repo_root, topdown=True, followlinks=False):
        dir_names[:] = sorted(d for d in dir_names if d not in EXCLUDED_DIRS)
        current_path = Path(current_root)
        for file_name in sorted(file_names):
            full_path = current_path / file_name
            if full_path.is_symlink():
                continue
            rel_posix = full_path.relative_to(repo_root).as_posix()
            if is_sensitive_or_excluded_file(rel_posix, file_name):
                continue
            files.append((full_path, rel_posix))
    files.sort(key=lambda item: item[1])
    return files


def create_reproducible_zip(repo_root: Path, output_zip: Path) -> int:
    output_zip.parent.mkdir(parents=True, exist_ok=True)
    entries = collect_source_files(repo_root)
    if not entries:
        raise RuntimeError(f"No source files found under {repo_root}")

    tmp_zip = output_zip.with_suffix(".zip.tmp")
    try:
        with zipfile.ZipFile(tmp_zip, "w", compression=zipfile.ZIP_DEFLATED, compresslevel=9) as zf:
            for full_path, rel_posix in entries:
                mode = full_path.stat().st_mode
                is_exec = bool(mode & (stat.S_IXUSR | stat.S_IXGRP | stat.S_IXOTH))
                posix_perms = 0o755 if is_exec else 0o644
                info = zipfile.ZipInfo(filename=rel_posix, date_time=FIXED_ZIP_TIME)
                info.compress_type = zipfile.ZIP_DEFLATED
                info.create_system = 3  # Unix permissions metadata
                info.external_attr = (stat.S_IFREG | posix_perms) << 16
                zf.writestr(info, full_path.read_bytes())
        tmp_zip.replace(output_zip)
    finally:
        if tmp_zip.exists():
            tmp_zip.unlink()
    return len(entries)


def verify_zip(output_zip: Path) -> tuple[int, str]:
    if not output_zip.is_file():
        raise RuntimeError(f"ZIP file was not created: {output_zip}")

    with zipfile.ZipFile(output_zip, "r") as zf:
        bad_member = zf.testzip()
        if bad_member is not None:
            raise RuntimeError(f"Corrupt ZIP entry detected: {bad_member}")
        names = set(zf.namelist())
        missing = [req for req in REQUIRED_ENTRIES if req not in names]
        if missing:
            raise RuntimeError(f"Missing required entries in source ZIP: {missing}")

        has_client_java = any(n.startswith("src/client/java/") and n.endswith(".java") for n in names)
        has_test_java = any(n.startswith("src/test/java/") and n.endswith(".java") for n in names)
        if not has_client_java or not has_test_java:
            raise RuntimeError("Source ZIP is missing client or test Java sources")

        forbidden = [
            n
            for n in names
            if n.startswith((".git/", ".gradle/", "build/", "dist/"))
            or n.endswith((".env", ".pem", ".key", ".p12", ".pfx", ".jks", ".keystore"))
        ]
        if forbidden:
            raise RuntimeError(f"Forbidden paths leaked into source ZIP: {forbidden[:10]}")

    sha256 = hashlib.sha256(output_zip.read_bytes()).hexdigest()
    return len(names), sha256


def main() -> int:
    parser = argparse.ArgumentParser(description="Package Customizable HUD source tree into a reproducible ZIP.")
    parser.add_argument(
        "--output",
        default="dist/customizablehud-source.zip",
        help="Output ZIP path relative to repository root (default: dist/customizablehud-source.zip)",
    )
    args = parser.parse_args()

    repo_root = Path(__file__).resolve().parent.parent
    output_zip = (repo_root / args.output).resolve()

    count = create_reproducible_zip(repo_root, output_zip)
    verified_count, sha256 = verify_zip(output_zip)
    size_bytes = output_zip.stat().st_size

    print(f"Source ZIP created : {output_zip}")
    print(f"Relative path      : {output_zip.relative_to(repo_root)}")
    print(f"Entries packed     : {count} (verified: {verified_count})")
    print(f"Size               : {size_bytes} bytes ({size_bytes / 1024:.2f} KiB)")
    print(f"SHA-256            : {sha256}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
