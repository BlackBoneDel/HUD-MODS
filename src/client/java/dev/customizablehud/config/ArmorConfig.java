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
	public ArmorBorderStyle borderStyle = ArmorBorderStyle.PER_SLOT;
	public ArmorMetricPosition metricPosition = ArmorMetricPosition.ABOVE;
	public int slotSpacing = 0;
	public boolean showIcon = true;
	public boolean hideEmptySlots = false;
	public boolean showDurabilityBar = true;
	public DurabilityDisplayMode durabilityDisplayMode = DurabilityDisplayMode.POINTS_ONLY;
	public boolean showMaxDurability = false;
	public boolean showItemName = false;
	public boolean showHandItemName = false;
	public boolean colorByDurability = true;
	public int warningThresholdPercent = 60;
	public int criticalThresholdPercent = 20;
	public String healthyColorHex = "#6EEB83";
	public String warningColorHex = "#FFE066";
	public String criticalColorHex = "#FF5555";
	public boolean lowDurabilityWarning = false;
	public boolean showMainHand = false;
	public boolean showOffHand = false;
	public List<ArmorSlotKind> armorOrder = new ArrayList<>(ArmorSlotKind.DEFAULT_ARMOR_ORDER);
	public boolean individualPositionsEnabled = false;
	public Map<ArmorSlotKind, PointOffset> individualOffsets = createDefaultIndividualOffsets();

	public static Map<ArmorSlotKind, PointOffset> createDefaultIndividualOffsets() {
		EnumMap<ArmorSlotKind, PointOffset> map = new EnumMap<>(ArmorSlotKind.class);
		for (ArmorSlotKind kind : ArmorSlotKind.values()) {
			map.put(kind, new PointOffset());
		}
		return map;
	}

	public boolean shouldShowNameFor(ArmorSlotKind kind) {
		if (kind == null) {
			return false;
		}
		return kind.isArmorPiece() ? showItemName : showHandItemName;
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
		for (ArmorSlotKind required : ArmorSlotKind.DEFAULT_ARMOR_ORDER) {
			if (!result.contains(required)) {
				result.add(required);
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
		copy.borderStyle = borderStyle;
		copy.metricPosition = metricPosition;
		copy.slotSpacing = slotSpacing;
		copy.showIcon = showIcon;
		copy.hideEmptySlots = hideEmptySlots;
		copy.showDurabilityBar = showDurabilityBar;
		copy.durabilityDisplayMode = durabilityDisplayMode;
		copy.showMaxDurability = showMaxDurability;
		copy.showItemName = showItemName;
		copy.showHandItemName = showHandItemName;
		copy.colorByDurability = colorByDurability;
		copy.warningThresholdPercent = warningThresholdPercent;
		copy.criticalThresholdPercent = criticalThresholdPercent;
		copy.healthyColorHex = healthyColorHex;
		copy.warningColorHex = warningColorHex;
		copy.criticalColorHex = criticalColorHex;
		copy.lowDurabilityWarning = lowDurabilityWarning;
		copy.showMainHand = showMainHand;
		copy.showOffHand = showOffHand;
		copy.armorOrder = armorOrder != null ? new ArrayList<>(armorOrder) : new ArrayList<>(ArmorSlotKind.DEFAULT_ARMOR_ORDER);
		copy.individualPositionsEnabled = individualPositionsEnabled;
		copy.individualOffsets = new EnumMap<>(ArmorSlotKind.class);
		for (ArmorSlotKind kind : ArmorSlotKind.values()) {
			PointOffset offset = individualOffsets != null ? individualOffsets.get(kind) : null;
			copy.individualOffsets.put(kind, offset != null ? offset.copy() : new PointOffset());
		}
		return copy;
	}
}
