package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorMetricPosition;
import dev.customizablehud.config.ArmorSlotKind;

public record ArmorSlotPlacement(
		ArmorSlotKind kind,
		int unscaledX,
		int unscaledY,
		int unscaledWidth,
		int unscaledHeight,
		int slotBoxSize,
		int textBlockHeight,
		int iconOffsetX,
		int iconOffsetY,
		int textOffsetX,
		int textOffsetY,
		int textAreaWidth,
		ArmorMetricPosition metricPosition,
		HudRect screenBounds
) {
	public int unscaledIconX() {
		return unscaledX + iconOffsetX;
	}

	public int unscaledIconY() {
		return unscaledY + iconOffsetY;
	}
}
