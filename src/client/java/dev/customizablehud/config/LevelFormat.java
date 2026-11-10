package dev.customizablehud.config;

public enum LevelFormat {
	ROMAN("customizablehud.effects.level_format.roman"),
	ARABIC("customizablehud.effects.level_format.arabic");

	private final String translationKey;

	LevelFormat(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
