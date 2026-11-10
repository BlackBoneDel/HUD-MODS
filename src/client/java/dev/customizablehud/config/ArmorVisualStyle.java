package dev.customizablehud.config;

public enum ArmorVisualStyle {
	VANILLA_HOTBAR("customizablehud.armor.style.vanilla_hotbar"),
	INVENTORY_SLOT("customizablehud.armor.style.inventory_slot"),
	MINIMAL("customizablehud.armor.style.minimal"),
	BORDERLESS("customizablehud.armor.style.borderless");

	private final String translationKey;

	ArmorVisualStyle(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
