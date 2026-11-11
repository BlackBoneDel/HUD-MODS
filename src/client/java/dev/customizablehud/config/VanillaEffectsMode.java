package dev.customizablehud.config;

public enum VanillaEffectsMode {
	SHOW_BOTH("customizablehud.effects.vanilla_mode.show_both", "customizablehud.tooltip.effects.vanilla_mode.show_both"),
	CUSTOM_ONLY("customizablehud.effects.vanilla_mode.custom_only", "customizablehud.tooltip.effects.vanilla_mode.custom_only"),
	VANILLA_ONLY("customizablehud.effects.vanilla_mode.vanilla_only", "customizablehud.tooltip.effects.vanilla_mode.vanilla_only");

	private final String translationKey;
	private final String tooltipKey;

	VanillaEffectsMode(String translationKey, String tooltipKey) {
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
