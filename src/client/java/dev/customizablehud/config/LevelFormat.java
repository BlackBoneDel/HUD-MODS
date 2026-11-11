package dev.customizablehud.config;

public enum LevelFormat {
	ROMAN("customizablehud.effects.level_format.roman", "customizablehud.tooltip.effects.level_format.roman"),
	ARABIC("customizablehud.effects.level_format.arabic", "customizablehud.tooltip.effects.level_format.arabic");

	private final String translationKey;
	private final String tooltipKey;

	LevelFormat(String translationKey, String tooltipKey) {
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
