package dev.customizablehud.config;

public enum PotionHudMode {
	NORMAL("customizablehud.effects.potion_mode.normal", "customizablehud.tooltip.effects.potion_mode.normal"),
	MINI("customizablehud.effects.potion_mode.mini", "customizablehud.tooltip.effects.potion_mode.mini");

	private final String translationKey;
	private final String tooltipKey;

	PotionHudMode(String translationKey, String tooltipKey) {
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
