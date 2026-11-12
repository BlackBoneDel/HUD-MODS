package dev.customizablehud.config;

public enum PotionMiniDisplayMode {
	TIMER_TEXT("customizablehud.effects.mini_display.timer", "customizablehud.tooltip.effects.mini_display.timer"),
	DURATION_BAR("customizablehud.effects.mini_display.duration_bar", "customizablehud.tooltip.effects.mini_display.duration_bar");

	private final String translationKey;
	private final String tooltipKey;

	PotionMiniDisplayMode(String translationKey, String tooltipKey) {
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
