package dev.customizablehud.config;

import dev.customizablehud.format.ModTranslations;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
	void migratesSchemaV1AndV2DefaultsOncePreservingCustomizationsAndModdedData(@TempDir Path tempDir) throws Exception {
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
		assertEquals(60, migrated.armor.warningThresholdPercent);
		assertEquals(20, migrated.armor.criticalThresholdPercent);
		assertEquals(1.35f, migrated.armor.scale);
		assertEquals(EffectsVisualStyle.CLEAN_CARD, migrated.effects.style);
		assertFalse(migrated.effects.showEffectBackground);
		assertTrue(migrated.effects.showEffectBorder);

		// Unknown modded effect ID preserved and hex normalized; malformed ID dropped
		assertEquals(1, migrated.effects.customEffectColors.size());
		EffectColorOverride moddedOverride = migrated.effects.customEffectColors.get("othermod:mana_regen");
		assertNotNull(moddedOverride);
		assertTrue(moddedOverride.enabled);
		assertEquals("#FF66B2", moddedOverride.colorHex);
		assertEquals(ConfigManager.SaveStatus.SAVED, manager.getSaveStatus());

		// After schema v3 migration, subsequent manual choices (PERCENT_ONLY, showEffectBackground=true) persist untouched
		HudConfig userEditedV3 = migrated.copy();
		userEditedV3.armor.durabilityDisplayMode = DurabilityDisplayMode.PERCENT_ONLY;
		userEditedV3.effects.showEffectBackground = true;
		assertTrue(manager.applyAndSaveNow(userEditedV3));

		HudConfig reloadedV3 = new ConfigManager(cfgFile).load();
		assertEquals(DurabilityDisplayMode.PERCENT_ONLY, reloadedV3.armor.durabilityDisplayMode);
		assertTrue(reloadedV3.effects.showEffectBackground);

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

		// Schedule debounced change: must NOT save before 450ms
		HudConfig edited = initial.copy();
		edited.language = ModLanguage.SPANISH;
		edited.armor.showMainHand = true;
		manager.applyAndScheduleSave(edited);

		assertTrue(manager.isDirty());
		assertEquals(ConfigManager.SaveStatus.UNSAVED, manager.getSaveStatus());
		clock.addAndGet(200L);
		assertFalse(manager.tickAutoSave());
		assertEquals(ConfigManager.SaveStatus.UNSAVED, manager.getSaveStatus());

		// Reloading fresh manager from disk still reads previous state before debounce expires
		HudConfig beforeDebounceOnDisk = new ConfigManager(cfgFile).load();
		assertEquals(ModLanguage.ENGLISH, beforeDebounceOnDisk.language);

		// Flush explicitly before debounce expires -> writes immediately
		assertTrue(manager.flushPendingSaves());
		assertFalse(manager.isDirty());
		assertEquals(ConfigManager.SaveStatus.SAVED, manager.getSaveStatus());
		assertEquals(ModLanguage.SPANISH, new ConfigManager(cfgFile).load().language);

		// Schedule another debounced change and advance past debounce -> writes atomically on tickAutoSave
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

		// Corrupt JSON recovery creates backup and raises one-shot notice
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

		// Real write failure MUST report FAILED and keep dirty=true (never lie "Saved")
		Path directoryAsFile = Files.createDirectory(tempDir.resolve("locked_as_dir"));
		ConfigManager failingManager = new ConfigManager(directoryAsFile, clock::get, 450L);
		assertFalse(failingManager.applyAndSaveNow(HudConfig.createDefault()));
		assertEquals(ConfigManager.SaveStatus.FAILED, failingManager.getSaveStatus());
		assertTrue(failingManager.isDirty());
		assertNotNull(failingManager.getLastSaveError());
	}

	@Test
	void enforcesEnglishSpanishTranslationParityAndFallbackResolution() {
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

		// Immediate switching in ModTranslations without touching global locale
		assertEquals("Saved", ModTranslations.translateBundled(ModLanguage.ENGLISH, "customizablehud.save_status.saved"));
		assertEquals("Guardado", ModTranslations.translateBundled(ModLanguage.SPANISH, "customizablehud.save_status.saved"));

		// Resource-pack JSON stream parsing and fallback priority chain
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
