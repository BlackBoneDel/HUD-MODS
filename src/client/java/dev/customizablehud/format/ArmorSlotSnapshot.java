package dev.customizablehud.format;

import dev.customizablehud.config.ArmorSlotKind;

public record ArmorSlotSnapshot(
		ArmorSlotKind kind,
		boolean present,
		String itemName,
		boolean damageable,
		boolean unbreakable,
		int maxDamage,
		int damageValue,
		int stackCount,
		boolean barVisible,
		float barFraction
) {
	public static ArmorSlotSnapshot empty(ArmorSlotKind kind) {
		return new ArmorSlotSnapshot(kind, false, "", false, false, 0, 0, 0, false, 0.0f);
	}

	public boolean hasValidDurability() {
		return present && damageable && !unbreakable && maxDamage > 0;
	}

	public int remaining() {
		if (!hasValidDurability()) {
			return 0;
		}
		int clampedDamage = Math.max(0, Math.min(maxDamage, damageValue));
		return Math.max(0, maxDamage - clampedDamage);
	}

	public int remainingPercent() {
		if (!hasValidDurability()) {
			return 0;
		}
		int clamped = Math.round((remaining() * 100.0f) / maxDamage);
		return Math.max(0, Math.min(100, clamped));
	}

	public float resolvedBarFraction() {
		if (!hasValidDurability()) {
			return 0.0f;
		}
		return Math.max(0.0f, Math.min(1.0f, (float) remaining() / (float) maxDamage));
	}
}
