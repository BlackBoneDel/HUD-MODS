package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ConfigManager;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.PointOffset;
import dev.customizablehud.config.PresetCatalog;
import dev.customizablehud.config.PresetId;
import dev.customizablehud.config.VanillaEffectsMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutAndConfigTest {
	@Test
	void computesArmorLayoutHotbarLeftAndClampsOnSmallScreens() {
		ArmorConfig config = new ArmorConfig();
		List<ArmorSlotMetric> metrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 40, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, false, 0, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 40, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 40, 0)
		);

		ArmorLayoutResult standard = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		assertEquals(4, standard.slots().size());
		HudRect hotbar = HudLayoutEngine.vanillaHotbarRect(640, 360);
		assertFalse(standard.bounds().intersects(hotbar));

		config.hideEmptySlots = true;
		ArmorLayoutResult hiddenEmpty = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		assertEquals(3, hiddenEmpty.slots().size());

		// Tiny screen still clamps inside viewport
		ArmorLayoutResult clamped = HudLayoutEngine.computeArmorLayout(config, metrics, 200, 120);
		assertTrue(clamped.bounds().x() >= 0);
		assertTrue(clamped.bounds().y() >= 0);
		assertTrue(clamped.bounds().right() <= 200);
		assertTrue(clamped.bounds().bottom() <= 120);
	}

	@Test
	void supportsIndividualArmorPositioningAndAutoLayoutCapture() {
		ArmorConfig config = new ArmorConfig();
		List<ArmorSlotMetric> metrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 36, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 36, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 36, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 36, 0)
		);

		Map<ArmorSlotKind, PointOffset> captured = HudLayoutEngine.captureIndividualOffsetsFromAuto(config, metrics, 640, 360);
		config.individualPositionsEnabled = true;
		config.anchor = HudAnchor.CUSTOM;
		config.offsetX = 100;
		config.offsetY = 100;
		config.individualOffsets = captured;
		config.individualOffsets.put(ArmorSlotKind.BOOTS, new PointOffset(200, 80));

		ArmorLayoutResult result = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		assertEquals(4, result.slots().size());
		ArmorSlotPlacement boots = result.slots().get(3);
		assertEquals(ArmorSlotKind.BOOTS, boots.kind());
		assertEquals(300, boots.screenBounds().x());
		assertEquals(180, boots.screenBounds().y());
	}

	@Test
	void preservesExactAutoSlotPositionsWithVariedNamesAndWidths() {
		ArmorConfig config = new ArmorConfig();
		config.showItemName = true;
		config.showMaxDurability = true;
		List<ArmorSlotMetric> variedMetrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 64, 82),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 68, 96),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 56, 76),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 12, 72)
		);

		ArmorLayoutResult auto = HudLayoutEngine.computeArmorLayout(config, variedMetrics, 854, 480);
		Map<ArmorSlotKind, PointOffset> captured = HudLayoutEngine.captureIndividualOffsetsFromAuto(config, variedMetrics, 854, 480);

		ArmorConfig individual = config.copy();
		individual.individualPositionsEnabled = true;
		individual.anchor = HudAnchor.CUSTOM;
		individual.offsetX = auto.originX();
		individual.offsetY = auto.originY();
		individual.individualOffsets = captured;

		ArmorLayoutResult afterCapture = HudLayoutEngine.computeArmorLayout(individual, variedMetrics, 854, 480);
		assertEquals(auto.slots().size(), afterCapture.slots().size());
		for (int i = 0; i < auto.slots().size(); i++) {
			assertEquals(auto.slots().get(i).screenBounds(), afterCapture.slots().get(i).screenBounds());
		}
	}

	@Test
	void snapsToGridCenterGuidesAndClampsToScreen() {
		HudRect hotbarRef = new HudRect(229, 338, 182, 22);
		HudLayoutEngine.SnapResult gridSnapped = HudLayoutEngine.snapPosition(
				19, 41, 80, 24, 640, 360, 4, true, true, List.of(hotbarRef)
		);
		assertEquals(20, gridSnapped.x());
		assertEquals(40, gridSnapped.y());

		HudLayoutEngine.SnapResult centerSnapped = HudLayoutEngine.snapPosition(
				282, 50, 80, 24, 640, 360, 1, false, true, List.of(hotbarRef)
		);
		assertEquals(280, centerSnapped.x());
		assertFalse(centerSnapped.guides().isEmpty());

		HudLayoutEngine.SnapResult clamped = HudLayoutEngine.snapPosition(
				-40, 999, 80, 24, 640, 360, 4, true, false, List.of()
		);
		assertEquals(2, clamped.x());
		assertEquals(334, clamped.y());
	}

	@Test
	void computesEffectsLayoutTopLeftAndDetectsOverlaps() {
		EffectsConfig config = new EffectsConfig();
		List<EffectRowMetric> rows = List.of(
				new EffectRowMetric("minecraft:speed", 54, 24),
				new EffectRowMetric("minecraft:haste", 46, 24)
		);
		EffectsLayoutResult layout = HudLayoutEngine.computeEffectsLayout(config, rows, 640, 360);
		assertEquals(6, layout.bounds().x());
		assertEquals(6, layout.bounds().y());
		assertEquals(2, layout.rows().size());

		HudRect a = new HudRect(10, 10, 40, 20);
		HudRect b = new HudRect(30, 15, 40, 20);
		HudRect c = new HudRect(100, 100, 30, 20);
		assertTrue(HudLayoutEngine.hasAnyOverlap(List.of(a, b)));
		assertFalse(HudLayoutEngine.hasAnyOverlap(List.of(a, c)));
	}

	@Test
	void validatesAndSanitizesOutOfBoundsAndCorruptConfigFields() {
		HudConfig dirty = new HudConfig();
		dirty.schemaVersion = 999;
		dirty.armor.scale = 12.0f;
		dirty.armor.slotSpacing = -5;
		dirty.armor.lowDurabilityThresholdPercent = 99;
		dirty.armor.armorOrder = List.of(ArmorSlotKind.BOOTS, ArmorSlotKind.BOOTS);
		dirty.armor.individualOffsets = null;
		dirty.effects.vanillaEffectsMode = null;

		ConfigValidator.ValidationResult result = ConfigValidator.validateAndSanitize(dirty);
		assertTrue(result.modified());
		HudConfig clean = result.config();
		assertEquals(HudConfig.CURRENT_SCHEMA_VERSION, clean.schemaVersion);
		assertEquals(ConfigValidator.MAX_SCALE, clean.armor.scale);
		assertEquals(ConfigValidator.MIN_SPACING, clean.armor.slotSpacing);
		assertEquals(ConfigValidator.MAX_THRESHOLD, clean.armor.lowDurabilityThresholdPercent);
		assertEquals(4, clean.armor.armorOrder.size());
		assertEquals(ArmorSlotKind.values().length, clean.armor.individualOffsets.size());
		assertEquals(VanillaEffectsMode.SHOW_BOTH, clean.effects.vanillaEffectsMode);
	}

	@Test
	void appliesNeutralPresetsCorrectly() {
		HudConfig cfg = HudConfig.createDefault();
		PresetCatalog.applyPreset(cfg, PresetId.COMPACT_TEXT);
		assertEquals(HudOrientation.VERTICAL, cfg.armor.orientation);
		assertTrue(cfg.armor.showMainHand);
		assertTrue(cfg.armor.showOffHand);

		PresetCatalog.applyPreset(cfg, PresetId.EXPANDED_READOUT);
		assertEquals(HudAnchor.HOTBAR_RIGHT, cfg.armor.anchor);
		assertTrue(cfg.armor.showMaxDurability);
		assertTrue(cfg.armor.lowDurabilityWarning);

		PresetCatalog.applyPreset(cfg, PresetId.VANILLA_COMPACT);
		assertEquals(HudAnchor.HOTBAR_LEFT, cfg.armor.anchor);
		assertEquals(VanillaEffectsMode.SHOW_BOTH, cfg.effects.vanillaEffectsMode);
	}

	@Test
	void configManagerPersistsAndRecoversFromCorruptJson(@TempDir Path tempDir) throws Exception {
		Path cfgFile = tempDir.resolve("customizablehud.json");
		ConfigManager manager = new ConfigManager(cfgFile);

		// First load initializes defaults on disk
		HudConfig initial = manager.load();
		assertTrue(Files.exists(cfgFile));
		assertEquals(HudAnchor.HOTBAR_LEFT, initial.armor.anchor);
		assertFalse(manager.consumeRecoveryNotice());

		// Save custom modification and reload
		initial.armor.showMainHand = true;
		initial.effects.hideLevelOne = false;
		manager.save();

		ConfigManager reloadedManager = new ConfigManager(cfgFile);
		HudConfig reloaded = reloadedManager.load();
		assertTrue(reloaded.armor.showMainHand);
		assertFalse(reloaded.effects.hideLevelOne);

		// Write corrupt JSON and verify backup + recovery
		Files.writeString(cfgFile, "{ broken_json: [ ", StandardCharsets.UTF_8);
		HudConfig recovered = reloadedManager.load();
		assertNotNull(recovered);
		assertTrue(reloadedManager.consumeRecoveryNotice());
		assertFalse(reloadedManager.consumeRecoveryNotice());
		assertEquals(HudAnchor.HOTBAR_LEFT, recovered.armor.anchor);

		try (Stream<Path> files = Files.list(tempDir)) {
			long backupCount = files.filter(p -> p.getFileName().toString().contains(".corrupt-")).count();
			assertEquals(1L, backupCount);
		}

		// Sanitize non-finite float values and unknown/null enum deserialization
		HudConfig nonFinite = new HudConfig();
		nonFinite.armor.scale = Float.NaN;
		nonFinite.effects.scale = Float.POSITIVE_INFINITY;
		ConfigValidator.ValidationResult sanitized = ConfigValidator.validateAndSanitize(nonFinite);
		assertEquals(1.0f, sanitized.config().armor.scale);
		assertEquals(1.0f, sanitized.config().effects.scale);

		Files.writeString(cfgFile, "{\"armor\":{\"anchor\":\"UNKNOWN_ANCHOR\",\"armorOrder\":[null,\"BOOTS\"]}}", StandardCharsets.UTF_8);
		HudConfig enumRepaired = reloadedManager.load();
		assertEquals(HudAnchor.HOTBAR_LEFT, enumRepaired.armor.anchor);
		assertEquals(4, enumRepaired.armor.armorOrder.size());
	}
}
