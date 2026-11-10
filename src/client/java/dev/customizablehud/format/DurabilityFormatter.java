package dev.customizablehud.format;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.DurabilityDisplayMode;

public final class DurabilityFormatter {
	private DurabilityFormatter() {
	}

	public static String format(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || !slot.present() || config == null) {
			return "";
		}
		DurabilityDisplayMode mode = config.durabilityDisplayMode != null
				? config.durabilityDisplayMode
				: DurabilityDisplayMode.POINTS_AND_PERCENT;
		if (mode == DurabilityDisplayMode.NONE) {
			return "";
		}
		if (slot.unbreakable()) {
			return "∞";
		}
		if (!slot.damageable() || slot.maxDamage() <= 0) {
			return "";
		}
		int remaining = slot.remaining();
		int max = slot.maxDamage();
		int percent = slot.remainingPercent();
		return switch (mode) {
			case POINTS_AND_PERCENT -> config.showMaxDurability
					? remaining + "/" + max + " · " + percent + "%"
					: remaining + " · " + percent + "%";
			case PERCENT_ONLY -> percent + "%";
			case POINTS_ONLY -> config.showMaxDurability
					? remaining + "/" + max
					: Integer.toString(remaining);
			case NONE -> "";
		};
	}

	public static boolean isLowDurability(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || config == null || !slot.present() || slot.unbreakable()) {
			return false;
		}
		if (!slot.damageable() || slot.maxDamage() <= 0) {
			return false;
		}
		return slot.remainingPercent() <= config.lowDurabilityThresholdPercent;
	}

	public static int textColorArgb(ArmorSlotSnapshot slot, ArmorConfig config, long timeMillis) {
		if (slot == null || !slot.present() || config == null) {
			return 0xFFFFFFFF;
		}
		if (config.lowDurabilityWarning && isLowDurability(slot, config)) {
			boolean flash = ((timeMillis / 350L) % 2L) == 0L;
			return flash ? 0xFFFF5555 : 0xFFFFB84D;
		}
		if (!config.colorByDurability || slot.unbreakable() || !slot.damageable() || slot.maxDamage() <= 0) {
			return 0xFFFFFFFF;
		}
		int pct = slot.remainingPercent();
		if (pct >= 70) {
			return 0xFF6EEB83;
		}
		if (pct >= 40) {
			return 0xFFFFE066;
		}
		if (pct > 15) {
			return 0xFFFF9F43;
		}
		return 0xFFFF5C5C;
	}

	public static int barColorRgb(float fraction) {
		float clamped = Math.max(0.0f, Math.min(1.0f, fraction));
		if (clamped >= 0.70f) {
			return 0xFF55FF55;
		}
		if (clamped >= 0.40f) {
			return 0xFFFFFF55;
		}
		if (clamped > 0.15f) {
			return 0xFFFFAA33;
		}
		return 0xFFFF4444;
	}
}
