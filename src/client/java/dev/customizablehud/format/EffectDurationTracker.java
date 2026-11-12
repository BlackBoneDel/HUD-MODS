package dev.customizablehud.format;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Tracks observed peak tick counts per active effect instance so the PotionHUD mini duration bar
 * reflects genuine remaining Minecraft ticks without fabricating static max durations.
 */
public final class EffectDurationTracker {
	private final Map<String, Integer> peakTicksByKey = new HashMap<>();

	public static String keyFor(EffectSnapshot snap) {
		if (snap == null) {
			return "";
		}
		String id = snap.id() != null ? snap.id() : "?";
		return id + "#" + snap.amplifier();
	}

	public synchronized void syncAndObserve(List<EffectSnapshot> snapshots) {
		if (snapshots == null || snapshots.isEmpty()) {
			peakTicksByKey.clear();
			return;
		}
		Set<String> activeKeys = new HashSet<>(snapshots.size());
		for (EffectSnapshot snap : snapshots) {
			if (snap == null) {
				continue;
			}
			String key = keyFor(snap);
			activeKeys.add(key);
			if (!snap.infinite() && snap.durationTicks() > 0) {
				Integer existing = peakTicksByKey.get(key);
				if (existing == null || snap.durationTicks() > existing) {
					peakTicksByKey.put(key, snap.durationTicks());
				}
			} else {
				peakTicksByKey.remove(key);
			}
		}
		peakTicksByKey.keySet().retainAll(activeKeys);
	}

	public synchronized int observedPeakTicks(EffectSnapshot snap) {
		if (snap == null || snap.infinite() || snap.durationTicks() <= 0) {
			return 0;
		}
		Integer peak = peakTicksByKey.get(keyFor(snap));
		return peak != null ? Math.max(peak, snap.durationTicks()) : snap.durationTicks();
	}

	public synchronized float remainingFraction(EffectSnapshot snap) {
		if (snap == null) {
			return 0.0f;
		}
		if (snap.infinite() || snap.durationTicks() < 0) {
			return 1.0f;
		}
		if (snap.durationTicks() == 0) {
			return 0.0f;
		}
		int peak = Math.max(1, observedPeakTicks(snap));
		return Math.max(0.0f, Math.min(1.0f, snap.durationTicks() / (float) peak));
	}

	public synchronized void clear() {
		peakTicksByKey.clear();
	}
}
