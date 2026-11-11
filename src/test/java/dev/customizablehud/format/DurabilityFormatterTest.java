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
	void defaultsToPointsOnlyWithoutMaxAndNeverCombinesPointsAndPercent() {
		assertEquals(3, DurabilityDisplayMode.values().length);

		ArmorConfig config = new ArmorConfig();
		assertEquals(DurabilityDisplayMode.POINTS_ONLY, config.durabilityDisplayMode);
		assertFalse(config.showMaxDurability);
		assertFalse(config.showItemName);
		assertFalse(config.showHandItemName);
		assertFalse(config.lowDurabilityWarning);

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

		ArmorSlotSnapshot unbreakable = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Unbreakable Crown", false, true, 0, 0, 1, false, 1.0f
		);
		assertFalse(unbreakable.hasValidDurability());
		assertEquals("∞", DurabilityFormatter.format(unbreakable, config));
		assertFalse(DurabilityFormatter.isLowDurability(unbreakable, config));

		ArmorSlotSnapshot pumpkin = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Carved Pumpkin", false, false, 0, 0, 1, false, 0.0f
		);
		assertFalse(pumpkin.hasValidDurability());
		assertEquals(0, pumpkin.remainingPercent());
		assertEquals("", DurabilityFormatter.format(pumpkin, config));
		assertFalse(DurabilityFormatter.isLowDurability(pumpkin, config));

		ArmorSlotSnapshot brokenZeroMax = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Invalid Item", true, false, 0, 0, 1, false, 0.0f
		);
		assertFalse(brokenZeroMax.hasValidDurability());
		assertEquals("", DurabilityFormatter.format(brokenZeroMax, config));
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
	void evaluatesExactUnroundedThreeTierThresholdsAndMatchesTextWithBar() {
		ArmorConfig config = new ArmorConfig();
		config.colorByDurability = true;
		config.warningThresholdPercent = 60;
		config.criticalThresholdPercent = 20;
		config.lowDurabilityWarning = false;

		ArmorSlotSnapshot at61 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 100, 39, 1, true, 0.61f
		);
		assertEquals(DurabilityFormatter.DurabilityLevel.HEALTHY, DurabilityFormatter.classifyLevel(at61, config));
		assertEquals(0xFF6EEB83, DurabilityFormatter.textColorArgb(at61, config, 0L));
		assertEquals(0xFF6EEB83, DurabilityFormatter.barColorArgb(at61, config));

		// 302/500 = 60.4% -> stays Healthy
		ArmorSlotSnapshot at60Point4 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 500, 198, 1, true, 0.604f
		);
		assertEquals(60, at60Point4.remainingPercent());
		assertEquals(DurabilityFormatter.DurabilityLevel.HEALTHY, DurabilityFormatter.classifyLevel(at60Point4, config));

		// Exactly 60.0% -> Warning
		ArmorSlotSnapshot at60 = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Helmet", true, false, 100, 40, 1, true, 0.60f
		);
		assertEquals(DurabilityFormatter.DurabilityLevel.WARNING, DurabilityFormatter.classifyLevel(at60, config));
		assertEquals(0xFFFFE066, DurabilityFormatter.textColorArgb(at60, config, 0L));
		assertEquals(0xFFFFE066, DurabilityFormatter.barColorArgb(at60, config));

		// 102/500 = 20.4% -> stays Warning
		ArmorSlotSnapshot at20Point4 = new ArmorSlotSnapshot(
				ArmorSlotKind.CHESTPLATE, true, "Chest", true, false, 500, 398, 1, true, 0.204f
		);
		assertEquals(20, at20Point4.remainingPercent());
		assertEquals(DurabilityFormatter.DurabilityLevel.WARNING, DurabilityFormatter.classifyLevel(at20Point4, config));

		// Exactly 20.0% -> Critical
		ArmorSlotSnapshot at20 = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 80, 1, true, 0.20f
		);
		assertTrue(DurabilityFormatter.isLowDurability(at20, config));
		assertEquals(DurabilityFormatter.DurabilityLevel.CRITICAL, DurabilityFormatter.classifyLevel(at20, config));
		assertEquals(0xFFFF5555, DurabilityFormatter.textColorArgb(at20, config, 0L));
		assertEquals(0xFFFF5555, DurabilityFormatter.barColorArgb(at20, config));

		// 0% -> Critical
		ArmorSlotSnapshot at0 = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 100, 1, true, 0.0f
		);
		assertEquals(DurabilityFormatter.DurabilityLevel.CRITICAL, DurabilityFormatter.classifyLevel(at0, config));
		assertEquals(0xFFFF5555, DurabilityFormatter.barColorArgb(at0, config));
	}

	@Test
	void computesBoundedNonHidingWarningBlinkOnlyForCriticalSlotsAndHandlesCorruptTimes() {
		ArmorConfig config = new ArmorConfig();
		config.colorByDurability = true;
		config.warningThresholdPercent = 75;
		config.criticalThresholdPercent = 25;
		config.healthyColorHex = "#55FFFF";
		config.warningColorHex = "#FFA500";
		config.criticalColorHex = "#FF00AA";
		config.lowDurabilityWarning = true;

		ArmorSlotSnapshot warningItem = new ArmorSlotSnapshot(
				ArmorSlotKind.LEGGINGS, true, "Leggings", true, false, 100, 30, 1, true, 0.70f
		);
		assertFalse(DurabilityFormatter.computeWarningBlink(warningItem, config, 0L).active());
		assertEquals(0xFFFFA500, DurabilityFormatter.textColorArgb(warningItem, config, 0L));
		assertEquals(0xFFFFA500, DurabilityFormatter.barColorArgb(warningItem, config));

		ArmorSlotSnapshot criticalItem = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 80, 1, true, 0.20f
		);
		// Text color remains solid & legible while only the warning indicator blinks with bounded alpha
		assertEquals(0xFFFF00AA, DurabilityFormatter.barColorArgb(criticalItem, config));
		assertEquals(0xFFFF00AA, DurabilityFormatter.textColorArgb(criticalItem, config, 0L));
		assertEquals(0xFFFF00AA, DurabilityFormatter.textColorArgb(criticalItem, config, 350L));

		DurabilityFormatter.WarningBlinkState phaseHigh = DurabilityFormatter.computeWarningBlink(criticalItem, config, 0L);
		DurabilityFormatter.WarningBlinkState phaseLow = DurabilityFormatter.computeWarningBlink(criticalItem, config, 350L);
		assertTrue(phaseHigh.active());
		assertTrue(phaseHigh.highPhase());
		assertTrue(phaseLow.active());
		assertFalse(phaseLow.highPhase());
		assertTrue(phaseHigh.indicatorAlpha() >= DurabilityFormatter.BLINK_MIN_ALPHA);
		assertTrue(phaseLow.indicatorAlpha() >= DurabilityFormatter.BLINK_MIN_ALPHA);
		assertTrue(phaseLow.indicatorAlpha() <= DurabilityFormatter.BLINK_MAX_ALPHA);

		// Negative or extreme Long.MIN_VALUE timestamps never crash or drop alpha to zero
		DurabilityFormatter.WarningBlinkState minTime = DurabilityFormatter.computeWarningBlink(criticalItem, config, Long.MIN_VALUE);
		assertTrue(minTime.active());
		assertTrue(minTime.indicatorAlpha() >= DurabilityFormatter.BLINK_MIN_ALPHA);

		// Empty / non-damageable / unbreakable slots never blink
		assertFalse(DurabilityFormatter.computeWarningBlink(ArmorSlotSnapshot.empty(ArmorSlotKind.HELMET), config, 0L).active());
		assertFalse(DurabilityFormatter.computeWarningBlink(
				new ArmorSlotSnapshot(ArmorSlotKind.HELMET, true, "Pumpkin", false, false, 0, 0, 1, false, 0.0f),
				config,
				0L
		).active());

		// Disabling stops blink immediately
		config.lowDurabilityWarning = false;
		assertFalse(DurabilityFormatter.computeWarningBlink(criticalItem, config, 0L).active());
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
