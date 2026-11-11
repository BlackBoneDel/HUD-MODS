package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.PointOffset;
import dev.customizablehud.config.PresetCatalog;
import dev.customizablehud.config.PresetId;
import dev.customizablehud.config.VanillaEffectsMode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutAndConfigTest {
	@Test
	void computesCompactArmorLayoutAndPreventsTextOverlap() {
		ArmorConfig config = new ArmorConfig();
		assertEquals(DurabilityDisplayMode.PERCENT_ONLY, config.durabilityDisplayMode);
		assertFalse(config.showMainHand);
		assertFalse(config.showOffHand);
		assertFalse(config.lowDurabilityWarning);
		assertEquals(0, config.slotSpacing);
		assertEquals(20, HudLayoutEngine.slotBoxSizeFor(ArmorVisualStyle.VANILLA_HOTBAR));

		List<ArmorSlotMetric> metrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 24, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, false, 0, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 54, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 24, 0)
		);

		ArmorLayoutResult standard = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		assertEquals(4, standard.slots().size());
		HudRect hotbar = HudLayoutEngine.vanillaHotbarRect(640, 360);
		assertFalse(standard.bounds().intersects(hotbar));

		// Even with wide text label (54px raw), consecutive cells do not overlap and reserve text space above icon
		for (int i = 0; i < standard.slots().size(); i++) {
			ArmorSlotPlacement p = standard.slots().get(i);
			assertTrue(p.textBlockHeight() >= HudLayoutEngine.ARMOR_TEXT_LINE_HEIGHT);
			assertTrue(p.height() >= p.boxSize() + p.textBlockHeight());
			if (i + 1 < standard.slots().size()) {
				assertTrue(p.screenBounds().right() <= standard.slots().get(i + 1).screenBounds().x());
			}
		}

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
	void separatesHandsVisuallyWhenEnabled() {
		ArmorConfig config = new ArmorConfig();
		config.showMainHand = true;
		config.slotSpacing = 0;

		List<ArmorSlotMetric> metrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 22, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 22, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 22, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 22, 0),
				new ArmorSlotMetric(ArmorSlotKind.MAIN_HAND, true, 22, 0)
		);

		ArmorLayoutResult result = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		assertEquals(5, result.slots().size());
		ArmorSlotPlacement boots = result.slots().get(3);
		ArmorSlotPlacement mainHand = result.slots().get(4);
		assertTrue(mainHand.screenBounds().x() - boots.screenBounds().right() >= HudLayoutEngine.HAND_SEPARATOR_GAP);
	}

	@Test
	void supportsIndividualArmorPositioningAndAutoLayoutCapture() {
		ArmorConfig config = new ArmorConfig();
		List<ArmorSlotMetric> metrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 24, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 24, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 24, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 24, 0)
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
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 40, 62),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 44, 72),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 36, 56),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 12, 48)
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
	void validatesThresholdsColorsAndSanitizesOutOfBoundsConfigFields() {
		HudConfig dirty = new HudConfig();
		dirty.schemaVersion = 999;
		dirty.language = null;
		dirty.armor.scale = 12.0f;
		dirty.armor.slotSpacing = -5;
		dirty.armor.warningThresholdPercent = 50;
		dirty.armor.criticalThresholdPercent = 55; // invalid >= warning
		dirty.armor.healthyColorHex = "not_hex";
		dirty.armor.armorOrder = List.of(ArmorSlotKind.BOOTS, ArmorSlotKind.BOOTS);
		dirty.armor.individualOffsets = null;
		dirty.effects.vanillaEffectsMode = null;

		ConfigValidator.ValidationResult result = ConfigValidator.validateAndSanitize(dirty);
		assertTrue(result.modified());
		HudConfig clean = result.config();
		assertEquals(HudConfig.CURRENT_SCHEMA_VERSION, clean.schemaVersion);
		assertEquals(ConfigValidator.MAX_SCALE, clean.armor.scale);
		assertEquals(ConfigValidator.MIN_SPACING, clean.armor.slotSpacing);
		assertTrue(clean.armor.criticalThresholdPercent < clean.armor.warningThresholdPercent);
		assertEquals("#6EEB83", clean.armor.healthyColorHex);
		assertEquals(4, clean.armor.armorOrder.size());
		assertEquals(ArmorSlotKind.values().length, clean.armor.individualOffsets.size());
		assertEquals(VanillaEffectsMode.SHOW_BOTH, clean.effects.vanillaEffectsMode);
	}

	@Test
	void appliesPresetsWithSingleMetricOnlyAndPreservesLanguageAndCustomColors() {
		HudConfig cfg = HudConfig.createDefault();
		cfg.language = dev.customizablehud.config.ModLanguage.SPANISH;
		cfg.effects.customEffectColors.put("minecraft:regeneration",
				new dev.customizablehud.config.EffectColorOverride(true, "#FF66B2"));

		PresetCatalog.applyPreset(cfg, PresetId.COMPACT_TEXT);
		assertEquals(HudOrientation.VERTICAL, cfg.armor.orientation);
		assertEquals(DurabilityDisplayMode.PERCENT_ONLY, cfg.armor.durabilityDisplayMode);
		assertEquals(dev.customizablehud.config.ModLanguage.SPANISH, cfg.language);
		assertTrue(cfg.effects.customEffectColors.containsKey("minecraft:regeneration"));

		PresetCatalog.applyPreset(cfg, PresetId.EXPANDED_READOUT);
		assertEquals(HudAnchor.HOTBAR_RIGHT, cfg.armor.anchor);
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, cfg.armor.durabilityDisplayMode);
		assertTrue(cfg.armor.showMaxDurability);

		PresetCatalog.applyPreset(cfg, PresetId.VANILLA_COMPACT);
		assertEquals(HudAnchor.HOTBAR_LEFT, cfg.armor.anchor);
		assertEquals(DurabilityDisplayMode.PERCENT_ONLY, cfg.armor.durabilityDisplayMode);
		assertEquals(VanillaEffectsMode.SHOW_BOTH, cfg.effects.vanillaEffectsMode);
	}
}
