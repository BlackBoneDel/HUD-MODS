package dev.customizablehud.config;

import java.util.LinkedHashMap;
import java.util.Map;

public final class EffectsConfig {
	public boolean enabled = true;
	public HudAnchor anchor = HudAnchor.CENTER_RIGHT;
	public int offsetX = 0;
	public int offsetY = 0;
	public float scale = 1.0f;
	public HudOrientation orientation = HudOrientation.VERTICAL;
	public EffectsVisualStyle style = EffectsVisualStyle.MINECRAFT_COMPACT;
	public PotionHudMode potionHudMode = PotionHudMode.NORMAL;
	public PotionMiniDisplayMode potionMiniDisplay = PotionMiniDisplayMode.TIMER_TEXT;
	public int rowSpacing = 2;
	public boolean showEffectBackground = false;
	public int backgroundOpacityPercent = 65;
	public boolean showEffectBorder = true;
	public boolean showIcon = true;
	public boolean showName = true;
	public boolean showDuration = true;
	public boolean showLevel = true;
	public boolean hideLevelOne = true;
	public LevelFormat levelFormat = LevelFormat.ROMAN;
	public boolean showEffectsWithoutParticles = true;
	public boolean groupByEffectCategory = true;
	public EffectSortMode sortMode = EffectSortMode.CATEGORY_THEN_NAME;
	public VanillaEffectsMode vanillaEffectsMode = VanillaEffectsMode.SHOW_BOTH;
	public boolean fadeOnLowDuration = true;
	public boolean colorDurationByRemaining = false;
	public Map<String, EffectColorOverride> customEffectColors = new LinkedHashMap<>();

	public EffectsConfig copy() {
		EffectsConfig copy = new EffectsConfig();
		copy.enabled = enabled;
		copy.anchor = anchor;
		copy.offsetX = offsetX;
		copy.offsetY = offsetY;
		copy.scale = scale;
		copy.orientation = orientation;
		copy.style = style;
		copy.potionHudMode = potionHudMode;
		copy.potionMiniDisplay = potionMiniDisplay;
		copy.rowSpacing = rowSpacing;
		copy.showEffectBackground = showEffectBackground;
		copy.backgroundOpacityPercent = backgroundOpacityPercent;
		copy.showEffectBorder = showEffectBorder;
		copy.showIcon = showIcon;
		copy.showName = showName;
		copy.showDuration = showDuration;
		copy.showLevel = showLevel;
		copy.hideLevelOne = hideLevelOne;
		copy.levelFormat = levelFormat;
		copy.showEffectsWithoutParticles = showEffectsWithoutParticles;
		copy.groupByEffectCategory = groupByEffectCategory;
		copy.sortMode = sortMode;
		copy.vanillaEffectsMode = vanillaEffectsMode;
		copy.fadeOnLowDuration = fadeOnLowDuration;
		copy.colorDurationByRemaining = colorDurationByRemaining;
		copy.customEffectColors = new LinkedHashMap<>();
		if (customEffectColors != null) {
			for (Map.Entry<String, EffectColorOverride> entry : customEffectColors.entrySet()) {
				if (entry.getKey() != null && entry.getValue() != null) {
					copy.customEffectColors.put(entry.getKey(), entry.getValue().copy());
				}
			}
		}
		return copy;
	}
}
