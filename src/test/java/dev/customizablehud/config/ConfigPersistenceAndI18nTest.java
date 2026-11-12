package dev.customizablehud.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.customizablehud.format.ModTranslations;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigPersistenceAndI18nTest {
	@Test
	void migratesSchemaV1ToV4OncePreservingCustomizationsPositionsAndModdedData(@TempDir Path tempDir) throws Exception {
		Path cfgFile = tempDir.resolve("customizablehud.json");
		String v2Alpha2Json = """
				{
				  "schemaVersion": 2,
				  "language": "SPANISH",
				  "armor": {
				    "enabled": false,
				    "durabilityDisplayMode": "PERCENT_ONLY",
				    "slotSpacing": -2,
				    "scale": 1.35
				  },
				  "effects": {
				    "style": "CLEAN_CARD",
				    "anchor": "TOP_LEFT",
				    "offsetX": 0,
				    "offsetY": 0,
				    "showEffectBackground": true,
				    "showEffectBorder": true,
				    "customEffectColors": {
				      "othermod:mana_regen": {
				        "enabled": true,
				        "colorHex": "#ff66b2"
				      },
				      "invalid id!!": {
				        "enabled": true,
				        "colorHex": "#123456"
				      }
				    }
				  }
				}
				""";
		Files.writeString(cfgFile, v2Alpha2Json, StandardCharsets.UTF_8);

		ConfigManager manager = new ConfigManager(cfgFile);
		HudConfig migrated = manager.load();

		assertEquals(HudConfig.CURRENT_SCHEMA_VERSION, migrated.schemaVersion);
		assertEquals(ModLanguage.SPANISH, migrated.language);
		assertFalse(migrated.armor.enabled);
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, migrated.armor.durabilityDisplayMode);
		assertEquals(ArmorBorderStyle.PER_SLOT, migrated.armor.borderStyle);
		assertEquals(ArmorMetricPosition.ABOVE, migrated.armor.metricPosition);
		assertFalse(migrated.armor.showHandItemName);
		assertEquals(-2, migrated.armor.slotSpacing);
		assertEquals(1.0f, migrated.armor.numberScale);
		assertEquals(95, migrated.armor.veryHighThresholdPercent);
		assertEquals(60, migrated.armor.warningThresholdPercent);
		assertEquals(20, migrated.armor.criticalThresholdPercent);
		assertEquals("#FFFFFF", migrated.armor.veryHighColorHex);
		assertEquals("#55FF55", migrated.armor.healthyColorHex);
		assertEquals("#FFFF55", migrated.armor.warningColorHex);
		assertEquals("#FF5555", migrated.armor.criticalColorHex);
		assertTrue(migrated.armor.showLowDurabilityAlert);
		assertEquals(10, migrated.armor.lowDurabilityAlertThresholdPercent);
		assertEquals(1.35f, migrated.armor.scale);
		assertEquals(EffectsVisualStyle.CLEAN_CARD, migrated.effects.style);
		assertEquals(HudAnchor.CENTER_RIGHT, migrated.effects.anchor);
		assertFalse(migrated.effects.showEffectBackground);
		assertTrue(migrated.effects.showEffectBorder);
		assertEquals(PotionHudMode.NORMAL, migrated.effects.potionHudMode);
		assertEquals(PotionMiniDisplayMode.TIMER_TEXT, migrated.effects.potionMiniDisplay);
		assertEquals(65, migrated.effects.backgroundOpacityPercent);

		// Unknown modded effect ID preserved and hex normalized; malformed ID dropped
		assertEquals(1, migrated.effects.customEffectColors.size());
		EffectColorOverride moddedOverride = migrated.effects.customEffectColors.get("othermod:mana_regen");
		assertNotNull(moddedOverride);
		assertTrue(moddedOverride.enabled);
		assertEquals("#FF66B2", moddedOverride.colorHex);
		assertEquals(ConfigManager.SaveStatus.SAVED, manager.getSaveStatus());

		// After schema v4 migration, subsequent manual choices persist untouched (including TOP_LEFT, custom colors, PotionHUD Mini)
		HudConfig userEditedV4 = migrated.copy();
		userEditedV4.armor.durabilityDisplayMode = DurabilityDisplayMode.PERCENT_ONLY;
		userEditedV4.armor.numberScale = 1.65f;
		userEditedV4.armor.showLowDurabilityAlert = false;
		userEditedV4.armor.healthyColorHex = "#6EEB83";
		userEditedV4.effects.anchor = HudAnchor.TOP_LEFT;
		userEditedV4.effects.showEffectBackground = true;
		userEditedV4.effects.style = EffectsVisualStyle.POTION_HUD;
		userEditedV4.effects.potionHudMode = PotionHudMode.MINI;
		userEditedV4.effects.potionMiniDisplay = PotionMiniDisplayMode.DURATION_BAR;
		userEditedV4.effects.backgroundOpacityPercent = 40;
		assertTrue(manager.applyAndSaveNow(userEditedV4));

		HudConfig reloadedV4 = new ConfigManager(cfgFile).load();
		assertEquals(DurabilityDisplayMode.PERCENT_ONLY, reloadedV4.armor.durabilityDisplayMode);
		assertEquals(1.65f, reloadedV4.armor.numberScale);
		assertFalse(reloadedV4.armor.showLowDurabilityAlert);
		assertEquals("#6EEB83", reloadedV4.armor.healthyColorHex);
		assertEquals(HudAnchor.TOP_LEFT, reloadedV4.effects.anchor);
		assertTrue(reloadedV4.effects.showEffectBackground);
		assertEquals(EffectsVisualStyle.POTION_HUD, reloadedV4.effects.style);
		assertEquals(PotionHudMode.MINI, reloadedV4.effects.potionHudMode);
		assertEquals(PotionMiniDisplayMode.DURATION_BAR, reloadedV4.effects.potionMiniDisplay);
		assertEquals(40, reloadedV4.effects.backgroundOpacityPercent);

		// Schema v3 with custom Effects position and custom Armor healthy color keeps them intact on v4 migration
		Path v3CustomFile = tempDir.resolve("custom-v3.json");
		String v3CustomJson = """
				{
				  "schemaVersion": 3,
				  "armor": {
				    "anchor": "CUSTOM",
				    "offsetX": 42,
				    "offsetY": 188,
				    "healthyColorHex": "#34D399"
				  },
				  "effects": {
				    "anchor": "CUSTOM",
				    "offsetX": 220,
				    "offsetY": 44
				  }
				}
				""";
		Files.writeString(v3CustomFile, v3CustomJson, StandardCharsets.UTF_8);
		HudConfig migratedV3 = new ConfigManager(v3CustomFile).load();
		assertEquals(HudAnchor.CUSTOM, migratedV3.armor.anchor);
		assertEquals(42, migratedV3.armor.offsetX);
		assertEquals(188, migratedV3.armor.offsetY);
		assertEquals("#34D399", migratedV3.armor.healthyColorHex);
		assertEquals(HudAnchor.CUSTOM, migratedV3.effects.anchor);
		assertEquals(220, migratedV3.effects.offsetX);
		assertEquals(44, migratedV3.effects.offsetY);

		// Legacy schema v1 with explicit NONE durability keeps NONE and migrates legacy low threshold
		Path v1File = tempDir.resolve("legacy-v1.json");
		String v1Json = """
				{
				  "schemaVersion": 1,
				  "armor": {
				    "durabilityDisplayMode": "NONE",
				    "lowDurabilityThresholdPercent": 12
				  }
				}
				""";
		Files.writeString(v1File, v1Json, StandardCharsets.UTF_8);
		HudConfig migratedV1 = new ConfigManager(v1File).load();
		assertEquals(DurabilityDisplayMode.NONE, migratedV1.armor.durabilityDisplayMode);
		assertEquals(12, migratedV1.armor.criticalThresholdPercent);
		assertFalse(migratedV1.effects.showEffectBackground);
		assertEquals(HudAnchor.CENTER_RIGHT, migratedV1.effects.anchor);
	}

	@Test
	void debouncesAutoSaveFlushesOnDemandAndHandlesCorruptAndFailedSaves(@TempDir Path tempDir) throws Exception {
		AtomicLong clock = new AtomicLong(1000L);
		Path cfgFile = tempDir.resolve("customizablehud.json");
		ConfigManager manager = new ConfigManager(cfgFile, clock::get, 450L);

		HudConfig initial = manager.load();
		assertTrue(Files.exists(cfgFile));
		assertEquals(ConfigManager.SaveStatus.SAVED, manager.getSaveStatus());
		assertFalse(manager.consumeRecoveryNotice());

		HudConfig edited = initial.copy();
		edited.language = ModLanguage.SPANISH;
		edited.armor.showMainHand = true;
		manager.applyAndScheduleSave(edited);

		assertTrue(manager.isDirty());
		assertEquals(ConfigManager.SaveStatus.UNSAVED, manager.getSaveStatus());
		clock.addAndGet(200L);
		assertFalse(manager.tickAutoSave());
		assertEquals(ConfigManager.SaveStatus.UNSAVED, manager.getSaveStatus());

		HudConfig beforeDebounceOnDisk = new ConfigManager(cfgFile).load();
		assertEquals(ModLanguage.ENGLISH, beforeDebounceOnDisk.language);

		assertTrue(manager.flushPendingSaves());
		assertFalse(manager.isDirty());
		assertEquals(ConfigManager.SaveStatus.SAVED, manager.getSaveStatus());
		assertEquals(ModLanguage.SPANISH, new ConfigManager(cfgFile).load().language);

		HudConfig edited2 = edited.copy();
		edited2.effects.showEffectBackground = true;
		manager.applyAndScheduleSave(edited2);
		assertEquals(ConfigManager.SaveStatus.UNSAVED, manager.getSaveStatus());
		clock.addAndGet(500L);
		assertTrue(manager.tickAutoSave());
		assertFalse(manager.isDirty());
		assertEquals(ConfigManager.SaveStatus.SAVED, manager.getSaveStatus());

		HudConfig afterDebounce = new ConfigManager(cfgFile).load();
		assertEquals(ModLanguage.SPANISH, afterDebounce.language);
		assertTrue(afterDebounce.armor.showMainHand);
		assertTrue(afterDebounce.effects.showEffectBackground);

		Files.writeString(cfgFile, "{ corrupt json [", StandardCharsets.UTF_8);
		HudConfig recovered = new ConfigManager(cfgFile).load();
		assertNotNull(recovered);
		try (Stream<Path> files = Files.list(tempDir)) {
			assertEquals(1L, files.filter(p -> p.getFileName().toString().contains(".corrupt-")).count());
		}

		ConfigManager corruptManager = new ConfigManager(cfgFile);
		Files.writeString(cfgFile, "not json", StandardCharsets.UTF_8);
		corruptManager.load();
		assertTrue(corruptManager.consumeRecoveryNotice());
		assertFalse(corruptManager.consumeRecoveryNotice());

		Path directoryAsFile = Files.createDirectory(tempDir.resolve("locked_as_dir"));
		ConfigManager failingManager = new ConfigManager(directoryAsFile, clock::get, 450L);
		assertFalse(failingManager.applyAndSaveNow(HudConfig.createDefault()));
		assertEquals(ConfigManager.SaveStatus.FAILED, failingManager.getSaveStatus());
		assertTrue(failingManager.isDirty());
		assertNotNull(failingManager.getLastSaveError());
	}

	@Test
	void enforcesHudUtilsBrandingEnglishSpanishParityAndFallbackResolution() throws Exception {
		// Verify our module's fabric.mod.json on the test classpath carries Hud Utils / Denvdrite with customizablehud ID
		JsonObject ownFmod = null;
		Enumeration<URL> fmodResources = ConfigPersistenceAndI18nTest.class.getClassLoader().getResources("fabric.mod.json");
		while (fmodResources.hasMoreElements()) {
			URL url = fmodResources.nextElement();
			try (InputStream stream = url.openStream()) {
				JsonObject parsed = JsonParser.parseString(new String(stream.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
				if (parsed.has("id") && "customizablehud".equals(parsed.get("id").getAsString())) {
					ownFmod = parsed;
					break;
				}
			}
		}
		assertNotNull(ownFmod, "customizablehud fabric.mod.json must be present on test classpath");
		assertEquals("customizablehud", ownFmod.get("id").getAsString());
		assertEquals("Hud Utils", ownFmod.get("name").getAsString());
		assertEquals("1.0.0-alpha.1", ownFmod.get("version").getAsString());
		assertTrue(ownFmod.getAsJsonArray("authors").toString().contains("Denvdrite"));

		Map<String, String> en = ModTranslations.getBundledTranslations(ModLanguage.ENGLISH);
		Map<String, String> es = ModTranslations.getBundledTranslations(ModLanguage.SPANISH);

		assertFalse(en.isEmpty());
		assertEquals(en.keySet(), es.keySet());
		for (Map.Entry<String, String> entry : es.entrySet()) {
			assertFalse(entry.getValue().isBlank(), "Blank es_es value for key " + entry.getKey());
		}
		for (Map.Entry<String, String> entry : en.entrySet()) {
			assertFalse(entry.getValue().isBlank(), "Blank en_us value for key " + entry.getKey());
		}

		assertEquals("Hud Utils", ModTranslations.translateBundled(ModLanguage.ENGLISH, "customizablehud.screen.main.title"));
		assertEquals("Hud Utils", ModTranslations.translateBundled(ModLanguage.SPANISH, "customizablehud.screen.main.title"));
		assertTrue(ModTranslations.translateBundled(ModLanguage.ENGLISH, "customizablehud.screen.main.subtitle").contains("Denvdrite"));
		assertTrue(ModTranslations.translateBundled(ModLanguage.SPANISH, "customizablehud.screen.main.subtitle").contains("Denvdrite"));

		assertEquals("Saved", ModTranslations.translateBundled(ModLanguage.ENGLISH, "customizablehud.save_status.saved"));
		assertEquals("Guardado", ModTranslations.translateBundled(ModLanguage.SPANISH, "customizablehud.save_status.saved"));

		Map<String, String> resourcePackOverride = ModTranslations.parseJsonStream(
				new ByteArrayInputStream("{\"effect.examplemod.mana_regen\": \"Regeneración de maná\"}".getBytes(StandardCharsets.UTF_8))
		);
		assertEquals(
				"Regeneración de maná",
				ModTranslations.resolveWithFallbacks(
						"effect.examplemod.mana_regen",
						resourcePackOverride,
						Map.of(),
						en,
						"examplemod:mana_regen"
				)
		);
		assertEquals(
				"Mana Regen",
				ModTranslations.resolveWithFallbacks(
						"effect.examplemod.mana_regen",
						Map.of(),
						Map.of(),
						en,
						"examplemod:mana_regen"
				)
		);
	}
}
