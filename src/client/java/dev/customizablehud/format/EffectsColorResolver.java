package dev.customizablehud.format;

import dev.customizablehud.config.ColorHexUtils;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.EffectColorOverride;
import dev.customizablehud.config.EffectsConfig;
import java.util.Optional;
import java.util.OptionalInt;

public final class EffectsColorResolver {
	public static final int DEFAULT_TITLE_COLOR_ARGB = 0xFFE6EDF3;
	public static final int UNREADABLE_FALLBACK_RGB = 0xE6EDF3;
	public static final float MIN_READABLE_LUMINANCE = 0.34f;
	public static final float TARGET_READABLE_LUMINANCE = 0.44f;

	private EffectsColorResolver() {
	}

	public static boolean hasNativeColor(int rawColorRgb) {
		return (rawColorRgb & 0x00FFFFFF) != 0;
	}

	public static int resolveNativeDisplayRgb(int rawColorRgb) {
		int rgb = rawColorRgb & 0x00FFFFFF;
		if (rgb == 0) {
			return UNREADABLE_FALLBACK_RGB;
		}
		int r = (rgb >> 16) & 0xFF;
		int g = (rgb >> 8) & 0xFF;
		int b = rgb & 0xFF;
		float lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255.0f;
		if (lum >= MIN_READABLE_LUMINANCE) {
			return rgb;
		}
		float blend = Math.max(0.0f, Math.min(0.75f, (TARGET_READABLE_LUMINANCE - lum) / Math.max(0.05f, 1.0f - lum)));
		int nr = Math.min(255, Math.round(r + (255 - r) * blend));
		int ng = Math.min(255, Math.round(g + (255 - g) * blend));
		int nb = Math.min(255, Math.round(b + (255 - b) * blend));
		return (nr << 16) | (ng << 8) | nb;
	}

	public static OptionalInt resolveCustomRgb(String effectId, EffectsConfig config) {
		if (effectId == null || config == null || config.customEffectColors == null) {
			return OptionalInt.empty();
		}
		Optional<String> normalized = ConfigValidator.normalizeEffectId(effectId);
		if (normalized.isEmpty()) {
			return OptionalInt.empty();
		}
		EffectColorOverride override = config.customEffectColors.get(normalized.get());
		if (override == null || !override.enabled) {
			return OptionalInt.empty();
		}
		Optional<Integer> rgb = ColorHexUtils.parseRgb(override.colorHex);
		if (rgb.isEmpty()) {
			return OptionalInt.empty();
		}
		return OptionalInt.of(rgb.get());
	}

	public static int resolveEffectiveRgb(EffectSnapshot snap, EffectsConfig config) {
		if (snap != null) {
			OptionalInt customRgb = resolveCustomRgb(snap.id(), config);
			if (customRgb.isPresent()) {
				return customRgb.getAsInt();
			}
			return resolveNativeDisplayRgb(snap.colorRgb());
		}
		return UNREADABLE_FALLBACK_RGB;
	}

	public static int titleColorArgb(EffectSnapshot snap, EffectsConfig config) {
		return 0xFF000000 | resolveEffectiveRgb(snap, config);
	}

	public static int durationColorArgb(EffectSnapshot snap, EffectsConfig config, float ticksPerSecond) {
		if (snap != null) {
			OptionalInt customRgb = resolveCustomRgb(snap.id(), config);
			if (customRgb.isPresent()) {
				return 0xFF000000 | customRgb.getAsInt();
			}
		}
		if (snap == null) {
			return DurationFormatter.durationColorArgb(0, ticksPerSecond, false, false);
		}
		if (config != null && config.colorDurationByRemaining) {
			return DurationFormatter.durationColorArgb(snap.durationTicks(), ticksPerSecond, snap.infinite(), true);
		}
		return 0xFF000000 | resolveNativeDisplayRgb(snap.colorRgb());
	}

	public static int accentColorArgb(EffectSnapshot snap, EffectsConfig config) {
		return 0xFF000000 | resolveEffectiveRgb(snap, config);
	}

	public static int iconTintArgb(float alpha) {
		return ColorHexUtils.withAlpha(0xFFFFFF, alpha);
	}
}
