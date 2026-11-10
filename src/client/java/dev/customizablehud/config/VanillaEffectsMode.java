package dev.customizablehud.config;

public enum VanillaEffectsMode {
	SHOW_BOTH("customizablehud.effects.vanilla_mode.show_both"),
	CUSTOM_ONLY("customizablehud.effects.vanilla_mode.custom_only"),
	VANILLA_ONLY("customizablehud.effects.vanilla_mode.vanilla_only");

	private final String translationKey;

	VanillaEffectsMode(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
