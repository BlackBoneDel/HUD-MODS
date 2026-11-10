package dev.customizablehud.config;

public final class PresetCatalog {
	private PresetCatalog() {
	}

	public static HudConfig createPreset(PresetId presetId) {
		HudConfig cfg = HudConfig.createDefault();
		applyPreset(cfg, presetId);
		return cfg;
	}

	public static void applyPreset(HudConfig target, PresetId presetId) {
		if (target == null || presetId == null) {
			return;
		}
		target.schemaVersion = HudConfig.CURRENT_SCHEMA_VERSION;
		switch (presetId) {
			case VANILLA_COMPACT -> {
				target.armor = new ArmorConfig();
				target.effects = new EffectsConfig();
			}
			case COMPACT_TEXT -> {
				ArmorConfig armor = new ArmorConfig();
				armor.enabled = true;
				armor.anchor = HudAnchor.CENTER_LEFT;
				armor.orientation = HudOrientation.VERTICAL;
				armor.style = ArmorVisualStyle.BORDERLESS;
				armor.slotSpacing = 1;
				armor.scale = 0.90f;
				armor.showIcon = true;
				armor.showDurabilityBar = false;
				armor.durabilityDisplayMode = DurabilityDisplayMode.POINTS_AND_PERCENT;
				armor.showMaxDurability = false;
				armor.showItemName = false;
				armor.colorByDurability = true;
				armor.lowDurabilityWarning = false;
				armor.showMainHand = true;
				armor.showOffHand = true;
				target.armor = armor;

				EffectsConfig effects = new EffectsConfig();
				effects.enabled = true;
				effects.anchor = HudAnchor.TOP_LEFT;
				effects.orientation = HudOrientation.VERTICAL;
				effects.style = EffectsVisualStyle.MINIMAL_TEXT;
				effects.rowSpacing = 1;
				effects.scale = 0.90f;
				effects.showIcon = true;
				effects.showName = true;
				effects.showDuration = true;
				effects.showLevel = true;
				effects.hideLevelOne = true;
				effects.levelFormat = LevelFormat.ARABIC;
				effects.vanillaEffectsMode = VanillaEffectsMode.SHOW_BOTH;
				target.effects = effects;
			}
			case EXPANDED_READOUT -> {
				ArmorConfig armor = new ArmorConfig();
				armor.enabled = true;
				armor.anchor = HudAnchor.HOTBAR_RIGHT;
				armor.orientation = HudOrientation.HORIZONTAL;
				armor.style = ArmorVisualStyle.INVENTORY_SLOT;
				armor.slotSpacing = 3;
				armor.scale = 1.15f;
				armor.showIcon = true;
				armor.showDurabilityBar = true;
				armor.durabilityDisplayMode = DurabilityDisplayMode.POINTS_AND_PERCENT;
				armor.showMaxDurability = true;
				armor.showItemName = true;
				armor.colorByDurability = true;
				armor.lowDurabilityWarning = true;
				armor.lowDurabilityThresholdPercent = 20;
				armor.showMainHand = false;
				armor.showOffHand = true;
				target.armor = armor;

				EffectsConfig effects = new EffectsConfig();
				effects.enabled = true;
				effects.anchor = HudAnchor.TOP_LEFT;
				effects.orientation = HudOrientation.VERTICAL;
				effects.style = EffectsVisualStyle.CLEAN_CARD;
				effects.rowSpacing = 3;
				effects.scale = 1.10f;
				effects.showIcon = true;
				effects.showName = true;
				effects.showDuration = true;
				effects.showLevel = true;
				effects.hideLevelOne = false;
				effects.levelFormat = LevelFormat.ROMAN;
				effects.colorDurationByRemaining = true;
				effects.vanillaEffectsMode = VanillaEffectsMode.SHOW_BOTH;
				target.effects = effects;
			}
		}
	}
}
