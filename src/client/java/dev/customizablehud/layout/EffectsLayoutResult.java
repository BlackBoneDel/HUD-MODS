package dev.customizablehud.layout;

import java.util.List;

public record EffectsLayoutResult(
		int originX,
		int originY,
		int unscaledWidth,
		int unscaledHeight,
		float scale,
		HudRect bounds,
		List<EffectRowPlacement> rows
) {
}
