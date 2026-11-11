# Pruebas manuales en cliente Minecraft (QA)

Lista de verificación funcional y visual dentro de **Minecraft Java Edition 1.21.11 (Fabric)** para `Customizable HUD`.

**Regla de honestidad:** ninguna casilla debe marcarse como `PASS` sin haberla ejecutado realmente dentro de un cliente vivo de Minecraft `1.21.11`.

---

## Entorno de prueba (rellenar al probar)

- **Minecraft**: `1.21.11`
- **Fabric Loader**: `________`
- **Fabric API**: `________`
- **Mod Menu**: `[ ] Instalado (versión: ______)` / `[ ] Ausente`
- **Archivo JAR probado**: `________` (SHA-256: `________`)
- **Resolución / GUI Scale**: `________`

---

## 1. Carga del mod, Mod Menu y atajos

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-01 | Arranque del cliente sin errores en `latest.log` solo con Fabric API (sin Mod Menu) | `NOT RUN` | |
| QA-02 | Arranque con Mod Menu `17.0.0` / `17.0.1`: botón de configuración abre `MainConfigScreen` | `NOT RUN` | |
| QA-03 | Atajo `O` abre la pantalla principal de configuración | `NOT RUN` | |
| QA-04 | Atajo `H` abre directamente el Editor Visual de HUD | `NOT RUN` | |
| QA-05 | Atajos configurables para alternar Armor HUD / Effects HUD muestran aviso en actionbar | `NOT RUN` | |
| QA-06 | Escribir en chat, yunque, cartel o buscador del inventario no dispara `O`, `H` ni toggles | `NOT RUN` | |

## 2. HUD de Armadura

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-07 | Armadura completa equipada se muestra junto a la izquierda de la hotbar (`HOTBAR_LEFT`) sin tapar el slot de mano secundaria | `NOT RUN` | |
| QA-08 | Piezas ausentes muestran silueta tenue con `Ocultar ranuras vacías = No` y desaparecen con `Sí` | `NOT RUN` | |
| QA-09 | Objetos sin durabilidad (ej. calabaza tallada / cabeza / bloque) no muestran barra ni texto de durabilidad ni alerta falsa | `NOT RUN` | |
| QA-10 | Objetos con componente `minecraft:unbreakable` muestran `∞` y sin barra de daño | `NOT RUN` | |
| QA-11 | Activación independiente de **Mano principal** y **Mano secundaria** (incluyendo stacks > 1) | `NOT RUN` | |
| QA-12 | Modos de métrica única (`Solo %` por defecto, `Solo puntos`, con máximo `153/203`, u `Oculto`), nombre del objeto, color por rango y parpadeo opcional | `NOT RUN` | |
| QA-13 | Estilos visuales (`Hotbar vanilla`, `Ranura inventario`, `Minimalista`, `Sin bordes`), orientación horizontal/vertical y reordenamiento de piezas | `NOT RUN` | |

## 3. HUD de Efectos

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-14 | Efectos beneficiosos, neutrales y perjudiciales en `TOP_LEFT` muestran icono, nombre, nivel y duración | `NOT RUN` | |
| QA-15 | Niveles I, II, IV y niveles altos; opción **Ocultar nivel I** y formato romano/arábigo | `NOT RUN` | |
| QA-16 | Duraciones cortas (`<10s`, `<30s`), largas (`>1h`) y duración infinita (`∞`) con parpadeo y coloreado opcionales | `NOT RUN` | |
| QA-17 | Efectos sin partículas (`/effect give @s speed 30 1 true`) respetan **Efectos sin partículas** | `NOT RUN` | |
| QA-18 | Modos frente al HUD vanilla: `Ambos` (`SHOW_BOTH`), `Solo personalizado` (`CUSTOM_ONLY` oculta iconos vanilla), `Solo vanilla` (`VANILLA_ONLY`) | `NOT RUN` | |
| QA-19 | Al desactivar **HUD de Efectos**, el HUD vanilla de efectos permanece intacto | `NOT RUN` | |

## 4. Independencia, Editor Visual, Visibilidad y Persistencia

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-20 | Activar y desactivar Armor HUD y Effects HUD de manera totalmente independiente | `NOT RUN` | |
| QA-21 | En el Editor Visual: arrastrar módulos, mover con flechas `1px` y `Shift+Flechas 5px`, barra arriba/abajo, aviso de superposición | `NOT RUN` | |
| QA-22 | Modo individual por pieza en Armor HUD: activar con nombres/máximo activados sin salto brusco, mover cada pieza, no acumular desfase contra bordes y restablecer diseño | `NOT RUN` | |
| QA-23 | Escala (`0.50x`–`2.50x`) sincronizada entre slider y cuadro numérico, y cambio de resolución / `GUI Scale` manteniendo todo dentro de pantalla | `NOT RUN` | |
| QA-24 | Visibilidad: `F1` oculta ambos módulos junto con el HUD vanilla; opciones de ocultar en inventario, chat y menú de pausa responden correctamente | `NOT RUN` | |
| QA-25 | Presets (`Vanilla compacto`, `Texto compacto`, `Lectura ampliada`), guardado, `Cancelar` descartando cambios, persistencia en `config/customizablehud.json` entre reinicios y recuperación con `.corrupt-*.bak` ante JSON dañado | `NOT RUN` | |

## 5. Nuevas pruebas de persistencia, idioma, tooltips, previews, armadura y colores por efecto

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-26 | Modificar cualquier ajuste desde Mod Menu / pantalla principal, pulsar `Guardar` o cerrar y reiniciar Minecraft: todos los ajustes se conservan en `config/customizablehud.json` | `NOT RUN` | |
| QA-27 | Modificar posiciones en el Editor Visual, pulsar `Guardar diseño`, reiniciar el juego y verificar que las posiciones exactas se conservan | `NOT RUN` | |
| QA-28 | Editar ajustes dentro de un mundo, salir al menú principal y entrar a otro mundo o servidor: la configuración no se reinicia en ningún cambio de sesión | `NOT RUN` | |
| QA-29 | Cambiar **Idioma del mod** entre `English` y `Español` desde la pantalla principal y comprobar actualización inmediata en todas las pantallas, tooltips y textos del HUD sin reiniciar ni alterar el idioma global de Minecraft | `NOT RUN` | |
| QA-30 | Probar el mod en `Español` mientras Minecraft está en `English`, y el mod en `English` mientras Minecraft está en `Español` | `NOT RUN` | |
| QA-31 | Comprobar tooltips contextuales tanto al pasar el ratón como al navegar con teclado (`Tab` / flechas) sobre botones, toggles, ciclos y sliders (incluyendo valores dinámicos) | `NOT RUN` | |
| QA-32 | Abrir **Ajustes del HUD de armadura** con armadura real equipada y sin armadura/fuera de mundo: el panel de vista previa refleja la armadura real o datos de ejemplo sin filtrar ejemplos al HUD del juego | `NOT RUN` | |
| QA-33 | Desactivar **HUD de armadura** y abrir su pantalla de ajustes: la vista previa sigue mostrando piezas de ejemplo con aviso de módulo desactivado, sin activar el HUD en partida | `NOT RUN` | |
| QA-34 | Abrir **Ajustes del HUD de efectos** con efectos activos y sin efectos/en menú principal: la vista previa refleja los efectos reales o una lista de ejemplo sin filtrar ejemplos al HUD real | `NOT RUN` | |
| QA-35 | Probar las 4 combinaciones independientes de **Fondo del efecto** (`ON/OFF`) y **Borde del efecto** (`ON/OFF`) y verificar actualización inmediata en vista previa y HUD real | `NOT RUN` | |
| QA-36 | Abrir el Editor Visual en resoluciones estándar y reducidas (`GUI Scale` 1, 2, 3, 4): ningún botón ni `Guardar diseño` / `Save Layout` queda cortado ni fuera de la pantalla | `NOT RUN` | |
| QA-37 | Verificar en Armor HUD que solo aparece una métrica por vez (`Solo %` o `Solo puntos`, nunca puntos + porcentaje simultáneamente) y `POINTS_AND_PERCENT` legado migra a `Solo %` | `NOT RUN` | |
| QA-38 | Verificar en Armor HUD que el porcentaje o los puntos aparecen centrados horizontalmente encima de cada icono correspondiente | `NOT RUN` | |
| QA-39 | Verificar que `100%`, valores grandes en puntos (ej. `342/407`) y **Mostrar nombre** activo con ítems renombrados en yunque no se solapan entre ranuras contiguas | `NOT RUN` | |
| QA-40 | Verificar que las piezas de armadura usan por defecto separación compacta similar a los slots de la hotbar (`slotSpacing = 0`) y manos ocultas hasta activarlas | `NOT RUN` | |
| QA-41 | Probar umbrales por defecto en piezas reales dañadas: `>60%` (ej. `61%` y `60.4%`) verde, `<=60% y >20%` (ej. `60%` y `20.4%`) amarillo, `<=20%` (ej. `20%` y `0%`) rojo tanto en texto como en barra | `NOT RUN` | |
| QA-42 | Editar umbrales de Aviso/Crítico y colores Sano/Aviso/Crítico en **Ajustes del HUD de armadura** y verificar coherencia inmediata entre vista previa, HUD real, texto y barra | `NOT RUN` | |
| QA-43 | Equipar objeto sin durabilidad (calabaza tallada), dejar ranuras vacías y equipar objeto irrompible (`∞`): nunca se reporta `100%` falso | `NOT RUN` | |
| QA-44 | Verificar en **Ajustes del HUD de efectos** que `HUD vanilla` inicia en `Ambos` (`SHOW_BOTH`) y conmuta correctamente entre `Ambos`, `Solo este HUD` y `Solo vanilla` | `NOT RUN` | |
| QA-45 | Abrir **Colores por efecto**, buscar `Regeneración` (`minecraft:regeneration`), aplicar rosa (`#FF66B2`) y comprobar en vista previa y partida que nombre y duración cambian a rosa mientras el icono conserva intacta su textura original | `NOT RUN` | |
| QA-46 | Activar/desactivar un color personalizado en **Colores por efecto** y usar **Restablecer**: al desactivar conserva el hex `#RRGGBB` guardado volviendo al color normal, y al restablecer elimina el override | `NOT RUN` | |
| QA-47 | Añadir una entrada de efecto de otro mod en `config/customizablehud.json`, editar otras opciones desde el juego, guardar y confirmar que la entrada del otro mod sigue intacta en el JSON | `NOT RUN` | |
| QA-48 | Corromper `config/customizablehud.json` o provocar fallo de escritura (archivo de solo lectura): confirmar respaldo `.corrupt-*.bak` + aviso único al recuperar y badge real `Error al guardar` (`Save failed`) sin falsos positivos | `NOT RUN` | |
| QA-49 | Activar un resource pack que sobrescriba claves `assets/customizablehud/lang/en_us.json` o `es_es.json` (o nombres vanilla) y verificar carga correcta tras recargar recursos (`F3+T`) y fallback seguro ante traducciones ausentes | `NOT RUN` | |
