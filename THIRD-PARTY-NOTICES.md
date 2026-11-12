# Third-Party Notices and Functional References

**Hud Utils** (`customizablehud`, by **Denvdrite**) is built as an original client-side Fabric implementation for Minecraft `1.21.11`.

## Bundled / Compile-time Ecosystem Dependencies (Not Redistributed Inside Mod JAR)

- **Minecraft / Mojang Official Mappings** — compiled via Fabric Loom (`net.fabricmc.fabric-loom-remap`).
- **Fabric Loader & Fabric API** — licensed under Apache License 2.0 (`https://github.com/FabricMC/fabric`).
- **Mod Menu** *(optional compile-time integration only, never bundled in JAR)* — licensed under MIT (`https://github.com/TerraformersMC/ModMenu`).

## Visual & Functional Inspiration References (Clean-Room Implementation)

No third-party source code or texture files were copied into this repository. The following projects were inspected solely as visual/functional references:

1. **uku3lig/armor-hud** (`https://github.com/uku3lig/armor-hud`, MIT License) — functional/visual reference for compact hotbar-style square armor slots, hotbar-adjacent spacing, and low-durability warning indicator positioning.
2. **NotRyken/armor-hud-numbers** (`https://github.com/NotRyken/armor-hud-numbers`, CC0-1.0 Universal) — functional/visual reference for centering remaining durability numbers above each armor piece icon.
3. **InventoryHUD+ / CuiZhenhang/Inventory-HUD-Plus** (`https://github.com/CuiZhenhang/Inventory-HUD-Plus`, GitHub MIT License; Modrinth listing not fetched/verified from sandbox) — functional reference only for the optional compact PotionHUD status-effect presentation (Normal/Mini modes, orientation, opacity, timer/duration bar). All PotionHUD layout and rendering code in Hud Utils was written cleanly from scratch without copying any external code or assets.
