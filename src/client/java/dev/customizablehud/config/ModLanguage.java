package dev.customizablehud.config;

import java.util.Locale;

public enum ModLanguage {
	ENGLISH("en_us", "English", "customizablehud.tooltip.language.english"),
	SPANISH("es_es", "Español", "customizablehud.tooltip.language.spanish");

	private final String code;
	private final String displayName;
	private final String tooltipKey;

	ModLanguage(String code, String displayName, String tooltipKey) {
		this.code = code;
		this.displayName = displayName;
		this.tooltipKey = tooltipKey;
	}

	public String code() {
		return code;
	}

	public String displayName() {
		return displayName;
	}

	public String tooltipKey() {
		return tooltipKey;
	}

	public static ModLanguage fromCode(String raw) {
		if (raw == null) {
			return ENGLISH;
		}
		String norm = raw.trim().toLowerCase(Locale.ROOT);
		for (ModLanguage value : values()) {
			if (value.code.equals(norm) || value.name().toLowerCase(Locale.ROOT).equals(norm)) {
				return value;
			}
		}
		if (norm.startsWith("es")) {
			return SPANISH;
		}
		return ENGLISH;
	}
}
