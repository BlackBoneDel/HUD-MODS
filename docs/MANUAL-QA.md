# Pruebas manuales en cliente Minecraft (QA)

Lista de verificación funcional y visual dentro de **Minecraft Java Edition 1.21.11 (Fabric)** para **Hud Utils** (por **Denvdrite**, ID técnico `customizablehud`).

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
| QA-02 | Arranque con Mod Menu `17.0.0` / `17.0.1`: muestra **Hud Utils** por **Denvdrite** y abre `MainConfigScreen` | `NOT RUN` | |
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
| QA-12 | Modos de métrica única (`Solo puntos` por defecto, `Solo %`, con máximo `153/203`, u `Oculto`), nombre del objeto, color por rango y parpadeo opcional | `NOT RUN` | |
| QA-13 | Estilos visuales (`Hotbar vanilla`, `Ranura inventario`, `Minimalista`, `Sin bordes`), orientación horizontal/vertical y reordenamiento de piezas | `NOT RUN` | |

## 3. HUD de Efectos

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-14 | Efectos beneficiosos, neutrales y perjudiciales en `CENTER_RIGHT` muestran icono, nombre, nivel (nivel I oculto) y duración sin fondo | `NOT RUN` | |
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

## 5. Pruebas de persistencia, idioma, tooltips, previews, armadura y colores por efecto

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
| QA-37 | Verificar en Armor HUD que solo aparece una métrica por vez (`Solo puntos` por defecto o `Solo %`, nunca puntos + porcentaje simultáneamente) | `NOT RUN` | |
| QA-38 | Verificar en Armor HUD que el porcentaje o los puntos aparecen centrados respecto al icono en Arriba/Abajo o alineados limpiamente en Izquierda/Derecha | `NOT RUN` | |
| QA-39 | Verificar que `100%`, valores grandes en puntos (ej. `342/407`) y **Nombres armadura** activo con ítems renombrados en yunque no se solapan entre ranuras contiguas | `NOT RUN` | |
| QA-40 | Verificar que las piezas de armadura usan por defecto separación compacta similar a los slots de la hotbar (`slotSpacing = 0`) y manos (y nombres en manos) ocultos hasta activarlos | `NOT RUN` | |
| QA-41 | Probar umbrales por defecto en piezas reales dañadas: `>95%` blanco, `>60%..95%` verde, `>20%..60%` amarillo, `<=20%` rojo | `NOT RUN` | |
| QA-42 | Editar umbrales y colores en **Ajustes del HUD de armadura** y verificar coherencia inmediata entre vista previa, HUD real, texto y barra | `NOT RUN` | |
| QA-43 | Equipar objeto sin durabilidad (calabaza tallada), dejar ranuras vacías y equipar objeto irrompible (`∞`): nunca se reporta `100%` falso | `NOT RUN` | |
| QA-44 | Verificar en **Ajustes del HUD de efectos** que `HUD vanilla` inicia en `Ambos` (`SHOW_BOTH`) y conmuta correctamente entre `Ambos`, `Solo este HUD` y `Solo vanilla` | `NOT RUN` | |
| QA-45 | Abrir **Colores por efecto**, buscar `Regeneración` (`minecraft:regeneration`), aplicar rosa (`#FF66B2`) y comprobar en vista previa y partida que nombre y duración cambian a rosa mientras el icono conserva intacta su textura original | `NOT RUN` | |
| QA-46 | Activar/desactivar un color personalizado en **Colores por efecto** y usar **Restablecer**: al desactivar conserva el hex `#RRGGBB` guardado volviendo al color normal, y al restablecer elimina el override | `NOT RUN` | |
| QA-47 | Añadir una entrada de efecto de otro mod en `config/customizablehud.json`, editar otras opciones desde el juego, guardar y confirmar que la entrada del otro mod sigue intacta en el JSON | `NOT RUN` | |
| QA-48 | Corromper `config/customizablehud.json` o provocar fallo de escritura (archivo de solo lectura): confirmar respaldo `.corrupt-*.bak` + aviso único al recuperar y badge real `Error al guardar` (`Save failed`) sin falsos positivos | `NOT RUN` | |
| QA-49 | Activar un resource pack que sobrescriba claves `assets/customizablehud/lang/en_us.json` o `es_es.json` (o nombres vanilla) y verificar carga correcta tras recargar recursos (`F3+T`) y fallback seguro ante traducciones ausentes | `NOT RUN` | |

## 6. Ajustes y correcciones Alpha 2.1

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-50 | Arranque limpio sin `config/customizablehud.json`: Armor HUD inicia con **Solo puntos** (`POINTS_ONLY`) y Effects HUD inicia con **Fondo del efecto** desactivado (`OFF`) | `NOT RUN` | |
| QA-51 | Migración única desde configuraciones Alpha 2 (`schemaVersion: 2`) manteniendo idioma, posición personalizada y colores por efecto | `NOT RUN` | |
| QA-52 | Activar **Parpadeo alerta** (`Alert Blink`) con una pieza `<=10%` y otras sanas: solo parpadea suavemente el marcador `!` de esa pieza afectada sin ocultar nunca el módulo, el icono ni la métrica; al desactivar se detiene al instante | `NOT RUN` | |
| QA-53 | Ajustar **Espaciado** en Armadura desde `-4 px` hasta `16 px`: a `-4 px` los iconos `16x16` se tocan limpiamente sin recortarse ni solapar métricas o nombres largos | `NOT RUN` | |
| QA-54 | Cambiar **Estilo de borde** en Armadura entre `Por casilla` (`PER_SLOT`) y `Marco exterior` (`OUTER_FRAME`): en `Marco exterior` todas las piezas visibles forman una sola barra continua sin divisores internos | `NOT RUN` | |
| QA-55 | Cambiar **Lado métrica** (`Arriba`, `Abajo`, `Izquierda`, `Derecha`) en Armadura: verificar centrado arriba/abajo, alineación limpia izquierda/derecha y actualización en vivo en vista previa y HUD | `NOT RUN` | |
| QA-56 | Activar **Mano principal** / **Mano secundaria** y probar **Nombres armadura** vs **Nombres en manos** (incluyendo ítems renombrados en yunque con nombres largos): manos inician sin nombre por defecto y truncan limpiamente sin solaparse | `NOT RUN` | |
| QA-57 | En Effects HUD sin personalizaciones: aplicar múltiples efectos (Velocidad, Fuerza, Regeneración, Veneno, Ceguera, etc.) y comprobar que cada uno usa automáticamente su color nativo de Minecraft sin rosa global | `NOT RUN` | |
| QA-58 | Abrir **Colores por efecto**: comprobar que cada efecto muestra su muestra de color automático real, editar cualquier efecto vanilla o de mod, desactivar sin borrar hex y restablecer a automático | `NOT RUN` | |
| QA-59 | En Effects HUD: comprobar que `showEffectBackground = false` elimina todo relleno en todos los estilos manteniendo intacto el HUD vanilla y el toggle independiente de **Borde del efecto** | `NOT RUN` | |
| QA-60 | En **Editor visual del HUD**: mover Effects HUD arrastrando su área directamente y redimensionarlo proporcionalmente desde los tiradores de las 4 esquinas, comprobando anclaje opuesto y sincronía con el slider de `Escala` | `NOT RUN` | |
| QA-61 | Verificar prioridad de hit-test entre los tiradores de esquina y el arrastre para mover, límites de escala (`0.50x`–`2.50x`), límites de pantalla y comportamiento bajo distintas resoluciones y `GUI Scale` | `NOT RUN` | |
| QA-62 | Verificar guardado tras redimensionar/mover Effects HUD, cambiar posición/borde/espaciado de Armadura y reiniciar el cliente | `NOT RUN` | |

## 7. Revisión final — Hud Utils (`schemaVersion = 4`)

| ID | Prueba | Estado | Notas |
| --- | --- | --- | --- |
| QA-63 | Verificar marca **Hud Utils** y autor **Denvdrite** en la lista de Mod Menu, pantalla principal del mod, categoría de controles y ausencia de errores de recursos con `customizablehud` como mod ID y `config/customizablehud.json` | `NOT RUN` | |
| QA-64 | Arranque limpio en Survival/Creative: Armor HUD muestra tira horizontal compacta de 4 ranuras cuadradas `20×20` estilo hotbar (`Casco → Pechera → Pantalones → Botas`) pegadas entre sí sin huecos, manteniendo marcadores discretos para slots vacíos y manos ocultas | `NOT RUN` | |
| QA-65 | Probar los botones rápidos **Cerca de barra: Izq.** (`HOTBAR_LEFT`) y **Cerca de barra: Der.** (`HOTBAR_RIGHT`) con escudo/tótem en mano secundaria (mano principal derecha e izquierda) e indicador de ataque en hotbar: cero solapamiento y sigue siendo arrastrable en el Editor Visual | `NOT RUN` | |
| QA-66 | Verificar que los presets rápidos / botón cerca de hotbar no sobrescriben un layout manual guardado al reiniciar Minecraft | `NOT RUN` | |
| QA-67 | Probar en partida los 4 rangos de color por durabilidad exacta sin redondear: `100%` y `95.2%` blanco (`#FFFFFF`), `95%` y `60.4%` verde (`#55FF55`), `60%` y `20.4%` amarillo (`#FFFF55`), `20%` y `0%` rojo (`#FF5555`) | `NOT RUN` | |
| QA-68 | Editar en Ajustes de Armadura los 3 umbrales (`Alta >`, `Aviso >`, `Crítico <=`) y los 4 colores mediante presets o `#RRGGBB` manual (incluyendo entradas inválidas parciales): validación consistente sin crashes y actualización inmediata en vista previa y HUD | `NOT RUN` | |
| QA-69 | Mover el slider **Tamaño números** entre `0.50×` y `2.00×` (default `1.00×`): escala únicamente los números de durabilidad sin escalar iconos ni deformar los slots cuadrados, sin solapes entre dígitos vecinos ni recortes bajo distintas `GUI Scale` y resource packs de fuente | `NOT RUN` | |
| QA-70 | Dañar una pieza a `<=10%` de durabilidad real manteniendo otras sanas, una ranura vacía y una calabaza/ítem irrompible: el símbolo pixelado rojo **`!`** aparece por defecto únicamente sobre la pieza afectada (visible incluso en modo `Oculto` de números o con otros colores), nunca en sanas/vacías/sin durabilidad | `NOT RUN` | |
| QA-71 | Cambiar umbral `Alerta en` y color del marcador `!` en Settings; activar/desactivar **Parpadeo alerta** (`OFF` por defecto) comprobando que nunca oculta el HUD y no reproduce sonidos por defecto | `NOT RUN` | |
| QA-72 | Arranque limpio en Effects HUD: aparece centrado a la derecha (`CENTER_RIGHT`), transparente sin caja de fondo (sin alterar el fondo vanilla), con colores propios de cada efecto y números romanos ocultando el nivel I (`Regeneración 0:42`) | `NOT RUN` | |
| QA-73 | Migrar un `config/customizablehud.json` previo con Effects en posición personalizada (`CUSTOM` o `TOP_RIGHT`) y otro con el antiguo default (`TOP_LEFT`, `0, 0`): la posición personalizada se conserva intacta y solo el antiguo default migra a `CENTER_RIGHT` | `NOT RUN` | |
| QA-74 | Activar estilo **PotionHUD (Compacto)** en Effects HUD y probar modos **Normal** y **Mini**, orientación horizontal/vertical, separación (`Spacing`) y **Opacidad del fondo** con **Fondo del efecto** activado/desactivado | `NOT RUN` | |
| QA-75 | En **PotionHUD Mini**: conmutar **Indicador mini** entre **Temporizador** y **Barra duración** con efectos recién aplicados, refrescados y permanentes (`∞`); ejecutar `/tick freeze` en juego y comprobar que ni la barra ni el temporizador se presentan como cuenta regresiva activa exacta | `NOT RUN` | |
| QA-76 | Confirmar que **Hud Utils** no incluye ni dibuja ningún grid de inventario, contador de ítems, espacios libres ni flechas al activar PotionHUD o Near Hotbar | `NOT RUN` | |
| QA-77 | Reiniciar Minecraft tras modificar tamaño de números, umbrales, alerta `!`, PotionHUD Mini/opacidad y posición `CENTER_RIGHT`/`HOTBAR_RIGHT` y verificar persistencia completa en `config/customizablehud.json` | `NOT RUN` | |
