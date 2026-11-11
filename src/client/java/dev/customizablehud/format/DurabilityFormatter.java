package dev.customizablehud.format;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ColorHexUtils;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;

public final class DurabilityFormatter {
	public enum DurabilityLevel {
		HEALTHY,
		WARNING,
		CRITICAL,
		NONE
	}

	private DurabilityFormatter() {
	}

	public static String format(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || !slot.present() || config == null) {
			return "";
		}
		DurabilityDisplayMode mode = config.durabilityDisplayMode != null
				? config.durabilityDisplayMode
				: DurabilityDisplayMode.PERCENT_ONLY;
		if (mode == DurabilityDisplayMode.NONE) {
			return "";
		}
		if (slot.unbreakable()) {
			return "∞";
		}
		if (!slot.hasValidDurability()) {
			return "";
		}
		int remaining = slot.remaining();
		int max = slot.maxDamage();
		int percent = slot.remainingPercent();
		return switch (mode) {
			case PERCENT_ONLY -> percent + "%";
			case POINTS_ONLY -> config.showMaxDurability
					? remaining + "/" + max
					: Integer.toString(remaining);
			case NONE -> "";
		};
	}

	public static DurabilityLevel classifyLevel(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || config == null || !slot.hasValidDurability()) {
			return DurabilityLevel.NONE;
		}
		int warning = ConfigValidator.clampInt(
				config.warningThresholdPercent,
				ConfigValidator.MIN_WARNING_THRESHOLD,
				ConfigValidator.MAX_WARNING_THRESHOLD
		);
		int critical = ConfigValidator.clampInt(
				config.criticalThresholdPercent,
				ConfigValidator.MIN_CRITICAL_THRESHOLD,
				ConfigValidator.MAX_CRITICAL_THRESHOLD
		);
		if (critical >= warning) {
			critical = Math.max(0, warning - 1);
		}

		long remainingScaled = (long) slot.remaining() * 100L;
		long criticalBound = (long) critical * (long) slot.maxDamage();
		long warningBound = (long) warning * (long) slot.maxDamage();

		if (remainingScaled <= criticalBound) {
			return DurabilityLevel.CRITICAL;
		}
		if (remainingScaled <= warningBound) {
			return DurabilityLevel.WARNING;
		}
		return DurabilityLevel.HEALTHY;
	}

	public static boolean isLowDurability(ArmorSlotSnapshot slot, ArmorConfig config) {
		return classifyLevel(slot, config) == DurabilityLevel.CRITICAL;
	}

	public static int colorForLevel(DurabilityLevel level, ArmorConfig config) {
		if (level == null || level == DurabilityLevel.NONE || config == null) {
			return 0xFFFFFFFF;
		}
		return switch (level) {
			case HEALTHY -> ColorHexUtils.parseOpaqueArgbOrDefault(config.healthyColorHex, 0x6EEB83);
			case WARNING -> ColorHexUtils.parseOpaqueArgbOrDefault(config.warningColorHex, 0xFFE066);
			case CRITICAL -> ColorHexUtils.parseOpaqueArgbOrDefault(config.criticalColorHex, 0xFF5555);
			case NONE -> 0xFFFFFFFF;
		};
	}

	public static int textColorArgb(ArmorSlotSnapshot slot, ArmorConfig config, long timeMillis) {
		if (slot == null || !slot.present() || config == null) {
			return 0xFFFFFFFF;
		}
		DurabilityLevel level = classifyLevel(slot, config);
		if (config.lowDurabilityWarning && level == DurabilityLevel.CRITICAL) {
			boolean flash = ((timeMillis / 350L) % 2L) == 0L;
			return flash ? colorForLevel(DurabilityLevel.CRITICAL, config) : 0xFFFFB84D;
		}
		if (!config.colorByDurability || level == DurabilityLevel.NONE) {
			return 0xFFFFFFFF;
		}
		return colorForLevel(level, config);
	}

	public static int barColorArgb(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || config == null || !slot.hasValidDurability()) {
			return 0xFF55FF55;
		}
		if (config.colorByDurability) {
			return colorForLevel(classifyLevel(slot, config), config);
		}
		return barColorRgb(slot.resolvedBarFraction());
	}

	public static int barColorRgb(float fraction) {
		float clamped = Math.max(0.0f, Math.min(1.0f, fraction));
		if (clamped > 0.60f) {
			return 0xFF6EEB83;
		}
		if (clamped > 0.20f) {
			return 0xFFFFE066;
		}
		return 0xFFFF5555;
	}
}
