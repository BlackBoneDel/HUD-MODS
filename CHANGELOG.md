# Changelog

## Revision Final — Hud Utils (`schemaVersion = 4`)

### Identidad y empaquetado
- Actualización de nombre visible a **Hud Utils** y autor a **Denvdrite** en `fabric.mod.json`, pantallas del mod, Mod Menu, traducciones, scripts, flujo CI y documentación, manteniendo el ID técnico `customizablehud` y el archivo `config/customizablehud.json` para compatibilidad.
- Nombre de artefacto Gradle y GitHub Actions actualizado a `hud-utils` (`hud-utils-1.0.0.jar`, `hud-utils-source.zip`, `hud-utils-mod-jar`, `hud-utils-source-zip`, `hud-utils-test-reports`).

### Armor HUD
- Tira horizontal compacta de 4 ranuras cuadradas `20 × 20` estilo hotbar (`VANILLA_HOTBAR`, `slotSpacing = 0`) sin huecos visibles entre slots, manteniendo visibles los 4 espacios vacíos con marcadores discretos y manos desactivadas por defecto.
- Botones rápidos **Cerca de barra: Izq.** (`HOTBAR_LEFT`) y **Cerca de barra: Der.** (`HOTBAR_RIGHT`) en Ajustes de Armadura, respetando hotbar, offhand e indicador de ataque sin pisar layouts guardados.
- Escala de colores en 4 rangos evaluada antes del redondeo: `>95%` Blanco (`#FFFFFF`), `>60%..95%` Verde (`#55FF55`), `>20%..60%` Amarillo (`#FFFF55`), `<=20%` Rojo (`#FF5555`), con 3 umbrales y 4 colores configurables (`#RRGGBB` + presets) y nota localizada en Settings.
- Nuevo control **Tamaño números** (`0.50×`–`2.00×`, `1.00×` por defecto) que escala únicamente los números de durabilidad sin agrandar iconos ni deformar los slots cuadrados.
- Nuevo marcador pixelado rojo **`!`** de durabilidad muy baja activado por defecto al `<=10%` (umbral y color configurables, nunca en piezas sanas/vacías/irrompibles, parpadeo opcional desactivado por defecto y sin sonidos).

### Effects HUD
- Nuevos defaults para configuraciones nuevas (y migración única desde el antiguo default `TOP_LEFT`): anclaje en **Centro derecha** (`CENTER_RIGHT`), fondo transparente (`showEffectBackground = false` solo en el HUD propio) y nivel I oculto con números romanos.
- Nueva presentación opcional **PotionHUD (Compacto)** con modo `Normal` y `Mini`, orientación vertical/horizontal, separación, opacidad configurable del fondo cuando está activo y selector en modo Mini entre temporizador textual o barra de ticks reales restantes (con indicador de ticks congelados/suspendidos), sin añadir inventario ni contadores ajenos al alcance.

### Migración, traducciones y pruebas
- Migración de esquema `v1/v2/v3 -> v4` preservando posiciones manuales y `customEffectColors` (incluyendo mods no cargados).
- Paridad completa `en_us.json` / `es_es.json`, pruebas unitarias JUnit 5 ampliadas y checklist `QA-63`–`QA-77` en `docs/MANUAL-QA.md`.

---

## Alpha 2.1 (`schemaVersion = 3`)
- Ajustes de métrica única (`POINTS_ONLY`), `ArmorBorderStyle.OUTER_FRAME`, `ArmorMetricPosition`, espaciado negativo seguro (`-4 px`), colores nativos automáticos de Minecraft por efecto, fondo transparente por defecto y tiradores de 4 esquinas en el Editor Visual.

## Alpha 2 (`schemaVersion = 2`)
- Pantallas con vista previa en vivo, idiomas EN/ES instantáneos, tooltips contextuales, `EffectColorsScreen`, validación robusta y guardado atómico con debounce.
