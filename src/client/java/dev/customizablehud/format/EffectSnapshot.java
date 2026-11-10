package dev.customizablehud.format;

public record EffectSnapshot(
		String id,
		String displayName,
		EffectCategoryKind category,
		int amplifier,
		int durationTicks,
		boolean infinite,
		boolean ambient,
		boolean particlesVisible,
		boolean showIcon,
		int colorRgb
) {
	public String safeName() {
		return displayName == null || displayName.isBlank() ? (id == null ? "?" : id) : displayName;
	}

	public EffectCategoryKind safeCategory() {
		return category == null ? EffectCategoryKind.NEUTRAL : category;
	}
}
