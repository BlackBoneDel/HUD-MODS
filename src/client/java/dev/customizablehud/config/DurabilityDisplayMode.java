package dev.customizablehud.config;

public enum DurabilityDisplayMode {
	PERCENT_ONLY("customizablehud.durability.mode.percent_only", "customizablehud.tooltip.durability_mode.percent_only"),
	POINTS_ONLY("customizablehud.durability.mode.points_only", "customizablehud.tooltip.durability_mode.points_only"),
	NONE("customizablehud.durability.mode.none", "customizablehud.tooltip.durability_mode.none");

	private final String translationKey;
	private final String tooltipKey;

	DurabilityDisplayMode(String translationKey, String tooltipKey) {
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
