# Hud Utils

Mod **client-side** ligero, informativo y modular para **Minecraft Java Edition 1.21.11** (Fabric), creado por **Denvdrite** (id técnico `customizablehud`), que añade dos módulos independientes y altamente personalizables:

1. **HUD de Armadura** (activado por defecto)
2. **HUD de Efectos** (activado por defecto)

Incluye pantallas de configuración nativas (sin depender de Cloth Config, YACL ni Mod Menu), integración opcional con **Mod Menu**, tres presets rápidos y un **Editor Visual de HUD** con arrastrar y soltar en vivo.

---

## Requisitos e instalación

- **Minecraft**: `1.21.11` (solo verificado contra las APIs de `1.21.11`)
- **Java**: `21` o superior
- **Fabric Loader**: `0.20.1` o superior
- **Fabric API**: `0.141.6+1.21.11` (compatible con `1.21.11`)
- **Mod Menu** *(opcional)*: `17.0.0` (compatible también con `17.0.1` mediante `>=17.0.0`; si está instalado, añade el botón de configuración en la lista de mods; no es obligatorio)

Instalación:
1. Instala Fabric Loader para Minecraft `1.21.11` y coloca **Fabric API** en tu carpeta `mods/`.
2. Copia `hud-utils-1.0.0.jar` en `mods/`.
3. Inicia el cliente. El archivo de configuración se mantiene en `config/customizablehud.json` (conservando compatibilidad con instalaciones previas).

---

## Módulos incluidos

### 1. HUD de Armadura
- **Posición y estilo por defecto**: tira horizontal compacta de 4 ranuras cuadradas `20 × 20` estilo hotbar (`VANILLA_HOTBAR`, `PER_SLOT`, `slotSpacing = 0`, sin huecos visibles entre slots), colocada junto a la izquierda de la hotbar (`HOTBAR_LEFT`) sin solapar la hotbar, el slot de offhand ni el indicador de ataque, mostrando siempre las 4 piezas (`Casco → Pechera → Pantalones → Botas`) con marcadores discretos para espacios vacíos (`hideEmptySlots = false`) y manos desactivadas por defecto.
- **Acceso rápido cerca de la hotbar**: botones directos **Cerca de barra: Izq.** (`HOTBAR_LEFT`) y **Cerca de barra: Der.** (`HOTBAR_RIGHT`) en los ajustes de Armadura para ubicar la fila a izquierda o derecha de la hotbar sin cubrir offhand ni indicador de ataque y sin bloquear el movimiento manual en el Editor Visual.
- **Información mostrada**:
  - Icono real del objeto (nunca imagen fija) y barra de durabilidad opcional sincronizada con los umbrales configurados.
  - Métrica única configurable en cualquiera de los 4 lados del icono (`Arriba` por defecto, `Abajo`, `Izquierda`, `Derecha`): **Solo puntos** (`POINTS_ONLY` por defecto, centrado encima del icono, opcionalmente `153/203`), **Solo %** (`PERCENT_ONLY`) u **Oculto** (`NONE`) — nunca puntos y porcentaje simultáneamente.
  - **Escala independiente de números (`Tamaño números`)**: slider `0.50×` – `2.00×` (`1.00×` por defecto) que escala exclusivamente los números de durabilidad sin agrandar los iconos ni deformar los slots cuadrados, calculando ancho/alto reales para evitar recortes o solapamientos bajo distintas escalas GUI y resource packs de fuentes.
  - Estilo de borde configurable: **Por casilla** (`PER_SLOT` por defecto) o **Marco exterior continuo** (`OUTER_FRAME` sin divisores internos).
  - Nombres reales de ítems con controles separados para armadura y herramientas en manos (ambos desactivados por defecto, con soporte de nombres personalizados de yunque y truncado limpio sin tapar el número).
- **Colores y umbrales en 4 rangos** *(evaluados contra el porcentaje real sin redondear)*:
  - **`> 95 %`**: Blanco (`#FFFFFF`) — Muy alta / completa.
  - **`> 60 %` y `<= 95 %`**: Verde (`#55FF55`) — Normal.
  - **`> 20 %` y `<= 60 %`**: Amarillo (`#FFFF55`) — Aviso.
  - **`<= 20 %`**: Rojo (`#FF5555`) — Crítico.
  - Los 3 umbrales (`95`, `60`, `20`) y los 4 colores (`#RRGGBB` + rotación de presets) son configurables en Settings con validación estricta e indicación localizada de la regla sin redondeo.
- **Símbolo de alerta de durabilidad muy baja**:
  - Marcador pixelado rojo **`!`** activado por defecto cuando la durabilidad real de una pieza es **`<= 10 %`** (umbral y color configurables en Settings).
  - Aparece únicamente en el slot de la pieza afectada (nunca en piezas sanas, vacías, irrompibles o sin durabilidad), independientemente de la métrica o color elegido, y con animación de parpadeo opcional desactivada por defecto (`lowDurabilityWarning = false`) que afecta únicamente al propio marcador sin ocultar el HUD ni reproducir sonidos.
- **Manos adicionales y estilos**: interruptores independientes para **Mano principal**, **Mano secundaria** y **Nombres en manos**; estilos `Hotbar vanilla`, `Ranura inventario`, `Minimalista` y `Sin bordes`; orientación `Horizontal`/`Vertical`, escala general (`0.50x` – `2.50x`), espaciado (`-4` a `16 px`), ocultación opcional de vacíos, reordenamiento y modo individual.

### 2. HUD de Efectos
- **Posición y estilo por defecto**: lista vertical compacta centrada a la derecha de la pantalla (`CENTER_RIGHT`), transparente sin caja de fondo (`showEffectBackground = false`, afectando solo al HUD personalizado de Hud Utils y manteniendo intacto el HUD vanilla), nombre + duración y niveles en **números romanos con el nivel I oculto** (`hideLevelOne = true`, mostrando p. ej. `Regeneración 0:42` y `Velocidad II 2:34`).
- **Presentación alternativa inspirada en PotionHUD (opción en Settings)**:
  - Selector de estilo con **Minecraft compacto** (predeterminado actual de Hud Utils), **Tarjeta limpia**, **Texto minimalista** y el modo compacto **PotionHUD (Compacto)**.
  - En PotionHUD: selector entre modo **Normal** y **Mini** (`PotionHudMode`), orientación vertical u horizontal, control de separación, transparencia ajustable del fondo (`Opacidad del fondo`, `0–100%`) cuando el fondo está activado, y en modo Mini selector entre **Temporizador** textual o **Barra de duración restante** basada en ticks reales de Minecraft (señalizando claramente cuando los ticks están congelados/suspendidos).
  - Reutiliza los mismos nombres localizados, colores por efecto, filtros y controles de visibilidad, sin añadir ningún componente de inventario.
- **Comportamiento frente al HUD vanilla**:
  - Por defecto en modo **`Ambos` (`SHOW_BOTH`)**; modos `Solo este HUD` (`CUSTOM_ONLY`) y `Solo vanilla` (`VANILLA_ONLY`). Desactivar Effects HUD deja intacto el HUD vanilla.
- **Colores por efecto**:
  - Sin personalización activa, cada efecto vanilla o de mod usa automáticamente su color registrado en Minecraft (`MobEffect.getColor()`, con ajuste de legibilidad para tonos muy oscuros y respaldo seguro).
  - Pantalla dedicada **Colores por efecto** con buscador, paleta accesible, entrada `#RRGGBB` validada, toggle independiente y restablecimiento; colorea texto y acento sin teñir jamás los iconos.

---

## Controles y editor visual

### Atajos de teclado
Configurables en **Opciones → Controles → Asignación de teclas → Hud Utils**:
- **`O`**: abrir la pantalla principal de configuración.
- **`H`**: abrir directamente el Editor Visual de HUD.
- **Alternar HUD de Armadura** / **Alternar HUD de Efectos**: sin asignar por defecto.

### Editor Visual de HUD
- Arrastrar y soltar dentro de los límites de pantalla para ambos módulos.
- Tiradores en las 4 esquinas para redimensionar/escalar proporcionalmente ambos módulos (visibles incluso con fondo de efectos desactivado).
- Ajuste fino con flechas (`1 px` / `Shift + flechas 5 px`), snap a cuadrícula, guías de alineación, barra superior/inferior y modo individual por pieza.

---

## Persistencia, idioma y compilación

- **Idioma independiente (`English` / `Español`)** con actualización inmediata en pantallas, tooltips y etiquetas.
- **Persistencia**: `config/customizablehud.json` (`schemaVersion = 4`) con migración automática desde v1/v2/v3 preservando posiciones personalizadas, ajustes válidos y colores de mods externos.

### Compilar en GitHub Actions
El workflow [`.github/workflows/build.yml`](.github/workflows/build.yml) ejecuta `python3 scripts/validate-repo.py`, `python3 scripts/package-source.py`, `./gradlew --no-daemon clean test build` y `python3 scripts/validate-jar.py`, subiendo los artefactos temporales (30 días):
- **`hud-utils-mod-jar`**: `hud-utils-1.0.0.jar`, `.sha256` y `build-info.txt`.
- **`hud-utils-source-zip`**: `dist/hud-utils-source.zip`.
- **`hud-utils-test-reports`**: reportes de Gradle y JUnit (`build/reports/` y `build/test-results/`).

### Compilar localmente (JDK 21 + acceso Maven)
- Linux / macOS: `./scripts/build-local.sh`
- Windows PowerShell: `.\scripts\build-local.ps1`
- Manual: `./gradlew --no-daemon clean test build && python3 scripts/validate-jar.py`

El JAR final compilado se genera en `build/libs/hud-utils-1.0.0.jar`. Las pruebas manuales pendientes dentro del juego se encuentran en [`docs/MANUAL-QA.md`](docs/MANUAL-QA.md).
