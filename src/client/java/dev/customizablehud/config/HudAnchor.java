package dev.customizablehud.config;

public enum HudAnchor {
	HOTBAR_LEFT("customizablehud.anchor.hotbar_left", "customizablehud.tooltip.anchor.hotbar_left"),
	HOTBAR_RIGHT("customizablehud.anchor.hotbar_right", "customizablehud.tooltip.anchor.hotbar_right"),
	CENTER_LEFT("customizablehud.anchor.center_left", "customizablehud.tooltip.anchor.center_left"),
	CENTER_RIGHT("customizablehud.anchor.center_right", "customizablehud.tooltip.anchor.center_right"),
	CENTER_BOTTOM("customizablehud.anchor.center_bottom", "customizablehud.tooltip.anchor.center_bottom"),
	TOP_LEFT("customizablehud.anchor.top_left", "customizablehud.tooltip.anchor.top_left"),
	TOP_RIGHT("customizablehud.anchor.top_right", "customizablehud.tooltip.anchor.top_right"),
	BOTTOM_LEFT("customizablehud.anchor.bottom_left", "customizablehud.tooltip.anchor.bottom_left"),
	BOTTOM_RIGHT("customizablehud.anchor.bottom_right", "customizablehud.tooltip.anchor.bottom_right"),
	CUSTOM("customizablehud.anchor.custom", "customizablehud.tooltip.anchor.custom");

	private final String translationKey;
	private final String tooltipKey;

	HudAnchor(String translationKey, String tooltipKey) {
		this.translationKey = translationKey;
		this.tooltipKey = tooltipKey;
	}

	public String translationKey() {
		return translationKey;
	}

	public String tooltipKey() {
		return tooltipKey;
	}
}
