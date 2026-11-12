package dev.customizablehud.format;

import dev.customizablehud.config.LevelFormat;

public final class RomanNumerals {
	private static final int[] VALUES = {
			1000, 900, 500, 400,
			100, 90, 50, 40,
			10, 9, 5, 4,
			1
	};
	private static final String[] SYMBOLS = {
			"M", "CM", "D", "CD",
			"C", "XC", "L", "XL",
			"X", "IX", "V", "IV",
			"I"
	};

	private RomanNumerals() {
	}

	public static String format(int number) {
		if (number <= 0 || number >= 4000) {
			return Integer.toString(number);
		}
		StringBuilder sb = new StringBuilder();
		int remaining = number;
		for (int i = 0; i < VALUES.length && remaining > 0; i++) {
			while (remaining >= VALUES[i]) {
				sb.append(SYMBOLS[i]);
				remaining -= VALUES[i];
			}
		}
		return sb.toString();
	}

	public static String formatLevel(int amplifier, LevelFormat format, boolean hideLevelOne) {
		int level = amplifier >= Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(1, amplifier + 1);
		if (hideLevelOne && level == 1) {
			return "";
		}
		if (format == LevelFormat.ARABIC) {
			return Integer.toString(level);
		}
		return format(level);
	}
}
