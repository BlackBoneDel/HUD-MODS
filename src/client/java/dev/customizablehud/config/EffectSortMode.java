package dev.customizablehud.config;

public enum EffectSortMode {
	CATEGORY_THEN_NAME("customizablehud.effects.sort.category_then_name"),
	DURATION_ASC("customizablehud.effects.sort.duration_asc"),
	DURATION_DESC("customizablehud.effects.sort.duration_desc"),
	NAME_ASC("customizablehud.effects.sort.name_asc"),
	NONE("customizablehud.effects.sort.none");

	private final String translationKey;

	EffectSortMode(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
