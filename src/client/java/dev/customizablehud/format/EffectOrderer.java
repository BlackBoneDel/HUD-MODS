package dev.customizablehud.format;

import dev.customizablehud.config.EffectSortMode;
import dev.customizablehud.config.EffectsConfig;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class EffectOrderer {
	private EffectOrderer() {
	}

	public static List<EffectSnapshot> prepare(List<EffectSnapshot> rawEffects, EffectsConfig config) {
		if (rawEffects == null || rawEffects.isEmpty()) {
			return List.of();
		}
		EffectsConfig safeConfig = config != null ? config : new EffectsConfig();
		List<EffectSnapshot> filtered = new ArrayList<>(rawEffects.size());
		for (EffectSnapshot effect : rawEffects) {
			if (effect == null) {
				continue;
			}
			if (!safeConfig.showEffectsWithoutParticles && !effect.particlesVisible()) {
				continue;
			}
			filtered.add(effect);
		}
		if (filtered.isEmpty()) {
			return List.of();
		}

		Comparator<EffectSnapshot> comparator = buildComparator(safeConfig);
		if (comparator != null) {
			filtered.sort(comparator);
		}
		return filtered;
	}

	private static Comparator<EffectSnapshot> buildComparator(EffectsConfig config) {
		EffectSortMode sortMode = config.sortMode != null ? config.sortMode : EffectSortMode.CATEGORY_THEN_NAME;
		Comparator<EffectSnapshot> categoryCmp = Comparator.comparingInt(e -> e.safeCategory().sortPriority());
		Comparator<EffectSnapshot> nameCmp = Comparator
				.comparing((EffectSnapshot e) -> e.safeName(), String.CASE_INSENSITIVE_ORDER)
				.thenComparing(e -> e.id() == null ? "" : e.id());

		Comparator<EffectSnapshot> primary = switch (sortMode) {
			case CATEGORY_THEN_NAME -> categoryCmp.thenComparing(nameCmp);
			case DURATION_ASC -> Comparator
					.comparing(EffectSnapshot::infinite)
					.thenComparingInt(EffectSnapshot::durationTicks)
					.thenComparing(nameCmp);
			case DURATION_DESC -> Comparator
					.comparing(EffectSnapshot::infinite, Comparator.reverseOrder())
					.thenComparing(Comparator.comparingInt(EffectSnapshot::durationTicks).reversed())
					.thenComparing(nameCmp);
			case NAME_ASC -> nameCmp;
			case NONE -> null;
		};

		if (config.groupByEffectCategory) {
			if (primary == null) {
				return categoryCmp;
			}
			if (sortMode != EffectSortMode.CATEGORY_THEN_NAME) {
				return categoryCmp.thenComparing(primary);
			}
		}
		return primary;
	}
}
