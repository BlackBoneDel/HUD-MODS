package dev.customizablehud.config;

public final class EffectColorOverride {
	public boolean enabled = true;
	public String colorHex = "#FF66B2";

	public EffectColorOverride() {
	}

	public EffectColorOverride(boolean enabled, String colorHex) {
		this.enabled = enabled;
		this.colorHex = colorHex;
	}

	public EffectColorOverride copy() {
		return new EffectColorOverride(enabled, colorHex);
	}
}
