package dev.customizablehud.format;

import dev.customizablehud.config.ColorHexUtils;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.EffectColorOverride;
import dev.customizablehud.config.EffectsConfig;
import java.util.Optional;
import java.util.OptionalInt;

public final class EffectsColorResolver {
	public static final int DEFAULT_TITLE_COLOR_ARGB = 0xFFF3F6FA;

	private EffectsColorResolver() {
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

	public static int titleColorArgb(EffectSnapshot snap, EffectsConfig config) {
		if (snap != null) {
			OptionalInt customRgb = resolveCustomRgb(snap.id(), config);
			if (customRgb.isPresent()) {
				return 0xFF000000 | customRgb.getAsInt();
			}
		}
		return DEFAULT_TITLE_COLOR_ARGB;
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
		boolean colorByRemaining = config != null && config.colorDurationByRemaining;
		return DurationFormatter.durationColorArgb(snap.durationTicks(), ticksPerSecond, snap.infinite(), colorByRemaining);
	}

	public static int iconTintArgb(float alpha) {
		return ColorHexUtils.withAlpha(0xFFFFFF, alpha);
	}
}
