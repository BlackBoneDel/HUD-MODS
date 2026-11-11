package dev.customizablehud.client.hud;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.config.ArmorConfig;
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
	private static final Identifier HOTBAR_OFFHAND_LEFT_SPRITE = Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
	private static final Identifier INVENTORY_SLOT_SPRITE = Identifier.withDefaultNamespace("container/slot");

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
		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(drawX, drawY);
		pose.scale(fitScale, fitScale);
		renderPlacedSlots(graphics, font, previewConfig, layout, byKind, nowMs, alpha);
		pose.popMatrix();
		graphics.disableScissor();
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
		for (LiveDataCollector.ArmorRenderEntry entry : entries) {
			String label = DurabilityFormatter.format(entry.snapshot(), config);
			String name = config.showItemName && entry.snapshot().present() ? entry.snapshot().itemName() : "";
			metrics.add(new ArmorSlotMetric(
					entry.kind(),
					entry.snapshot().present(),
					label.isEmpty() ? 0 : font.width(label),
					name.isEmpty() ? 0 : font.width(name)
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
		Matrix3x2fStack pose = graphics.pose();

		if (config.individualPositionsEnabled) {
			for (ArmorSlotPlacement placement : layout.slots()) {
				LiveDataCollector.ArmorRenderEntry entry = byKind.get(placement.kind());
				if (entry == null) {
					continue;
				}
				pose.pushMatrix();
				pose.translate(placement.screenBounds().x(), placement.screenBounds().y());
				pose.scale(layout.scale(), layout.scale());
				drawSlot(graphics, font, config, entry, placement, 0, 0, nowMs, alpha);
				pose.popMatrix();
			}
			return layout;
		}

		pose.pushMatrix();
		pose.translate(layout.originX(), layout.originY());
		pose.scale(layout.scale(), layout.scale());
		renderPlacedSlots(graphics, font, config, layout, byKind, nowMs, alpha);
		pose.popMatrix();
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
		ArmorSlotPlacement prevArmorPlacement = null;
		boolean drewHandDivider = false;

		for (ArmorSlotPlacement placement : layout.slots()) {
			LiveDataCollector.ArmorRenderEntry entry = byKind.get(placement.kind());
			if (entry == null) {
				continue;
			}
			if (!drewHandDivider && prevArmorPlacement != null && prevArmorPlacement.kind().isArmorPiece() && !placement.kind().isArmorPiece()) {
				drewHandDivider = true;
				int dividerColor = ARGB.color(0.45f * alpha, 0xFF94A3B8);
				if (vertical) {
					int divY = (prevArmorPlacement.unscaledY() + prevArmorPlacement.unscaledHeight() + placement.unscaledY()) / 2;
					int divW = Math.min(prevArmorPlacement.unscaledWidth(), placement.unscaledWidth());
					graphics.fill(0, divY, divW, divY + 1, dividerColor);
				} else {
					int divX = (prevArmorPlacement.unscaledX() + prevArmorPlacement.unscaledWidth() + placement.unscaledX()) / 2;
					int topY = Math.max(prevArmorPlacement.textBlockHeight(), placement.textBlockHeight());
					int bottomY = Math.max(prevArmorPlacement.unscaledHeight(), placement.unscaledHeight());
					graphics.fill(divX, topY + 2, divX + 1, Math.max(topY + 4, bottomY - 2), dividerColor);
				}
			}
			drawSlot(graphics, font, config, entry, placement, placement.unscaledX(), placement.unscaledY(), nowMs, alpha);
			if (placement.kind().isArmorPiece()) {
				prevArmorPlacement = placement;
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
			long nowMs,
			float alpha
	) {
		int boxSize = placement.slotBoxSize();
		int textAreaH = placement.textBlockHeight();
		int gapAboveIcon = (textAreaH > 0 && boxSize > 0) ? HudLayoutEngine.ARMOR_TEXT_ICON_GAP : 0;

		int iconBoxX = cellX + (placement.unscaledWidth() - boxSize) / 2;
		int iconBoxY = cellY + textAreaH + gapAboveIcon;

		String name = config.showItemName && entry.snapshot().present() ? entry.snapshot().itemName() : "";
		String label = DurabilityFormatter.format(entry.snapshot(), config);
		if (!name.isEmpty() || !label.isEmpty()) {
			int textCenterX = cellX + placement.unscaledWidth() / 2;
			int lines = (name.isEmpty() ? 0 : 1) + (label.isEmpty() ? 0 : 1);
			int blockHeight = lines * HudLayoutEngine.ARMOR_TEXT_LINE_HEIGHT;
			int textStartY = cellY + Math.max(0, textAreaH - blockHeight);
			float textScale = HudLayoutEngine.HORIZONTAL_ARMOR_TEXT_SCALE;
			int nameColor = ARGB.color(alpha, 0xFFE5E9F0);
			int labelColor = ARGB.multiplyAlpha(DurabilityFormatter.textColorArgb(entry.snapshot(), config, nowMs), alpha);

			Matrix3x2fStack pose = graphics.pose();
			if (!name.isEmpty()) {
				pose.pushMatrix();
				pose.translate(textCenterX, textStartY);
				pose.scale(textScale, textScale);
				graphics.drawString(font, name, -font.width(name) / 2, 0, nameColor, true);
				pose.popMatrix();
				textStartY += HudLayoutEngine.ARMOR_TEXT_LINE_HEIGHT;
			}
			if (!label.isEmpty()) {
				pose.pushMatrix();
				pose.translate(textCenterX, textStartY);
				pose.scale(textScale, textScale);
				graphics.drawString(font, label, -font.width(label) / 2, 0, labelColor, true);
				pose.popMatrix();
			}
		}

		if (config.showIcon && boxSize > 0) {
			drawSlotBackground(graphics, config.style, entry.kind(), iconBoxX, iconBoxY, boxSize, alpha);

			if (config.lowDurabilityWarning && DurabilityFormatter.isLowDurability(entry.snapshot(), config)) {
				boolean flash = ((nowMs / 350L) % 2L) == 0L;
				int warnColor = ARGB.color(alpha, flash ? 0xFFFF4444 : 0xFFFF9922);
				graphics.renderOutline(iconBoxX, iconBoxY, boxSize, boxSize, warnColor);
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
			case VANILLA_HOTBAR -> graphics.blitSprite(
					RenderPipelines.GUI_TEXTURED,
					HOTBAR_OFFHAND_LEFT_SPRITE,
					29,
					24,
					0,
					1,
					x,
					y,
					boxSize,
					boxSize,
					ARGB.white(alpha)
			);
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
