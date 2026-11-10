package dev.customizablehud.config;

public enum HudOrientation {
	HORIZONTAL("customizablehud.orientation.horizontal"),
	VERTICAL("customizablehud.orientation.vertical");

	private final String translationKey;

	HudOrientation(String translationKey) {
		this.translationKey = translationKey;
	}

	public String translationKey() {
		return translationKey;
	}
}
