package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorBorderStyle;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorMetricPosition;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.ModLanguage;
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
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, config.durabilityDisplayMode);
		assertEquals(ArmorBorderStyle.PER_SLOT, config.borderStyle);
		assertEquals(ArmorMetricPosition.ABOVE, config.metricPosition);
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

		for (int i = 0; i < standard.slots().size(); i++) {
			ArmorSlotPlacement p = standard.slots().get(i);
			assertTrue(p.textBlockHeight() >= HudLayoutEngine.ARMOR_TEXT_LINE_HEIGHT);
			assertTrue(p.unscaledHeight() >= p.slotBoxSize() + p.textBlockHeight());
			if (i + 1 < standard.slots().size()) {
				assertTrue(p.screenBounds().right() <= standard.slots().get(i + 1).screenBounds().x());
			}
		}

		config.hideEmptySlots = true;
		ArmorLayoutResult hiddenEmpty = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		assertEquals(3, hiddenEmpty.slots().size());

		ArmorLayoutResult clamped = HudLayoutEngine.computeArmorLayout(config, metrics, 200, 120);
		assertTrue(clamped.bounds().x() >= 0);
		assertTrue(clamped.bounds().y() >= 0);
		assertTrue(clamped.bounds().right() <= 200);
		assertTrue(clamped.bounds().bottom() <= 120);
	}

	@Test
	void supportsSafeNegativeSpacingWithoutClippingIconsOrOverlappingWideTexts() {
		ArmorConfig config = new ArmorConfig();
		config.style = ArmorVisualStyle.VANILLA_HOTBAR; // 20px box around 16px icon -> safe down to -4
		config.slotSpacing = -4;

		List<ArmorSlotMetric> narrowMetrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 14, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 14, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 14, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 14, 0)
		);
		ArmorLayoutResult tight = HudLayoutEngine.computeArmorLayout(config, narrowMetrics, 640, 360);
		assertEquals(4, tight.slots().size());
		// 20px box with -4px spacing yields 16px icon-to-icon step so 16x16 icons touch without overlapping
		for (int i = 0; i + 1 < tight.slots().size(); i++) {
			int iconStep = tight.slots().get(i + 1).unscaledIconX() - tight.slots().get(i).unscaledIconX();
			assertEquals(16, iconStep);
		}
		assertEquals(68, tight.unscaledWidth());

		// Borderless style (16px box) enforces minimum safe spacing 0 so icons never overlap each other
		config.style = ArmorVisualStyle.BORDERLESS;
		ArmorLayoutResult borderlessTight = HudLayoutEngine.computeArmorLayout(config, narrowMetrics, 640, 360);
		for (int i = 0; i + 1 < borderlessTight.slots().size(); i++) {
			int iconStep = borderlessTight.slots().get(i + 1).unscaledIconX() - borderlessTight.slots().get(i).unscaledIconX();
			assertTrue(iconStep >= 16);
		}

		// Wide text protects neighboring text even when slotSpacing is -4
		config.style = ArmorVisualStyle.VANILLA_HOTBAR;
		List<ArmorSlotMetric> wideMetrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 48, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 48, 0)
		);
		ArmorLayoutResult protectedWide = HudLayoutEngine.computeArmorLayout(config, wideMetrics, 640, 360);
		ArmorSlotPlacement first = protectedWide.slots().get(0);
		ArmorSlotPlacement second = protectedWide.slots().get(1);
		assertTrue(second.unscaledX() - first.unscaledX() >= first.unscaledWidth() - 4);
	}

	@Test
	void supportsMetricPositionsAboveBelowLeftRightAndOuterFrameGroups() {
		List<ArmorSlotMetric> metrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 24, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 24, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 24, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 24, 0)
		);

		ArmorConfig config = new ArmorConfig();
		config.metricPosition = ArmorMetricPosition.ABOVE;
		ArmorLayoutResult above = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pAbove = above.slots().getFirst();
		assertTrue(pAbove.textOffsetY() < pAbove.iconOffsetY());

		config.metricPosition = ArmorMetricPosition.BELOW;
		ArmorLayoutResult below = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pBelow = below.slots().getFirst();
		assertEquals(0, pBelow.iconOffsetY());
		assertTrue(pBelow.textOffsetY() > pBelow.iconOffsetY());

		config.orientation = HudOrientation.VERTICAL;
		config.metricPosition = ArmorMetricPosition.LEFT;
		ArmorLayoutResult left = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pLeft = left.slots().getFirst();
		assertEquals(0, pLeft.textOffsetX());
		assertTrue(pLeft.iconOffsetX() > pLeft.textOffsetX());

		config.metricPosition = ArmorMetricPosition.RIGHT;
		ArmorLayoutResult right = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pRight = right.slots().getFirst();
		assertEquals(0, pRight.iconOffsetX());
		assertTrue(pRight.textOffsetX() > pRight.iconOffsetX());

		// Outer frame groups contiguous armor into 1 continuous rect (and hands into a separate continuous rect)
		config.orientation = HudOrientation.HORIZONTAL;
		config.metricPosition = ArmorMetricPosition.ABOVE;
		config.borderStyle = ArmorBorderStyle.OUTER_FRAME;
		config.slotSpacing = 0;
		config.showMainHand = true;
		List<ArmorSlotMetric> withHand = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 20, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 20, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 20, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 20, 0),
				new ArmorSlotMetric(ArmorSlotKind.MAIN_HAND, true, 20, 0)
		);
		ArmorLayoutResult outerLayout = HudLayoutEngine.computeArmorLayout(config, withHand, 640, 360);
		List<HudRect> frames = HudLayoutEngine.computeArmorOuterFrameRects(outerLayout, false);
		assertEquals(2, frames.size());
		assertEquals(80, frames.get(0).width());
		assertEquals(20, frames.get(0).height());
		assertEquals(20, frames.get(1).width());
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
	void resizesProportionallyFromCornerHandlesKeepingOppositeCornerAnchoredAndClamped() {
		HudRect initial = new HudRect(100, 100, 100, 60);

		// Drag bottom-right corner outward to 2x size -> top-left stays anchored at (100, 100)
		HudLayoutEngine.ResizeResult br = HudLayoutEngine.computeResizeFromCorner(
				initial, 100, 60, HudLayoutEngine.ResizeCorner.BOTTOM_RIGHT, 300, 220, 800, 600
		);
		assertEquals(2.0f, br.scale());
		assertEquals(100, br.offsetX());
		assertEquals(100, br.offsetY());
		assertEquals(200, br.bounds().width());
		assertEquals(120, br.bounds().height());

		// Drag top-left corner inward to 0.5x size -> bottom-right stays anchored at (200, 160)
		HudLayoutEngine.ResizeResult tl = HudLayoutEngine.computeResizeFromCorner(
				initial, 100, 60, HudLayoutEngine.ResizeCorner.TOP_LEFT, 150, 130, 800, 600
		);
		assertEquals(0.5f, tl.scale());
		assertEquals(200, tl.bounds().right());
		assertEquals(160, tl.bounds().bottom());

		// Extreme drag clamps scale to MAX_SCALE and clamps inside small screen bounds
		HudLayoutEngine.ResizeResult clamped = HudLayoutEngine.computeResizeFromCorner(
				initial, 100, 60, HudLayoutEngine.ResizeCorner.BOTTOM_RIGHT, 999, 999, 320, 200
		);
		assertEquals(ConfigValidator.MAX_SCALE, clamped.scale());
		assertTrue(clamped.bounds().x() >= 0);
		assertTrue(clamped.bounds().y() >= 0);
		assertTrue(clamped.bounds().right() <= 320);
		assertTrue(clamped.bounds().bottom() <= 200);
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
	void validatesThresholdsColorsSpacingAndSanitizesOutOfBoundsConfigFields() {
		HudConfig dirty = new HudConfig();
		dirty.schemaVersion = 999;
		dirty.language = null;
		dirty.armor.scale = 12.0f;
		dirty.armor.slotSpacing = -9;
		dirty.armor.borderStyle = null;
		dirty.armor.metricPosition = null;
		dirty.armor.warningThresholdPercent = 50;
		dirty.armor.criticalThresholdPercent = 55;
		dirty.armor.healthyColorHex = "not_hex";
		dirty.armor.armorOrder = List.of(ArmorSlotKind.BOOTS, ArmorSlotKind.BOOTS);
		dirty.armor.individualOffsets = null;
		dirty.effects.rowSpacing = -2;
		dirty.effects.vanillaEffectsMode = null;

		ConfigValidator.ValidationResult result = ConfigValidator.validateAndSanitize(dirty);
		assertTrue(result.modified());
		HudConfig clean = result.config();
		assertEquals(HudConfig.CURRENT_SCHEMA_VERSION, clean.schemaVersion);
		assertEquals(ConfigValidator.MAX_SCALE, clean.armor.scale);
		assertEquals(ConfigValidator.MIN_ARMOR_SPACING, clean.armor.slotSpacing);
		assertEquals(ArmorBorderStyle.PER_SLOT, clean.armor.borderStyle);
		assertEquals(ArmorMetricPosition.ABOVE, clean.armor.metricPosition);
		assertEquals(ConfigValidator.MIN_SPACING, clean.effects.rowSpacing);
		assertTrue(clean.armor.criticalThresholdPercent < clean.armor.warningThresholdPercent);
		assertEquals("#6EEB83", clean.armor.healthyColorHex);
		assertEquals(4, clean.armor.armorOrder.size());
		assertEquals(ArmorSlotKind.values().length, clean.armor.individualOffsets.size());
		assertEquals(VanillaEffectsMode.SHOW_BOTH, clean.effects.vanillaEffectsMode);
	}

	@Test
	void appliesPresetsWithSingleMetricOnlyAndPreservesLanguageAndCustomColors() {
		HudConfig cfg = HudConfig.createDefault();
		cfg.language = ModLanguage.SPANISH;
		cfg.effects.customEffectColors.put("minecraft:regeneration",
				new dev.customizablehud.config.EffectColorOverride(true, "#FF66B2"));

		PresetCatalog.applyPreset(cfg, PresetId.COMPACT_TEXT);
		assertEquals(HudOrientation.VERTICAL, cfg.armor.orientation);
		assertEquals(DurabilityDisplayMode.PERCENT_ONLY, cfg.armor.durabilityDisplayMode);
		assertFalse(cfg.effects.showEffectBackground);
		assertEquals(ModLanguage.SPANISH, cfg.language);
		assertTrue(cfg.effects.customEffectColors.containsKey("minecraft:regeneration"));

		PresetCatalog.applyPreset(cfg, PresetId.EXPANDED_READOUT);
		assertEquals(HudAnchor.HOTBAR_RIGHT, cfg.armor.anchor);
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, cfg.armor.durabilityDisplayMode);
		assertTrue(cfg.armor.showMaxDurability);

		PresetCatalog.applyPreset(cfg, PresetId.VANILLA_COMPACT);
		assertEquals(HudAnchor.HOTBAR_LEFT, cfg.armor.anchor);
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, cfg.armor.durabilityDisplayMode);
		assertFalse(cfg.effects.showEffectBackground);
		assertEquals(VanillaEffectsMode.SHOW_BOTH, cfg.effects.vanillaEffectsMode);
	}
}
