package dev.customizablehud.layout;

public record EffectRowPlacement(
		int index,
		int unscaledX,
		int unscaledY,
		int unscaledWidth,
		int unscaledHeight,
		HudRect screenBounds
) {
}
