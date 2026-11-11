package dev.customizablehud.layout;

import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.EffectsVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.PointOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class HudLayoutEngine {
	public static final float HORIZONTAL_ARMOR_TEXT_SCALE = 0.72f;
	public static final int ARMOR_TEXT_LINE_HEIGHT = 8;
	public static final int ARMOR_TEXT_ICON_GAP = 2;
	public static final int HAND_SEPARATOR_GAP = 6;
	public static final int UNSCALED_LINE_HEIGHT = 9;
	private static final int SNAP_THRESHOLD = 4;

	private HudLayoutEngine() {
	}

	public record SnapResult(int x, int y, List<GuideLine> guides) {
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

	public static HudRect vanillaHotbarRect(int screenWidth, int screenHeight) {
		return new HudRect(screenWidth / 2 - 91, screenHeight - 22, 182, 22);
	}

	public static HudRect vanillaOffhandLeftRect(int screenWidth, int screenHeight) {
		return new HudRect(screenWidth / 2 - 120, screenHeight - 23, 22, 22);
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
		int boxSize = safeConfig.showIcon ? slotBoxSizeFor(safeConfig.style) : 0;
		int spacing = ConfigValidator.clampInt(safeConfig.slotSpacing, ConfigValidator.MIN_SPACING, ConfigValidator.MAX_SPACING);
		boolean vertical = safeConfig.orientation == HudOrientation.VERTICAL;

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

		record CellMetric(ArmorSlotMetric metric, int w, int rawTextH) {
		}

		List<CellMetric> rawCells = new ArrayList<>(visible.size());
		int maxRowTextH = 0;
		for (ArmorSlotMetric metric : visible) {
			boolean hasName = safeConfig.showItemName && metric.nameWidth() > 0;
			boolean hasLabel = metric.labelWidth() > 0;
			int lineCount = (hasName ? 1 : 0) + (hasLabel ? 1 : 0);
			int rawTextH = lineCount > 0 ? lineCount * ARMOR_TEXT_LINE_HEIGHT : 0;
			maxRowTextH = Math.max(maxRowTextH, rawTextH);

			int maxRawTextW = Math.max(hasName ? metric.nameWidth() : 0, hasLabel ? metric.labelWidth() : 0);
			int scaledTextW = maxRawTextW > 0 ? (int) Math.ceil(maxRawTextW * HORIZONTAL_ARMOR_TEXT_SCALE) + 2 : 0;
			int cellW = Math.max(boxSize, scaledTextW);
			if (cellW <= 0) {
				cellW = 16;
			}
			rawCells.add(new CellMetric(metric, cellW, rawTextH));
		}

		record CellSize(ArmorSlotMetric metric, int w, int h, int textBlockH) {
		}

		List<CellSize> cells = new ArrayList<>(rawCells.size());
		int totalW = 0;
		int totalH = 0;
		boolean prevWasArmorPiece = true;

		for (int i = 0; i < rawCells.size(); i++) {
			CellMetric raw = rawCells.get(i);
			int effectiveTextH = (!vertical && !safeConfig.individualPositionsEnabled) ? maxRowTextH : raw.rawTextH();
			int gapAboveIcon = (effectiveTextH > 0 && boxSize > 0) ? ARMOR_TEXT_ICON_GAP : 0;
			int cellH = effectiveTextH + gapAboveIcon + boxSize;
			if (cellH <= 0) {
				cellH = 14;
			}
			cells.add(new CellSize(raw.metric(), raw.w(), cellH, effectiveTextH));

			int stepGap = 0;
			if (i > 0) {
				boolean isHand = !raw.metric().kind().isArmorPiece();
				int handExtra = (prevWasArmorPiece && isHand) ? HAND_SEPARATOR_GAP : 0;
				stepGap = spacing + handExtra;
			}
			prevWasArmorPiece = raw.metric().kind().isArmorPiece();

			if (vertical) {
				totalW = Math.max(totalW, raw.w());
				totalH += cellH + stepGap;
			} else {
				totalW += raw.w() + stepGap;
				totalH = Math.max(totalH, cellH);
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

		List<ArmorSlotPlacement> placements = new ArrayList<>(cells.size());
		if (safeConfig.individualPositionsEnabled) {
			int baseOriginX = safeConfig.anchor == HudAnchor.CUSTOM
					? ConfigValidator.clampInt(safeConfig.offsetX, 0, Math.max(0, screenWidth - 16))
					: originRect.x();
			int baseOriginY = safeConfig.anchor == HudAnchor.CUSTOM
					? ConfigValidator.clampInt(safeConfig.offsetY, 0, Math.max(0, screenHeight - 16))
					: originRect.y();

			HudRect unionBounds = HudRect.EMPTY;
			for (CellSize cell : cells) {
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
						slotRect
				));
				unionBounds = HudRect.union(unionBounds, slotRect);
			}
			return new ArmorLayoutResult(baseOriginX, baseOriginY, totalW, totalH, scale, unionBounds, placements);
		}

		int cursorX = 0;
		int cursorY = 0;
		prevWasArmorPiece = true;
		for (int i = 0; i < cells.size(); i++) {
			CellSize cell = cells.get(i);
			if (i > 0) {
				boolean isHand = !cell.metric().kind().isArmorPiece();
				int handExtra = (prevWasArmorPiece && isHand) ? HAND_SEPARATOR_GAP : 0;
				int stepGap = spacing + handExtra;
				if (vertical) {
					cursorY += stepGap;
				} else {
					cursorX += stepGap;
				}
			}
			prevWasArmorPiece = cell.metric().kind().isArmorPiece();

			int sx = originRect.x() + Math.round(cursorX * scale);
			int sy = originRect.y() + Math.round(cursorY * scale);
			int sw = Math.max(1, Math.round(cell.w() * scale));
			int sh = Math.max(1, Math.round(cell.h() * scale));
			placements.add(new ArmorSlotPlacement(
					cell.metric().kind(),
					cursorX,
					cursorY,
					cell.w(),
					cell.h(),
					boxSize,
					cell.textBlockH(),
					new HudRect(sx, sy, sw, sh)
			));
			if (vertical) {
				cursorY += cell.h();
			} else {
				cursorX += cell.w();
			}
		}

		return new ArmorLayoutResult(originRect.x(), originRect.y(), totalW, totalH, scale, originRect, placements);
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

		if (metrics == null || metrics.isEmpty()) {
			HudRect emptyOrigin = resolveAnchorRect(safeConfig.anchor, safeConfig.offsetX, safeConfig.offsetY, 0, 0, screenWidth, screenHeight);
			return new EffectsLayoutResult(emptyOrigin.x(), emptyOrigin.y(), 0, 0, scale, emptyOrigin, List.of());
		}

		int iconBox = safeConfig.showIcon ? (style == EffectsVisualStyle.MINIMAL_TEXT ? 14 : 20) : 0;
		int padX = style == EffectsVisualStyle.MINIMAL_TEXT ? 2 : 4;
		int padY = style == EffectsVisualStyle.MINIMAL_TEXT ? 2 : 3;

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

			int textW;
			int textH;
			if (style == EffectsVisualStyle.MINIMAL_TEXT) {
				int gap = (hasTitle && hasDuration) ? 6 : 0;
				textW = metric.titleWidth() + gap + metric.durationWidth();
				textH = (hasTitle || hasDuration) ? UNSCALED_LINE_HEIGHT : 0;
			} else {
				textW = Math.max(metric.titleWidth(), metric.durationWidth());
				int lines = (hasTitle ? 1 : 0) + (hasDuration ? 1 : 0);
				textH = lines == 2 ? (UNSCALED_LINE_HEIGHT * 2 + 1) : (lines == 1 ? UNSCALED_LINE_HEIGHT : 0);
			}

			int rowW = padX * 2 + iconBox + (iconBox > 0 && textW > 0 ? 4 : 0) + textW;
			int rowH = Math.max(iconBox + padY * 2, textH + padY * 2);
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
			int rowW = vertical ? maxRowW : size.w();
			int rowH = size.h();
			int sx = originRect.x() + Math.round(cursorX * scale);
			int sy = originRect.y() + Math.round(cursorY * scale);
			int sw = Math.max(1, Math.round(rowW * scale));
			int sh = Math.max(1, Math.round(rowH * scale));
			placements.add(new EffectRowPlacement(
					i,
					cursorX,
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
