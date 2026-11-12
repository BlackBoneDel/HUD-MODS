package dev.customizablehud.format;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.DurabilityDisplayMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurabilityFormatterTest {
	@Test
	void defaultsToPointsOnlyWithoutMaxAndAlertOnWithoutBlink() {
		assertEquals(3, DurabilityDisplayMode.values().length);

		ArmorConfig config = new ArmorConfig();
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, config.durabilityDisplayMode);
		assertFalse(config.showMaxDurability);
		assertFalse(config.showItemName);
		assertFalse(config.showHandItemName);
		assertEquals(1.0f, config.numberScale);
		assertTrue(config.showLowDurabilityAlert);
		assertEquals(10, config.lowDurabilityAlertThresholdPercent);
		assertFalse(config.lowDurabilityWarning);
		assertEquals(95, config.veryHighThresholdPercent);
		assertEquals(60, config.warningThresholdPercent);
		assertEquals(20, config.criticalThresholdPercent);
		assertEquals("#FFFFFF", config.veryHighColorHex);
		assertEquals("#55FF55", config.healthyColorHex);
		assertEquals("#FFFF55", config.warningColorHex);
		assertEquals("#FF5555", config.criticalColorHex);

		ArmorSlotSnapshot chest = new ArmorSlotSnapshot(
				ArmorSlotKind.CHESTPLATE, true, "Diamond Chestplate", true, false, 203, 50, 1, true, 0.75f
		);

		assertEquals(153, chest.remaining());
		assertEquals(75, chest.remainingPercent());
		assertEquals("153", DurabilityFormatter.format(chest, config));

		config.showMaxDurability = true;
		assertEquals("153/203", DurabilityFormatter.format(chest, config));

		config.durabilityDisplayMode = DurabilityDisplayMode.PERCENT_ONLY;
		assertEquals("75%", DurabilityFormatter.format(chest, config));

		config.durabilityDisplayMode = DurabilityDisplayMode.NONE;
		assertEquals("", DurabilityFormatter.format(chest, config));
	}

	@Test
	void handlesEmptyUnbreakableAndNonDamageableItemsSafely() {
		ArmorConfig config = new ArmorConfig();

		ArmorSlotSnapshot empty = ArmorSlotSnapshot.empty(ArmorSlotKind.BOOTS);
		assertFalse(empty.hasValidDurability());
		assertEquals(0, empty.remainingPercent());
		assertEquals("", DurabilityFormatter.format(empty, config));
		assertEquals(DurabilityFormatter.DurabilityLevel.NONE, DurabilityFormatter.classifyLevel(empty, config));
		assertFalse(DurabilityFormatter.isLowDurability(empty, config));
		assertFalse(DurabilityFormatter.isAlertActive(empty, config));

		ArmorSlotSnapshot unbreakable = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Unbreakable Crown", false, true, 0, 0, 1, false, 1.0f
		);
		assertFalse(unbreakable.hasValidDurability());
		assertEquals("∞", DurabilityFormatter.format(unbreakable, config));
		assertFalse(DurabilityFormatter.isLowDurability(unbreakable, config));
		assertFalse(DurabilityFormatter.isAlertActive(unbreakable, config));

		ArmorSlotSnapshot pumpkin = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Carved Pumpkin", false, false, 0, 0, 1, false, 0.0f
		);
		assertFalse(pumpkin.hasValidDurability());
		assertEquals(0, pumpkin.remainingPercent());
		assertEquals("", DurabilityFormatter.format(pumpkin, config));
		assertFalse(DurabilityFormatter.isLowDurability(pumpkin, config));
		assertFalse(DurabilityFormatter.isAlertActive(pumpkin, config));

		ArmorSlotSnapshot brokenZeroMax = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Invalid Item", true, false, 0, 0, 1, false, 0.0f
		);
		assertFalse(brokenZeroMax.hasValidDurability());
		assertEquals("", DurabilityFormatter.format(brokenZeroMax, config));
		assertFalse(DurabilityFormatter.isAlertActive(brokenZeroMax, config));
	}

	@Test
	void clampsCorruptNegativeOrOverflowDamageValues() {
		ArmorSlotSnapshot negativeDamage = new ArmorSlotSnapshot(
				ArmorSlotKind.LEGGINGS, true, "Modded Greaves", true, false, 100, -15, 1, true, 1.0f
		);
		assertEquals(100, negativeDamage.remaining());
		assertEquals(100, negativeDamage.remainingPercent());

		ArmorSlotSnapshot overDamaged = new ArmorSlotSnapshot(
				ArmorSlotKind.LEGGINGS, true, "Broken Greaves", true, false, 100, 140, 1, true, 0.0f
		);
		assertEquals(0, overDamaged.remaining());
		assertEquals(0, overDamaged.remainingPercent());
	}

	@Test
	void evaluatesExactUnroundedFourTierThresholdsAndMatchesTextColors() {
		ArmorConfig config = new ArmorConfig();
		config.colorByDurability = true;

		// 100% -> White (#FFFFFF)
		ArmorSlotSnapshot at100 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 100, 0, 1, false, 1.0f
		);
		assertEquals(DurabilityFormatter.DurabilityLevel.VERY_HIGH, DurabilityFormatter.classifyLevel(at100, config));
		assertEquals(0xFFFFFFFF, DurabilityFormatter.textColorArgb(at100, config, 0L));
		assertEquals(0xFFFFFFFF, DurabilityFormatter.barColorArgb(at100, config));

		// 95.2% (476/500, visually rounds to 95%) -> still >95% so White (#FFFFFF)
		ArmorSlotSnapshot at95Point2 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 500, 24, 1, true, 0.952f
		);
		assertEquals(95, at95Point2.remainingPercent());
		assertEquals(DurabilityFormatter.DurabilityLevel.VERY_HIGH, DurabilityFormatter.classifyLevel(at95Point2, config));
		assertEquals(0xFFFFFFFF, DurabilityFormatter.textColorArgb(at95Point2, config, 0L));

		// Exactly 95.0% -> <=95% and >60% so Green (#55FF55)
		ArmorSlotSnapshot at95 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 100, 5, 1, true, 0.95f
		);
		assertEquals(DurabilityFormatter.DurabilityLevel.HEALTHY, DurabilityFormatter.classifyLevel(at95, config));
		assertEquals(0xFF55FF55, DurabilityFormatter.textColorArgb(at95, config, 0L));
		assertEquals(0xFF55FF55, DurabilityFormatter.barColorArgb(at95, config));

		// 60.4% (302/500, visually rounds to 60%) -> still >60% so Green (#55FF55)
		ArmorSlotSnapshot at60Point4 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 500, 198, 1, true, 0.604f
		);
		assertEquals(60, at60Point4.remainingPercent());
		assertEquals(DurabilityFormatter.DurabilityLevel.HEALTHY, DurabilityFormatter.classifyLevel(at60Point4, config));

		// Exactly 60.0% -> <=60% and >20% so Yellow (#FFFF55)
		ArmorSlotSnapshot at60 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 100, 40, 1, true, 0.60f
		);
		assertEquals(DurabilityFormatter.DurabilityLevel.WARNING, DurabilityFormatter.classifyLevel(at60, config));
		assertEquals(0xFFFFFF55, DurabilityFormatter.textColorArgb(at60, config, 0L));
		assertEquals(0xFFFFFF55, DurabilityFormatter.barColorArgb(at60, config));

		// 20.4% (102/500, visually rounds to 20%) -> still >20% so Yellow (#FFFF55)
		ArmorSlotSnapshot at20Point4 = new ArmorSlotSnapshot(
				ArmorSlotKind.CHESTPLATE, true, "Chest", true, false, 500, 398, 1, true, 0.204f
		);
		assertEquals(20, at20Point4.remainingPercent());
		assertEquals(DurabilityFormatter.DurabilityLevel.WARNING, DurabilityFormatter.classifyLevel(at20Point4, config));

		// Exactly 20.0% -> <=20% so Red (#FF5555)
		ArmorSlotSnapshot at20 = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 80, 1, true, 0.20f
		);
		assertTrue(DurabilityFormatter.isLowDurability(at20, config));
		assertEquals(DurabilityFormatter.DurabilityLevel.CRITICAL, DurabilityFormatter.classifyLevel(at20, config));
		assertEquals(0xFFFF5555, DurabilityFormatter.textColorArgb(at20, config, 0L));
		assertEquals(0xFFFF5555, DurabilityFormatter.barColorArgb(at20, config));

		// 0% -> Red (#FF5555)
		ArmorSlotSnapshot at0 = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 100, 1, true, 0.0f
		);
		assertEquals(DurabilityFormatter.DurabilityLevel.CRITICAL, DurabilityFormatter.classifyLevel(at0, config));
		assertEquals(0xFFFF5555, DurabilityFormatter.barColorArgb(at0, config));
	}

	@Test
	void showsLowDurabilityAlertOnlyAtThresholdAndComputesOptionalNonHidingBlink() {
		ArmorConfig config = new ArmorConfig();
		config.colorByDurability = false;
		config.durabilityDisplayMode = DurabilityDisplayMode.NONE;
		config.showLowDurabilityAlert = true;
		config.lowDurabilityAlertThresholdPercent = 10;
		config.lowDurabilityWarning = false;

		ArmorSlotSnapshot healthy = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 100, 20, 1, true, 0.80f
		);
		ArmorSlotSnapshot at10Point2 = new ArmorSlotSnapshot(
				ArmorSlotKind.CHESTPLATE, true, "Chest", true, false, 500, 449, 1, true, 0.102f
		);
		ArmorSlotSnapshot at10 = new ArmorSlotSnapshot(
				ArmorSlotKind.LEGGINGS, true, "Leggings", true, false, 100, 90, 1, true, 0.10f
		);
		ArmorSlotSnapshot at5 = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 95, 1, true, 0.05f
		);

		// Alert appears ONLY on <=10% pieces, independent of number mode or colorByDurability
		assertFalse(DurabilityFormatter.isAlertActive(healthy, config));
		assertFalse(DurabilityFormatter.isAlertActive(at10Point2, config));
		assertTrue(DurabilityFormatter.isAlertActive(at10, config));
		assertTrue(DurabilityFormatter.isAlertActive(at5, config));
		// Blink animation is OFF by default
		assertFalse(DurabilityFormatter.computeWarningBlink(at5, config, 0L).active());

		// Raising threshold in settings catches more damaged pieces
		config.lowDurabilityAlertThresholdPercent = 25;
		assertTrue(DurabilityFormatter.isAlertActive(healthy, config) == false);
		assertTrue(DurabilityFormatter.isAlertActive(at10Point2, config));

		// Optional blink only modulates the badge alpha cleanly, never hiding text/HUD
		config.lowDurabilityWarning = true;
		config.colorByDurability = true;
		config.criticalColorHex = "#FF00AA";
		config.alertColorHex = "#FF2222";
		assertEquals(0xFFFF2222, DurabilityFormatter.alertColorArgb(config));
		assertEquals(0xFFFF00AA, DurabilityFormatter.textColorArgb(at5, config, 0L));
		assertEquals(0xFFFF00AA, DurabilityFormatter.textColorArgb(at5, config, 350L));

		DurabilityFormatter.WarningBlinkState phaseHigh = DurabilityFormatter.computeWarningBlink(at5, config, 0L);
		DurabilityFormatter.WarningBlinkState phaseLow = DurabilityFormatter.computeWarningBlink(at5, config, 350L);
		assertTrue(phaseHigh.active());
		assertTrue(phaseHigh.highPhase());
		assertTrue(phaseLow.active());
		assertFalse(phaseLow.highPhase());
		assertTrue(phaseHigh.indicatorAlpha() >= DurabilityFormatter.BLINK_MIN_ALPHA);
		assertTrue(phaseLow.indicatorAlpha() >= DurabilityFormatter.BLINK_MIN_ALPHA);
		assertTrue(phaseLow.indicatorAlpha() <= DurabilityFormatter.BLINK_MAX_ALPHA);

		DurabilityFormatter.WarningBlinkState minTime = DurabilityFormatter.computeWarningBlink(at5, config, Long.MIN_VALUE);
		assertTrue(minTime.active());
		assertTrue(minTime.indicatorAlpha() >= DurabilityFormatter.BLINK_MIN_ALPHA);

		// Disabling alert stops both alert badge and blink
		config.showLowDurabilityAlert = false;
		assertFalse(DurabilityFormatter.isAlertActive(at5, config));
		assertFalse(DurabilityFormatter.computeWarningBlink(at5, config, 0L).active());
	}

	@Test
	void separatesArmorAndHandNamesAndTruncatesLongCustomNamesCleanly() {
		ArmorConfig config = new ArmorConfig();
		assertFalse(config.showItemName);
		assertFalse(config.showHandItemName);

		ArmorSlotSnapshot helmet = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Netherite Helmet of Invincibility", true, false, 407, 10, 1, true, 0.97f
		);
		ArmorSlotSnapshot sword = new ArmorSlotSnapshot(
				ArmorSlotKind.MAIN_HAND, true, "Excalibur Diamond Blade +10", true, false, 1561, 100, 1, true, 0.93f
		);

		assertEquals("", DurabilityFormatter.formatItemName(helmet, config));
		assertEquals("", DurabilityFormatter.formatItemName(sword, config));

		config.showItemName = true;
		String armorName = DurabilityFormatter.formatItemName(helmet, config);
		assertTrue(armorName.endsWith("…"));
		assertTrue(armorName.length() <= DurabilityFormatter.MAX_ITEM_NAME_CHARS);
		assertEquals("", DurabilityFormatter.formatItemName(sword, config));

		config.showHandItemName = true;
		String handName = DurabilityFormatter.formatItemName(sword, config);
		assertTrue(handName.endsWith("…"));
		assertTrue(handName.length() <= DurabilityFormatter.MAX_ITEM_NAME_CHARS);
	}
}
