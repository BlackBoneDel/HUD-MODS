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
| QA-12 | Modos de texto (`153 · 75%`, con máximo `153/203 · 75%`, solo `%`, solo puntos, oculto), nombre del objeto, color por rango y parpadeo bajo umbral | `NOT RUN` | |
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
