package dev.customizablehud.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ConfigValidator {
	public static final float MIN_SCALE = 0.50f;
	public static final float MAX_SCALE = 2.50f;
	public static final int MIN_SPACING = 0;
	public static final int MAX_SPACING = 16;
	public static final int MIN_OFFSET = -4000;
	public static final int MAX_OFFSET = 4000;
	public static final int MIN_THRESHOLD = 1;
	public static final int MAX_THRESHOLD = 50;
	public static final int MIN_GRID_SIZE = 1;
	public static final int MAX_GRID_SIZE = 32;

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
			armor.durabilityDisplayMode = DurabilityDisplayMode.POINTS_AND_PERCENT;
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

		int clampedThreshold = clampInt(armor.lowDurabilityThresholdPercent, MIN_THRESHOLD, MAX_THRESHOLD);
		if (clampedThreshold != armor.lowDurabilityThresholdPercent) {
			armor.lowDurabilityThresholdPercent = clampedThreshold;
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
