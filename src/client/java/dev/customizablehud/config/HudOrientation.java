package dev.customizablehud.config;

public enum HudOrientation {
	HORIZONTAL("customizablehud.orientation.horizontal", "customizablehud.tooltip.orientation.horizontal"),
	VERTICAL("customizablehud.orientation.vertical", "customizablehud.tooltip.orientation.vertical");

	private final String translationKey;
	private final String tooltipKey;

	HudOrientation(String translationKey, String tooltipKey) {
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
