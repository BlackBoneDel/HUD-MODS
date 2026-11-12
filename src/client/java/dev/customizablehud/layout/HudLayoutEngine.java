package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorMetricPosition;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.EffectsVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.PointOffset;
import dev.customizablehud.config.PotionHudMode;
import dev.customizablehud.config.PotionMiniDisplayMode;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HudLayoutEngine {
	public static final float HORIZONTAL_ARMOR_TEXT_SCALE = 0.72f;
	public static final float BASE_ARMOR_NUMBER_SCALE = 0.75f;
	public static final int ARMOR_TEXT_LINE_HEIGHT = 8;
	public static final int ARMOR_TEXT_ICON_GAP = 2;
	public static final int HAND_SEPARATOR_GAP = 6;
	public static final int UNSCALED_LINE_HEIGHT = 9;
	public static final int ALERT_BADGE_WIDTH = 7;
	public static final int ALERT_BADGE_HEIGHT = 8;
	public static final int ALERT_BADGE_GAP = 1;
	private static final int SNAP_THRESHOLD = 4;

	private HudLayoutEngine() {
	}

	public enum ResizeCorner {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT
	}

	public record ResizeResult(
			float scale,
			int offsetX,
			int offsetY,
			HudRect bounds
	) {
	}

	public record SnapResult(int x, int y, List<GuideLine> guides) {
	}

	public static float effectiveArmorNumberScale(ArmorConfig config) {
		float numberScale = config != null ? ConfigValidator.clampNumberScale(config.numberScale) : 1.0f;
		return BASE_ARMOR_NUMBER_SCALE * numberScale;
	}

	public static int slotBoxSizeFor(ArmorVisualStyle style) {
		if (style == null) {
			return 20;
		}
		return switch (style) {
			case VANILLA_HOTBAR -> 20;
			case INVENTORY_SLOT, MINIMAL -> 18;
			case BORDERLESS -> 16;
		};
	}

	public static int minSafeArmorSpacingFor(ArmorVisualStyle style, boolean showIcon) {
		if (!showIcon) {
			return 0;
		}
		int boxSize = slotBoxSizeFor(style);
		return Math.max(ConfigValidator.MIN_ARMOR_SPACING, -(boxSize - 16));
	}

	public static HudRect vanillaHotbarRect(int screenWidth, int screenHeight) {
		return new HudRect(screenWidth / 2 - 91, screenHeight - 22, 182, 22);
	}

	public static HudRect vanillaOffhandLeftRect(int screenWidth, int screenHeight) {
		return new HudRect(screenWidth / 2 - 120, screenHeight - 23, 22, 22);
	}

	public static HudRect vanillaOffhandRightRect(int screenWidth, int screenHeight) {
		return new HudRect(screenWidth / 2 + 98, screenHeight - 23, 22, 22);
	}

	public static HudRect vanillaAttackIndicatorLeftRect(int screenWidth, int screenHeight) {
		return new HudRect(screenWidth / 2 - 115, screenHeight - 20, 18, 18);
	}

	public static HudRect vanillaAttackIndicatorRightRect(int screenWidth, int screenHeight) {
		return new HudRect(screenWidth / 2 + 97, screenHeight - 20, 18, 18);
	}

	public static HudRect resolveAnchorRect(
			HudAnchor anchor,
			int offsetX,
			int offsetY,
			int scaledWidth,
			int scaledHeight,
			int screenWidth,
			int screenHeight
	) {
		HudAnchor safeAnchor = anchor != null ? anchor : HudAnchor.TOP_LEFT;
		int baseX;
		int baseY;
		switch (safeAnchor) {
			case HOTBAR_LEFT -> {
				baseX = screenWidth / 2 - 124 - scaledWidth;
				baseY = screenHeight - scaledHeight - 2;
			}
			case HOTBAR_RIGHT -> {
				baseX = screenWidth / 2 + 122;
				baseY = screenHeight - scaledHeight - 2;
			}
			case CENTER_LEFT -> {
				baseX = 8;
				baseY = (screenHeight - scaledHeight) / 2;
			}
			case CENTER_RIGHT -> {
				baseX = screenWidth - scaledWidth - 8;
				baseY = (screenHeight - scaledHeight) / 2;
			}
			case CENTER_BOTTOM -> {
				baseX = (screenWidth - scaledWidth) / 2;
				baseY = screenHeight - 58 - scaledHeight;
			}
			case TOP_LEFT -> {
				baseX = 6;
				baseY = 6;
			}
			case TOP_RIGHT -> {
				baseX = screenWidth - scaledWidth - 6;
				baseY = 6;
			}
			case BOTTOM_LEFT -> {
				baseX = 6;
				baseY = screenHeight - scaledHeight - 6;
			}
			case BOTTOM_RIGHT -> {
				baseX = screenWidth - scaledWidth - 6;
				baseY = screenHeight - scaledHeight - 6;
			}
			case CUSTOM -> {
				baseX = 0;
				baseY = 0;
			}
			default -> {
				baseX = 6;
				baseY = 6;
			}
		}

		HudRect raw = new HudRect(baseX + offsetX, baseY + offsetY, scaledWidth, scaledHeight);
		return raw.clampToScreen(screenWidth, screenHeight, 2);
	}

	public static ArmorLayoutResult computeArmorLayout(
			ArmorConfig config,
			List<ArmorSlotMetric> metrics,
			int screenWidth,
			int screenHeight
	) {
		ArmorConfig safeConfig = config != null ? config : new ArmorConfig();
		float scale = ConfigValidator.clampScale(safeConfig.scale);
		float numberTextScale = effectiveArmorNumberScale(safeConfig);
		int boxSize = safeConfig.showIcon ? slotBoxSizeFor(safeConfig.style) : 0;
		int configuredSpacing = ConfigValidator.clampInt(
				safeConfig.slotSpacing,
				ConfigValidator.MIN_ARMOR_SPACING,
				ConfigValidator.MAX_SPACING
		);
		int effectiveSpacing = Math.max(configuredSpacing, minSafeArmorSpacingFor(safeConfig.style, safeConfig.showIcon));
		boolean vertical = safeConfig.orientation == HudOrientation.VERTICAL;
		ArmorMetricPosition metricPos = safeConfig.metricPosition != null
				? safeConfig.metricPosition
				: ArmorMetricPosition.ABOVE;
		boolean verticalSide = metricPos.isVerticalSide();

		List<ArmorSlotMetric> visible = new ArrayList<>();
		if (metrics != null) {
			for (ArmorSlotMetric metric : metrics) {
				if (metric == null) {
					continue;
				}
				if (safeConfig.hideEmptySlots && !metric.present()) {
					continue;
				}
				visible.add(metric);
			}
		}

		if (visible.isEmpty()) {
			HudRect emptyOrigin = resolveAnchorRect(safeConfig.anchor, safeConfig.offsetX, safeConfig.offsetY, 0, 0, screenWidth, screenHeight);
			return new ArmorLayoutResult(emptyOrigin.x(), emptyOrigin.y(), 0, 0, scale, emptyOrigin, List.of());
		}

		record RawMetric(
				ArmorSlotMetric metric,
				int scaledTextW,
				int rawTextH,
				int numberLineHeight,
				boolean alertActive
		) {
		}

		List<RawMetric> rawMetrics = new ArrayList<>(visible.size());
		int maxRowStackH = 0;
		int maxColSideW = 0;
		for (ArmorSlotMetric metric : visible) {
			boolean hasName = safeConfig.shouldShowNameFor(metric.kind()) && metric.nameWidth() > 0;
			boolean hasLabel = metric.labelWidth() > 0;
			int fontLineH = Math.max(5, metric.fontLineHeight());
			int numLineH = hasLabel ? Math.max(5, (int) Math.ceil(fontLineH * numberTextScale) + 1) : 0;
			int nameLineH = hasName ? ARMOR_TEXT_LINE_HEIGHT : 0;
			int rawTextH = numLineH + nameLineH;

			boolean alertActive = safeConfig.showLowDurabilityAlert && metric.alertActive();
			int alertH = alertActive ? (ALERT_BADGE_HEIGHT + ALERT_BADGE_GAP) : 0;
			maxRowStackH = Math.max(maxRowStackH, rawTextH + alertH);

			int scaledLabelW = hasLabel ? (int) Math.ceil(metric.labelWidth() * numberTextScale) + 1 : 0;
			int scaledNameW = hasName ? (int) Math.ceil(metric.nameWidth() * HORIZONTAL_ARMOR_TEXT_SCALE) + 2 : 0;
			int scaledTextW = Math.max(scaledLabelW, scaledNameW);
			int alertW = alertActive ? (ALERT_BADGE_WIDTH + ALERT_BADGE_GAP) : 0;
			maxColSideW = Math.max(maxColSideW, scaledTextW + alertW);

			rawMetrics.add(new RawMetric(metric, scaledTextW, rawTextH, numLineH, alertActive));
		}

		record CellSpec(
				ArmorSlotMetric metric,
				int w,
				int h,
				int actualTextW,
				int rawTextH,
				int textBlockH,
				int iconOffsetX,
				int iconOffsetY,
				int textOffsetX,
				int textOffsetY,
				int textAreaWidth,
				int numberLineHeight,
				boolean alertActive,
				int alertOffsetX,
				int alertOffsetY
		) {
		}

		List<CellSpec> cells = new ArrayList<>(rawMetrics.size());
		for (RawMetric raw : rawMetrics) {
			int cellW;
			int cellH;
			int textBlockH;
			int iconOffsetX;
			int iconOffsetY;
			int textOffsetX;
			int textOffsetY;
			int textAreaWidth;
			int alertOffsetX = 0;
			int alertOffsetY = 0;

			if (verticalSide) {
				int ownAlertH = raw.alertActive() ? (ALERT_BADGE_HEIGHT + ALERT_BADGE_GAP) : 0;
				int effectiveAboveOrBelowH = (!vertical && !safeConfig.individualPositionsEnabled)
						? maxRowStackH
						: (raw.rawTextH() + ownAlertH);
				int gap = (effectiveAboveOrBelowH > 0 && boxSize > 0) ? ARMOR_TEXT_ICON_GAP : 0;
				int minContentW = Math.max(raw.scaledTextW(), raw.alertActive() ? ALERT_BADGE_WIDTH : 0);
				cellW = Math.max(boxSize, minContentW);
				if (cellW <= 0) {
					cellW = 16;
				}
				cellH = effectiveAboveOrBelowH + gap + boxSize;
				if (cellH <= 0) {
					cellH = 14;
				}
				textBlockH = effectiveAboveOrBelowH;
				iconOffsetX = boxSize > 0 ? (cellW - boxSize) / 2 : 0;
				textOffsetX = 0;
				textAreaWidth = cellW;
				alertOffsetX = boxSize > 0
						? iconOffsetX + (boxSize - ALERT_BADGE_WIDTH) / 2
						: (cellW - ALERT_BADGE_WIDTH) / 2;

				if (metricPos == ArmorMetricPosition.ABOVE) {
					textOffsetY = raw.rawTextH() > 0 ? Math.max(0, effectiveAboveOrBelowH - raw.rawTextH()) : 0;
					iconOffsetY = boxSize > 0 ? (effectiveAboveOrBelowH + gap) : 0;
					if (raw.alertActive()) {
						alertOffsetY = raw.rawTextH() > 0
								? Math.max(0, textOffsetY - ALERT_BADGE_GAP - ALERT_BADGE_HEIGHT)
								: Math.max(0, effectiveAboveOrBelowH - ALERT_BADGE_HEIGHT);
					}
				} else {
					iconOffsetY = 0;
					textOffsetY = boxSize > 0 ? (boxSize + gap) : 0;
					if (raw.alertActive()) {
						alertOffsetY = raw.rawTextH() > 0
								? textOffsetY + raw.rawTextH() + ALERT_BADGE_GAP
								: textOffsetY;
					}
				}
			} else {
				int ownAlertW = raw.alertActive() ? (ALERT_BADGE_WIDTH + ALERT_BADGE_GAP) : 0;
				int effectiveSideW = (vertical && !safeConfig.individualPositionsEnabled)
						? maxColSideW
						: (raw.scaledTextW() + ownAlertW);
				int gap = (effectiveSideW > 0 && boxSize > 0) ? ARMOR_TEXT_ICON_GAP : 0;
				cellW = effectiveSideW + gap + boxSize;
				if (cellW <= 0) {
					cellW = 16;
				}
				int minAlertH = raw.alertActive() ? ALERT_BADGE_HEIGHT : 0;
				cellH = Math.max(Math.max(boxSize, raw.rawTextH()), minAlertH);
				if (cellH <= 0) {
					cellH = 14;
				}
				textBlockH = raw.rawTextH();
				iconOffsetY = boxSize > 0 ? (cellH - boxSize) / 2 : 0;
				textOffsetY = raw.rawTextH() > 0 ? Math.max(0, (cellH - raw.rawTextH()) / 2) : 0;
				alertOffsetY = Math.max(0, (cellH - ALERT_BADGE_HEIGHT) / 2);
				textAreaWidth = raw.scaledTextW();
				if (metricPos == ArmorMetricPosition.LEFT) {
					textOffsetX = Math.max(0, effectiveSideW - raw.scaledTextW());
					iconOffsetX = boxSize > 0 ? (effectiveSideW + gap) : 0;
					if (raw.alertActive()) {
						alertOffsetX = raw.scaledTextW() > 0
								? Math.max(0, textOffsetX - ALERT_BADGE_GAP - ALERT_BADGE_WIDTH)
								: Math.max(0, effectiveSideW - ALERT_BADGE_WIDTH);
					}
				} else {
					iconOffsetX = 0;
					textOffsetX = boxSize > 0 ? (boxSize + gap) : 0;
					if (raw.alertActive()) {
						alertOffsetX = raw.scaledTextW() > 0
								? textOffsetX + raw.scaledTextW() + ALERT_BADGE_GAP
								: textOffsetX;
					}
				}
			}

			cells.add(new CellSpec(
					raw.metric(),
					cellW,
					cellH,
					raw.scaledTextW(),
					raw.rawTextH(),
					textBlockH,
					iconOffsetX,
					iconOffsetY,
					textOffsetX,
					textOffsetY,
					textAreaWidth,
					raw.numberLineHeight(),
					raw.alertActive(),
					alertOffsetX,
					alertOffsetY
			));
		}

		if (safeConfig.individualPositionsEnabled) {
			int totalW = 0;
			int totalH = 0;
			boolean prevWasArmor = true;
			for (int i = 0; i < cells.size(); i++) {
				CellSpec cell = cells.get(i);
				int stepGap = i > 0
						? effectiveSpacing + (prevWasArmor && !cell.metric().kind().isArmorPiece() ? HAND_SEPARATOR_GAP : 0)
						: 0;
				prevWasArmor = cell.metric().kind().isArmorPiece();
				if (vertical) {
					totalW = Math.max(totalW, cell.w());
					totalH += cell.h() + Math.max(0, stepGap);
				} else {
					totalW += cell.w() + Math.max(0, stepGap);
					totalH = Math.max(totalH, cell.h());
				}
			}
			int scaledTotalW = Math.max(1, Math.round(totalW * scale));
			int scaledTotalH = Math.max(1, Math.round(totalH * scale));
			HudRect originRect = resolveAnchorRect(
					safeConfig.anchor,
					safeConfig.offsetX,
					safeConfig.offsetY,
					scaledTotalW,
					scaledTotalH,
					screenWidth,
					screenHeight
			);
			int baseOriginX = safeConfig.anchor == HudAnchor.CUSTOM
					? ConfigValidator.clampInt(safeConfig.offsetX, 0, Math.max(0, screenWidth - 16))
					: originRect.x();
			int baseOriginY = safeConfig.anchor == HudAnchor.CUSTOM
					? ConfigValidator.clampInt(safeConfig.offsetY, 0, Math.max(0, screenHeight - 16))
					: originRect.y();

			List<ArmorSlotPlacement> placements = new ArrayList<>(cells.size());
			HudRect unionBounds = HudRect.EMPTY;
			for (CellSpec cell : cells) {
				PointOffset offset = safeConfig.individualOffsets != null
						? safeConfig.individualOffsets.getOrDefault(cell.metric().kind(), new PointOffset())
						: new PointOffset();
				int slotScreenW = Math.max(1, Math.round(cell.w() * scale));
				int slotScreenH = Math.max(1, Math.round(cell.h() * scale));
				HudRect slotRect = new HudRect(
						baseOriginX + Math.round(offset.x * scale),
						baseOriginY + Math.round(offset.y * scale),
						slotScreenW,
						slotScreenH
				).clampToScreen(screenWidth, screenHeight, 2);

				placements.add(new ArmorSlotPlacement(
						cell.metric().kind(),
						offset.x,
						offset.y,
						cell.w(),
						cell.h(),
						boxSize,
						cell.textBlockH(),
						cell.iconOffsetX(),
						cell.iconOffsetY(),
						cell.textOffsetX(),
						cell.textOffsetY(),
						cell.textAreaWidth(),
						numberTextScale,
						cell.numberLineHeight(),
						cell.alertActive(),
						cell.alertOffsetX(),
						cell.alertOffsetY(),
						metricPos,
						slotRect
				));
				unionBounds = HudRect.union(unionBounds, slotRect);
			}
			return new ArmorLayoutResult(baseOriginX, baseOriginY, totalW, totalH, scale, unionBounds, placements);
		}

		int[] xs = new int[cells.size()];
		int[] ys = new int[cells.size()];
		boolean prevWasArmorPiece = true;

		for (int i = 1; i < cells.size(); i++) {
			CellSpec prev = cells.get(i - 1);
			CellSpec curr = cells.get(i);
			boolean isHand = !curr.metric().kind().isArmorPiece();
			int handExtra = (prevWasArmorPiece && isHand) ? HAND_SEPARATOR_GAP : 0;
			prevWasArmorPiece = curr.metric().kind().isArmorPiece();

			if (!vertical) {
				ys[i] = 0;
				if (effectiveSpacing >= 0) {
					xs[i] = xs[i - 1] + prev.w() + effectiveSpacing + handExtra;
				} else if (verticalSide) {
					int prevIconX = xs[i - 1] + prev.iconOffsetX();
					int iconStep = boxSize > 0 ? Math.max(16, boxSize + effectiveSpacing) : 16;
					int candidateByIcon = (prevIconX + iconStep + handExtra) - curr.iconOffsetX();

					int candidateByText = Integer.MIN_VALUE;
					if (prev.actualTextW() > 0 && curr.actualTextW() > 0) {
						int prevTextRight = xs[i - 1] + (prev.w() + prev.actualTextW()) / 2;
						int currTextLocalLeft = (curr.w() - curr.actualTextW()) / 2;
						candidateByText = (prevTextRight + 1 + handExtra) - currTextLocalLeft;
					}
					xs[i] = Math.max(xs[i - 1] + handExtra, Math.max(candidateByIcon, candidateByText));
				} else {
					boolean eitherHasText = prev.actualTextW() > 0 || curr.actualTextW() > 0;
					int stepX = eitherHasText
							? prev.w() + handExtra
							: (boxSize > 0 ? Math.max(16, boxSize + effectiveSpacing) : prev.w()) + handExtra;
					xs[i] = xs[i - 1] + stepX;
				}
			} else {
				xs[i] = 0;
				if (effectiveSpacing >= 0) {
					ys[i] = ys[i - 1] + prev.h() + effectiveSpacing + handExtra;
				} else if (!verticalSide) {
					int prevIconY = ys[i - 1] + prev.iconOffsetY();
					int iconStep = boxSize > 0 ? Math.max(16, boxSize + effectiveSpacing) : 14;
					int candidateByIcon = (prevIconY + iconStep + handExtra) - curr.iconOffsetY();

					int candidateByText = Integer.MIN_VALUE;
					if (prev.rawTextH() > 0 && curr.rawTextH() > 0) {
						int prevTextBottom = ys[i - 1] + prev.textOffsetY() + prev.rawTextH();
						candidateByText = (prevTextBottom + 1 + handExtra) - curr.textOffsetY();
					}
					ys[i] = Math.max(ys[i - 1] + handExtra, Math.max(candidateByIcon, candidateByText));
				} else {
					boolean textBetween = (metricPos == ArmorMetricPosition.BELOW && prev.rawTextH() > 0)
							|| (metricPos == ArmorMetricPosition.ABOVE && curr.rawTextH() > 0);
					int stepY = textBetween
							? prev.h() + handExtra
							: (boxSize > 0 ? Math.max(16, boxSize + effectiveSpacing) : prev.h()) + handExtra;
					ys[i] = ys[i - 1] + stepY;
				}
			}
		}

		int minX = Integer.MAX_VALUE;
		int minY = Integer.MAX_VALUE;
		for (int i = 0; i < cells.size(); i++) {
			minX = Math.min(minX, xs[i]);
			minY = Math.min(minY, ys[i]);
		}
		int totalW = 0;
		int totalH = 0;
		for (int i = 0; i < cells.size(); i++) {
			xs[i] -= minX;
			ys[i] -= minY;
			totalW = Math.max(totalW, xs[i] + cells.get(i).w());
			totalH = Math.max(totalH, ys[i] + cells.get(i).h());
		}

		int scaledTotalW = Math.max(1, Math.round(totalW * scale));
		int scaledTotalH = Math.max(1, Math.round(totalH * scale));
		HudRect originRect = resolveAnchorRect(
				safeConfig.anchor,
				safeConfig.offsetX,
				safeConfig.offsetY,
				scaledTotalW,
				scaledTotalH,
				screenWidth,
				screenHeight
		);

		List<ArmorSlotPlacement> placements = new ArrayList<>(cells.size());
		for (int i = 0; i < cells.size(); i++) {
			CellSpec cell = cells.get(i);
			int sx = originRect.x() + Math.round(xs[i] * scale);
			int sy = originRect.y() + Math.round(ys[i] * scale);
			int sw = Math.max(1, Math.round(cell.w() * scale));
			int sh = Math.max(1, Math.round(cell.h() * scale));
			placements.add(new ArmorSlotPlacement(
					cell.metric().kind(),
					xs[i],
					ys[i],
					cell.w(),
					cell.h(),
					boxSize,
					cell.textBlockH(),
					cell.iconOffsetX(),
					cell.iconOffsetY(),
					cell.textOffsetX(),
					cell.textOffsetY(),
					cell.textAreaWidth(),
					numberTextScale,
					cell.numberLineHeight(),
					cell.alertActive(),
					cell.alertOffsetX(),
					cell.alertOffsetY(),
					metricPos,
					new HudRect(sx, sy, sw, sh)
			));
		}

		return new ArmorLayoutResult(originRect.x(), originRect.y(), totalW, totalH, scale, originRect, placements);
	}

	public static List<HudRect> computeArmorOuterFrameRects(ArmorLayoutResult layout, boolean vertical) {
		if (layout == null || layout.slots().isEmpty()) {
			return List.of();
		}
		List<HudRect> frames = new ArrayList<>();
		List<ArmorSlotPlacement> group = new ArrayList<>();
		Boolean groupIsArmor = null;

		for (ArmorSlotPlacement placement : layout.slots()) {
			boolean isArmor = placement.kind().isArmorPiece();
			if (groupIsArmor != null && groupIsArmor != isArmor) {
				frames.add(computeOuterFrameForGroup(group, vertical));
				group.clear();
			}
			groupIsArmor = isArmor;
			group.add(placement);
		}
		if (!group.isEmpty()) {
			frames.add(computeOuterFrameForGroup(group, vertical));
		}
		return frames;
	}

	private static HudRect computeOuterFrameForGroup(List<ArmorSlotPlacement> group, boolean vertical) {
		HudRect union = HudRect.EMPTY;
		boolean hasIcons = group.getFirst().slotBoxSize() > 0;
		boolean hasInterleavedText = false;
		if (hasIcons && group.size() > 1) {
			ArmorSlotPlacement first = group.getFirst();
			ArmorSlotPlacement second = group.get(1);
			if (!vertical && !first.metricPosition().isVerticalSide() && first.textAreaWidth() > 0) {
				hasInterleavedText = true;
			}
			if (vertical && first.metricPosition().isVerticalSide() && first.textBlockHeight() > 0) {
				hasInterleavedText = true;
			}
			if (!hasInterleavedText) {
				int maxGap = first.slotBoxSize() + ConfigValidator.MAX_SPACING + 2;
				int dist = vertical
						? Math.abs(second.unscaledIconY() - first.unscaledIconY())
						: Math.abs(second.unscaledIconX() - first.unscaledIconX());
				if (dist > maxGap) {
					hasInterleavedText = true;
				}
			}
		}

		for (ArmorSlotPlacement p : group) {
			HudRect part = (hasIcons && !hasInterleavedText)
					? new HudRect(p.unscaledIconX(), p.unscaledIconY(), p.slotBoxSize(), p.slotBoxSize())
					: new HudRect(p.unscaledX(), p.unscaledY(), p.unscaledWidth(), p.unscaledHeight());
			union = HudRect.union(union, part);
		}
		return union;
	}

	public static Map<ArmorSlotKind, PointOffset> captureIndividualOffsetsFromAuto(
			ArmorConfig config,
			List<ArmorSlotMetric> metrics,
			int screenWidth,
			int screenHeight
	) {
		ArmorConfig temp = config.copy();
		temp.individualPositionsEnabled = false;
		ArmorLayoutResult auto = computeArmorLayout(temp, metrics, screenWidth, screenHeight);
		EnumMap<ArmorSlotKind, PointOffset> map = new EnumMap<>(ArmorSlotKind.class);
		for (ArmorSlotKind kind : ArmorSlotKind.values()) {
			map.put(kind, new PointOffset());
		}
		for (ArmorSlotPlacement placement : auto.slots()) {
			map.put(placement.kind(), new PointOffset(placement.unscaledX(), placement.unscaledY()));
		}
		return map;
	}

	public static EffectsLayoutResult computeEffectsLayout(
			EffectsConfig config,
			List<EffectRowMetric> metrics,
			int screenWidth,
			int screenHeight
	) {
		EffectsConfig safeConfig = config != null ? config : new EffectsConfig();
		float scale = ConfigValidator.clampScale(safeConfig.scale);
		int spacing = ConfigValidator.clampInt(safeConfig.rowSpacing, ConfigValidator.MIN_SPACING, ConfigValidator.MAX_SPACING);
		boolean vertical = safeConfig.orientation != HudOrientation.HORIZONTAL;
		EffectsVisualStyle style = safeConfig.style != null ? safeConfig.style : EffectsVisualStyle.MINECRAFT_COMPACT;
		PotionHudMode potionMode = safeConfig.potionHudMode != null ? safeConfig.potionHudMode : PotionHudMode.NORMAL;
		PotionMiniDisplayMode miniDisplay = safeConfig.potionMiniDisplay != null ? safeConfig.potionMiniDisplay : PotionMiniDisplayMode.TIMER_TEXT;

		if (metrics == null || metrics.isEmpty()) {
			HudRect emptyOrigin = resolveAnchorRect(safeConfig.anchor, safeConfig.offsetX, safeConfig.offsetY, 0, 0, screenWidth, screenHeight);
			return new EffectsLayoutResult(emptyOrigin.x(), emptyOrigin.y(), 0, 0, scale, emptyOrigin, List.of());
		}

		boolean isPotionMini = style == EffectsVisualStyle.POTION_HUD && potionMode == PotionHudMode.MINI;
		boolean isPotionNormal = style == EffectsVisualStyle.POTION_HUD && potionMode == PotionHudMode.NORMAL;

		int iconBox = safeConfig.showIcon
				? (style == EffectsVisualStyle.MINIMAL_TEXT ? 14 : (isPotionMini ? 16 : (isPotionNormal ? 18 : 20)))
				: 0;
		int padX = (style == EffectsVisualStyle.MINIMAL_TEXT || isPotionMini) ? 2 : (isPotionNormal ? 3 : 4);
		int padY = (style == EffectsVisualStyle.MINIMAL_TEXT || isPotionMini) ? 2 : 3;

		record RowSize(int w, int h) {
		}
		List<RowSize> sizes = new ArrayList<>(metrics.size());
		int maxRowW = 0;
		int totalW = 0;
		int totalH = 0;

		for (int i = 0; i < metrics.size(); i++) {
			EffectRowMetric metric = metrics.get(i);
			boolean hasTitle = metric.titleWidth() > 0;
			boolean hasDuration = metric.durationWidth() > 0;

			int rowW;
			int rowH;
			if (isPotionMini) {
				int bottomW = 0;
				int bottomH = 0;
				if (safeConfig.showDuration) {
					if (miniDisplay == PotionMiniDisplayMode.DURATION_BAR) {
						bottomW = 20;
						bottomH = 4;
					} else if (hasDuration) {
						bottomW = (int) Math.ceil(metric.durationWidth() * 0.78f) + 2;
						bottomH = 7;
					}
				}
				int levelW = (!safeConfig.showIcon && metric.miniLevelWidth() > 0)
						? (int) Math.ceil(metric.miniLevelWidth() * 0.78f) + 2
						: 0;
				int topH = iconBox > 0 ? iconBox : (levelW > 0 ? 8 : 0);
				int contentW = Math.max(Math.max(iconBox, levelW), bottomW);
				int gap = (topH > 0 && bottomH > 0) ? 2 : 0;
				rowW = Math.max(22, padX * 2 + contentW);
				rowH = Math.max(16, padY * 2 + topH + gap + bottomH);
			} else if (style == EffectsVisualStyle.MINIMAL_TEXT) {
				int gap = (hasTitle && hasDuration) ? 6 : 0;
				int textW = metric.titleWidth() + gap + metric.durationWidth();
				int textH = (hasTitle || hasDuration) ? UNSCALED_LINE_HEIGHT : 0;
				rowW = padX * 2 + iconBox + (iconBox > 0 && textW > 0 ? 4 : 0) + textW;
				rowH = Math.max(iconBox + padY * 2, textH + padY * 2);
			} else {
				int textW = Math.max(metric.titleWidth(), metric.durationWidth());
				int lines = (hasTitle ? 1 : 0) + (hasDuration ? 1 : 0);
				int textH = lines == 2 ? (UNSCALED_LINE_HEIGHT * 2 + 1) : (lines == 1 ? UNSCALED_LINE_HEIGHT : 0);
				int accentExtra = isPotionNormal ? 2 : 0;
				rowW = padX * 2 + accentExtra + iconBox + (iconBox > 0 && textW > 0 ? 4 : 0) + textW;
				rowH = Math.max(iconBox + padY * 2, textH + padY * 2);
			}

			if (rowW < 18) {
				rowW = 18;
			}
			if (rowH < 14) {
				rowH = 14;
			}
			sizes.add(new RowSize(rowW, rowH));
			maxRowW = Math.max(maxRowW, rowW);
			if (vertical) {
				totalH += rowH + (i > 0 ? spacing : 0);
			} else {
				totalW += rowW + (i > 0 ? spacing : 0);
				totalH = Math.max(totalH, rowH);
			}
		}

		if (vertical) {
			totalW = maxRowW;
		}

		int scaledTotalW = Math.max(1, Math.round(totalW * scale));
		int scaledTotalH = Math.max(1, Math.round(totalH * scale));
		HudRect originRect = resolveAnchorRect(
				safeConfig.anchor,
				safeConfig.offsetX,
				safeConfig.offsetY,
				scaledTotalW,
				scaledTotalH,
				screenWidth,
				screenHeight
		);

		List<EffectRowPlacement> placements = new ArrayList<>(sizes.size());
		int cursorX = 0;
		int cursorY = 0;
		for (int i = 0; i < sizes.size(); i++) {
			RowSize size = sizes.get(i);
			int rowW = (vertical && !isPotionMini) ? maxRowW : size.w();
			int rowH = size.h();
			int drawOffsetX = (vertical && isPotionMini && maxRowW > rowW) ? (maxRowW - rowW) / 2 : 0;
			int sx = originRect.x() + Math.round((cursorX + drawOffsetX) * scale);
			int sy = originRect.y() + Math.round(cursorY * scale);
			int sw = Math.max(1, Math.round(rowW * scale));
			int sh = Math.max(1, Math.round(rowH * scale));
			placements.add(new EffectRowPlacement(
					i,
					cursorX + drawOffsetX,
					cursorY,
					rowW,
					rowH,
					new HudRect(sx, sy, sw, sh)
			));
			if (vertical) {
				cursorY += rowH + spacing;
			} else {
				cursorX += rowW + spacing;
			}
		}

		return new EffectsLayoutResult(originRect.x(), originRect.y(), totalW, totalH, scale, originRect, placements);
	}

	public static ResizeResult computeResizeFromCorner(
			HudRect initialBounds,
			int unscaledWidth,
			int unscaledHeight,
			ResizeCorner corner,
			int pointerX,
			int pointerY,
			int screenWidth,
			int screenHeight
	) {
		HudRect safeBounds = initialBounds != null && !initialBounds.isEmpty()
				? initialBounds
				: new HudRect(0, 0, Math.max(1, unscaledWidth), Math.max(1, unscaledHeight));
		ResizeCorner safeCorner = corner != null ? corner : ResizeCorner.BOTTOM_RIGHT;
		int uW = Math.max(1, unscaledWidth);
		int uH = Math.max(1, unscaledHeight);

		int anchorX;
		int anchorY;
		int desiredW;
		int desiredH;
		switch (safeCorner) {
			case TOP_LEFT -> {
				anchorX = safeBounds.right();
				anchorY = safeBounds.bottom();
				desiredW = anchorX - pointerX;
				desiredH = anchorY - pointerY;
			}
			case TOP_RIGHT -> {
				anchorX = safeBounds.x();
				anchorY = safeBounds.bottom();
				desiredW = pointerX - anchorX;
				desiredH = anchorY - pointerY;
			}
			case BOTTOM_LEFT -> {
				anchorX = safeBounds.right();
				anchorY = safeBounds.y();
				desiredW = anchorX - pointerX;
				desiredH = pointerY - anchorY;
			}
			case BOTTOM_RIGHT -> {
				anchorX = safeBounds.x();
				anchorY = safeBounds.y();
				desiredW = pointerX - anchorX;
				desiredH = pointerY - anchorY;
			}
			default -> {
				anchorX = safeBounds.x();
				anchorY = safeBounds.y();
				desiredW = pointerX - anchorX;
				desiredH = pointerY - anchorY;
			}
		}

		int clampedDesiredW = Math.max(8, desiredW);
		int clampedDesiredH = Math.max(8, desiredH);
		double numerator = (double) clampedDesiredW * uW + (double) clampedDesiredH * uH;
		double denominator = (double) uW * uW + (double) uH * uH;
		float projectedScale = (float) (numerator / Math.max(1.0, denominator));
		float newScale = ConfigValidator.clampScale(projectedScale);

		int newW = Math.max(1, Math.round(uW * newScale));
		int newH = Math.max(1, Math.round(uH * newScale));
		int rawX = (safeCorner == ResizeCorner.TOP_LEFT || safeCorner == ResizeCorner.BOTTOM_LEFT)
				? anchorX - newW
				: anchorX;
		int rawY = (safeCorner == ResizeCorner.TOP_LEFT || safeCorner == ResizeCorner.TOP_RIGHT)
				? anchorY - newH
				: anchorY;

		HudRect clamped = new HudRect(rawX, rawY, newW, newH).clampToScreen(screenWidth, screenHeight, 2);
		return new ResizeResult(newScale, clamped.x(), clamped.y(), clamped);
	}

	public static SnapResult snapPosition(
			int candidateX,
			int candidateY,
			int width,
			int height,
			int screenWidth,
			int screenHeight,
			int gridSize,
			boolean snapToGrid,
			boolean collectGuides,
			List<HudRect> referenceRects
	) {
		int x = candidateX;
		int y = candidateY;
		int safeGrid = ConfigValidator.clampInt(gridSize, ConfigValidator.MIN_GRID_SIZE, ConfigValidator.MAX_GRID_SIZE);

		if (snapToGrid && safeGrid > 1) {
			x = Math.round(x / (float) safeGrid) * safeGrid;
			y = Math.round(y / (float) safeGrid) * safeGrid;
		}

		List<GuideLine> guides = new ArrayList<>();
		int screenCenterX = screenWidth / 2;
		int screenCenterY = screenHeight / 2;
		int boxCenterX = x + width / 2;
		int boxCenterY = y + height / 2;

		boolean snappedX = false;
		boolean snappedY = false;

		if (Math.abs(boxCenterX - screenCenterX) <= SNAP_THRESHOLD) {
			x = screenCenterX - width / 2;
			snappedX = true;
			if (collectGuides) {
				guides.add(new GuideLine(true, screenCenterX));
			}
		}
		if (Math.abs(boxCenterY - screenCenterY) <= SNAP_THRESHOLD) {
			y = screenCenterY - height / 2;
			snappedY = true;
			if (collectGuides) {
				guides.add(new GuideLine(false, screenCenterY));
			}
		}

		if (referenceRects != null) {
			for (HudRect ref : referenceRects) {
				if (ref == null || ref.isEmpty()) {
					continue;
				}
				if (!snappedX) {
					if (Math.abs(x - ref.x()) <= SNAP_THRESHOLD) {
						x = ref.x();
						snappedX = true;
						if (collectGuides) {
							guides.add(new GuideLine(true, ref.x()));
						}
					} else if (Math.abs((x + width) - ref.right()) <= SNAP_THRESHOLD) {
						x = ref.right() - width;
						snappedX = true;
						if (collectGuides) {
							guides.add(new GuideLine(true, ref.right()));
						}
					} else if (Math.abs((x + width / 2) - ref.centerX()) <= SNAP_THRESHOLD) {
						x = ref.centerX() - width / 2;
						snappedX = true;
						if (collectGuides) {
							guides.add(new GuideLine(true, ref.centerX()));
						}
					}
				}
				if (!snappedY) {
					if (Math.abs(y - ref.y()) <= SNAP_THRESHOLD) {
						y = ref.y();
						snappedY = true;
						if (collectGuides) {
							guides.add(new GuideLine(false, ref.y()));
						}
					} else if (Math.abs((y + height) - ref.bottom()) <= SNAP_THRESHOLD) {
						y = ref.bottom() - height;
						snappedY = true;
						if (collectGuides) {
							guides.add(new GuideLine(false, ref.bottom()));
						}
					} else if (Math.abs((y + height / 2) - ref.centerY()) <= SNAP_THRESHOLD) {
						y = ref.centerY() - height / 2;
						snappedY = true;
						if (collectGuides) {
							guides.add(new GuideLine(false, ref.centerY()));
						}
					}
				}
			}
		}

		HudRect clamped = new HudRect(x, y, width, height).clampToScreen(screenWidth, screenHeight, 2);
		return new SnapResult(clamped.x(), clamped.y(), guides);
	}

	public static boolean hasAnyOverlap(List<HudRect> rects) {
		if (rects == null || rects.size() < 2) {
			return false;
		}
		for (int i = 0; i < rects.size(); i++) {
			HudRect a = rects.get(i);
			if (a == null || a.isEmpty()) {
				continue;
			}
			for (int j = i + 1; j < rects.size(); j++) {
				if (a.intersects(rects.get(j))) {
					return true;
				}
			}
		}
		return false;
	}
}
