package dev.customizablehud.config;

import java.util.List;

public enum ArmorSlotKind {
	HELMET("customizablehud.slot.helmet", true),
	CHESTPLATE("customizablehud.slot.chestplate", true),
	LEGGINGS("customizablehud.slot.leggings", true),
	BOOTS("customizablehud.slot.boots", true),
	MAIN_HAND("customizablehud.slot.main_hand", false),
	OFF_HAND("customizablehud.slot.off_hand", false);

	public static final List<ArmorSlotKind> DEFAULT_ARMOR_ORDER = List.of(
			HELMET, CHESTPLATE, LEGGINGS, BOOTS
	);

	private final String translationKey;
	private final boolean armorPiece;

	ArmorSlotKind(String translationKey, boolean armorPiece) {
		this.translationKey = translationKey;
		this.armorPiece = armorPiece;
	}

	public String translationKey() {
		return translationKey;
	}

	public boolean isArmorPiece() {
		return armorPiece;
	}
}
