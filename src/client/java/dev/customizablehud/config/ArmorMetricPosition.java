package dev.customizablehud.config;

public enum ArmorMetricPosition {
	ABOVE("customizablehud.armor.metric_pos.above", "customizablehud.tooltip.armor.metric_pos.above"),
	BELOW("customizablehud.armor.metric_pos.below", "customizablehud.tooltip.armor.metric_pos.below"),
	LEFT("customizablehud.armor.metric_pos.left", "customizablehud.tooltip.armor.metric_pos.left"),
	RIGHT("customizablehud.armor.metric_pos.right", "customizablehud.tooltip.armor.metric_pos.right");

	private final String translationKey;
	private final String tooltipKey;

	ArmorMetricPosition(String translationKey, String tooltipKey) {
		this.translationKey = translationKey;
		this.tooltipKey = tooltipKey;
	}

	public String translationKey() {
		return translationKey;
	}

	public String tooltipKey() {
		return tooltipKey;
	}

	public boolean isVerticalSide() {
		return this == ABOVE || this == BELOW;
	}
}
