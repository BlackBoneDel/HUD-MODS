package dev.customizablehud.config;

public enum HudAnchor {
	HOTBAR_LEFT("customizablehud.anchor.hotbar_left"),
	HOTBAR_RIGHT("customizablehud.anchor.hotbar_right"),
	CENTER_LEFT("customizablehud.anchor.center_left"),
	CENTER_RIGHT("customizablehud.anchor.center_right"),
	CENTER_BOTTOM("customizablehud.anchor.center_bottom"),
	TOP_LEFT("customizablehud.anchor.top_left"),
	TOP_RIGHT("customizablehud.anchor.top_right"),
	BOTTOM_LEFT("customizablehud.anchor.bottom_left"),
	BOTTOM_RIGHT("customizablehud.anchor.bottom_right"),
	CUSTOM("customizablehud.anchor.custom");

	private final String translationKey;

	HudAnchor(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
