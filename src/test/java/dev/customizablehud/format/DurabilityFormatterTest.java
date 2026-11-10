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
	void formatsRemainingPointsAndPercentageSimultaneously() {
		ArmorConfig config = new ArmorConfig();
		ArmorSlotSnapshot chest = new ArmorSlotSnapshot(
				ArmorSlotKind.CHESTPLATE, true, "Diamond Chestplate", true, false, 203, 50, 1, true, 0.75f
		);

		assertEquals(153, chest.remaining());
		assertEquals(75, chest.remainingPercent());
		assertEquals("153 · 75%", DurabilityFormatter.format(chest, config));

		config.showMaxDurability = true;
		assertEquals("153/203 · 75%", DurabilityFormatter.format(chest, config));
	}

	@Test
	void formatsAllDisplayModesCleanly() {
		ArmorConfig config = new ArmorConfig();
		ArmorSlotSnapshot helmet = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Iron Helmet", true, false, 100, 40, 1, true, 0.60f
		);

		config.durabilityDisplayMode = DurabilityDisplayMode.PERCENT_ONLY;
		assertEquals("60%", DurabilityFormatter.format(helmet, config));

		config.durabilityDisplayMode = DurabilityDisplayMode.POINTS_ONLY;
		config.showMaxDurability = false;
		assertEquals("60", DurabilityFormatter.format(helmet, config));

		config.showMaxDurability = true;
		assertEquals("60/100", DurabilityFormatter.format(helmet, config));

		config.durabilityDisplayMode = DurabilityDisplayMode.NONE;
		assertEquals("", DurabilityFormatter.format(helmet, config));
	}

	@Test
	void handlesEmptyUnbreakableAndNonDamageableItemsSafely() {
		ArmorConfig config = new ArmorConfig();

		ArmorSlotSnapshot empty = ArmorSlotSnapshot.empty(ArmorSlotKind.BOOTS);
		assertEquals("", DurabilityFormatter.format(empty, config));
		assertFalse(DurabilityFormatter.isLowDurability(empty, config));

		ArmorSlotSnapshot unbreakable = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Unbreakable Crown", false, true, 0, 0, 1, false, 1.0f
		);
		assertEquals("∞", DurabilityFormatter.format(unbreakable, config));
		assertEquals(100, unbreakable.remainingPercent());
		assertFalse(DurabilityFormatter.isLowDurability(unbreakable, config));

		ArmorSlotSnapshot pumpkin = new ArmorSlotSnapshot(
				ArmorSlotKind.HELMET, true, "Carved Pumpkin", false, false, 0, 0, 1, false, 0.0f
		);
		assertEquals("", DurabilityFormatter.format(pumpkin, config));
		assertFalse(DurabilityFormatter.isLowDurability(pumpkin, config));
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
	void evaluatesLowDurabilityThresholdAndColors() {
		ArmorConfig config = new ArmorConfig();
		config.colorByDurability = true;
		config.lowDurabilityWarning = true;
		config.lowDurabilityThresholdPercent = 15;

		ArmorSlotSnapshot lowItem = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 88, 1, true, 0.12f
		);
		assertTrue(DurabilityFormatter.isLowDurability(lowItem, config));
		assertEquals(0xFFFF5555, DurabilityFormatter.textColorArgb(lowItem, config, 0L));
		assertEquals(0xFFFFB84D, DurabilityFormatter.textColorArgb(lowItem, config, 350L));

		ArmorSlotSnapshot healthyItem = new ArmorSlotSnapshot(
				ArmorSlotKind.BOOTS, true, "Boots", true, false, 100, 10, 1, true, 0.90f
		);
		assertFalse(DurabilityFormatter.isLowDurability(healthyItem, config));
		assertEquals(0xFF6EEB83, DurabilityFormatter.textColorArgb(healthyItem, config, 0L));
	}
}
