# Changelog

## [1.0.0-alpha.1] — Primera alpha pública (`v1.0.0-alpha.1`)

Primera publicación pública en canal **alpha** de **Hud Utils** (por **Denvdrite**) para Minecraft Java Edition **`1.21.11`** (Fabric). No existen releases ni tags públicos previos en `main`; este release consolida todo el desarrollo previo de la rama de trabajo (que usó hitos internos `schemaVersion = 2` y `schemaVersion = 3` antes de esta primera alpha pública con `schemaVersion = 4`).

### Identidad, empaquetado y alcance
- Nombre visible **Hud Utils** y autor **Denvdrite** en `fabric.mod.json`, pantallas del mod, Mod Menu, traducciones, scripts y documentación, manteniendo el ID técnico `customizablehud`, el paquete `dev.customizablehud.*` y el archivo `config/customizablehud.json`.
- Empaquetado como `hud-utils-1.0.0-alpha.1.jar` (`version = 1.0.0-alpha.1`).
- Mod estrictamente **client-side** y de solo lectura sobre el estado local del cliente; sin grillas de inventario, contadores de ítems, espacios libres ni widgets de inventario.

### Armor HUD
- Tira horizontal compacta de 4 ranuras cuadradas contiguas `20 × 20` estilo hotbar (`VANILLA_HOTBAR`, `PER_SLOT`, `slotSpacing = 0`) en orden `Casco → Pechera → Pantalones → Botas`, manteniendo visibles las ranuras vacías con silueta tenue y manos desactivadas por defecto.
- Botones rápidos **Cerca de barra: Izq.** (`HOTBAR_LEFT`) y **Cerca de barra: Der.** (`HOTBAR_RIGHT`) respetando hotbar, slot de offhand e indicador de ataque sin bloquear el arrastre manual en el Editor Visual.
- Métrica única configurable (`Solo puntos` por defecto, `Solo %`, con máximo opcional o `Oculto`) y escala independiente de números (`Tamaño números`, `0.50×`–`2.00×`, `1.00×` por defecto) sin deformar iconos ni casillas.
- Escala de colores en 4 rangos evaluada antes del redondeo: `>95%` Blanco (`#FFFFFF`), `>60%..95%` Verde (`#55FF55`), `>20%..60%` Amarillo (`#FFFF55`), `<=20%` Rojo (`#FF5555`), con 3 umbrales y 4 colores `#RRGGBB` configurables.
- Marcador pixelado rojo **`!`** activado por defecto en piezas con durabilidad real `<=10%` (umbral y color configurables; nunca en piezas sanas, vacías, irrompibles o sin durabilidad; parpadeo opcional desactivado por defecto y sin sonidos).

### Effects HUD
- Posición por defecto en **Centro derecha** (`CENTER_RIGHT`), fondo transparente por defecto (`showEffectBackground = false`, sin modificar el HUD vanilla), nombre + duración y niveles en números romanos con el nivel I oculto (`hideLevelOne = true`).
- Colores nativos automáticos de Minecraft por efecto y pantalla **Colores por efecto** con buscador, paleta accesible, hex `#RRGGBB`, toggle independiente y restablecimiento (colorea texto/acento sin teñir iconos).
- Presentación compacta opcional **PotionHUD** (`Normal` / `Mini`, orientación vertical/horizontal, opacidad configurable del fondo y temporizador o barra de ticks reales restantes consciente de ticks congelados/suspendidos).

### Persistencia, idioma y validación
- Pantallas nativas (Mod Menu opcional `>=17.0.0`), Editor Visual con arrastrar/soltar y tiradores de 4 esquinas, presets rápidos e idioma independiente (`English` / `Español` al instante con paridad de 364 claves).
- Persistencia en `config/customizablehud.json` (`schemaVersion = 4`) con migración automática compatible desde `v1/v2/v3` preservando posiciones manuales y configuraciones válidas.
- Validación automatizada con `validate-repo.py`, `validate-jar.py` y 29 pruebas unitarias JUnit 5; la verificación manual dentro del juego (`docs/MANUAL-QA.md`) figura como `NOT RUN`.

---

## Notas históricas internas previas a `v1.0.0-alpha.1` (sin release público)
- **`schemaVersion = 3` (iteración interna previa)**: métrica única `POINTS_ONLY`, `OUTER_FRAME`, espaciado negativo seguro, colores nativos automáticos y tiradores de 4 esquinas.
- **`schemaVersion = 2` (iteración interna previa)**: vistas previas en vivo, i18n EN/ES en caliente, tooltips contextuales, `EffectColorsScreen` y guardado atómico con debounce.
