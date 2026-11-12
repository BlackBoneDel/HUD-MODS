package dev.customizablehud.format;

public enum EffectCategoryKind {
	BENEFICIAL(0),
	NEUTRAL(1),
	HARMFUL(2);

	private final int sortPriority;

	EffectCategoryKind(int sortPriority) {
		this.sortPriority = sortPriority;
	}

	public int sortPriority() {
		return sortPriority;
	}
}
