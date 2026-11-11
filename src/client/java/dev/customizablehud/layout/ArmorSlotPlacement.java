package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorSlotKind;

public record ArmorSlotPlacement(
		ArmorSlotKind kind,
		int unscaledX,
		int unscaledY,
		int unscaledWidth,
		int unscaledHeight,
		int slotBoxSize,
		int textBlockHeight,
		HudRect screenBounds
) {
}
