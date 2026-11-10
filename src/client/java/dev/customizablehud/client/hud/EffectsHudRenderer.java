package dev.customizablehud.client.hud;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.EffectsVisualStyle;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.format.DurationFormatter;
import dev.customizablehud.format.EffectSnapshot;
import dev.customizablehud.format.RomanNumerals;
import dev.customizablehud.layout.EffectRowMetric;
import dev.customizablehud.layout.EffectRowPlacement;
import dev.customizablehud.layout.EffectsLayoutResult;
import dev.customizablehud.layout.HudLayoutEngine;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;
import org.joml.Matrix3x2fStack;

public final class EffectsHudRenderer {
	private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("hud/effect_background");
	private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("hud/effect_background_ambient");

	private EffectsHudRenderer() {
	}

	public static void renderLive(GuiGraphics graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		HudConfig config = CustomizableHudClient.getConfig();
		if (config == null || !VisibilityRules.shouldRenderCustomEffects(config)) {
			return;
		}
		if (!VisibilityRules.shouldRenderLiveHud(client, config.visibility)) {
			return;
		}
		render(graphics, client, config.effects, false, 1.0f);
	}

	public static EffectsLayoutResult renderForEditor(GuiGraphics graphics, Minecraft client, EffectsConfig effectsConfig, float alpha) {
		return render(graphics, client, effectsConfig, true, alpha);
	}

	public static EffectsLayoutResult computeLayout(Minecraft client, EffectsConfig effectsConfig, boolean previewFallback) {
		List<LiveDataCollector.EffectRenderEntry> entries = LiveDataCollector.collectEffectEntries(client, effectsConfig, previewFallback);
		float tps = LiveDataCollector.currentTicksPerSecond(client);
		return computeLayoutFromEntries(client.font, effectsConfig, entries, tps, client.getWindow().getGuiScaledWidth(), client.getWindow().getGuiScaledHeight());
	}

	private record PreparedRow(
			LiveDataCollector.EffectRenderEntry entry,
			String title,
			String duration
	) {
	}

	private static EffectsLayoutResult computeLayoutFromEntries(
			Font font,
			EffectsConfig config,
			List<LiveDataCollector.EffectRenderEntry> entries,
			float tps,
			int screenWidth,
			int screenHeight
	) {
		List<PreparedRow> rows = prepareRows(config, entries, tps);
		List<EffectRowMetric> metrics = new ArrayList<>(rows.size());
		for (PreparedRow row : rows) {
			metrics.add(new EffectRowMetric(
					row.entry().snapshot().id(),
					row.title().isEmpty() ? 0 : font.width(row.title()),
					row.duration().isEmpty() ? 0 : font.width(row.duration())
			));
		}
		return HudLayoutEngine.computeEffectsLayout(config, metrics, screenWidth, screenHeight);
	}

	private static List<PreparedRow> prepareRows(EffectsConfig config, List<LiveDataCollector.EffectRenderEntry> entries, float tps) {
		List<PreparedRow> rows = new ArrayList<>(entries.size());
		for (LiveDataCollector.EffectRenderEntry entry : entries) {
			EffectSnapshot snap = entry.snapshot();
			String namePart = config.showName ? snap.safeName() : "";
			String levelPart = config.showLevel
					? RomanNumerals.formatLevel(snap.amplifier(), config.levelFormat, config.hideLevelOne)
					: "";
			String title;
			if (!namePart.isEmpty() && !levelPart.isEmpty()) {
				title = namePart + " " + levelPart;
			} else if (!namePart.isEmpty()) {
				title = namePart;
			} else {
				title = levelPart;
			}
			String duration = config.showDuration
					? DurationFormatter.formatTicks(snap.durationTicks(), tps, snap.infinite())
					: "";
			rows.add(new PreparedRow(entry, title, duration));
		}
		return rows;
	}

	private static EffectsLayoutResult render(
			GuiGraphics graphics,
			Minecraft client,
			EffectsConfig config,
			boolean previewFallback,
			float alpha
	) {
		List<LiveDataCollector.EffectRenderEntry> entries = LiveDataCollector.collectEffectEntries(client, config, previewFallback);
		if (entries.isEmpty()) {
			return HudLayoutEngine.computeEffectsLayout(config, List.of(), graphics.guiWidth(), graphics.guiHeight());
		}
		Font font = client.font;
		float tps = LiveDataCollector.currentTicksPerSecond(client);
		List<PreparedRow> rows = prepareRows(config, entries, tps);
		List<EffectRowMetric> metrics = new ArrayList<>(rows.size());
		for (PreparedRow row : rows) {
			metrics.add(new EffectRowMetric(
					row.entry().snapshot().id(),
					row.title().isEmpty() ? 0 : font.width(row.title()),
					row.duration().isEmpty() ? 0 : font.width(row.duration())
			));
		}
		EffectsLayoutResult layout = HudLayoutEngine.computeEffectsLayout(config, metrics, graphics.guiWidth(), graphics.guiHeight());
		if (layout.rows().isEmpty()) {
			return layout;
		}

		Matrix3x2fStack pose = graphics.pose();
		pose.pushMatrix();
		pose.translate(layout.originX(), layout.originY());
		pose.scale(layout.scale(), layout.scale());

		EffectsVisualStyle style = config.style != null ? config.style : EffectsVisualStyle.MINECRAFT_COMPACT;
		int iconBox = config.showIcon ? (style == EffectsVisualStyle.MINIMAL_TEXT ? 14 : 20) : 0;
		int padX = style == EffectsVisualStyle.MINIMAL_TEXT ? 2 : 4;

		for (int i = 0; i < layout.rows().size() && i < rows.size(); i++) {
			EffectRowPlacement placement = layout.rows().get(i);
			PreparedRow row = rows.get(i);
			drawRow(graphics, font, config, style, row, placement, iconBox, padX, tps, alpha);
		}

		pose.popMatrix();
		return layout;
	}

	private static void drawRow(
			GuiGraphics graphics,
			Font font,
			EffectsConfig config,
			EffectsVisualStyle style,
			PreparedRow row,
			EffectRowPlacement placement,
			int iconBox,
			int padX,
			float tps,
			float alpha
	) {
		EffectSnapshot snap = row.entry().snapshot();
		int x = placement.unscaledX();
		int y = placement.unscaledY();
		int w = placement.unscaledWidth();
		int h = placement.unscaledHeight();

		switch (style) {
			case MINECRAFT_COMPACT -> {
				int bg = ARGB.colorFromFloat(0.62f * alpha, 0.07f, 0.09f, 0.12f);
				int border = snap.ambient()
						? ARGB.colorFromFloat(0.75f * alpha, 0.35f, 0.65f, 0.85f)
						: ARGB.colorFromFloat(0.68f * alpha, 0.25f, 0.30f, 0.38f);
				graphics.fill(x, y, x + w, y + h, bg);
				graphics.renderOutline(x, y, w, h, border);
			}
			case CLEAN_CARD -> {
				int bg = ARGB.colorFromFloat(0.70f * alpha, 0.05f, 0.07f, 0.10f);
				int border = ARGB.colorFromFloat(0.55f * alpha, 0.22f, 0.27f, 0.35f);
				int accent = ARGB.color(alpha, ARGB.opaque(snap.colorRgb()));
				graphics.fill(x, y, x + w, y + h, bg);
				graphics.renderOutline(x, y, w, h, border);
				graphics.fill(x + 1, y + 1, x + 3, y + h - 1, accent);
			}
			case MINIMAL_TEXT -> {
				int bg = ARGB.colorFromFloat(0.32f * alpha, 0.0f, 0.0f, 0.0f);
				graphics.fill(x, y, x + w, y + h, bg);
			}
		}

		if (config.showIcon && iconBox > 0) {
			int iconBoxX = x + padX;
			int iconBoxY = y + (h - iconBox) / 2;
			if (style == EffectsVisualStyle.MINECRAFT_COMPACT) {
				Identifier frameSprite = snap.ambient() ? EFFECT_BACKGROUND_AMBIENT_SPRITE : EFFECT_BACKGROUND_SPRITE;
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, frameSprite, iconBoxX, iconBoxY, iconBox, iconBox, ARGB.white(alpha));
			}
			int spriteSize = style == EffectsVisualStyle.MINIMAL_TEXT ? 12 : 16;
			int spriteX = iconBoxX + (iconBox - spriteSize) / 2;
			int spriteY = iconBoxY + (iconBox - spriteSize) / 2;
			float fade = computeLowDurationFade(config, snap) * alpha;
			drawEffectIcon(graphics, row.entry().holder(), snap, spriteX, spriteY, spriteSize, fade);
		}

		int textX = x + padX + (iconBox > 0 ? iconBox + 4 : 0);
		String title = row.title();
		String duration = row.duration();
		int titleColor = ARGB.color(alpha, 0xFFF3F6FA);
		int durationColor = ARGB.multiplyAlpha(
				DurationFormatter.durationColorArgb(snap.durationTicks(), tps, snap.infinite(), config.colorDurationByRemaining),
				alpha
		);

		if (style == EffectsVisualStyle.MINIMAL_TEXT) {
			int textY = y + Math.max(0, (h - font.lineHeight) / 2);
			int cursorX = textX;
			if (!title.isEmpty()) {
				graphics.drawString(font, title, cursorX, textY, titleColor, true);
				cursorX += font.width(title) + 6;
			}
			if (!duration.isEmpty()) {
				graphics.drawString(font, duration, cursorX, textY, durationColor, true);
			}
		} else {
			int lines = (title.isEmpty() ? 0 : 1) + (duration.isEmpty() ? 0 : 1);
			int textBlockH = lines == 2 ? (font.lineHeight * 2 + 1) : font.lineHeight;
			int textY = y + Math.max(0, (h - textBlockH) / 2);
			if (!title.isEmpty()) {
				graphics.drawString(font, title, textX, textY, titleColor, true);
				textY += font.lineHeight + 1;
			}
			if (!duration.isEmpty()) {
				graphics.drawString(font, duration, textX, textY, durationColor, true);
			}
		}
	}

	private static void drawEffectIcon(
			GuiGraphics graphics,
			Holder<MobEffect> holder,
			EffectSnapshot snap,
			int x,
			int y,
			int size,
			float alpha
	) {
		if (holder != null && holder.unwrapKey().isPresent()) {
			Identifier sprite = Gui.getMobEffectSprite(holder);
			if (!MissingTextureAtlasSprite.getLocation().equals(sprite)) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size, ARGB.white(alpha));
				return;
			}
		}
		int fill = ARGB.color(alpha, ARGB.opaque(snap.colorRgb()));
		int border = ARGB.color(alpha, 0xFFFFFFFF);
		graphics.fill(x + 1, y + 1, x + size - 1, y + size - 1, fill);
		graphics.renderOutline(x + 1, y + 1, size - 2, size - 2, border);
	}

	private static float computeLowDurationFade(EffectsConfig config, EffectSnapshot snap) {
		if (!config.fadeOnLowDuration || snap.ambient() || snap.infinite() || snap.durationTicks() > 200) {
			return 1.0f;
		}
		int duration = Math.max(0, snap.durationTicks());
		int factor = 10 - duration / 20;
		float f = Mth.clamp(duration / 10.0F / 5.0F * 0.5F, 0.0F, 0.5F)
				+ Mth.cos(duration * (float) Math.PI / 5.0F) * Mth.clamp(factor / 10.0F * 0.25F, 0.0F, 0.25F);
		return Mth.clamp(f, 0.25f, 1.0F);
	}
}
