package dev.customizablehud.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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

		if (sourceVersion < HudConfig.CURRENT_SCHEMA_VERSION) {
			migrated = true;
			if (!rawRoot.has("language") || rawRoot.get("language").isJsonNull()) {
				target.language = ModLanguage.ENGLISH;
			}
			if (rawRoot.has("armor") && rawRoot.get("armor").isJsonObject() && target.armor != null) {
				JsonObject armorObj = rawRoot.getAsJsonObject("armor");
				if (armorObj.has("durabilityDisplayMode") && armorObj.get("durabilityDisplayMode").isJsonPrimitive()) {
					String modeRaw = armorObj.get("durabilityDisplayMode").getAsString();
					if ("POINTS_AND_PERCENT".equalsIgnoreCase(modeRaw)) {
						target.armor.durabilityDisplayMode = DurabilityDisplayMode.PERCENT_ONLY;
					}
				}
				if (target.armor.durabilityDisplayMode == null) {
					target.armor.durabilityDisplayMode = DurabilityDisplayMode.PERCENT_ONLY;
				}
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
				if (!effectsObj.has("showEffectBackground")) {
					target.effects.showEffectBackground = true;
				}
				if (!effectsObj.has("showEffectBorder")) {
					target.effects.showEffectBorder = true;
				}
				if (effectsObj.has("customEffectColors") && effectsObj.get("customEffectColors").isJsonObject()) {
					// Preserved through Gson + ConfigValidator
					JsonElement colorsEl = effectsObj.get("customEffectColors");
					if (colorsEl.isJsonNull()) {
						target.effects.customEffectColors.clear();
					}
				}
			}
			target.schemaVersion = HudConfig.CURRENT_SCHEMA_VERSION;
		}

		return migrated;
	}
}
