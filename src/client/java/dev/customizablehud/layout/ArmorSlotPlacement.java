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
		float numberTextScale,
		int numberLineHeight,
		boolean alertActive,
		int alertOffsetX,
		int alertOffsetY,
		ArmorMetricPosition metricPosition,
		HudRect screenBounds
) {
	public int unscaledIconX() {
		return unscaledX + iconOffsetX;
	}

	public int unscaledIconY() {
		return unscaledY + iconOffsetY;
	}

	public int unscaledAlertX() {
		return unscaledX + alertOffsetX;
	}

	public int unscaledAlertY() {
		return unscaledY + alertOffsetY;
	}
}
