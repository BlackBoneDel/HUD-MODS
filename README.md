# Hud Utils

**Alpha 1 (`1.0.0-alpha.1` — tag `v1.0.0-alpha.1`)**

Mod **client-side** ligero, informativo y de solo lectura para **Minecraft Java Edition 1.21.11** (Fabric), creado por **Denvdrite** (ID técnico `customizablehud`), que añade dos módulos independientes y altamente personalizables sin funciones ni grillas de inventario:

1. **HUD de Armadura** (activado por defecto)
2. **HUD de Efectos** (activado por defecto)

> **English summary:** Lightweight, read-only client-side Fabric `1.21.11` mod by **Denvdrite** (`customizablehud`) providing an independent **Armor HUD** and **Effects HUD**, native configuration screens (`English` / `Español` switchable live), optional **Mod Menu** integration, quick presets, and a live drag-and-drop **HUD Editor**.

---

## Requisitos e instalación / Requirements & Installation

- **Minecraft**: `1.21.11` (solo verificado contra las APIs de `1.21.11`)
- **Java**: `21` o superior
- **Fabric Loader**: `0.20.1` o superior (`>=0.20.1`)
- **Fabric API**: `0.141.6+1.21.11` (requerido)
- **Mod Menu** *(opcional)*: `17.0.0` o superior (`>=17.0.0`; añade el botón de configuración en la lista de mods; no es obligatorio)

Instalación:
1. Instala Fabric Loader `>=0.20.1` para Minecraft `1.21.11` y coloca **Fabric API** en tu carpeta `mods/`.
2. Copia **`hud-utils-1.0.0-alpha.1.jar`** en `mods/`.
3. Inicia el cliente. El archivo de configuración se guarda en `config/customizablehud.json` (conservando compatibilidad con configuraciones previas).

---

## Módulos incluidos

### 1. HUD de Armadura (`Armor HUD`)
- **Posición y estilo por defecto**: tira horizontal compacta de 4 ranuras cuadradas contiguas `20 × 20` estilo hotbar (`VANILLA_HOTBAR`, `PER_SLOT`, `slotSpacing = 0`, sin huecos visibles entre slots), colocada junto a la izquierda de la hotbar (`HOTBAR_LEFT`) sin solapar la hotbar, el slot de offhand ni el indicador de ataque, mostrando las 4 piezas en orden `Casco → Pechera → Pantalones → Botas` con siluetas tenues para ranuras vacías (`hideEmptySlots = false`) y manos desactivadas por defecto.
- **Botones rápidos cerca de la hotbar**: **Cerca de barra: Izq.** (`HOTBAR_LEFT`) y **Cerca de barra: Der.** (`HOTBAR_RIGHT`) en los ajustes de Armadura para ubicar la fila a izquierda o derecha de la hotbar sin cubrir offhand ni indicador de ataque y sin bloquear el arrastre manual en el Editor Visual.
- **Información mostrada**:
  - Icono real del objeto (nunca imagen fija) y barra de durabilidad opcional sincronizada con los umbrales configurados.
  - Métrica única configurable en cualquiera de los 4 lados del icono (`Arriba` por defecto, `Abajo`, `Izquierda`, `Derecha`): **Solo puntos** (`POINTS_ONLY` por defecto, centrado encima del icono, opcionalmente `153/203`), **Solo %** (`PERCENT_ONLY`) u **Oculto** (`NONE`) — nunca puntos y porcentaje simultáneamente.
  - **Tamaño números (`numberScale`)**: slider `0.50×` – `2.00×` (`1.00×` por defecto) que escala exclusivamente los números de durabilidad sin agrandar los iconos ni deformar los slots cuadrados.
  - Estilo de borde configurable: **Por casilla** (`PER_SLOT` por defecto) o **Marco exterior continuo** (`OUTER_FRAME` sin divisores internos).
  - Nombres reales de ítems con controles separados para armadura y herramientas en manos (ambos desactivados por defecto, con soporte de nombres de yunque y truncado limpio).
- **Colores y umbrales en 4 rangos** *(evaluados contra el porcentaje exacto sin redondear)*:
  - **`> 95 %`**: Blanco (`#FFFFFF`) — Muy alta / completa.
  - **`> 60 %` y `<= 95 %`**: Verde (`#55FF55`) — Normal.
  - **`> 20 %` y `<= 60 %`**: Amarillo (`#FFFF55`) — Aviso.
  - **`<= 20 %`**: Rojo (`#FF5555`) — Crítico.
  - Los 3 umbrales (`95`, `60`, `20`) y los 4 colores (`#RRGGBB` + presets) son editables en Settings.
- **Alerta de durabilidad muy baja**:
  - Marcador pixelado rojo **`!`** activado por defecto cuando la durabilidad real es **`<= 10 %`** (umbral y color configurables; nunca en piezas sanas, vacías, irrompibles o sin durabilidad) con parpadeo opcional desactivado por defecto y sin sonidos.

### 2. HUD de Efectos (`Effects HUD`)
- **Posición y estilo por defecto**: lista vertical compacta en **Centro derecha** (`CENTER_RIGHT`), transparente sin caja de fondo (`showEffectBackground = false`, afectando solo al HUD personalizado de Hud Utils y dejando intacto el HUD vanilla), nombre + duración visibles y niveles en **números romanos con el nivel I oculto** (`hideLevelOne = true`).
- **Presentación compacta opcional (`PotionHUD`)**:
  - Selector de estilo con **Minecraft compacto** (predeterminado), **Tarjeta limpia**, **Texto minimalista** y **PotionHUD (Compacto)**.
  - En PotionHUD: selector entre modo **Normal** y **Mini**, orientación vertical u horizontal, separación, **Opacidad del fondo** (`0–100%`) cuando el fondo está activo, y en modo Mini selector entre **Temporizador** o **Barra de duración restante** basada en ticks reales (señalizando ticks congelados/suspendidos con `~` y barra pausada).
- **HUD vanilla y colores por efecto**:
  - Por defecto en modo **`Ambos` (`SHOW_BOTH`)**; modos `Solo este HUD` (`CUSTOM_ONLY`) y `Solo vanilla` (`VANILLA_ONLY`). Desactivar Effects HUD mantiene intacto el HUD vanilla.
  - Usa automáticamente el color nativo de Minecraft de cada efecto (`MobEffect.getColor()`) y ofrece la pantalla **Colores por efecto** con buscador, paleta, `#RRGGBB` validado, toggle y restablecimiento (sin teñir iconos).

---

## Controles y editor visual

### Atajos de teclado
Configurables en **Opciones → Controles → Asignación de teclas → Hud Utils**:
- **`O`**: abrir la pantalla principal de configuración.
- **`H`**: abrir directamente el Editor Visual de HUD.
- **Alternar HUD de Armadura** / **Alternar HUD de Efectos**: sin asignar por defecto.

### Editor Visual de HUD
- Arrastrar y soltar dentro de los límites de pantalla y tiradores en las 4 esquinas para escalar proporcionalmente ambos módulos (`0.50x` – `2.50x`).
- Ajuste fino con flechas (`1 px` / `Shift + flechas 5 px`), snap a cuadrícula, guías de alineación, barra superior/inferior y modo individual por pieza.

---

## Persistencia, idioma y compilación

- **Idioma independiente (`English` / `Español`)**: cambio inmediato de pantallas, tooltips y etiquetas sin reiniciar.
- **Persistencia**: `config/customizablehud.json` (`schemaVersion = 4`) con migración compatible desde configuraciones previas (`v1/v2/v3`), escritura atómica y respaldo ante JSON dañado.

### Compilar en GitHub Actions
Los workflows [`.github/workflows/build.yml`](.github/workflows/build.yml) y [`.github/workflows/release.yml`](.github/workflows/release.yml) ejecutan `python3 scripts/validate-repo.py`, `python3 scripts/package-source.py`, `./gradlew --no-daemon clean test build` y `python3 scripts/validate-jar.py`, generando los artefactos:
- **`hud-utils-mod-jar`**: `hud-utils-1.0.0-alpha.1.jar`, `.sha256` y `build-info.txt`.
- **`hud-utils-source-zip`**: `dist/hud-utils-source.zip`.
- **`hud-utils-test-reports`**: reportes de Gradle y JUnit (`build/reports/` y `build/test-results/`).

### Compilar localmente (JDK 21 + acceso Maven)
- Linux / macOS: `./scripts/build-local.sh`
- Windows PowerShell: `.\scripts\build-local.ps1`
- Manual: `./gradlew --no-daemon clean test build && python3 scripts/validate-jar.py`

El JAR de producción se genera en `build/libs/hud-utils-1.0.0-alpha.1.jar`. Las comprobaciones manuales dentro del cliente Minecraft están documentadas en [`docs/MANUAL-QA.md`](docs/MANUAL-QA.md) (actualmente en estado `NOT RUN`).

---

## Alcance y privacidad

- Mod **estrictamente client-side, informativo y de solo lectura**: solo consulta el estado local ya conocido por el cliente (equipamiento equipado, efectos activos y `TickRateManager`) sin modificar mecánicas, paquetes de red ni inventario.
