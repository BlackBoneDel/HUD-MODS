package dev.customizablehud.format;

import java.util.Locale;

public final class DurationFormatter {
	private DurationFormatter() {
	}

	public static String formatTicks(int ticks, float ticksPerSecond, boolean infinite) {
		if (infinite || ticks < 0) {
			return "∞";
		}
		float safeTps = Float.isFinite(ticksPerSecond) && ticksPerSecond > 0.1f ? ticksPerSecond : 20.0f;
		long totalSeconds = Math.max(0L, (long) Math.floor(ticks / safeTps));
		long hours = totalSeconds / 3600L;
		long minutes = (totalSeconds % 3600L) / 60L;
		long seconds = totalSeconds % 60L;
		if (hours > 0L) {
			return String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds);
		}
		return String.format(Locale.ROOT, "%d:%02d", minutes, seconds);
	}

	public static int durationColorArgb(int ticks, float ticksPerSecond, boolean infinite, boolean colorByRemaining) {
		if (!colorByRemaining) {
			return 0xFFD8DEE9;
		}
		if (infinite || ticks < 0) {
			return 0xFF7CE8FF;
		}
		float safeTps = Float.isFinite(ticksPerSecond) && ticksPerSecond > 0.1f ? ticksPerSecond : 20.0f;
		double seconds = ticks / safeTps;
		if (seconds <= 10.0) {
			return 0xFFFF6B6B;
		}
		if (seconds <= 30.0) {
			return 0xFFFFD166;
		}
		return 0xFFE6EDF3;
	}
}
