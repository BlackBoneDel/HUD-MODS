package dev.customizablehud.config;

public enum ArmorBorderStyle {
	PER_SLOT("customizablehud.armor.border_style.per_slot", "customizablehud.tooltip.armor.border_style.per_slot"),
	OUTER_FRAME("customizablehud.armor.border_style.outer_frame", "customizablehud.tooltip.armor.border_style.outer_frame");

	private final String translationKey;
	private final String tooltipKey;

	ArmorBorderStyle(String translationKey, String tooltipKey) {
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
