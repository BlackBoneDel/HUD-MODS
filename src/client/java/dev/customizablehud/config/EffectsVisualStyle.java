package dev.customizablehud.config;

public enum EffectsVisualStyle {
	MINECRAFT_COMPACT("customizablehud.effects.style.minecraft_compact"),
	CLEAN_CARD("customizablehud.effects.style.clean_card"),
	MINIMAL_TEXT("customizablehud.effects.style.minimal_text");

	private final String translationKey;

	EffectsVisualStyle(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
