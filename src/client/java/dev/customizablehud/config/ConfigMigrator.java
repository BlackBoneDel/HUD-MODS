package dev.customizablehud.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Handles one-time JSON schema migrations across mod versions.
 *
 * <p>Migration rules from schema v1/v2 (Alpha 2) to schema v3 (Alpha 2.1):
 * <ul>
 *   <li>Because Alpha 2 configs did not track whether {@code armor.durabilityDisplayMode = "PERCENT_ONLY"}
 *       or {@code effects.showEffectBackground = true} were written as automatic defaults vs manually picked,
 *       upgrading a pre-v3 file once shifts those previous defaults to {@code POINTS_ONLY} and
 *       {@code showEffectBackground = false} while preserving explicit non-default choices ({@code NONE},
 *       {@code showEffectBackground = false}, custom colors including unloaded mods, positions, scale, etc.).</li>
 *   <li>Once saved at {@code schemaVersion = 3}, subsequent user changes (including choosing {@code PERCENT_ONLY}
 *       or enabling {@code showEffectBackground = true}) are never altered again.</li>
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

		if (sourceVersion < HudConfig.CURRENT_SCHEMA_VERSION) {
			target.schemaVersion = HudConfig.CURRENT_SCHEMA_VERSION;
			migrated = true;
		}

		return migrated;
	}
}
