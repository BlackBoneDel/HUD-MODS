package dev.customizablehud.format;

import dev.customizablehud.config.EffectColorOverride;
import dev.customizablehud.config.EffectSortMode;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.EffectsVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.LevelFormat;
import dev.customizablehud.config.PotionHudMode;
import dev.customizablehud.config.PotionMiniDisplayMode;
import dev.customizablehud.config.VanillaEffectsMode;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EffectsFormattingAndOrderTest {
	@Test
	void formatsRomanNumeralsAndHighLevelsSafely() {
		assertEquals("I", RomanNumerals.format(1));
		assertEquals("IV", RomanNumerals.format(4));
		assertEquals("IX", RomanNumerals.format(9));
		assertEquals("XLII", RomanNumerals.format(42));
		assertEquals("CCLV", RomanNumerals.format(255));
		assertEquals("MMMCMXCIX", RomanNumerals.format(3999));

		assertEquals("4000", RomanNumerals.format(4000));
		assertEquals("0", RomanNumerals.format(0));
		assertEquals("-3", RomanNumerals.format(-3));
	}

	@Test
	void formatsAmplifierWithHideLevelOneAndArabicOption() {
		assertEquals("", RomanNumerals.formatLevel(0, LevelFormat.ROMAN, true));
		assertEquals("I", RomanNumerals.formatLevel(0, LevelFormat.ROMAN, false));
		assertEquals("II", RomanNumerals.formatLevel(1, LevelFormat.ROMAN, true));
		assertEquals("", RomanNumerals.formatLevel(0, LevelFormat.ARABIC, true));
		assertEquals("1", RomanNumerals.formatLevel(0, LevelFormat.ARABIC, false));
		assertEquals("5", RomanNumerals.formatLevel(4, LevelFormat.ARABIC, true));
	}

	@Test
	void formatsDurationsIncludingHoursInfiniteVariableTpsAndSuspendedTicks() {
		assertEquals("0:00", DurationFormatter.formatTicks(0, 20.0f, false));
		assertEquals("0:09", DurationFormatter.formatTicks(185, 20.0f, false));
		assertEquals("2:34", DurationFormatter.formatTicks(3080, 20.0f, false));
		assertEquals("1:02:09", DurationFormatter.formatTicks(74580, 20.0f, false));
		assertEquals("∞", DurationFormatter.formatTicks(-1, 20.0f, false));
		assertEquals("∞", DurationFormatter.formatTicks(500, 20.0f, true));

		// At 10 TPS, 100 ticks equals 10 seconds -> 0:10
		assertEquals("0:10", DurationFormatter.formatTicks(100, 10.0f, false));
		// Invalid TPS safely falls back to 20 TPS
		assertEquals("0:05", DurationFormatter.formatTicks(100, 0.0f, false));

		// Suspended/frozen ticks never present finite timer as an active exact countdown
		assertEquals("~0:42", DurationFormatter.formatTicks(840, 20.0f, false, true));
		assertEquals("∞", DurationFormatter.formatTicks(-1, 20.0f, true, true));
	}

	@Test
	void tracksRealRemainingTickFractionsWithoutFabricatingStaticMaxDurations() {
		EffectDurationTracker tracker = new EffectDurationTracker();

		EffectSnapshot start = new EffectSnapshot(
				"minecraft:regeneration", "Regeneration", EffectCategoryKind.BENEFICIAL, 0, 600, false, false, true, true, 0xCD5CAB
		);
		EffectSnapshot infinite = new EffectSnapshot(
				"minecraft:night_vision", "Night Vision", EffectCategoryKind.BENEFICIAL, 0, -1, true, false, false, true, 0x1F1FA1
		);
		tracker.syncAndObserve(List.of(start, infinite));
		assertEquals(1.0f, tracker.remainingFraction(start), 0.0001f);
		assertEquals(1.0f, tracker.remainingFraction(infinite), 0.0001f);

		// Halfway through observed duration -> 0.50f
		EffectSnapshot half = new EffectSnapshot(
				"minecraft:regeneration", "Regeneration", EffectCategoryKind.BENEFICIAL, 0, 300, false, false, true, true, 0xCD5CAB
		);
		tracker.syncAndObserve(List.of(half, infinite));
		assertEquals(600, tracker.observedPeakTicks(half));
		assertEquals(0.5f, tracker.remainingFraction(half), 0.0001f);

		// Re-applying potion raises observed peak cleanly
		EffectSnapshot refreshed = new EffectSnapshot(
				"minecraft:regeneration", "Regeneration", EffectCategoryKind.BENEFICIAL, 0, 900, false, false, true, true, 0xCD5CAB
		);
		tracker.syncAndObserve(List.of(refreshed));
		assertEquals(900, tracker.observedPeakTicks(refreshed));
		assertEquals(1.0f, tracker.remainingFraction(refreshed), 0.0001f);

		// Expired effect purged when no longer active
		tracker.syncAndObserve(List.of());
		EffectSnapshot expired = new EffectSnapshot(
				"minecraft:regeneration", "Regeneration", EffectCategoryKind.BENEFICIAL, 0, 0, false, false, true, true, 0xCD5CAB
		);
		assertEquals(0.0f, tracker.remainingFraction(expired), 0.0001f);
	}

	@Test
	void filtersParticlesHiddenAndSortsByCategoryAndCriteria() {
		EffectSnapshot speed = new EffectSnapshot("minecraft:speed", "Speed", EffectCategoryKind.BENEFICIAL, 1, 1200, false, false, true, true, 0x33EBFF);
		EffectSnapshot nightVision = new EffectSnapshot("minecraft:night_vision", "Night Vision", EffectCategoryKind.BENEFICIAL, 0, -1, true, false, false, true, 0x1F1FA1);
		EffectSnapshot glowing = new EffectSnapshot("minecraft:glowing", "Glowing", EffectCategoryKind.NEUTRAL, 0, 400, false, false, true, true, 0x94A061);
		EffectSnapshot weakness = new EffectSnapshot("minecraft:weakness", "Weakness", EffectCategoryKind.HARMFUL, 0, 200, false, false, true, true, 0x484D48);

		List<EffectSnapshot> input = List.of(weakness, nightVision, glowing, speed);

		EffectsConfig config = new EffectsConfig();
		config.showEffectsWithoutParticles = true;
		config.groupByEffectCategory = true;
		config.sortMode = EffectSortMode.CATEGORY_THEN_NAME;

		List<EffectSnapshot> sorted = EffectOrderer.prepare(input, config);
		assertEquals(List.of(nightVision, speed, glowing, weakness), sorted);

		config.showEffectsWithoutParticles = false;
		List<EffectSnapshot> filtered = EffectOrderer.prepare(input, config);
		assertEquals(List.of(speed, glowing, weakness), filtered);

		config.showEffectsWithoutParticles = true;
		config.groupByEffectCategory = false;
		config.sortMode = EffectSortMode.DURATION_ASC;
		List<EffectSnapshot> durationAsc = EffectOrderer.prepare(input, config);
		assertEquals(List.of(weakness, glowing, speed, nightVision), durationAsc);

		config.sortMode = EffectSortMode.DURATION_DESC;
		List<EffectSnapshot> durationDesc = EffectOrderer.prepare(input, config);
		assertEquals(List.of(nightVision, speed, glowing, weakness), durationDesc);

		config.sortMode = EffectSortMode.NAME_ASC;
		List<EffectSnapshot> nameAsc = EffectOrderer.prepare(input, config);
		assertEquals(List.of(glowing, nightVision, speed, weakness), nameAsc);

		config.sortMode = EffectSortMode.NONE;
		List<EffectSnapshot> gameOrder = EffectOrderer.prepare(input, config);
		assertEquals(input, gameOrder);
	}

	@Test
	void computesDurationColorsByRemainingSecondsAndInfiniteState() {
		assertEquals(0xFFD8DEE9, DurationFormatter.durationColorArgb(100, 20.0f, false, false));
		assertEquals(0xFF7CE8FF, DurationFormatter.durationColorArgb(-1, 20.0f, true, true));
		assertEquals(0xFFFF6B6B, DurationFormatter.durationColorArgb(200, 20.0f, false, true));
		assertEquals(0xFFFFD166, DurationFormatter.durationColorArgb(400, 20.0f, false, true));
		assertEquals(0xFFE6EDF3, DurationFormatter.durationColorArgb(800, 20.0f, false, true));
	}

	@Test
	void usesCenterRightTransparentDefaultsNativeColorsAndPreservesUntintedIcons() {
		EffectsConfig config = new EffectsConfig();
		assertEquals(HudAnchor.CENTER_RIGHT, config.anchor);
		assertFalse(config.showEffectBackground);
		assertTrue(config.showEffectBorder);
		assertTrue(config.hideLevelOne);
		assertEquals(LevelFormat.ROMAN, config.levelFormat);
		assertEquals(EffectsVisualStyle.MINECRAFT_COMPACT, config.style);
		assertEquals(PotionHudMode.NORMAL, config.potionHudMode);
		assertEquals(PotionMiniDisplayMode.TIMER_TEXT, config.potionMiniDisplay);
		assertEquals(65, config.backgroundOpacityPercent);
		assertTrue(config.customEffectColors.isEmpty());
		assertEquals(VanillaEffectsMode.SHOW_BOTH, config.vanillaEffectsMode);

		// All 4 combinations of independent background/border toggles + PotionHUD options copy cleanly
		for (boolean bg : new boolean[]{false, true}) {
			for (boolean border : new boolean[]{false, true}) {
				config.showEffectBackground = bg;
				config.showEffectBorder = border;
				config.style = EffectsVisualStyle.POTION_HUD;
				config.potionHudMode = PotionHudMode.MINI;
				config.potionMiniDisplay = PotionMiniDisplayMode.DURATION_BAR;
				config.backgroundOpacityPercent = 42;
				EffectsConfig copy = config.copy();
				assertEquals(bg, copy.showEffectBackground);
				assertEquals(border, copy.showEffectBorder);
				assertEquals(EffectsVisualStyle.POTION_HUD, copy.style);
				assertEquals(PotionHudMode.MINI, copy.potionHudMode);
				assertEquals(PotionMiniDisplayMode.DURATION_BAR, copy.potionMiniDisplay);
				assertEquals(42, copy.backgroundOpacityPercent);
			}
		}

		EffectSnapshot speed = new EffectSnapshot(
				"minecraft:speed", "Speed", EffectCategoryKind.BENEFICIAL, 1, 800, false, false, true, true, 0x33EBFF
		);
		EffectSnapshot regen = new EffectSnapshot(
				"minecraft:regeneration", "Regeneration", EffectCategoryKind.BENEFICIAL, 0, 120, false, false, true, true, 0xCD5CAB
		);
		EffectSnapshot strength = new EffectSnapshot(
				"minecraft:strength", "Strength", EffectCategoryKind.BENEFICIAL, 0, 800, false, false, true, true, 0xFFC700
		);
		EffectSnapshot poison = new EffectSnapshot(
				"minecraft:poison", "Poison", EffectCategoryKind.HARMFUL, 0, 400, false, false, true, true, 0x87A363
		);
		EffectSnapshot modded = new EffectSnapshot(
				"examplemod:mana_regen", "Mana Regen", EffectCategoryKind.BENEFICIAL, 1, 800, false, false, true, true, 0x3366FF
		);
		EffectSnapshot untinted = new EffectSnapshot(
				"examplemod:void_aura", "Void Aura", EffectCategoryKind.NEUTRAL, 0, 800, false, false, true, true, 0x000000
		);
		EffectSnapshot darkNative = new EffectSnapshot(
				"minecraft:blindness", "Blindness", EffectCategoryKind.HARMFUL, 0, 400, false, false, true, true, 0x1F1F23
		);

		assertEquals(0xFF33EBFF, EffectsColorResolver.titleColorArgb(speed, config));
		assertEquals(0xFF33EBFF, EffectsColorResolver.durationColorArgb(speed, config, 20.0f));
		assertEquals(0xFFCD5CAB, EffectsColorResolver.titleColorArgb(regen, config));
		assertEquals(0xFFFFC700, EffectsColorResolver.titleColorArgb(strength, config));
		assertEquals(0xFF87A363, EffectsColorResolver.titleColorArgb(poison, config));
		assertEquals(0xFF3366FF, EffectsColorResolver.titleColorArgb(modded, config));
		assertNotEquals(EffectsColorResolver.titleColorArgb(speed, config), EffectsColorResolver.titleColorArgb(regen, config));

		assertFalse(EffectsColorResolver.hasNativeColor(untinted.colorRgb()));
		assertEquals(0xFF000000 | EffectsColorResolver.UNREADABLE_FALLBACK_RGB, EffectsColorResolver.titleColorArgb(untinted, config));

		int blindArgb = EffectsColorResolver.titleColorArgb(darkNative, config);
		int r = (blindArgb >> 16) & 0xFF;
		int g = (blindArgb >> 8) & 0xFF;
		int b = blindArgb & 0xFF;
		float lum = (0.2126f * r + 0.7152f * g + 0.0722f * b) / 255.0f;
		assertTrue(lum >= EffectsColorResolver.MIN_READABLE_LUMINANCE);

		config.customEffectColors.put("minecraft:regeneration", new EffectColorOverride(true, "#FF66B2"));
		config.customEffectColors.put("examplemod:mana_regen", new EffectColorOverride(false, "#55FFFF"));

		assertEquals(OptionalInt.of(0xFF66B2), EffectsColorResolver.resolveCustomRgb("minecraft:regeneration", config));
		assertEquals(0xFFFF66B2, EffectsColorResolver.titleColorArgb(regen, config));
		assertEquals(0xFFFF66B2, EffectsColorResolver.durationColorArgb(regen, config, 20.0f));

		assertEquals(OptionalInt.empty(), EffectsColorResolver.resolveCustomRgb("examplemod:mana_regen", config));
		assertEquals(0xFF3366FF, EffectsColorResolver.titleColorArgb(modded, config));
		config.colorDurationByRemaining = true;
		assertEquals(0xFFE6EDF3, EffectsColorResolver.durationColorArgb(modded, config, 20.0f));
		assertEquals(0xFFFF66B2, EffectsColorResolver.durationColorArgb(regen, config, 20.0f));

		assertEquals(0xFFFFFFFF, EffectsColorResolver.iconTintArgb(1.0f));
	}
}
