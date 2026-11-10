package dev.customizablehud.config;

public enum DurabilityDisplayMode {
	POINTS_AND_PERCENT("customizablehud.durability.mode.points_and_percent"),
	PERCENT_ONLY("customizablehud.durability.mode.percent_only"),
	POINTS_ONLY("customizablehud.durability.mode.points_only"),
	NONE("customizablehud.durability.mode.none");

	private final String translationKey;

	DurabilityDisplayMode(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
