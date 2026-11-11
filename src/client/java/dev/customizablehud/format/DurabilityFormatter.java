package dev.customizablehud.format;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ColorHexUtils;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;

public final class DurabilityFormatter {
	public static final long BLINK_PHASE_MS = 350L;
	public static final float BLINK_MIN_ALPHA = 0.45f;
	public static final float BLINK_MAX_ALPHA = 1.0f;
	public static final int MAX_ITEM_NAME_CHARS = 16;

	public enum DurabilityLevel {
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

	public static WarningBlinkState computeWarningBlink(ArmorSlotSnapshot slot, ArmorConfig config, long timeMillis) {
		if (slot == null || config == null || !config.lowDurabilityWarning || !isLowDurability(slot, config)) {
			return WarningBlinkState.INACTIVE;
		}
		long safeMs = timeMillis == Long.MIN_VALUE ? 0L : Math.abs(timeMillis);
		boolean highPhase = ((safeMs / BLINK_PHASE_MS) % 2L) == 0L;
		int criticalRgb = colorForLevel(DurabilityLevel.CRITICAL, config) & 0x00FFFFFF;
		int indicatorRgb = highPhase ? criticalRgb : 0xFF9922;
		float indicatorAlpha = highPhase ? BLINK_MAX_ALPHA : BLINK_MIN_ALPHA;
		return new WarningBlinkState(true, highPhase, indicatorAlpha, indicatorRgb);
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
