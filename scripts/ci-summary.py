#!/usr/bin/env python3
"""Parse JUnit XML test results and emit a factual CI console & GitHub Actions job summary."""

from __future__ import annotations

import os
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path


def parse_gradle_properties(path: Path) -> dict[str, str]:
    props: dict[str, str] = {}
    if not path.is_file():
        return props
    for line in path.read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        k, v = stripped.split("=", 1)
        props[k.strip()] = v.strip()
    return props


def collect_junit_stats(results_dir: Path) -> tuple[list[dict], dict[str, int]]:
    suites: list[dict] = []
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    if not results_dir.is_dir():
        return suites, totals

    for xml_path in sorted(results_dir.glob("TEST-*.xml")):
        tree = ET.parse(xml_path)
        root = tree.getroot()
        tests = int(root.attrib.get("tests", "0"))
        failures = int(root.attrib.get("failures", "0"))
        errors = int(root.attrib.get("errors", "0"))
        skipped = int(root.attrib.get("skipped", "0"))
        time_sec = root.attrib.get("time", "0")
        name = root.attrib.get("name", xml_path.stem)

        suites.append(
            {
                "name": name,
                "tests": tests,
                "failures": failures,
                "errors": errors,
                "skipped": skipped,
                "time": time_sec,
            }
        )
        totals["tests"] += tests
        totals["failures"] += failures
        totals["errors"] += errors
        totals["skipped"] += skipped
    return suites, totals


def read_key_value_file(path: Path) -> dict[str, str]:
    data: dict[str, str] = {}
    if not path.is_file():
        return data
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line:
            k, v = line.split("=", 1)
            data[k.strip()] = v.strip()
    return data


def java_version_line() -> str:
    try:
        out = subprocess.check_output(["java", "-version"], stderr=subprocess.STDOUT, text=True)
        return out.splitlines()[0].strip() if out else "unknown"
    except Exception:
        return "not found"


def main() -> int:
    repo_root = Path(__file__).resolve().parent.parent
    props = parse_gradle_properties(repo_root / "gradle.properties")
    suites, totals = collect_junit_stats(repo_root / "build" / "test-results" / "test")
    build_info = read_key_value_file(repo_root / "build" / "release-meta" / "build-info.txt")

    commit = os.environ.get("GITHUB_SHA", build_info.get("git_commit", "local"))
    ref_name = os.environ.get("GITHUB_REF_NAME", build_info.get("git_branch", "local"))
    java_ver = java_version_line()
    source_zip = repo_root / "dist" / "customizablehud-source.zip"

    lines: list[str] = [
        "## Customizable HUD — Resumen de compilación CI",
        "",
        f"- **Commit**: `{commit}`",
        f"- **Rama / Ref**: `{ref_name}`",
        f"- **Java**: `{java_ver}`",
        f"- **Minecraft**: `{props.get('minecraft_version', '?')}` | **Fabric Loader**: `{props.get('loader_version', '?')}` | **Fabric API**: `{props.get('fabric_api_version', '?')}` | **Loom**: `{props.get('loom_version', '?')}` | **Mod Menu (opcional)**: `{props.get('modmenu_version', '?')}`",
        "",
        "### Comandos ejecutados",
        "- `python3 scripts/validate-repo.py`",
        "- `python3 scripts/package-source.py`",
        "- `./gradlew --no-daemon clean test build`",
        "- `python3 scripts/validate-jar.py` *(si la compilación pasó)*",
        "",
        "### Resultados de pruebas JUnit 5",
    ]

    if suites:
        lines.extend(
            [
                "| Suite | Tests | Fallos | Errores | Omitidos | Tiempo (s) |",
                "| --- | ---: | ---: | ---: | ---: | ---: |",
            ]
        )
        for s in suites:
            lines.append(
                f"| `{s['name']}` | {s['tests']} | {s['failures']} | {s['errors']} | {s['skipped']} | {s['time']} |"
            )
        lines.append("")
        lines.append(
            f"**Total**: {totals['tests']} pruebas "
            f"(fallos: {totals['failures']}, errores: {totals['errors']}, omitidos: {totals['skipped']})."
        )
    else:
        lines.append("No se encontraron archivos XML de resultados JUnit en `build/test-results/test/`.")

    lines.extend(["", "### Artefactos generados"])
    if build_info:
        lines.append(
            f"- **JAR remapeado (`customizablehud-mod-jar`)**: `{build_info.get('artifact_name')}` "
            f"(`{build_info.get('artifact_size_bytes')} bytes`, SHA-256: `{build_info.get('artifact_sha256')}`)"
        )
    else:
        lines.append("- **JAR remapeado (`customizablehud-mod-jar`)**: no generado o validación no completada.")

    if source_zip.is_file():
        lines.append(
            f"- **Código fuente (`customizablehud-source-zip`)**: `dist/customizablehud-source.zip` "
            f"(`{source_zip.stat().st_size} bytes`)"
        )
    else:
        lines.append("- **Código fuente (`customizablehud-source-zip`)**: no generado.")

    lines.extend(
        [
            "- **Reportes (`customizablehud-test-reports`)**: `build/reports/` y `build/test-results/` (si existen).",
            "",
            "> Descarga los artefactos desde la sección **Artifacts** en la parte inferior de esta ejecución en la pestaña **Actions** del repositorio (retención temporal de 30 días).",
        ]
    )

    summary_md = "\n".join(lines) + "\n"
    print(summary_md)

    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as fh:
            fh.write(summary_md)

    return 0


if __name__ == "__main__":
    sys.exit(main())
