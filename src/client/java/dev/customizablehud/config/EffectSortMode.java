package dev.customizablehud.config;

public enum EffectSortMode {
	CATEGORY_THEN_NAME("customizablehud.effects.sort.category_then_name", "customizablehud.tooltip.effects.sort.category_then_name"),
	DURATION_ASC("customizablehud.effects.sort.duration_asc", "customizablehud.tooltip.effects.sort.duration_asc"),
	DURATION_DESC("customizablehud.effects.sort.duration_desc", "customizablehud.tooltip.effects.sort.duration_desc"),
	NAME_ASC("customizablehud.effects.sort.name_asc", "customizablehud.tooltip.effects.sort.name_asc"),
	NONE("customizablehud.effects.sort.none", "customizablehud.tooltip.effects.sort.none");

	private final String translationKey;
	private final String tooltipKey;

	EffectSortMode(String translationKey, String tooltipKey) {
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
