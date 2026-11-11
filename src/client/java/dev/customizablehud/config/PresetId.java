package dev.customizablehud.config;

public enum PresetId {
	VANILLA_COMPACT("customizablehud.preset.vanilla_compact", "customizablehud.tooltip.preset.vanilla_compact"),
	COMPACT_TEXT("customizablehud.preset.compact_text", "customizablehud.tooltip.preset.compact_text"),
	EXPANDED_READOUT("customizablehud.preset.expanded_readout", "customizablehud.tooltip.preset.expanded_readout");

	private final String translationKey;
	private final String tooltipKey;

	PresetId(String translationKey, String tooltipKey) {
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
