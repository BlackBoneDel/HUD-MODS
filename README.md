# Customizable HUD

Mod **client-side** ligero, informativo y modular para **Minecraft Java Edition 1.21.11** (Fabric) que añade dos módulos independientes y altamente personalizables:

1. **HUD de Armadura** (activado por defecto)
2. **HUD de Efectos** (activado por defecto)

Incluye pantallas de configuración nativas (sin depender de Cloth Config, YACL ni Mod Menu), integración opcional con **Mod Menu**, tres presets neutrales listos para usar y un **Editor Visual de HUD** con arrastrar y soltar en vivo.

---

## Requisitos e instalación

- **Minecraft**: `1.21.11` (solo verificado contra las APIs de `1.21.11`)
- **Java**: `21` o superior
- **Fabric Loader**: `0.20.1` o superior
- **Fabric API**: `0.141.6+1.21.11` (compatible con `1.21.11`)
- **Mod Menu** *(opcional)*: `17.0.0` (compatible también con `17.0.1` mediante `>=17.0.0`; si está instalado, añade el botón de configuración en la lista de mods; no es obligatorio)

Instalación:
1. Instala Fabric Loader para Minecraft `1.21.11` y coloca **Fabric API** en tu carpeta `mods/`.
2. Copia `customizablehud-1.0.0.jar` en `mods/`.
3. Inicia el cliente. El archivo de configuración se genera automáticamente en `config/customizablehud.json`.

---

## Módulos incluidos

### 1. HUD de Armadura
- **Posición por defecto**: fila horizontal compacta junto a la izquierda de la hotbar (`HOTBAR_LEFT`), estilo `VANILLA_HOTBAR`, mostrando las 4 piezas de armadura (`Casco → Pechera → Pantalones → Botas`) con separación compacta equivalente a la barra rápida.
- **Información mostrada**:
  - Icono real del objeto (y silueta vanilla tenue cuando la ranura está vacía y no se ocultan vacías).
  - Barra de durabilidad opcional sincronizada con los mismos rangos y colores de durabilidad.
  - Métrica única configurable en cualquiera de los 4 lados del icono (`Arriba` por defecto, `Abajo`, `Izquierda`, `Derecha`): **Solo puntos** (`POINTS_ONLY` por defecto, opcionalmente `153/203`), **Solo %** (`PERCENT_ONLY`) u **Oculto** (`NONE`) — nunca puntos y porcentaje simultáneamente.
  - Estilo de borde configurable: **Por casilla** (`PER_SLOT` por defecto) o **Marco exterior continuo** (`OUTER_FRAME` sin divisores internos).
  - Nombres reales de ítems con controles separados para armadura y para herramientas en manos (ambos desactivados por defecto, con soporte de nombres personalizados de yunque, traducciones y truncado seguro).
- **Manos adicionales**: interruptores independientes para **Mano principal**, **Mano secundaria** y **Nombres en manos** (desactivados por defecto).
- **Estilos visuales**: `Hotbar vanilla`, `Ranura inventario`, `Minimalista` y `Sin bordes`.
- **Umbrales y seguridad**:
  - Rango de colores en 3 niveles evaluado sin redondear (`>60%` sano/verde, `<=60%` y `>20%` aviso/amarillo, `<=20%` crítico/rojo), con umbrales y colores configurables por el usuario y parpadeo opcional de advertencia aislado solo al borde/indicador sin ocultar nunca iconos ni el módulo (desactivado por defecto).
  - Orientación `Horizontal` / `Vertical`, escala (`0.50x` – `2.50x`), espaciado (`-4` a `16 px` con acercamiento seguro de iconos hasta tocarse sin solapamiento), anclajes estándar y personalizados, ocultar ranuras vacías, reordenamiento de piezas, vista previa en vivo (con muestras si no hay ítems) y modo individual por pieza.
  - Respeta automáticamente F1 (`hideGui`, al integrarse con las capas vanilla de `HudElementRegistry`) y permite configurar ocultación en inventario, chat y menú de pausa.
  - Manejo seguro de ranuras vacías, objetos irrompibles (`∞`), objetos sin durabilidad (como calabazas o cabezas, sin reportar `100%`) y objetos de otros mods.

### 2. HUD de Efectos
- **Posición por defecto**: lista vertical compacta estilo Minecraft en la esquina superior izquierda (`TOP_LEFT`), estilo `Compacto Minecraft`.
- **Comportamiento frente al HUD vanilla**:
  - Por defecto en modo **`Ambos` (`SHOW_BOTH`)**: mantiene intacto el HUD vanilla de efectos y muestra también el HUD personalizado.
  - Modos disponibles: `Ambos`, `Solo personalizado` (implementado mediante `HudElementRegistry.replaceElement(VanillaHudElements.STATUS_EFFECTS, ...)`) y `Solo vanilla`.
  - Si desactivas el módulo de Efectos, el HUD vanilla permanece siempre intacto.
- **Fondo, borde y colores por efecto**:
  - **Fondo del efecto** desactivado por defecto (`showEffectBackground = false`) en todos los estilos visuales, con interruptores independientes para **Fondo del efecto** y **Borde del efecto** (las 4 combinaciones disponibles tanto en partida como en la vista previa en vivo).
  - **Color automático nativo por efecto**: sin personalización activa, cada efecto vanilla o de mods usa automáticamente el color propio registrado en Minecraft (`MobEffect.getColor()`, con ajuste de legibilidad para tintes muy oscuros o fallback neutro seguro si un efecto no define color).
  - Pantalla dedicada **Colores por efecto** con buscador y muestra visual del color automático para efectos vanilla y modificados, paleta accesible (incluyendo rosa `#FF66B2`), entrada `#RRGGBB` validada, toggle independiente y botón restablecer; aplica el color únicamente al nombre, tiempo y acento sin teñir jamás el icono del efecto.
- **Elementos configurables de forma independiente**:
  - Icono del efecto (con respaldo gráfico seguro si un efecto de otro mod no tiene sprite estándar).
  - Nombre, duración (`m:ss`, `h:mm:ss`, `∞` para duración infinita y consciente del tickrate) y nivel.
  - Ocultar nivel I (`true` por defecto), formato romano (`II`, `IV`) o arábigo (`2`, `4`) con respaldo numérico seguro para niveles extremos.
  - Mostrar efectos sin partículas (`true` por defecto), agrupar por categoría (`Beneficioso`, `Neutral`, `Perjudicial`) y ordenar por categoría/nombre, duración ascendente/descendente, alfabético o del juego.

---

## Controles y editor visual

### Atajos de teclado
Configurables en **Opciones → Controles → Asignación de teclas → Customizable HUD**:
- **`O`**: abrir la pantalla principal de configuración.
- **`H`**: abrir directamente el Editor Visual de HUD.
- **Alternar HUD de Armadura**: sin asignar por defecto.
- **Alternar HUD de Efectos**: sin asignar por defecto.

Los atajos nunca se disparan mientras hay pantallas abiertas ni al escribir en chat, carteles, yunques o campos de texto.

### Editor Visual de HUD
- **Arrastrar y soltar** con el ratón dentro de los límites de pantalla tanto para Armadura como para Efectos.
- **Tiradores visibles en las 4 esquinas** para redimensionar/escalar directamente ambos módulos desde el overlay (con prioridad sobre el arrastre para mover, esquina opuesta anclada, límites de escala y pantalla, visibles incluso con fondo de efectos desactivado).
- **Ajuste fino con teclado**: flechas mueven `1 px`; `Shift + flechas` mueven `5 px`.
- **Módulos desactivados visibles**: siguen renderizándose atenuados con etiqueta `(desactivado)` para poder moverlos, escalarlos o reactivarlos.
- **Modo individual de armadura**: permite mover cada pieza por separado y restablecerlas al diseño automático con un botón.
- **Escala interactiva**: deslizador (`0.50x` – `2.50x`) y cuadro numérico sincronizados.
- **Snap y guías**: ajuste a cuadrícula, líneas guía de alineación y advertencia visual si se detecta superposición entre módulos o con la hotbar vanilla.
- **Barra reubicable**: botón para mover la barra de controles entre arriba y abajo y no tapar la zona inferior al editar cerca de la hotbar.

---

## Idioma, tooltips y configuración (`config/customizablehud.json`)

- **Idioma del mod independiente (`English` / `Español`)**: selector directo en la pantalla principal que actualiza al instante pantallas, tooltips, mensajes y textos del HUD sin reiniciar ni modificar el idioma global de Minecraft, respetando resource packs.
- **Tooltips contextuales**: cada control, selector cíclico y deslizador muestra ayuda contextual tanto con ratón como con navegación por teclado.
- **Presets rápidos**:
  1. **Vanilla compacto**: configuración equilibrada predeterminada (`HOTBAR_LEFT`, `Solo puntos` sin máximo, efectos compactos sin caja de fondo arriba a la izquierda, `Ambos`).
  2. **Texto compacto**: diseño ligero vertical centrado a la izquierda, sin bordes, incluye mano principal y secundaria (sin nombres de mano), efectos minimalistas.
  3. **Lectura ampliada**: escala aumentada, `Solo puntos` con máximo visible (`153/203`), nombres de armadura, aviso de baja durabilidad y tarjetas limpias.

El archivo `config/customizablehud.json` (actualmente `schemaVersion = 3`) incluye migración automática desde versiones previas (v1/v2 → v3), validación y saneamiento automático, guardado automático con debounce, flush al cerrar pantalla / desconectar / detener el cliente, estado real (`Guardado` / `Sin guardar` / `Error al guardar`), escritura atómica segura y respaldo (`.corrupt-*.bak`) si el JSON está dañado.

---

## Compilación y validación automática

### Opción 1: Compilar y descargar desde GitHub Actions (sin instalar Java ni Gradle)

Si el repositorio está alojado en GitHub y **GitHub Actions** está habilitado, no necesitas instalar JDK ni Gradle en tu equipo:

1. El workflow [`.github/workflows/build.yml`](.github/workflows/build.yml) se ejecuta en `push`, `pull_request` y de forma manual (`workflow_dispatch`).
2. Ejecuta `python3 scripts/validate-repo.py`, empaqueta el código fuente (`dist/customizablehud-source.zip`), compila y corre las pruebas unitarias con JDK 21 (`./gradlew --no-daemon clean test build`), valida el JAR remapeado final con `python3 scripts/validate-jar.py` y calcula su SHA-256.
3. **Importante**: Actions no habrá generado artefactos hasta que el workflow se ejecute sobre una rama, PR o commit visible en el repositorio. Los artefactos de Actions son temporales (retención configurada a **30 días**).
4. Para descargar los archivos tras una ejecución completada:
   - Abre la pestaña **Actions** del repositorio en GitHub.
   - Selecciona la ejecución deseada del workflow **build**.
   - Baja hasta la sección **Artifacts** y descarga:
     - **`customizablehud-mod-jar`**: contiene el mod compilado (`customizablehud-1.0.0.jar`), su checksum (`.sha256`) y `build-info.txt` (solo se sube si compila y pasa la validación).
     - **`customizablehud-source-zip`**: contiene `customizablehud-source.zip` con el código fuente del repositorio.
     - **`customizablehud-test-reports`**: contiene los reportes de Gradle y JUnit (`build/reports/` y `build/test-results/`).

### Opción 2: Compilar localmente desde el código fuente

Requiere **JDK 21** (no solo JRE) y acceso a los repositorios Maven de Gradle, Fabric, Mojang y TerraformersMC (`https://maven.terraformersmc.com/releases/`):

- **Linux / macOS**:
  ```bash
  ./scripts/build-local.sh
  ```
- **Windows (PowerShell)**:
  ```powershell
  .\scripts\build-local.ps1
  ```
- O directamente mediante el Gradle Wrapper incluido:
  ```bash
  ./gradlew --no-daemon clean test build
  python3 scripts/validate-jar.py
  ```

Para generar manualmente el paquete de código fuente en `dist/customizablehud-source.zip`:

```bash
python3 scripts/package-source.py
```

Al finalizar correctamente una compilación local, el `.jar` remapeado final se encontrará en:

```text
build/libs/customizablehud-1.0.0.jar
```

La lista de comprobaciones manuales dentro del juego está documentada en [`docs/MANUAL-QA.md`](docs/MANUAL-QA.md).

---

## Alcance y privacidad

- Mod **estrictamente client-side e informativo**: solo lee el estado ya conocido por el cliente (equipamiento equipado y efectos activos del jugador local) sin modificar mecánicas, paquetes de red ni lógica del servidor.
