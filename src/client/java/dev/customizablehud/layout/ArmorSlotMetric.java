package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorSlotKind;

public record ArmorSlotMetric(
		ArmorSlotKind kind,
		boolean present,
		int labelWidth,
		int nameWidth
) {
}
