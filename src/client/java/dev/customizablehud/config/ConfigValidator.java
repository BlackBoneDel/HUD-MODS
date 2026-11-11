package dev.customizablehud.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

public final class ConfigValidator {
	public static final float MIN_SCALE = 0.50f;
	public static final float MAX_SCALE = 2.50f;
	public static final int MIN_SPACING = 0;
	public static final int MAX_SPACING = 16;
	public static final int MIN_OFFSET = -4000;
	public static final int MAX_OFFSET = 4000;
	public static final int MIN_CRITICAL_THRESHOLD = 0;
	public static final int MAX_CRITICAL_THRESHOLD = 99;
	public static final int MIN_WARNING_THRESHOLD = 1;
	public static final int MAX_WARNING_THRESHOLD = 100;
	public static final int MIN_GRID_SIZE = 1;
	public static final int MAX_GRID_SIZE = 32;

	private static final Pattern NAMESPACED_ID_PATTERN = Pattern.compile("^[a-z0-9_.-]+:[a-z0-9/._-]+$");
	private static final Pattern PATH_ONLY_PATTERN = Pattern.compile("^[a-z0-9/._-]+$");

	private ConfigValidator() {
	}

	public record ValidationResult(HudConfig config, boolean modified) {
	}

	public static ValidationResult validateAndSanitize(HudConfig input) {
		boolean modified = false;
		if (input == null) {
			return new ValidationResult(HudConfig.createDefault(), true);
		}

		HudConfig cfg = input.copy();
		if (cfg.schemaVersion <= 0 || cfg.schemaVersion > HudConfig.CURRENT_SCHEMA_VERSION) {
			cfg.schemaVersion = HudConfig.CURRENT_SCHEMA_VERSION;
			modified = true;
		}
		if (input.language == null) {
			cfg.language = ModLanguage.ENGLISH;
			modified = true;
		}

		if (input.armor == null) {
			cfg.armor = new ArmorConfig();
			modified = true;
		}
		if (input.effects == null) {
			cfg.effects = new EffectsConfig();
			modified = true;
		}
		if (input.visibility == null) {
			cfg.visibility = new VisibilityConfig();
			modified = true;
		}

		modified |= sanitizeArmor(cfg.armor);
		modified |= sanitizeEffects(cfg.effects);

		int clampedGrid = clampInt(cfg.editorGridSize, MIN_GRID_SIZE, MAX_GRID_SIZE);
		if (clampedGrid != cfg.editorGridSize) {
			cfg.editorGridSize = clampedGrid;
			modified = true;
		}

		return new ValidationResult(cfg, modified);
	}

	public static Optional<String> normalizeEffectId(String rawId) {
		if (rawId == null) {
			return Optional.empty();
		}
		String trimmed = rawId.trim().toLowerCase(Locale.ROOT);
		if (trimmed.isEmpty()) {
			return Optional.empty();
		}
		if (!trimmed.contains(":")) {
			if (PATH_ONLY_PATTERN.matcher(trimmed).matches()) {
				return Optional.of("minecraft:" + trimmed);
			}
			return Optional.empty();
		}
		if (NAMESPACED_ID_PATTERN.matcher(trimmed).matches()) {
			return Optional.of(trimmed);
		}
		return Optional.empty();
	}

	private static boolean sanitizeArmor(ArmorConfig armor) {
		boolean modified = false;
		if (armor.anchor == null) {
			armor.anchor = HudAnchor.HOTBAR_LEFT;
			modified = true;
		}
		if (armor.orientation == null) {
			armor.orientation = HudOrientation.HORIZONTAL;
			modified = true;
		}
		if (armor.style == null) {
			armor.style = ArmorVisualStyle.VANILLA_HOTBAR;
			modified = true;
		}
		if (armor.durabilityDisplayMode == null) {
			armor.durabilityDisplayMode = DurabilityDisplayMode.PERCENT_ONLY;
			modified = true;
		}

		float clampedScale = clampScale(armor.scale);
		if (Float.compare(clampedScale, armor.scale) != 0) {
			armor.scale = clampedScale;
			modified = true;
		}

		int clampedSpacing = clampInt(armor.slotSpacing, MIN_SPACING, MAX_SPACING);
		if (clampedSpacing != armor.slotSpacing) {
			armor.slotSpacing = clampedSpacing;
			modified = true;
		}

		int clampedOffsetX = clampInt(armor.offsetX, MIN_OFFSET, MAX_OFFSET);
		int clampedOffsetY = clampInt(armor.offsetY, MIN_OFFSET, MAX_OFFSET);
		if (clampedOffsetX != armor.offsetX || clampedOffsetY != armor.offsetY) {
			armor.offsetX = clampedOffsetX;
			armor.offsetY = clampedOffsetY;
			modified = true;
		}

		int clampedWarn = clampInt(armor.warningThresholdPercent, MIN_WARNING_THRESHOLD, MAX_WARNING_THRESHOLD);
		int clampedCrit = clampInt(armor.criticalThresholdPercent, MIN_CRITICAL_THRESHOLD, MAX_CRITICAL_THRESHOLD);
		if (clampedWarn != armor.warningThresholdPercent || clampedCrit != armor.criticalThresholdPercent) {
			armor.warningThresholdPercent = clampedWarn;
			armor.criticalThresholdPercent = clampedCrit;
			modified = true;
		}
		if (armor.criticalThresholdPercent >= armor.warningThresholdPercent) {
			if (armor.warningThresholdPercent > MIN_CRITICAL_THRESHOLD) {
				armor.criticalThresholdPercent = armor.warningThresholdPercent - 1;
			} else {
				armor.criticalThresholdPercent = 20;
				armor.warningThresholdPercent = 60;
			}
			modified = true;
		}

		String healthy = ColorHexUtils.normalizeOrDefault(armor.healthyColorHex, "#6EEB83");
		if (!healthy.equals(armor.healthyColorHex)) {
			armor.healthyColorHex = healthy;
			modified = true;
		}
		String warning = ColorHexUtils.normalizeOrDefault(armor.warningColorHex, "#FFE066");
		if (!warning.equals(armor.warningColorHex)) {
			armor.warningColorHex = warning;
			modified = true;
		}
		String critical = ColorHexUtils.normalizeOrDefault(armor.criticalColorHex, "#FF5555");
		if (!critical.equals(armor.criticalColorHex)) {
			armor.criticalColorHex = critical;
			modified = true;
		}

		List<ArmorSlotKind> sanitizedOrder = new ArrayList<>(4);
		if (armor.armorOrder != null) {
			for (ArmorSlotKind kind : armor.armorOrder) {
				if (kind != null && kind.isArmorPiece() && !sanitizedOrder.contains(kind)) {
					sanitizedOrder.add(kind);
				} else {
					modified = true;
				}
			}
		} else {
			modified = true;
		}
		for (ArmorSlotKind required : ArmorSlotKind.DEFAULT_ARMOR_ORDER) {
			if (!sanitizedOrder.contains(required)) {
				sanitizedOrder.add(required);
				modified = true;
			}
		}
		if (armor.armorOrder == null || !armor.armorOrder.equals(sanitizedOrder)) {
			armor.armorOrder = sanitizedOrder;
			modified = true;
		}

		Map<ArmorSlotKind, PointOffset> sanitizedOffsets = new EnumMap<>(ArmorSlotKind.class);
		for (ArmorSlotKind kind : ArmorSlotKind.values()) {
			PointOffset existing = armor.individualOffsets != null ? armor.individualOffsets.get(kind) : null;
			if (existing == null) {
				sanitizedOffsets.put(kind, new PointOffset());
				modified = true;
			} else {
				int sx = clampInt(existing.x, MIN_OFFSET, MAX_OFFSET);
				int sy = clampInt(existing.y, MIN_OFFSET, MAX_OFFSET);
				if (sx != existing.x || sy != existing.y) {
					modified = true;
				}
				sanitizedOffsets.put(kind, new PointOffset(sx, sy));
			}
		}
		armor.individualOffsets = sanitizedOffsets;

		return modified;
	}

	private static boolean sanitizeEffects(EffectsConfig effects) {
		boolean modified = false;
		if (effects.anchor == null) {
			effects.anchor = HudAnchor.TOP_LEFT;
			modified = true;
		}
		if (effects.orientation == null) {
			effects.orientation = HudOrientation.VERTICAL;
			modified = true;
		}
		if (effects.style == null) {
			effects.style = EffectsVisualStyle.MINECRAFT_COMPACT;
			modified = true;
		}
		if (effects.levelFormat == null) {
			effects.levelFormat = LevelFormat.ROMAN;
			modified = true;
		}
		if (effects.sortMode == null) {
			effects.sortMode = EffectSortMode.CATEGORY_THEN_NAME;
			modified = true;
		}
		if (effects.vanillaEffectsMode == null) {
			effects.vanillaEffectsMode = VanillaEffectsMode.SHOW_BOTH;
			modified = true;
		}

		float clampedScale = clampScale(effects.scale);
		if (Float.compare(clampedScale, effects.scale) != 0) {
			effects.scale = clampedScale;
			modified = true;
		}

		int clampedSpacing = clampInt(effects.rowSpacing, MIN_SPACING, MAX_SPACING);
		if (clampedSpacing != effects.rowSpacing) {
			effects.rowSpacing = clampedSpacing;
			modified = true;
		}

		int clampedOffsetX = clampInt(effects.offsetX, MIN_OFFSET, MAX_OFFSET);
		int clampedOffsetY = clampInt(effects.offsetY, MIN_OFFSET, MAX_OFFSET);
		if (clampedOffsetX != effects.offsetX || clampedOffsetY != effects.offsetY) {
			effects.offsetX = clampedOffsetX;
			effects.offsetY = clampedOffsetY;
			modified = true;
		}

		if (effects.customEffectColors == null) {
			effects.customEffectColors = new LinkedHashMap<>();
			modified = true;
		} else {
			Map<String, EffectColorOverride> cleaned = new LinkedHashMap<>();
			for (Map.Entry<String, EffectColorOverride> entry : effects.customEffectColors.entrySet()) {
				Optional<String> normId = normalizeEffectId(entry.getKey());
				EffectColorOverride override = entry.getValue();
				if (normId.isEmpty() || override == null) {
					modified = true;
					continue;
				}
				Optional<String> normHex = ColorHexUtils.normalizeHex(override.colorHex);
				if (normHex.isEmpty()) {
					modified = true;
					continue;
				}
				if (!normId.get().equals(entry.getKey()) || !normHex.get().equals(override.colorHex)) {
					modified = true;
				}
				cleaned.put(normId.get(), new EffectColorOverride(override.enabled, normHex.get()));
			}
			effects.customEffectColors = cleaned;
		}

		return modified;
	}

	public static float clampScale(float value) {
		if (!Float.isFinite(value)) {
			return 1.0f;
		}
		float clamped = Math.max(MIN_SCALE, Math.min(MAX_SCALE, value));
		return Math.round(clamped * 100.0f) / 100.0f;
	}

	public static int clampInt(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}
}
