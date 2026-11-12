package dev.customizablehud.format;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ColorHexUtils;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;

public final class DurabilityFormatter {
	public static final long BLINK_PHASE_MS = 350L;
	public static final float BLINK_MIN_ALPHA = 0.50f;
	public static final float BLINK_MAX_ALPHA = 1.0f;
	public static final int MAX_ITEM_NAME_CHARS = 16;

	public enum DurabilityLevel {
		VERY_HIGH,
		HEALTHY,
		WARNING,
		CRITICAL,
		NONE
	}

	public record WarningBlinkState(
			boolean active,
			boolean highPhase,
			float indicatorAlpha,
			int indicatorRgb
	) {
		public static final WarningBlinkState INACTIVE = new WarningBlinkState(false, false, 0.0f, 0);
	}

	private DurabilityFormatter() {
	}

	public static String format(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || !slot.present() || config == null) {
			return "";
		}
		DurabilityDisplayMode mode = config.durabilityDisplayMode != null
				? config.durabilityDisplayMode
				: DurabilityDisplayMode.POINTS_ONLY;
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

	public static String formatItemName(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || !slot.present() || config == null) {
			return "";
		}
		if (!config.shouldShowNameFor(slot.kind())) {
			return "";
		}
		return truncateItemName(slot.itemName(), MAX_ITEM_NAME_CHARS);
	}

	public static String truncateItemName(String rawName, int maxChars) {
		if (rawName == null) {
			return "";
		}
		String trimmed = rawName.trim();
		if (trimmed.isEmpty()) {
			return "";
		}
		int limit = Math.max(4, maxChars);
		if (trimmed.length() <= limit) {
			return trimmed;
		}
		String cut = trimmed.substring(0, limit - 1).trim();
		if (cut.isEmpty()) {
			cut = trimmed.substring(0, limit - 1);
		}
		return cut + "…";
	}

	public static DurabilityLevel classifyLevel(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || config == null || !slot.hasValidDurability()) {
			return DurabilityLevel.NONE;
		}
		int veryHigh = ConfigValidator.clampInt(
				config.veryHighThresholdPercent,
				ConfigValidator.MIN_VERY_HIGH_THRESHOLD,
				ConfigValidator.MAX_VERY_HIGH_THRESHOLD
		);
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
		if (warning >= veryHigh) {
			warning = Math.max(ConfigValidator.MIN_WARNING_THRESHOLD, veryHigh - 1);
		}
		if (critical >= warning) {
			critical = Math.max(ConfigValidator.MIN_CRITICAL_THRESHOLD, warning - 1);
		}

		long remainingScaled = (long) slot.remaining() * 100L;
		long veryHighBound = (long) veryHigh * (long) slot.maxDamage();
		long warningBound = (long) warning * (long) slot.maxDamage();
		long criticalBound = (long) critical * (long) slot.maxDamage();

		if (remainingScaled > veryHighBound) {
			return DurabilityLevel.VERY_HIGH;
		}
		if (remainingScaled > warningBound) {
			return DurabilityLevel.HEALTHY;
		}
		if (remainingScaled > criticalBound) {
			return DurabilityLevel.WARNING;
		}
		return DurabilityLevel.CRITICAL;
	}

	public static boolean isLowDurability(ArmorSlotSnapshot slot, ArmorConfig config) {
		return classifyLevel(slot, config) == DurabilityLevel.CRITICAL;
	}

	public static boolean isAlertActive(ArmorSlotSnapshot slot, ArmorConfig config) {
		if (slot == null || config == null || !config.showLowDurabilityAlert || !slot.hasValidDurability()) {
			return false;
		}
		int alertThreshold = ConfigValidator.clampInt(
				config.lowDurabilityAlertThresholdPercent,
				ConfigValidator.MIN_ALERT_THRESHOLD,
				ConfigValidator.MAX_ALERT_THRESHOLD
		);
		long remainingScaled = (long) slot.remaining() * 100L;
		long alertBound = (long) alertThreshold * (long) slot.maxDamage();
		return remainingScaled <= alertBound;
	}

	public static int alertColorArgb(ArmorConfig config) {
		return ColorHexUtils.parseOpaqueArgbOrDefault(config != null ? config.alertColorHex : "#FF5555", 0xFF5555);
	}

	public static WarningBlinkState computeWarningBlink(ArmorSlotSnapshot slot, ArmorConfig config, long timeMillis) {
		if (slot == null || config == null || !config.lowDurabilityWarning || !isAlertActive(slot, config)) {
			return WarningBlinkState.INACTIVE;
		}
		long safeMs = timeMillis == Long.MIN_VALUE ? 0L : Math.abs(timeMillis);
		boolean highPhase = ((safeMs / BLINK_PHASE_MS) % 2L) == 0L;
		int alertRgb = alertColorArgb(config) & 0x00FFFFFF;
		int indicatorRgb = highPhase ? alertRgb : 0xFF9922;
		float indicatorAlpha = highPhase ? BLINK_MAX_ALPHA : BLINK_MIN_ALPHA;
		return new WarningBlinkState(true, highPhase, indicatorAlpha, indicatorRgb);
	}

	public static int colorForLevel(DurabilityLevel level, ArmorConfig config) {
		if (level == null || level == DurabilityLevel.NONE || config == null) {
			return 0xFFFFFFFF;
		}
		return switch (level) {
			case VERY_HIGH -> ColorHexUtils.parseOpaqueArgbOrDefault(config.veryHighColorHex, 0xFFFFFF);
			case HEALTHY -> ColorHexUtils.parseOpaqueArgbOrDefault(config.healthyColorHex, 0x55FF55);
			case WARNING -> ColorHexUtils.parseOpaqueArgbOrDefault(config.warningColorHex, 0xFFFF55);
			case CRITICAL -> ColorHexUtils.parseOpaqueArgbOrDefault(config.criticalColorHex, 0xFF5555);
			case NONE -> 0xFFFFFFFF;
		};
	}

	public static int textColorArgb(ArmorSlotSnapshot slot, ArmorConfig config, long timeMillis) {
		if (slot == null || !slot.present() || config == null) {
			return 0xFFFFFFFF;
		}
		DurabilityLevel level = classifyLevel(slot, config);
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
			return 0xFF55FF55;
		}
		if (clamped > 0.20f) {
			return 0xFFFFFF55;
		}
		return 0xFFFF5555;
	}
}
