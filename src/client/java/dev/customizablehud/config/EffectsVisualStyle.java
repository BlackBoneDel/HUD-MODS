package dev.customizablehud.config;

public enum EffectsVisualStyle {
	MINECRAFT_COMPACT("customizablehud.effects.style.minecraft_compact", "customizablehud.tooltip.effects.style.minecraft_compact"),
	CLEAN_CARD("customizablehud.effects.style.clean_card", "customizablehud.tooltip.effects.style.clean_card"),
	MINIMAL_TEXT("customizablehud.effects.style.minimal_text", "customizablehud.tooltip.effects.style.minimal_text"),
	POTION_HUD("customizablehud.effects.style.potion_hud", "customizablehud.tooltip.effects.style.potion_hud");

	private final String translationKey;
	private final String tooltipKey;

	EffectsVisualStyle(String translationKey, String tooltipKey) {
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
