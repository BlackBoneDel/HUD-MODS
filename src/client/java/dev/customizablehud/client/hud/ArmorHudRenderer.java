package dev.customizablehud.client.hud;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
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
		render(graphics, client, config.armor, false, 1.0f);
	}

	public static ArmorLayoutResult renderForEditor(GuiGraphics graphics, Minecraft client, ArmorConfig armorConfig, float alpha) {
		return render(graphics, client, armorConfig, true, alpha);
	}

	public static ArmorLayoutResult computeLayout(Minecraft client, ArmorConfig armorConfig, boolean previewFallback) {
		List<LiveDataCollector.ArmorRenderEntry> entries = LiveDataCollector.collectArmorEntries(client, armorConfig, previewFallback);
		return computeLayoutFromEntries(client.font, armorConfig, entries, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
	}

	public static List<ArmorSlotKind> captureIndividualPositionsFromAuto(
			Minecraft client,
			ArmorConfig armor,
			int screenWidth,
			int screenHeight
	) {
		List<LiveDataCollector.ArmorRenderEntry> entries = LiveDataCollector.collectArmorEntries(client, armor, true);
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

	private static ArmorLayoutResult render(
			GuiGraphics graphics,
			Minecraft client,
			ArmorConfig config,
			boolean previewFallback,
			float alpha
	) {
		List<LiveDataCollector.ArmorRenderEntry> entries = LiveDataCollector.collectArmorEntries(client, config, previewFallback);
		Font font = client.font;
		int screenW = graphics.guiWidth();
		int screenH = graphics.guiHeight();
		ArmorLayoutResult layout = computeLayoutFromEntries(font, config, entries, screenW, screenH);
		if (layout.slots().isEmpty()) {
			return layout;
		}

		Map<ArmorSlotKind, LiveDataCollector.ArmorRenderEntry> byKind = new EnumMap<>(ArmorSlotKind.class);
		for (LiveDataCollector.ArmorRenderEntry entry : entries) {
			byKind.put(entry.kind(), entry);
		}

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
		for (ArmorSlotPlacement placement : layout.slots()) {
			LiveDataCollector.ArmorRenderEntry entry = byKind.get(placement.kind());
			if (entry == null) {
				continue;
			}
			drawSlot(graphics, font, config, entry, placement, placement.unscaledX(), placement.unscaledY(), nowMs, alpha);
		}
		pose.popMatrix();
		return layout;
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
		boolean vertical = config.orientation == HudOrientation.VERTICAL;
		int boxSize = placement.slotBoxSize();
		int iconBoxX = vertical ? cellX : cellX + (placement.unscaledWidth() - boxSize) / 2;
		int iconBoxY = vertical ? cellY + (placement.unscaledHeight() - boxSize) / 2 : cellY;

		if (config.showIcon && boxSize > 0) {
			drawSlotBackground(graphics, config.style, iconBoxX, iconBoxY, boxSize, alpha);

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
					drawDurabilityBar(graphics, entry, itemX, itemY, alpha);
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

		String name = config.showItemName && entry.snapshot().present() ? entry.snapshot().itemName() : "";
		String label = DurabilityFormatter.format(entry.snapshot(), config);
		if (name.isEmpty() && label.isEmpty()) {
			return;
		}

		int labelColor = ARGB.multiplyAlpha(DurabilityFormatter.textColorArgb(entry.snapshot(), config, nowMs), alpha);
		int nameColor = ARGB.color(alpha, 0xFFE5E9F0);

		if (vertical) {
			int textX = cellX + (boxSize > 0 ? boxSize + 4 : 0);
			int lines = (name.isEmpty() ? 0 : 1) + (label.isEmpty() ? 0 : 1);
			int textBlockH = lines == 2 ? (font.lineHeight * 2 + 1) : font.lineHeight;
			int textY = cellY + Math.max(0, (placement.unscaledHeight() - textBlockH) / 2);
			if (!name.isEmpty()) {
				graphics.drawString(font, name, textX, textY, nameColor, true);
				textY += font.lineHeight + 1;
			}
			if (!label.isEmpty()) {
				graphics.drawString(font, label, textX, textY, labelColor, true);
			}
		} else {
			int textCenterX = cellX + placement.unscaledWidth() / 2;
			int textY = cellY + (boxSize > 0 ? boxSize + 2 : 0);
			float textScale = HudLayoutEngine.HORIZONTAL_ARMOR_TEXT_SCALE;
			Matrix3x2fStack pose = graphics.pose();
			if (!name.isEmpty()) {
				pose.pushMatrix();
				pose.translate(textCenterX, textY);
				pose.scale(textScale, textScale);
				graphics.drawString(font, name, -font.width(name) / 2, 0, nameColor, true);
				pose.popMatrix();
				textY += 8;
			}
			if (!label.isEmpty()) {
				pose.pushMatrix();
				pose.translate(textCenterX, textY);
				pose.scale(textScale, textScale);
				graphics.drawString(font, label, -font.width(label) / 2, 0, labelColor, true);
				pose.popMatrix();
			}
		}
	}

	private static void drawSlotBackground(GuiGraphics graphics, ArmorVisualStyle style, int x, int y, int boxSize, float alpha) {
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
	}

	private static void drawDurabilityBar(GuiGraphics graphics, LiveDataCollector.ArmorRenderEntry entry, int itemX, int itemY, float alpha) {
		ItemStack stack = entry.stack();
		boolean showVanillaBar = stack.isBarVisible();
		boolean showDamageableBar = entry.snapshot().damageable() && !entry.snapshot().unbreakable();
		if (!showVanillaBar && !showDamageableBar) {
			return;
		}
		int barX = itemX + 2;
		int barY = itemY + 13;
		int barWidth;
		int barColor;
		if (showVanillaBar) {
			barWidth = Math.max(0, Math.min(13, stack.getBarWidth()));
			barColor = ARGB.color(alpha, ARGB.opaque(stack.getBarColor()));
		} else {
			float fraction = entry.snapshot().resolvedBarFraction();
			barWidth = Math.max(0, Math.min(13, Math.round(fraction * 13.0f)));
			barColor = ARGB.multiplyAlpha(DurabilityFormatter.barColorRgb(fraction), alpha);
		}
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
