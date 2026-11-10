package dev.customizablehud.layout;

import java.util.List;

public record ArmorLayoutResult(
		int originX,
		int originY,
		int unscaledWidth,
		int unscaledHeight,
		float scale,
		HudRect bounds,
		List<ArmorSlotPlacement> slots
) {
}
