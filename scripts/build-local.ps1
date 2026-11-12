#Requires -Version 5.1
Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
Set-Location $RootDir

Write-Host "=== Hud Utils - Local Build & Validation (Windows PowerShell) ==="

$javaCmd = Get-Command java -ErrorAction SilentlyContinue
if (-not $javaCmd) {
    Write-Error @"
ERROR: No se encontró 'java' en el PATH.
Solución: instala JDK 21 (por ejemplo Eclipse Temurin 21), define JAVA_HOME y agrega %JAVA_HOME%\bin al PATH.
Si no deseas instalar Java localmente, puedes compilar mediante GitHub Actions (.github/workflows/build.yml).
"@
    exit 1
}

$javacCmd = Get-Command javac -ErrorAction SilentlyContinue
if (-not $javacCmd) {
    Write-Error "ERROR: Se encontró 'java', pero falta 'javac'. Necesitas un JDK 21 completo, no solo un JRE."
    exit 1
}

$javaVersionOutput = (& java -version 2>&1) | Out-String
if ($javaVersionOutput -match 'version "(\d+)') {
    $javaMajor = [int]$Matches[1]
} else {
    $javaMajor = 0
}

if ($javaMajor -lt 21) {
    Write-Error "ERROR: Se requiere JDK 21 o superior. Salida detectada:`n$javaVersionOutput"
    exit 1
}
Write-Host "Java detectado: JDK $javaMajor"

$pythonCmd = Get-Command python -ErrorAction SilentlyContinue
if (-not $pythonCmd) {
    $pythonCmd = Get-Command python3 -ErrorAction SilentlyContinue
}
if ($pythonCmd) {
    & $pythonCmd.Source scripts/validate-repo.py
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    Write-Warning "Python no encontrado; se omiten las validaciones estáticas en Python."
}

Write-Host "Ejecutando: .\gradlew.bat --no-daemon clean test build"
& .\gradlew.bat --no-daemon clean test build
if ($LASTEXITCODE -ne 0) {
    Write-Error @"
ERROR: Falló '.\gradlew.bat --no-daemon clean test build'.
Causas frecuentes:
  - Sin conexión a maven.fabricmc.net, maven.terraformersmc.com o Mojang.
  - JAVA_HOME incorrecto o versión de Java incompatible.
  - Revisa build\reports\tests\test\index.html si algún test unitario falló.
"@
    exit $LASTEXITCODE
}

if ($pythonCmd) {
    & $pythonCmd.Source scripts/validate-jar.py
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} else {
    $props = @{}
    foreach ($line in Get-Content (Join-Path $RootDir "gradle.properties")) {
        if ($line -match '^\s*([^#=\s]+)\s*=\s*(.+?)\s*$') {
            $props[$Matches[1]] = $Matches[2]
        }
    }
    $jarName = "$($props['archives_base_name'])-$($props['mod_version']).jar"
    $jarPath = Join-Path $RootDir "build\libs\$jarName"
    if (-not (Test-Path $jarPath)) {
        Write-Error "ERROR: No se encontró el JAR esperado en $jarPath"
        exit 1
    }
    $hash = Get-FileHash -Path $jarPath -Algorithm SHA256
    Write-Host "JAR validado: $jarPath"
    Write-Host "SHA-256: $($hash.Hash.ToLowerInvariant())"
}

Write-Host "=== Compilación y validación local completadas ==="
