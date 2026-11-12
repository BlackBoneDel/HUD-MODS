#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

echo "=== Hud Utils - Local Build & Validation (Linux/macOS) ==="

if ! command -v java >/dev/null 2>&1; then
  echo "ERROR: No se encontró el comando 'java' en el PATH."
  echo "Solución: instala JDK 21 (p. ej. Eclipse Temurin 21), configura JAVA_HOME y añade \$JAVA_HOME/bin al PATH."
  echo "Si no quieres instalar Java localmente, puedes ejecutar la compilación en GitHub Actions (.github/workflows/build.yml)."
  exit 1
fi

JAVA_VER_OUT="$(java -version 2>&1)"
JAVA_MAJOR="$(printf '%s\n' "$JAVA_VER_OUT" | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1)"
if [[ -z "$JAVA_MAJOR" || "$JAVA_MAJOR" -lt 21 ]]; then
  echo "ERROR: Se requiere JDK 21 o superior. Versión detectada:"
  echo "$JAVA_VER_OUT"
  exit 1
fi

if ! command -v javac >/dev/null 2>&1; then
  echo "ERROR: Se encontró 'java', pero falta 'javac' (parece haber un JRE en lugar de un JDK completo)."
  echo "Instala un JDK 21 completo (no solo JRE) y configura JAVA_HOME."
  exit 1
fi

echo "Java detectado: JDK $JAVA_MAJOR"

if command -v python3 >/dev/null 2>&1; then
  python3 scripts/validate-repo.py
else
  echo "AVISO: python3 no encontrado; se omiten las validaciones estáticas auxiliares en Python."
fi

chmod +x ./gradlew
echo "Ejecutando: ./gradlew --no-daemon clean test build"
if ! ./gradlew --no-daemon clean test build; then
  echo ""
  echo "ERROR: Falló la compilación o los tests con Gradle."
  echo "Causas frecuentes:"
  echo "  - Sin conexión o bloqueo hacia maven.fabricmc.net / maven.terraformersmc.com / repo.maven.apache.org."
  echo "  - JAVA_HOME apunta a un JRE o a una versión de Java distinta de JDK 21."
  echo "  - Revisa los reportes en build/reports/tests/test/index.html si algún test falló."
  exit 1
fi

if command -v python3 >/dev/null 2>&1; then
  python3 scripts/validate-jar.py
else
  JAR_PATH="build/libs/hud-utils-1.0.0.jar"
  if [[ ! -f "$JAR_PATH" ]]; then
    echo "ERROR: No se encontró el JAR esperado en $JAR_PATH"
    exit 1
  fi
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$JAR_PATH"
  elif command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "$JAR_PATH"
  fi
fi

echo "=== Compilación y validación local completadas ==="
