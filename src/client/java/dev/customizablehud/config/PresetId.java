package dev.customizablehud.config;

public enum PresetId {
	VANILLA_COMPACT("customizablehud.preset.vanilla_compact"),
	COMPACT_TEXT("customizablehud.preset.compact_text"),
	EXPANDED_READOUT("customizablehud.preset.expanded_readout");

	private final String translationKey;

	PresetId(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
