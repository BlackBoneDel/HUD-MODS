package dev.customizablehud.config;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ArmorConfig {
	public boolean enabled = true;
	public HudAnchor anchor = HudAnchor.HOTBAR_LEFT;
	public int offsetX = 0;
	public int offsetY = 0;
	public float scale = 1.0f;
	public HudOrientation orientation = HudOrientation.HORIZONTAL;
	public ArmorVisualStyle style = ArmorVisualStyle.VANILLA_HOTBAR;
	public int slotSpacing = 2;
	public boolean hideEmptySlots = false;
	public boolean showIcon = true;
	public boolean showDurabilityBar = true;
	public DurabilityDisplayMode durabilityDisplayMode = DurabilityDisplayMode.POINTS_AND_PERCENT;
	public boolean showMaxDurability = false;
	public boolean showItemName = false;
	public boolean colorByDurability = false;
	public boolean lowDurabilityWarning = false;
	public int lowDurabilityThresholdPercent = 15;
	public boolean showMainHand = false;
	public boolean showOffHand = false;
	public boolean individualPositionsEnabled = false;
	public List<ArmorSlotKind> armorOrder = new ArrayList<>(ArmorSlotKind.DEFAULT_ARMOR_ORDER);
	public Map<ArmorSlotKind, PointOffset> individualOffsets = createDefaultIndividualOffsets();

	public static Map<ArmorSlotKind, PointOffset> createDefaultIndividualOffsets() {
		EnumMap<ArmorSlotKind, PointOffset> map = new EnumMap<>(ArmorSlotKind.class);
		int index = 0;
		for (ArmorSlotKind kind : ArmorSlotKind.values()) {
			map.put(kind, new PointOffset(index * 24, 0));
			index++;
		}
		return map;
	}

	public List<ArmorSlotKind> activeSlotOrder() {
		List<ArmorSlotKind> result = new ArrayList<>(6);
		if (armorOrder != null) {
			for (ArmorSlotKind kind : armorOrder) {
				if (kind != null && kind.isArmorPiece() && !result.contains(kind)) {
					result.add(kind);
				}
			}
		}
		for (ArmorSlotKind fallback : ArmorSlotKind.DEFAULT_ARMOR_ORDER) {
			if (!result.contains(fallback)) {
				result.add(fallback);
			}
		}
		if (showMainHand) {
			result.add(ArmorSlotKind.MAIN_HAND);
		}
		if (showOffHand) {
			result.add(ArmorSlotKind.OFF_HAND);
		}
		return result;
	}

	public ArmorConfig copy() {
		ArmorConfig copy = new ArmorConfig();
		copy.enabled = enabled;
		copy.anchor = anchor;
		copy.offsetX = offsetX;
		copy.offsetY = offsetY;
		copy.scale = scale;
		copy.orientation = orientation;
		copy.style = style;
		copy.slotSpacing = slotSpacing;
		copy.hideEmptySlots = hideEmptySlots;
		copy.showIcon = showIcon;
		copy.showDurabilityBar = showDurabilityBar;
		copy.durabilityDisplayMode = durabilityDisplayMode;
		copy.showMaxDurability = showMaxDurability;
		copy.showItemName = showItemName;
		copy.colorByDurability = colorByDurability;
		copy.lowDurabilityWarning = lowDurabilityWarning;
		copy.lowDurabilityThresholdPercent = lowDurabilityThresholdPercent;
		copy.showMainHand = showMainHand;
		copy.showOffHand = showOffHand;
		copy.individualPositionsEnabled = individualPositionsEnabled;
		copy.armorOrder = new ArrayList<>(armorOrder);
		copy.individualOffsets = new EnumMap<>(ArmorSlotKind.class);
		for (ArmorSlotKind kind : ArmorSlotKind.values()) {
			PointOffset offset = individualOffsets != null ? individualOffsets.get(kind) : null;
			copy.individualOffsets.put(kind, offset != null ? offset.copy() : new PointOffset());
		}
		return copy;
	}
}
