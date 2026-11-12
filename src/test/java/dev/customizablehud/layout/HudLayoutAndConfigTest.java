package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorBorderStyle;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorMetricPosition;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.EffectsVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.ModLanguage;
import dev.customizablehud.config.PointOffset;
import dev.customizablehud.config.PotionHudMode;
import dev.customizablehud.config.PotionMiniDisplayMode;
import dev.customizablehud.config.PresetCatalog;
import dev.customizablehud.config.PresetId;
import dev.customizablehud.config.VanillaEffectsMode;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudLayoutAndConfigTest {
	@Test
	void computesCompactSquareArmorStripWithoutGapsAndAvoidsHotbarOffhandAndAttackIndicator() {
		ArmorConfig config = new ArmorConfig();
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, config.durabilityDisplayMode);
		assertEquals(ArmorBorderStyle.PER_SLOT, config.borderStyle);
		assertEquals(ArmorMetricPosition.ABOVE, config.metricPosition);
		assertFalse(config.hideEmptySlots);
		assertFalse(config.showMainHand);
		assertFalse(config.showOffHand);
		assertTrue(config.showLowDurabilityAlert);
		assertFalse(config.lowDurabilityWarning);
		assertEquals(0, config.slotSpacing);
		assertEquals(1.0f, config.numberScale);
		assertEquals(20, HudLayoutEngine.slotBoxSizeFor(ArmorVisualStyle.VANILLA_HOTBAR));
		assertEquals(ArmorSlotKind.DEFAULT_ARMOR_ORDER, config.activeSlotOrder());

		// Real 3-digit Minecraft durability numbers ("363", "428", "215", "17") have ~17px raw width -> fit inside 20px square
		List<ArmorSlotMetric> standardPoints = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, false, 0, 0)
		);

		ArmorLayoutResult strip = HudLayoutEngine.computeArmorLayout(config, standardPoints, 640, 360);
		assertEquals(4, strip.slots().size());
		assertEquals(80, strip.unscaledWidth());
		for (int i = 0; i < strip.slots().size(); i++) {
			ArmorSlotPlacement p = strip.slots().get(i);
			assertEquals(20, p.slotBoxSize());
			if (i + 1 < strip.slots().size()) {
				// Zero gap between adjacent 20x20 square slots
				assertEquals(p.unscaledIconX() + 20, strip.slots().get(i + 1).unscaledIconX());
				assertEquals(p.unscaledIconY(), strip.slots().get(i + 1).unscaledIconY());
				assertTrue(p.screenBounds().right() <= strip.slots().get(i + 1).screenBounds().x());
			}
		}

		// Check HOTBAR_LEFT & HOTBAR_RIGHT across common GUI resolutions: never overlap hotbar, offhand, or attack indicator
		for (int[] res : new int[][]{{427, 240}, {640, 360}, {854, 480}, {960, 540}}) {
			int w = res[0];
			int h = res[1];
			HudRect hotbar = HudLayoutEngine.vanillaHotbarRect(w, h);
			HudRect offLeft = HudLayoutEngine.vanillaOffhandLeftRect(w, h);
			HudRect offRight = HudLayoutEngine.vanillaOffhandRightRect(w, h);
			HudRect atkLeft = HudLayoutEngine.vanillaAttackIndicatorLeftRect(w, h);
			HudRect atkRight = HudLayoutEngine.vanillaAttackIndicatorRightRect(w, h);

			config.anchor = HudAnchor.HOTBAR_LEFT;
			ArmorLayoutResult leftHud = HudLayoutEngine.computeArmorLayout(config, standardPoints, w, h);
			assertFalse(leftHud.bounds().intersects(hotbar));
			assertFalse(leftHud.bounds().intersects(offLeft));
			assertFalse(leftHud.bounds().intersects(atkLeft));
			assertTrue(leftHud.bounds().x() >= 0 && leftHud.bounds().y() >= 0);

			config.anchor = HudAnchor.HOTBAR_RIGHT;
			ArmorLayoutResult rightHud = HudLayoutEngine.computeArmorLayout(config, standardPoints, w, h);
			assertFalse(rightHud.bounds().intersects(hotbar));
			assertFalse(rightHud.bounds().intersects(offRight));
			assertFalse(rightHud.bounds().intersects(atkRight));
			assertTrue(rightHud.bounds().right() <= w && rightHud.bounds().bottom() <= h);
		}

		config.hideEmptySlots = true;
		ArmorLayoutResult hiddenEmpty = HudLayoutEngine.computeArmorLayout(config, standardPoints, 640, 360);
		assertEquals(3, hiddenEmpty.slots().size());

		ArmorLayoutResult clamped = HudLayoutEngine.computeArmorLayout(config, standardPoints, 200, 120);
		assertTrue(clamped.bounds().x() >= 0);
		assertTrue(clamped.bounds().y() >= 0);
		assertTrue(clamped.bounds().right() <= 200);
		assertTrue(clamped.bounds().bottom() <= 120);
	}

	@Test
	void scalesOnlyNumbersWithoutDeformingSquareSlotsAndHandlesAlertBadgeSafely() {
		ArmorConfig config = new ArmorConfig();
		config.numberScale = 0.50f;
		List<ArmorSlotMetric> alertMetrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 17, 0, 9, false),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 17, 0, 9, false),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 17, 0, 9, false),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 17, 0, 9, true)
		);
		ArmorLayoutResult smallNums = HudLayoutEngine.computeArmorLayout(config, alertMetrics, 640, 360);

		config.numberScale = 2.00f;
		ArmorLayoutResult bigNums = HudLayoutEngine.computeArmorLayout(config, alertMetrics, 640, 360);

		// Square icon box remains strictly 20x20 regardless of number scale
		assertEquals(20, smallNums.slots().getFirst().slotBoxSize());
		assertEquals(20, bigNums.slots().getFirst().slotBoxSize());
		// Number scale & line height grow cleanly without touching icon box size or overlapping neighboring numbers
		assertTrue(bigNums.slots().getFirst().numberTextScale() > smallNums.slots().getFirst().numberTextScale());
		assertTrue(bigNums.slots().getFirst().numberLineHeight() > smallNums.slots().getFirst().numberLineHeight());
		for (int i = 0; i + 1 < bigNums.slots().size(); i++) {
			assertTrue(bigNums.slots().get(i).screenBounds().right() <= bigNums.slots().get(i + 1).screenBounds().x());
		}

		// Alert badge only active on Boots and placed cleanly above text/icon without clipping
		assertFalse(bigNums.slots().get(0).alertActive());
		ArmorSlotPlacement boots = bigNums.slots().get(3);
		assertTrue(boots.alertActive());
		assertTrue(boots.alertOffsetY() >= 0);
		assertTrue(boots.alertOffsetY() + HudLayoutEngine.ALERT_BADGE_HEIGHT <= boots.textOffsetY());
		assertTrue(boots.unscaledAlertX() >= boots.unscaledX());
		assertTrue(boots.unscaledAlertX() + HudLayoutEngine.ALERT_BADGE_WIDTH <= boots.unscaledX() + boots.unscaledWidth());

		// Custom larger font pack lineHeight (13px) expands headroom cleanly without clipping
		List<ArmorSlotMetric> tallFontMetrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 24, 0, 13, true)
		);
		ArmorLayoutResult tallFontLayout = HudLayoutEngine.computeArmorLayout(config, tallFontMetrics, 640, 360);
		ArmorSlotPlacement tallHelmet = tallFontLayout.slots().getFirst();
		assertTrue(tallHelmet.numberLineHeight() >= 19);
		assertTrue(tallHelmet.textOffsetY() + tallHelmet.numberLineHeight() <= tallHelmet.iconOffsetY());
	}

	@Test
	void supportsSafeNegativeSpacingWithoutClippingIconsOrOverlappingWideTexts() {
		ArmorConfig config = new ArmorConfig();
		config.style = ArmorVisualStyle.VANILLA_HOTBAR;
		config.slotSpacing = -4;

		List<ArmorSlotMetric> narrowMetrics = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 14, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 14, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 14, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 14, 0)
		);
		ArmorLayoutResult tight = HudLayoutEngine.computeArmorLayout(config, narrowMetrics, 640, 360);
		assertEquals(4, tight.slots().size());
		for (int i = 0; i + 1 < tight.slots().size(); i++) {
			int iconStep = tight.slots().get(i + 1).unscaledIconX() - tight.slots().get(i).unscaledIconX();
			assertEquals(16, iconStep);
		}
		assertEquals(68, tight.unscaledWidth());

		config.style = ArmorVisualStyle.BORDERLESS;
		ArmorLayoutResult borderlessTight = HudLayoutEngine.computeArmorLayout(config, narrowMetrics, 640, 360);
		for (int i = 0; i + 1 < borderlessTight.slots().size(); i++) {
			int iconStep = borderlessTight.slots().get(i + 1).unscaledIconX() - borderlessTight.slots().get(i).unscaledIconX();
			assertTrue(iconStep >= 16);
		}

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
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 17, 0, 9, true),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 17, 0, 9, false),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 17, 0, 9, false),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 17, 0, 9, false)
		);

		ArmorConfig config = new ArmorConfig();
		config.metricPosition = ArmorMetricPosition.ABOVE;
		ArmorLayoutResult above = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pAbove = above.slots().getFirst();
		assertTrue(pAbove.textOffsetY() < pAbove.iconOffsetY());
		assertTrue(pAbove.alertOffsetY() < pAbove.textOffsetY());

		config.metricPosition = ArmorMetricPosition.BELOW;
		ArmorLayoutResult below = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pBelow = below.slots().getFirst();
		assertEquals(0, pBelow.iconOffsetY());
		assertTrue(pBelow.textOffsetY() > pBelow.iconOffsetY());
		assertTrue(pBelow.alertOffsetY() > pBelow.textOffsetY());

		config.orientation = HudOrientation.VERTICAL;
		config.metricPosition = ArmorMetricPosition.LEFT;
		ArmorLayoutResult left = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pLeft = left.slots().getFirst();
		assertTrue(pLeft.alertOffsetX() < pLeft.textOffsetX());
		assertTrue(pLeft.iconOffsetX() > pLeft.textOffsetX());

		config.metricPosition = ArmorMetricPosition.RIGHT;
		ArmorLayoutResult right = HudLayoutEngine.computeArmorLayout(config, metrics, 640, 360);
		ArmorSlotPlacement pRight = right.slots().getFirst();
		assertEquals(0, pRight.iconOffsetX());
		assertTrue(pRight.textOffsetX() > pRight.iconOffsetX());
		assertTrue(pRight.alertOffsetX() > pRight.textOffsetX());

		config.orientation = HudOrientation.HORIZONTAL;
		config.metricPosition = ArmorMetricPosition.ABOVE;
		config.borderStyle = ArmorBorderStyle.OUTER_FRAME;
		config.slotSpacing = 0;
		config.showMainHand = true;
		List<ArmorSlotMetric> withHand = List.of(
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.MAIN_HAND, true, 17, 0)
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
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.MAIN_HAND, true, 17, 0)
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
				new ArmorSlotMetric(ArmorSlotKind.HELMET, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.CHESTPLATE, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.LEGGINGS, true, 17, 0),
				new ArmorSlotMetric(ArmorSlotKind.BOOTS, true, 17, 0)
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

		HudLayoutEngine.ResizeResult br = HudLayoutEngine.computeResizeFromCorner(
				initial, 100, 60, HudLayoutEngine.ResizeCorner.BOTTOM_RIGHT, 300, 220, 800, 600
		);
		assertEquals(2.0f, br.scale());
		assertEquals(100, br.offsetX());
		assertEquals(100, br.offsetY());
		assertEquals(200, br.bounds().width());
		assertEquals(120, br.bounds().height());

		HudLayoutEngine.ResizeResult tl = HudLayoutEngine.computeResizeFromCorner(
				initial, 100, 60, HudLayoutEngine.ResizeCorner.TOP_LEFT, 150, 130, 800, 600
		);
		assertEquals(0.5f, tl.scale());
		assertEquals(200, tl.bounds().right());
		assertEquals(160, tl.bounds().bottom());

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
	void computesEffectsCenterRightDefaultAndPotionHudNormalAndMiniLayouts() {
		EffectsConfig config = new EffectsConfig();
		List<EffectRowMetric> rows = List.of(
				new EffectRowMetric("minecraft:speed", 54, 24, 10),
				new EffectRowMetric("minecraft:haste", 46, 24, 0)
		);
		EffectsLayoutResult defaultLayout = HudLayoutEngine.computeEffectsLayout(config, rows, 640, 360);
		assertEquals(640 - defaultLayout.bounds().width() - 8, defaultLayout.bounds().x());
		assertEquals((360 - defaultLayout.bounds().height()) / 2, defaultLayout.bounds().y());
		assertEquals(2, defaultLayout.rows().size());

		// PotionHUD Mini horizontal with duration bar produces compact non-overlapping tiles
		config.style = EffectsVisualStyle.POTION_HUD;
		config.potionHudMode = PotionHudMode.MINI;
		config.potionMiniDisplay = PotionMiniDisplayMode.DURATION_BAR;
		config.orientation = HudOrientation.HORIZONTAL;
		config.rowSpacing = 3;
		EffectsLayoutResult miniHorizontal = HudLayoutEngine.computeEffectsLayout(config, rows, 640, 360);
		assertEquals(2, miniHorizontal.rows().size());
		EffectRowPlacement firstMini = miniHorizontal.rows().get(0);
		EffectRowPlacement secondMini = miniHorizontal.rows().get(1);
		assertEquals(firstMini.unscaledX() + firstMini.unscaledWidth() + 3, secondMini.unscaledX());
		assertFalse(firstMini.screenBounds().intersects(secondMini.screenBounds()));

		// PotionHUD Normal vertical also maintains non-overlapping rows
		config.potionHudMode = PotionHudMode.NORMAL;
		config.orientation = HudOrientation.VERTICAL;
		EffectsLayoutResult normalVertical = HudLayoutEngine.computeEffectsLayout(config, rows, 640, 360);
		assertFalse(normalVertical.rows().get(0).screenBounds().intersects(normalVertical.rows().get(1).screenBounds()));

		HudRect a = new HudRect(10, 10, 40, 20);
		HudRect b = new HudRect(30, 15, 40, 20);
		HudRect c = new HudRect(100, 100, 30, 20);
		assertTrue(HudLayoutEngine.hasAnyOverlap(List.of(a, b)));
		assertFalse(HudLayoutEngine.hasAnyOverlap(List.of(a, c)));
	}

	@Test
	void validatesThresholdsNumberScaleOpacityColorsAndSanitizesNaNAndInvalidInputs() {
		HudConfig dirty = new HudConfig();
		dirty.schemaVersion = 999;
		dirty.language = null;
		dirty.armor.scale = 12.0f;
		dirty.armor.numberScale = Float.NaN;
		dirty.armor.slotSpacing = -9;
		dirty.armor.borderStyle = null;
		dirty.armor.metricPosition = null;
		dirty.armor.veryHighThresholdPercent = 50;
		dirty.armor.warningThresholdPercent = 60;
		dirty.armor.criticalThresholdPercent = 55;
		dirty.armor.veryHighColorHex = "bad";
		dirty.armor.healthyColorHex = "not_hex";
		dirty.armor.lowDurabilityAlertThresholdPercent = 500;
		dirty.armor.alertColorHex = "oops";
		dirty.armor.armorOrder = List.of(ArmorSlotKind.BOOTS, ArmorSlotKind.BOOTS);
		dirty.armor.individualOffsets = null;
		dirty.effects.anchor = null;
		dirty.effects.rowSpacing = -2;
		dirty.effects.backgroundOpacityPercent = 180;
		dirty.effects.potionHudMode = null;
		dirty.effects.potionMiniDisplay = null;
		dirty.effects.vanillaEffectsMode = null;

		ConfigValidator.ValidationResult result = ConfigValidator.validateAndSanitize(dirty);
		assertTrue(result.modified());
		HudConfig clean = result.config();
		assertEquals(HudConfig.CURRENT_SCHEMA_VERSION, clean.schemaVersion);
		assertEquals(ConfigValidator.MAX_SCALE, clean.armor.scale);
		assertEquals(1.0f, clean.armor.numberScale);
		assertEquals(ConfigValidator.MIN_ARMOR_SPACING, clean.armor.slotSpacing);
		assertEquals(ArmorBorderStyle.PER_SLOT, clean.armor.borderStyle);
		assertEquals(ArmorMetricPosition.ABOVE, clean.armor.metricPosition);
		assertTrue(clean.armor.criticalThresholdPercent < clean.armor.warningThresholdPercent);
		assertTrue(clean.armor.warningThresholdPercent < clean.armor.veryHighThresholdPercent);
		assertEquals("#FFFFFF", clean.armor.veryHighColorHex);
		assertEquals("#55FF55", clean.armor.healthyColorHex);
		assertEquals("#FF5555", clean.armor.alertColorHex);
		assertEquals(ConfigValidator.MAX_ALERT_THRESHOLD, clean.armor.lowDurabilityAlertThresholdPercent);
		assertEquals(4, clean.armor.armorOrder.size());
		assertEquals(ArmorSlotKind.values().length, clean.armor.individualOffsets.size());
		assertEquals(HudAnchor.CENTER_RIGHT, clean.effects.anchor);
		assertEquals(ConfigValidator.MIN_SPACING, clean.effects.rowSpacing);
		assertEquals(100, clean.effects.backgroundOpacityPercent);
		assertEquals(PotionHudMode.NORMAL, clean.effects.potionHudMode);
		assertEquals(PotionMiniDisplayMode.TIMER_TEXT, clean.effects.potionMiniDisplay);
		assertEquals(VanillaEffectsMode.SHOW_BOTH, clean.effects.vanillaEffectsMode);

		// Rejects Infinity, -Infinity, zero, and clamps extremes on numberScale
		assertEquals(1.0f, ConfigValidator.clampNumberScale(Float.NaN));
		assertEquals(1.0f, ConfigValidator.clampNumberScale(Float.POSITIVE_INFINITY));
		assertEquals(1.0f, ConfigValidator.clampNumberScale(Float.NEGATIVE_INFINITY));
		assertEquals(1.0f, ConfigValidator.clampNumberScale(0.0f));
		assertEquals(1.0f, ConfigValidator.clampNumberScale(-2.0f));
		assertEquals(0.50f, ConfigValidator.clampNumberScale(0.25f));
		assertEquals(2.00f, ConfigValidator.clampNumberScale(5.00f));
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
		assertEquals(HudAnchor.CENTER_RIGHT, cfg.effects.anchor);
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
		assertEquals(HudAnchor.CENTER_RIGHT, cfg.effects.anchor);
		assertFalse(cfg.effects.showEffectBackground);
		assertEquals(VanillaEffectsMode.SHOW_BOTH, cfg.effects.vanillaEffectsMode);
	}

	@Test
	void includesZeroInventoryHudPlusInventoryGridOrCounterComponents() {
		for (Class<?> configClass : new Class<?>[]{HudConfig.class, ArmorConfig.class, EffectsConfig.class}) {
			for (Field f : configClass.getDeclaredFields()) {
				String name = f.getName().toLowerCase(Locale.ROOT);
				assertFalse(
						name.contains("inventory") || name.contains("arrow") || name.contains("freeslot"),
						"Unexpected inventory HUD field in " + configClass.getSimpleName() + ": " + f.getName()
				);
			}
		}
	}
}
