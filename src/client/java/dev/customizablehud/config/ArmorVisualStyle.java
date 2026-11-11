package dev.customizablehud.config;

public enum ArmorVisualStyle {
	VANILLA_HOTBAR("customizablehud.armor.style.vanilla_hotbar", "customizablehud.tooltip.armor.style.vanilla_hotbar"),
	INVENTORY_SLOT("customizablehud.armor.style.inventory_slot", "customizablehud.tooltip.armor.style.inventory_slot"),
	MINIMAL("customizablehud.armor.style.minimal", "customizablehud.tooltip.armor.style.minimal"),
	BORDERLESS("customizablehud.armor.style.borderless", "customizablehud.tooltip.armor.style.borderless");

	private final String translationKey;
	private final String tooltipKey;

	ArmorVisualStyle(String translationKey, String tooltipKey) {
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
