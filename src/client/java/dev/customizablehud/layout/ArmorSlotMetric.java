package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorSlotKind;

public record ArmorSlotMetric(
		ArmorSlotKind kind,
		boolean present,
		int labelWidth,
		int nameWidth,
		int fontLineHeight,
		boolean alertActive
) {
	public ArmorSlotMetric(ArmorSlotKind kind, boolean present, int labelWidth, int nameWidth) {
		this(kind, present, labelWidth, nameWidth, 9, false);
	}
}
