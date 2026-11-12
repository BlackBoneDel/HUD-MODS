package dev.customizablehud.layout;

public record EffectRowMetric(
		String id,
		int titleWidth,
		int durationWidth,
		int miniLevelWidth
) {
	public EffectRowMetric(String id, int titleWidth, int durationWidth) {
		this(id, titleWidth, durationWidth, 0);
	}
}
