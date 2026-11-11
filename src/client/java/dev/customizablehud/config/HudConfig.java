package dev.customizablehud.config;

public final class HudConfig {
	public static final int CURRENT_SCHEMA_VERSION = 3;

	public int schemaVersion = CURRENT_SCHEMA_VERSION;
	public ModLanguage language = ModLanguage.ENGLISH;
	public ArmorConfig armor = new ArmorConfig();
	public EffectsConfig effects = new EffectsConfig();
	public VisibilityConfig visibility = new VisibilityConfig();
	public boolean editorSnapToGrid = true;
	public int editorGridSize = 4;
	public boolean editorShowGuides = true;

	public static HudConfig createDefault() {
		return new HudConfig();
	}

	public HudConfig copy() {
		HudConfig copy = new HudConfig();
		copy.schemaVersion = schemaVersion;
		copy.language = language != null ? language : ModLanguage.ENGLISH;
		copy.armor = armor != null ? armor.copy() : new ArmorConfig();
		copy.effects = effects != null ? effects.copy() : new EffectsConfig();
		copy.visibility = visibility != null ? visibility.copy() : new VisibilityConfig();
		copy.editorSnapToGrid = editorSnapToGrid;
		copy.editorGridSize = editorGridSize;
		copy.editorShowGuides = editorShowGuides;
		return copy;
	}
}
