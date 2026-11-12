package dev.customizablehud.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Handles one-time JSON schema migrations across mod versions.
 *
 * <p>Migration rules:
 * <ul>
 *   <li>Schema v1/v2 -> v3: upgrades legacy Alpha 2 defaults ({@code PERCENT_ONLY}, {@code showEffectBackground=true})
 *       once to {@code POINTS_ONLY} and {@code showEffectBackground=false} while preserving explicit customizations.</li>
 *   <li>Schema v1/v2/v3 -> v4 (Hud Utils): upgrades old 3-tier default colors ({@code #6EEB83}, {@code #FFE066})
 *       and missing 4-tier fields ({@code veryHighThresholdPercent=95}, {@code veryHighColorHex=#FFFFFF},
 *       {@code numberScale=1.0f}, {@code showLowDurabilityAlert=true}, {@code lowDurabilityAlertThresholdPercent=10}),
 *       and migrates the old default Effects position ({@code TOP_LEFT} with zero offset) once to
 *       {@code CENTER_RIGHT} while leaving custom/non-default positions untouched.</li>
 * </ul>
 */
public final class ConfigMigrator {
	private ConfigMigrator() {
	}

	public static boolean migrate(JsonObject rawRoot, HudConfig target) {
		if (rawRoot == null || target == null) {
			return false;
		}
		boolean migrated = false;
		int sourceVersion = 1;
		if (rawRoot.has("schemaVersion") && rawRoot.get("schemaVersion").isJsonPrimitive()) {
			try {
				sourceVersion = rawRoot.get("schemaVersion").getAsInt();
			} catch (RuntimeException ignored) {
				sourceVersion = 1;
			}
		} else {
			migrated = true;
		}

		if (sourceVersion < 2) {
			migrated = true;
			if (!rawRoot.has("language") || rawRoot.get("language").isJsonNull()) {
				target.language = ModLanguage.ENGLISH;
			}
			if (rawRoot.has("armor") && rawRoot.get("armor").isJsonObject() && target.armor != null) {
				JsonObject armorObj = rawRoot.getAsJsonObject("armor");
				if (!armorObj.has("criticalThresholdPercent")
						&& armorObj.has("lowDurabilityThresholdPercent")
						&& armorObj.get("lowDurabilityThresholdPercent").isJsonPrimitive()) {
					try {
						int legacyLow = armorObj.get("lowDurabilityThresholdPercent").getAsInt();
						if (legacyLow != 15 && legacyLow >= 0 && legacyLow < target.armor.warningThresholdPercent) {
							target.armor.criticalThresholdPercent = legacyLow;
						}
					} catch (RuntimeException ignored) {
						// Keep default criticalThresholdPercent
					}
				}
			}
			if (rawRoot.has("effects") && rawRoot.get("effects").isJsonObject() && target.effects != null) {
				JsonObject effectsObj = rawRoot.getAsJsonObject("effects");
				if (!effectsObj.has("showEffectBorder")) {
					target.effects.showEffectBorder = true;
				}
				if (effectsObj.has("customEffectColors") && effectsObj.get("customEffectColors").isJsonNull()) {
					target.effects.customEffectColors.clear();
				}
			}
		}

		if (sourceVersion < 3) {
			migrated = true;
			if (!rawRoot.has("language") || rawRoot.get("language").isJsonNull()) {
				target.language = ModLanguage.ENGLISH;
			}
			if (target.armor != null) {
				JsonObject armorObj = rawRoot.has("armor") && rawRoot.get("armor").isJsonObject()
						? rawRoot.getAsJsonObject("armor")
						: null;
				if (armorObj == null || !armorObj.has("durabilityDisplayMode") || !armorObj.get("durabilityDisplayMode").isJsonPrimitive()) {
					target.armor.durabilityDisplayMode = DurabilityDisplayMode.POINTS_ONLY;
				} else {
					String modeRaw = armorObj.get("durabilityDisplayMode").getAsString();
					if ("PERCENT_ONLY".equalsIgnoreCase(modeRaw) || "POINTS_AND_PERCENT".equalsIgnoreCase(modeRaw)) {
						target.armor.durabilityDisplayMode = DurabilityDisplayMode.POINTS_ONLY;
					} else if (target.armor.durabilityDisplayMode == null) {
						target.armor.durabilityDisplayMode = DurabilityDisplayMode.POINTS_ONLY;
					}
				}
				if (target.armor.borderStyle == null) {
					target.armor.borderStyle = ArmorBorderStyle.PER_SLOT;
				}
				if (target.armor.metricPosition == null) {
					target.armor.metricPosition = ArmorMetricPosition.ABOVE;
				}
				if (armorObj == null || !armorObj.has("showHandItemName")) {
					target.armor.showHandItemName = false;
				}
			}

			if (target.effects != null) {
				JsonObject effectsObj = rawRoot.has("effects") && rawRoot.get("effects").isJsonObject()
						? rawRoot.getAsJsonObject("effects")
						: null;
				if (effectsObj == null || !effectsObj.has("showEffectBackground")) {
					target.effects.showEffectBackground = false;
				} else if (effectsObj.get("showEffectBackground").isJsonPrimitive()) {
					try {
						if (effectsObj.get("showEffectBackground").getAsBoolean()) {
							target.effects.showEffectBackground = false;
						}
					} catch (RuntimeException ignored) {
						target.effects.showEffectBackground = false;
					}
				}
				if (effectsObj != null && effectsObj.has("customEffectColors")) {
					JsonElement colorsEl = effectsObj.get("customEffectColors");
					if (colorsEl.isJsonNull()) {
						target.effects.customEffectColors.clear();
					}
				}
			}
		}

		if (sourceVersion < 4) {
			migrated = true;
			if (target.armor != null) {
				JsonObject armorObj = rawRoot.has("armor") && rawRoot.get("armor").isJsonObject()
						? rawRoot.getAsJsonObject("armor")
						: null;
				if (armorObj == null || !armorObj.has("veryHighThresholdPercent")) {
					target.armor.veryHighThresholdPercent = target.armor.warningThresholdPercent < 95
							? 95
							: Math.min(100, target.armor.warningThresholdPercent + 1);
				}
				if (armorObj == null || !armorObj.has("veryHighColorHex")) {
					target.armor.veryHighColorHex = "#FFFFFF";
				}
				if ("#6EEB83".equalsIgnoreCase(target.armor.healthyColorHex)) {
					target.armor.healthyColorHex = "#55FF55";
				}
				if ("#FFE066".equalsIgnoreCase(target.armor.warningColorHex)) {
					target.armor.warningColorHex = "#FFFF55";
				}
				if (armorObj == null || !armorObj.has("numberScale")) {
					target.armor.numberScale = 1.0f;
				}
				if (armorObj == null || !armorObj.has("showLowDurabilityAlert")) {
					target.armor.showLowDurabilityAlert = true;
				}
				if (armorObj == null || !armorObj.has("lowDurabilityAlertThresholdPercent")) {
					target.armor.lowDurabilityAlertThresholdPercent = 10;
				}
				if (armorObj == null || !armorObj.has("alertColorHex")) {
					target.armor.alertColorHex = "#FF5555";
				}
			}

			if (target.effects != null) {
				JsonObject effectsObj = rawRoot.has("effects") && rawRoot.get("effects").isJsonObject()
						? rawRoot.getAsJsonObject("effects")
						: null;
				// Only shift the old known default (TOP_LEFT with 0,0 offset) to CENTER_RIGHT.
				// Any custom or non-default position/anchor is preserved verbatim.
				if (effectsObj == null || !effectsObj.has("anchor")) {
					target.effects.anchor = HudAnchor.CENTER_RIGHT;
				} else if (target.effects.anchor == HudAnchor.TOP_LEFT
						&& target.effects.offsetX == 0
						&& target.effects.offsetY == 0) {
					target.effects.anchor = HudAnchor.CENTER_RIGHT;
				}
				if (effectsObj == null || !effectsObj.has("potionHudMode") || target.effects.potionHudMode == null) {
					target.effects.potionHudMode = PotionHudMode.NORMAL;
				}
				if (effectsObj == null || !effectsObj.has("potionMiniDisplay") || target.effects.potionMiniDisplay == null) {
					target.effects.potionMiniDisplay = PotionMiniDisplayMode.TIMER_TEXT;
				}
				if (effectsObj == null || !effectsObj.has("backgroundOpacityPercent")) {
					target.effects.backgroundOpacityPercent = 65;
				}
			}
		}

		if (sourceVersion < HudConfig.CURRENT_SCHEMA_VERSION) {
			target.schemaVersion = HudConfig.CURRENT_SCHEMA_VERSION;
			migrated = true;
		}

		return migrated;
	}
}
