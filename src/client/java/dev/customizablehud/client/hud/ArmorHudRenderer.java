package dev.customizablehud.client.hud;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.config.ArmorBorderStyle;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorMetricPosition;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.ModLanguage;
import dev.customizablehud.config.PointOffset;
import dev.customizablehud.format.DurabilityFormatter;
import dev.customizablehud.layout.ArmorLayoutResult;
import dev.customizablehud.layout.ArmorSlotMetric;
import dev.customizablehud.layout.ArmorSlotPlacement;
import dev.customizablehud.layout.HudLayoutEngine;
import dev.customizablehud.layout.HudRect;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Util;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix3x2fStack;

public final class ArmorHudRenderer {
	private static final Identifier HOTBAR_SPRITE = Identifier.withDefaultNamespace("hud/hotbar");
	private static final Identifier INVENTORY_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");
	private static final System.Logger LOGGER = System.getLogger("CustomizableHUD/ArmorRenderer");
	private static long lastErrorLogMs = Long.MIN_VALUE;

	private ArmorHudRenderer() {
	}

	public static void renderLive(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		HudConfig config = CustomizableHudClient.getConfig();
		if (config == null || config.armor == null || !config.armor.enabled) {
			return;
		}
		if (!VisibilityRules.shouldRenderLiveHud(client, config.visibility)) {
			return;
		}
		render(graphics, client, config.armor, config.language, false, 1.0f);
	}

	public static ArmorLayoutResult renderForEditor(
			GuiGraphics graphics,
			Minecraft client,
			ArmorConfig armorConfig,
			ModLanguage language,
			float alpha
	) {
		return render(graphics, client, armorConfig, language, true, alpha);
	}

	public static void renderPreviewInBox(
			GuiGraphics graphics,
			Minecraft client,
			ArmorConfig armorConfig,
			ModLanguage language,
			int boxX,
			int boxY,
			int boxW,
			int boxH,
			float alpha
	) {
		if (client == null || armorConfig == null || boxW <= 4 || boxH <= 4) {
			return;
		}
		ArmorConfig previewConfig = armorConfig.copy();
		previewConfig.individualPositionsEnabled = false;
		previewConfig.anchor = HudAnchor.TOP_LEFT;
		previewConfig.offsetX = 0;
		previewConfig.offsetY = 0;

		List<LiveDataCollector.ArmorRenderEntry> entries = LiveDataCollector.collectArmorEntries(client, previewConfig, language, true);
		Font font = client.font;
		ArmorLayoutResult layout = computeLayoutFromEntries(font, previewConfig, entries, Math.max(320, boxW), Math.max(200, boxH));
		if (layout.slots().isEmpty() || layout.unscaledWidth() <= 0 || layout.unscaledHeight() <= 0) {
			return;
		}

		float maxAvailW = Math.max(12.0f, boxW - 10.0f);
		float maxAvailH = Math.max(12.0f, boxH - 10.0f);
		float fitScale = Math.min(layout.scale(), Math.min(maxAvailW / layout.unscaledWidth(), maxAvailH / layout.unscaledHeight()));
		fitScale = Math.max(0.45f, fitScale);

		int drawW = Math.round(layout.unscaledWidth() * fitScale);
		int drawH = Math.round(layout.unscaledHeight() * fitScale);
		int drawX = boxX + Math.max(0, (boxW - drawW) / 2);
		int drawY = boxY + Math.max(0, (boxH - drawH) / 2);

		Map<ArmorSlotKind, LiveDataCollector.ArmorRenderEntry> byKind = mapByKind(entries);
		long nowMs = Util.getMillis();

		graphics.enableScissor(boxX, boxY, boxX + boxW, boxY + boxH);
		try {
			Matrix3x2fStack pose = graphics.pose();
			pose.pushMatrix();
			try {
				pose.translate(drawX, drawY);
				pose.scale(fitScale, fitScale);
				renderPlacedSlots(graphics, font, previewConfig, layout, byKind, nowMs, alpha);
			} finally {
				pose.popMatrix();
			}
		} finally {
			graphics.disableScissor();
		}
	}

	public static List<ArmorSlotKind> captureIndividualPositionsFromAuto(
			Minecraft client,
			ArmorConfig armor,
			ModLanguage language,
			int screenWidth,
			int screenHeight
	) {
		List<LiveDataCollector.ArmorRenderEntry> entries = LiveDataCollector.collectArmorEntries(client, armor, language, true);
		List<ArmorSlotMetric> metrics = computeSlotMetrics(client.font, armor, entries);
		ArmorConfig temp = armor.copy();
		temp.individualPositionsEnabled = false;
		ArmorLayoutResult auto = HudLayoutEngine.computeArmorLayout(temp, metrics, screenWidth, screenHeight);
		Map<ArmorSlotKind, PointOffset> captured = HudLayoutEngine.captureIndividualOffsetsFromAuto(temp, metrics, screenWidth, screenHeight);
		armor.individualOffsets = captured;
		armor.anchor = HudAnchor.CUSTOM;
		armor.offsetX = auto.originX();
		armor.offsetY = auto.originY();
		List<ArmorSlotKind> visibleKinds = new ArrayList<>(auto.slots().size());
		for (ArmorSlotPlacement placement : auto.slots()) {
			visibleKinds.add(placement.kind());
		}
		return visibleKinds;
	}

	private static List<ArmorSlotMetric> computeSlotMetrics(
			Font font,
			ArmorConfig config,
			List<LiveDataCollector.ArmorRenderEntry> entries
	) {
		List<ArmorSlotMetric> metrics = new ArrayList<>(entries.size());
		int fontLineH = Math.max(5, font.lineHeight);
		for (LiveDataCollector.ArmorRenderEntry entry : entries) {
			String label = DurabilityFormatter.format(entry.snapshot(), config);
			String name = DurabilityFormatter.formatItemName(entry.snapshot(), config);
			boolean alertActive = DurabilityFormatter.isAlertActive(entry.snapshot(), config);
			metrics.add(new ArmorSlotMetric(
					entry.kind(),
					entry.snapshot().present(),
					label.isEmpty() ? 0 : font.width(label),
					name.isEmpty() ? 0 : font.width(name),
					fontLineH,
					alertActive
			));
		}
		return metrics;
	}

	private static ArmorLayoutResult computeLayoutFromEntries(
			Font font,
			ArmorConfig config,
			List<LiveDataCollector.ArmorRenderEntry> entries,
			int screenWidth,
			int screenHeight
	) {
		List<ArmorSlotMetric> metrics = computeSlotMetrics(font, config, entries);
		return HudLayoutEngine.computeArmorLayout(config, metrics, screenWidth, screenHeight);
	}

	private static Map<ArmorSlotKind, LiveDataCollector.ArmorRenderEntry> mapByKind(List<LiveDataCollector.ArmorRenderEntry> entries) {
		Map<ArmorSlotKind, LiveDataCollector.ArmorRenderEntry> byKind = new EnumMap<>(ArmorSlotKind.class);
		for (LiveDataCollector.ArmorRenderEntry entry : entries) {
			byKind.put(entry.kind(), entry);
		}
		return byKind;
	}

	private static ArmorLayoutResult render(
			GuiGraphics graphics,
			Minecraft client,
			ArmorConfig config,
			ModLanguage language,
			boolean previewFallback,
			float alpha
	) {
		List<LiveDataCollector.ArmorRenderEntry> entries = LiveDataCollector.collectArmorEntries(client, config, language, previewFallback);
		Font font = client.font;
		int screenW = graphics.guiWidth();
		int screenH = graphics.guiHeight();
		ArmorLayoutResult layout = computeLayoutFromEntries(font, config, entries, screenW, screenH);
		if (layout.slots().isEmpty()) {
			return layout;
		}

		Map<ArmorSlotKind, LiveDataCollector.ArmorRenderEntry> byKind = mapByKind(entries);
		long nowMs = Util.getMillis();
		float safeAlpha = Math.max(0.0f, Math.min(1.0f, alpha));
		Matrix3x2fStack pose = graphics.pose();

		if (config.individualPositionsEnabled) {
			for (ArmorSlotPlacement placement : layout.slots()) {
				LiveDataCollector.ArmorRenderEntry entry = byKind.get(placement.kind());
				if (entry == null) {
					continue;
				}
				pose.pushMatrix();
				try {
					pose.translate(placement.screenBounds().x(), placement.screenBounds().y());
					pose.scale(layout.scale(), layout.scale());
					drawSlotSafe(graphics, font, config, entry, placement, 0, 0, true, nowMs, safeAlpha);
				} finally {
					pose.popMatrix();
				}
			}
			return layout;
		}

		pose.pushMatrix();
		try {
			pose.translate(layout.originX(), layout.originY());
			pose.scale(layout.scale(), layout.scale());
			renderPlacedSlots(graphics, font, config, layout, byKind, nowMs, safeAlpha);
		} finally {
			pose.popMatrix();
		}
		return layout;
	}

	private static void renderPlacedSlots(
			GuiGraphics graphics,
			Font font,
			ArmorConfig config,
			ArmorLayoutResult layout,
			Map<ArmorSlotKind, LiveDataCollector.ArmorRenderEntry> byKind,
			long nowMs,
			float alpha
	) {
		boolean vertical = config.orientation == HudOrientation.VERTICAL;
		boolean outerFrame = config.borderStyle == ArmorBorderStyle.OUTER_FRAME;

		if (outerFrame) {
			List<HudRect> frameRects = HudLayoutEngine.computeArmorOuterFrameRects(layout, vertical);
			for (HudRect frame : frameRects) {
				if (frame != null && !frame.isEmpty()) {
					drawContinuousOuterFrame(graphics, config.style, frame.x(), frame.y(), frame.width(), frame.height(), alpha);
				}
			}
		}

		ArmorSlotPlacement prevArmorPlacement = null;
		boolean drewHandDivider = false;

		for (ArmorSlotPlacement placement : layout.slots()) {
			LiveDataCollector.ArmorRenderEntry entry = byKind.get(placement.kind());
			if (entry == null) {
				continue;
			}
			if (!outerFrame && !drewHandDivider && prevArmorPlacement != null && prevArmorPlacement.kind().isArmorPiece() && !placement.kind().isArmorPiece()) {
				drewHandDivider = true;
				int dividerColor = ARGB.color(0.45f * alpha, 0xFF94A3B8);
				if (vertical) {
					int divY = (prevArmorPlacement.unscaledY() + prevArmorPlacement.unscaledHeight() + placement.unscaledY()) / 2;
					int divW = Math.min(prevArmorPlacement.unscaledWidth(), placement.unscaledWidth());
					graphics.fill(0, divY, divW, divY + 1, dividerColor);
				} else {
					int divX = (prevArmorPlacement.unscaledX() + prevArmorPlacement.unscaledWidth() + placement.unscaledX()) / 2;
					int topY = Math.min(prevArmorPlacement.unscaledIconY(), placement.unscaledIconY());
					int bottomY = Math.max(
							prevArmorPlacement.unscaledIconY() + prevArmorPlacement.slotBoxSize(),
							placement.unscaledIconY() + placement.slotBoxSize()
					);
					graphics.fill(divX, topY + 2, divX + 1, Math.max(topY + 4, bottomY - 2), dividerColor);
				}
			}
			drawSlotSafe(graphics, font, config, entry, placement, placement.unscaledX(), placement.unscaledY(), !outerFrame, nowMs, alpha);
			if (placement.kind().isArmorPiece()) {
				prevArmorPlacement = placement;
			}
		}
	}

	private static void drawSlotSafe(
			GuiGraphics graphics,
			Font font,
			ArmorConfig config,
			LiveDataCollector.ArmorRenderEntry entry,
			ArmorSlotPlacement placement,
			int cellX,
			int cellY,
			boolean drawSlotBox,
			long nowMs,
			float alpha
	) {
		try {
			drawSlot(graphics, font, config, entry, placement, cellX, cellY, drawSlotBox, nowMs, alpha);
		} catch (RuntimeException ex) {
			if (lastErrorLogMs == Long.MIN_VALUE || (nowMs - lastErrorLogMs) >= 10000L) {
				LOGGER.log(System.Logger.Level.WARNING, "Failed rendering armor HUD slot " + entry.kind(), ex);
				lastErrorLogMs = nowMs;
			}
		}
	}

	private static void drawSlot(
			GuiGraphics graphics,
			Font font,
			ArmorConfig config,
			LiveDataCollector.ArmorRenderEntry entry,
			ArmorSlotPlacement placement,
			int cellX,
			int cellY,
			boolean drawSlotBox,
			long nowMs,
			float alpha
	) {
		int boxSize = placement.slotBoxSize();
		int iconBoxX = cellX + placement.iconOffsetX();
		int iconBoxY = cellY + placement.iconOffsetY();

		String name = DurabilityFormatter.formatItemName(entry.snapshot(), config);
		String label = DurabilityFormatter.format(entry.snapshot(), config);
		if (!name.isEmpty() || !label.isEmpty()) {
			drawSlotText(graphics, font, config, entry, placement, cellX, cellY, name, label, nowMs, alpha);
		}

		if (config.showIcon && boxSize > 0) {
			if (drawSlotBox) {
				drawSlotBackground(graphics, config.style, entry.kind(), iconBoxX, iconBoxY, boxSize, alpha);
			}

			int itemX = iconBoxX + (boxSize - 16) / 2;
			int itemY = iconBoxY + (boxSize - 16) / 2;
			ItemStack stack = entry.stack();
			if (!stack.isEmpty()) {
				graphics.renderFakeItem(stack, itemX, itemY);
				if (config.showDurabilityBar) {
					drawDurabilityBar(graphics, config, entry, itemX, itemY, alpha);
				}
				if (stack.getCount() > 1) {
					String countStr = Integer.toString(stack.getCount());
					graphics.drawString(font, countStr, itemX + 17 - font.width(countStr), itemY + 9, ARGB.white(alpha), true);
				}
			} else {
				Identifier emptySprite = emptySlotSpriteFor(entry.kind());
				if (emptySprite != null) {
					graphics.blitSprite(RenderPipelines.GUI_TEXTURED, emptySprite, itemX, itemY, 16, 16, ARGB.white(0.42f * alpha));
				}
			}
		}

		if (placement.alertActive() && config.showLowDurabilityAlert && DurabilityFormatter.isAlertActive(entry.snapshot(), config)) {
			DurabilityFormatter.WarningBlinkState blink = DurabilityFormatter.computeWarningBlink(entry.snapshot(), config, nowMs);
			float badgeAlpha = blink.active()
					? Math.max(0.0f, Math.min(1.0f, blink.indicatorAlpha() * alpha))
					: alpha;
			int alertRgb = DurabilityFormatter.alertColorArgb(config) & 0x00FFFFFF;
			drawLowDurabilityAlertBadge(
					graphics,
					cellX + placement.alertOffsetX(),
					cellY + placement.alertOffsetY(),
					alertRgb,
					badgeAlpha
			);
		}
	}

	private static void drawSlotText(
			GuiGraphics graphics,
			Font font,
			ArmorConfig config,
			LiveDataCollector.ArmorRenderEntry entry,
			ArmorSlotPlacement placement,
			int cellX,
			int cellY,
			String name,
			String label,
			long nowMs,
			float alpha
	) {
		ArmorMetricPosition metricPos = placement.metricPosition() != null
				? placement.metricPosition()
				: ArmorMetricPosition.ABOVE;
		float numberScale = placement.numberTextScale() > 0.0f
				? placement.numberTextScale()
				: HudLayoutEngine.effectiveArmorNumberScale(config);
		int numberLineH = placement.numberLineHeight() > 0
				? placement.numberLineHeight()
				: HudLayoutEngine.ARMOR_TEXT_LINE_HEIGHT;
		int nameColor = ARGB.color(alpha, 0xFFE5E9F0);
		int labelColor = ARGB.multiplyAlpha(DurabilityFormatter.textColorArgb(entry.snapshot(), config, nowMs), alpha);

		record TextLine(String text, int color, float scale, int lineHeight) {
		}
		List<TextLine> lines = new ArrayList<>(2);
		if (metricPos == ArmorMetricPosition.BELOW) {
			if (!label.isEmpty()) {
				lines.add(new TextLine(label, labelColor, numberScale, numberLineH));
			}
			if (!name.isEmpty()) {
				lines.add(new TextLine(name, nameColor, HudLayoutEngine.HORIZONTAL_ARMOR_TEXT_SCALE, HudLayoutEngine.ARMOR_TEXT_LINE_HEIGHT));
			}
		} else {
			if (!name.isEmpty()) {
				lines.add(new TextLine(name, nameColor, HudLayoutEngine.HORIZONTAL_ARMOR_TEXT_SCALE, HudLayoutEngine.ARMOR_TEXT_LINE_HEIGHT));
			}
			if (!label.isEmpty()) {
				lines.add(new TextLine(label, labelColor, numberScale, numberLineH));
			}
		}

		int textAreaX = cellX + placement.textOffsetX();
		int textY = cellY + placement.textOffsetY();
		int areaW = Math.max(1, placement.textAreaWidth());
		int boxSize = placement.slotBoxSize();
		float verticalCenterX = boxSize > 0
				? cellX + placement.iconOffsetX() + (boxSize / 2.0f)
				: textAreaX + (areaW / 2.0f);
		Matrix3x2fStack pose = graphics.pose();

		for (TextLine line : lines) {
			int w = font.width(line.text());
			float anchorX;
			int drawOffsetX;
			switch (metricPos) {
				case LEFT -> {
					anchorX = textAreaX + areaW - 1;
					drawOffsetX = -w;
				}
				case RIGHT -> {
					anchorX = textAreaX + 1;
					drawOffsetX = 0;
				}
				case ABOVE, BELOW -> {
					anchorX = verticalCenterX;
					drawOffsetX = -Math.round(w / 2.0f);
				}
				default -> {
					anchorX = verticalCenterX;
					drawOffsetX = -Math.round(w / 2.0f);
				}
			}

			pose.pushMatrix();
			try {
				pose.translate(anchorX, textY);
				pose.scale(line.scale(), line.scale());
				graphics.drawString(font, line.text(), drawOffsetX, 0, line.color(), true);
			} finally {
				pose.popMatrix();
			}
			textY += line.lineHeight();
		}
	}

	/**
	 * Renders an original 7x8 pixel-art octagonal warning badge with a centered crisp '!' symbol,
	 * inspired by Uku's Armor HUD low-durability warning position.
	 */
	private static void drawLowDurabilityAlertBadge(
			GuiGraphics graphics,
			int x,
			int y,
			int fillRgb,
			float alpha
	) {
		int r = (fillRgb >> 16) & 0xFF;
		int g = (fillRgb >> 8) & 0xFF;
		int b = fillRgb & 0xFF;
		int borderRgb = ((Math.max(24, r / 3) & 0xFF) << 16)
				| ((Math.max(8, g / 3) & 0xFF) << 8)
				| (Math.max(8, b / 3) & 0xFF);
		int highlightRgb = 0xFFF7F7;

		int border = ARGB.color(alpha, borderRgb);
		int fill = ARGB.color(alpha, fillRgb);
		int mark = ARGB.color(alpha, highlightRgb);

		// Octagonal 7x8 border + fill
		graphics.fill(x + 2, y, x + 5, y + 1, border);
		graphics.fill(x + 1, y + 1, x + 6, y + 2, border);
		graphics.fill(x, y + 2, x + 7, y + 6, border);
		graphics.fill(x + 1, y + 6, x + 6, y + 7, border);
		graphics.fill(x + 2, y + 7, x + 5, y + 8, border);

		graphics.fill(x + 2, y + 1, x + 5, y + 2, fill);
		graphics.fill(x + 1, y + 2, x + 6, y + 6, fill);
		graphics.fill(x + 2, y + 6, x + 5, y + 7, fill);

		// Centered crisp 1x3 stem + 1x1 spacer + 1x1 dot '!'
		graphics.fill(x + 3, y + 1, x + 4, y + 4, mark);
		graphics.fill(x + 3, y + 5, x + 4, y + 6, mark);
	}

	private static void drawContinuousOuterFrame(
			GuiGraphics graphics,
			ArmorVisualStyle style,
			int x,
			int y,
			int w,
			int h,
			float alpha
	) {
		ArmorVisualStyle safeStyle = style != null ? style : ArmorVisualStyle.VANILLA_HOTBAR;
		switch (safeStyle) {
			case VANILLA_HOTBAR -> drawVanillaHotbarStrip(graphics, x, y, w, h, alpha);
			case INVENTORY_SLOT -> {
				graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, ARGB.colorFromFloat(0.75f * alpha, 0.545f, 0.545f, 0.545f));
				int darkBevel = ARGB.colorFromFloat(0.92f * alpha, 0.215f, 0.215f, 0.215f);
				int lightBevel = ARGB.colorFromFloat(0.92f * alpha, 0.96f, 0.96f, 0.96f);
				graphics.fill(x, y, x + w - 1, y + 1, darkBevel);
				graphics.fill(x, y, x + 1, y + h - 1, darkBevel);
				graphics.fill(x + 1, y + h - 1, x + w, y + h, lightBevel);
				graphics.fill(x + w - 1, y + 1, x + w, y + h, lightBevel);
			}
			case MINIMAL -> {
				graphics.fill(x, y, x + w, y + h, ARGB.colorFromFloat(0.45f * alpha, 0.08f, 0.10f, 0.14f));
				graphics.renderOutline(x, y, w, h, ARGB.colorFromFloat(0.65f * alpha, 0.45f, 0.52f, 0.64f));
			}
			case BORDERLESS -> graphics.renderOutline(x, y, w, h, ARGB.colorFromFloat(0.65f * alpha, 0.45f, 0.52f, 0.64f));
		}
	}

	private static void drawVanillaHotbarSquare(GuiGraphics graphics, int x, int y, int boxSize, float alpha) {
		int tint = ARGB.white(alpha);
		int half = boxSize / 2;
		int rem = boxSize - half;
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0, x, y, half, half, tint);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 182 - rem, 0, x + half, y, rem, half, tint);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 22 - rem, x, y + half, half, rem, tint);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 182 - rem, 22 - rem, x + half, y + half, rem, rem, tint);
	}

	private static void drawVanillaHotbarStrip(GuiGraphics graphics, int x, int y, int w, int h, float alpha) {
		int tint = ARGB.white(alpha);
		if (w <= 20 && h <= 20) {
			drawVanillaHotbarSquare(graphics, x, y, Math.min(w, h), alpha);
			return;
		}
		int border = 2;
		int innerW = Math.max(0, w - border * 2);
		int innerH = Math.max(0, h - border * 2);

		// Corners
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 0, x, y, border, border, tint);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 180, 0, x + w - border, y, border, border, tint);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 20, x, y + h - border, border, border, tint);
		graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 180, 20, x + w - border, y + h - border, border, border, tint);

		// Top & bottom borders tiled from clean 16px span inside slot 0
		for (int dx = 0; dx < innerW; dx += 16) {
			int segW = Math.min(16, innerW - dx);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 2, 0, x + border + dx, y, segW, border, tint);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 2, 20, x + border + dx, y + h - border, segW, border, tint);
		}
		// Left & right borders tiled cleanly
		for (int dy = 0; dy < innerH; dy += 16) {
			int segH = Math.min(16, innerH - dy);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 0, 2, x, y + border + dy, border, segH, tint);
			graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 180, 2, x + w - border, y + border + dy, border, segH, tint);
		}
		// Translucent hotbar interior without internal dividers
		for (int dy = 0; dy < innerH; dy += 16) {
			int segH = Math.min(16, innerH - dy);
			for (int dx = 0; dx < innerW; dx += 16) {
				int segW = Math.min(16, innerW - dx);
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, HOTBAR_SPRITE, 182, 22, 2, 2, x + border + dx, y + border + dy, segW, segH, tint);
			}
		}
	}

	private static void drawSlotBackground(
			GuiGraphics graphics,
			ArmorVisualStyle style,
			ArmorSlotKind kind,
			int x,
			int y,
			int boxSize,
			float alpha
	) {
		ArmorVisualStyle safeStyle = style != null ? style : ArmorVisualStyle.VANILLA_HOTBAR;
		switch (safeStyle) {
			case VANILLA_HOTBAR -> drawVanillaHotbarSquare(graphics, x, y, boxSize, alpha);
			case INVENTORY_SLOT -> graphics.blitSprite(
					RenderPipelines.GUI_TEXTURED,
					INVENTORY_SLOT_SPRITE,
					x,
					y,
					boxSize,
					boxSize,
					ARGB.white(alpha)
			);
			case MINIMAL -> {
				graphics.fill(x, y, x + boxSize, y + boxSize, ARGB.colorFromFloat(0.45f * alpha, 0.08f, 0.10f, 0.14f));
				graphics.renderOutline(x, y, boxSize, boxSize, ARGB.colorFromFloat(0.65f * alpha, 0.45f, 0.52f, 0.64f));
			}
			case BORDERLESS -> {
				// No background box
			}
		}
		if (kind != null && !kind.isArmorPiece() && safeStyle != ArmorVisualStyle.BORDERLESS) {
			graphics.renderOutline(x, y, boxSize, boxSize, ARGB.color(0.42f * alpha, 0xFF60A5FA));
		}
	}

	private static void drawDurabilityBar(
			GuiGraphics graphics,
			ArmorConfig config,
			LiveDataCollector.ArmorRenderEntry entry,
			int itemX,
			int itemY,
			float alpha
	) {
		if (!entry.snapshot().hasValidDurability()) {
			return;
		}
		ItemStack stack = entry.stack();
		if (!stack.isBarVisible() && !entry.snapshot().hasValidDurability()) {
			return;
		}
		int barX = itemX + 2;
		int barY = itemY + 13;
		float fraction = entry.snapshot().resolvedBarFraction();
		int barWidth = Math.max(0, Math.min(13, Math.round(fraction * 13.0f)));
		int barColor = ARGB.multiplyAlpha(DurabilityFormatter.barColorArgb(entry.snapshot(), config), alpha);
		graphics.fill(barX, barY, barX + 13, barY + 2, ARGB.color(alpha, 0xFF000000));
		if (barWidth > 0) {
			graphics.fill(barX, barY, barX + barWidth, barY + 1, barColor);
		}
	}

	private static Identifier emptySlotSpriteFor(ArmorSlotKind kind) {
		return switch (kind) {
			case HELMET -> InventoryMenu.EMPTY_ARMOR_SLOT_HELMET;
			case CHESTPLATE -> InventoryMenu.EMPTY_ARMOR_SLOT_CHESTPLATE;
			case LEGGINGS -> InventoryMenu.EMPTY_ARMOR_SLOT_LEGGINGS;
			case BOOTS -> InventoryMenu.EMPTY_ARMOR_SLOT_BOOTS;
			case OFF_HAND -> InventoryMenu.EMPTY_ARMOR_SLOT_SHIELD;
			case MAIN_HAND -> null;
		};
	}
}
