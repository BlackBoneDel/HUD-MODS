package dev.customizablehud.client.screen;

import dev.customizablehud.client.hud.EffectsHudRenderer;
import dev.customizablehud.client.i18n.ModI18n;
import dev.customizablehud.config.ColorHexUtils;
import dev.customizablehud.config.EffectColorOverride;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.format.EffectsColorResolver;
import dev.customizablehud.format.ModTranslations;
import dev.customizablehud.layout.HudRect;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffect;

public final class EffectColorsScreen extends AbstractCustomizableScreen {
	private static final int HEADER_HEIGHT = 38;
	private static final int FOOTER_HEIGHT = 34;
	private static final int LIST_ROW_HEIGHT = 24;

	private record EffectEntry(
			String id,
			String displayName,
			Holder<MobEffect> holder,
			int rawNativeRgb,
			int autoDisplayRgb,
			boolean hasNativeColor,
			boolean registered
	) {
		String autoHex() {
			return String.format(Locale.ROOT, "#%06X", autoDisplayRgb & 0x00FFFFFF);
		}
	}

	private final List<EffectEntry> allEntries = new ArrayList<>();
	private final List<EffectEntry> filteredEntries = new ArrayList<>();

	private String selectedEffectId = "minecraft:regeneration";
	private String searchText = "";
	private int listScrollOffset = 0;
	private boolean draggingListScrollbar = false;

	private EditBox searchBox;
	private Button toggleOverrideBtn;
	private EditBox hexEditBox;
	private Button applyHexBtn;
	private Button resetColorBtn;
	private boolean suppressHexResponder = false;
	private boolean hexInputInvalid = false;

	private HudRect listRect = HudRect.EMPTY;
	private HudRect previewRect = HudRect.EMPTY;

	public EffectColorsScreen(Screen parent, HudConfig workingConfig) {
		super("customizablehud.screen.effect_colors.title", parent, workingConfig);
	}

	private void rebuildEffectCatalog() {
		allEntries.clear();
		Set<String> seenIds = new LinkedHashSet<>();

		BuiltInRegistries.MOB_EFFECT.listElements().forEach(holder -> {
			String id = holder.key().identifier().toString();
			seenIds.add(id);
			MobEffect effect = holder.value();
			String name = ModI18n.translateExternal(activeLang(), effect.getDescriptionId(), effect.getDisplayName(), id);
			int rawColor = effect.getColor() & 0x00FFFFFF;
			boolean hasNative = EffectsColorResolver.hasNativeColor(rawColor);
			int autoRgb = EffectsColorResolver.resolveNativeDisplayRgb(rawColor);
			allEntries.add(new EffectEntry(id, name, holder, rawColor, autoRgb, hasNative, true));
		});

		if (workingConfig.effects != null && workingConfig.effects.customEffectColors != null) {
			for (String savedId : workingConfig.effects.customEffectColors.keySet()) {
				if (savedId != null && !seenIds.contains(savedId)) {
					seenIds.add(savedId);
					allEntries.add(new EffectEntry(
							savedId,
							ModTranslations.humanizeIdentifier(savedId),
							null,
							0,
							EffectsColorResolver.UNREADABLE_FALLBACK_RGB,
							false,
							false
					));
				}
			}
		}

		allEntries.sort(Comparator.comparing(EffectEntry::displayName, String.CASE_INSENSITIVE_ORDER).thenComparing(EffectEntry::id));

		boolean foundSelection = false;
		for (EffectEntry entry : allEntries) {
			if (entry.id().equals(selectedEffectId)) {
				foundSelection = true;
				break;
			}
		}
		if (!foundSelection && !allEntries.isEmpty()) {
			selectedEffectId = allEntries.getFirst().id();
		}
		applyFilter();
	}

	private void applyFilter() {
		filteredEntries.clear();
		String q = searchText == null ? "" : searchText.trim().toLowerCase(Locale.ROOT);
		for (EffectEntry entry : allEntries) {
			if (q.isEmpty()
					|| entry.displayName().toLowerCase(Locale.ROOT).contains(q)
					|| entry.id().toLowerCase(Locale.ROOT).contains(q)) {
				filteredEntries.add(entry);
			}
		}
		clampListScroll();
	}

	private int maxListScroll() {
		int totalH = filteredEntries.size() * LIST_ROW_HEIGHT;
		return Math.max(0, totalH - Math.max(20, listRect.height()));
	}

	private void clampListScroll() {
		listScrollOffset = Mth.clamp(listScrollOffset, 0, maxListScroll());
	}

	@Override
	protected void init() {
		rebuildEffectCatalog();

		int bodyTop = HEADER_HEIGHT + 6;
		int bodyBottom = this.height - FOOTER_HEIGHT - 6;
		int bodyH = Math.max(80, bodyBottom - bodyTop);
		boolean sideBySide = this.width >= 480;

		int leftX = 12;
		int leftW;
		int rightX;
		int rightW;

		if (sideBySide) {
			leftW = Math.max(160, Math.min(240, (this.width - 36) / 2));
			rightX = leftX + leftW + 12;
			rightW = this.width - rightX - 12;
		} else {
			leftW = this.width - 24;
			rightX = 12;
			rightW = leftW;
		}

		int searchY = bodyTop;
		searchBox = addRenderableWidget(new EditBox(this.font, leftX, searchY, leftW, 18, t("customizablehud.effect_colors.search")));
		searchBox.setMaxLength(64);
		searchBox.setValue(searchText);
		searchBox.setTooltip(Tooltip.create(t("customizablehud.tooltip.effect_colors.search")));
		searchBox.setResponder(text -> {
			searchText = text != null ? text : "";
			applyFilter();
		});

		if (sideBySide) {
			int listTop = searchY + 22;
			listRect = new HudRect(leftX, listTop, leftW, Math.max(40, bodyBottom - listTop));

			int previewH = Math.max(54, Math.min(108, bodyH / 3));
			previewRect = new HudRect(rightX, bodyTop, rightW, previewH);
			initRightControls(rightX, previewRect.bottom() + 8, rightW);
		} else {
			int listTop = searchY + 22;
			int listH = Math.max(52, Math.min(92, bodyH / 3));
			listRect = new HudRect(leftX, listTop, leftW, listH);

			int controlsY = listRect.bottom() + 6;
			int afterControlsY = initRightControls(rightX, controlsY, rightW);
			int remH = Math.max(44, bodyBottom - afterControlsY - 4);
			previewRect = new HudRect(rightX, afterControlsY + 4, rightW, remH);
		}

		int footerY = this.height - FOOTER_HEIGHT + 7;
		int btnW = Math.min(120, (this.width - 36) / 2);
		int gap = 8;
		int footerX = (this.width - (btnW * 2 + gap)) / 2;

		addRenderableWidget(
				Button.builder(t("customizablehud.button.save"), btn -> saveConfigNow())
						.bounds(footerX, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.save_now")))
						.build()
		);
		addRenderableWidget(
				Button.builder(t("customizablehud.button.back"), btn -> onClose())
						.bounds(footerX + btnW + gap, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.back_screen")))
						.build()
		);

		clampListScroll();
		syncSelectedControls();
	}

	private int initRightControls(int rx, int startY, int rw) {
		int y = startY + 14;

		toggleOverrideBtn = addRenderableWidget(
				Button.builder(Component.empty(), btn -> toggleSelectedOverride())
						.bounds(rx, y, rw, 20)
						.build()
		);
		y += 24;

		int paletteCount = ColorHexUtils.EFFECT_PALETTE.size();
		int cols = 5;
		int gap = 4;
		int swatchW = Math.max(20, (rw - gap * (cols - 1)) / cols);
		for (int i = 0; i < paletteCount; i++) {
			ColorHexUtils.PaletteColor pc = ColorHexUtils.EFFECT_PALETTE.get(i);
			int col = i % cols;
			int row = i / cols;
			int bx = rx + col * (swatchW + gap);
			int by = y + row * 20;
			int rgb = ColorHexUtils.parseRgb(pc.hex()).orElse(0xFFFFFF);
			Component label = Component.literal("██").withColor(rgb);
			addRenderableWidget(
					Button.builder(label, btn -> applyHexToSelected(pc.hex(), true))
							.bounds(bx, by, swatchW, 18)
							.tooltip(Tooltip.create(t("customizablehud.tooltip.effect_colors.swatch", s(pc.translationKey()), pc.hex())))
							.build()
			);
		}
		y += 42;

		int resetW = Math.max(66, Math.min(96, rw / 3));
		int applyW = Math.max(52, Math.min(72, rw / 4));
		int hexW = Math.max(60, rw - resetW - applyW - 8);

		hexEditBox = addRenderableWidget(new EditBox(this.font, rx, y, hexW, 18, t("customizablehud.effect_colors.hex_label")));
		hexEditBox.setMaxLength(7);
		hexEditBox.setFilter(text -> text.matches("#?[0-9a-fA-F]{0,6}"));
		hexEditBox.setTooltip(Tooltip.create(t("customizablehud.tooltip.effect_colors.hex")));
		hexEditBox.setResponder(text -> {
			if (suppressHexResponder) {
				return;
			}
			Optional<String> valid = ColorHexUtils.normalizeHex(text);
			hexInputInvalid = text != null && !text.isBlank() && valid.isEmpty();
			if (valid.isPresent()) {
				applyHexToSelected(valid.get(), true);
			}
		});

		applyHexBtn = addRenderableWidget(
				Button.builder(t("customizablehud.effect_colors.apply"), btn -> applyCurrentHexBox())
						.bounds(rx + hexW + 4, y, applyW, 18)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.effect_colors.apply_hex")))
						.build()
		);

		resetColorBtn = addRenderableWidget(
				Button.builder(t("customizablehud.effect_colors.reset"), btn -> resetSelectedOverride())
						.bounds(rx + hexW + applyW + 8, y, resetW, 18)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.effect_colors.reset")))
						.build()
		);

		return y + 20;
	}

	private EffectColorOverride currentOverride() {
		if (workingConfig.effects == null || workingConfig.effects.customEffectColors == null || selectedEffectId == null) {
			return null;
		}
		return workingConfig.effects.customEffectColors.get(selectedEffectId);
	}

	private String defaultHexForSelected() {
		EffectEntry sel = selectedEntry();
		return sel != null ? sel.autoHex() : "#E6EDF3";
	}

	private void toggleSelectedOverride() {
		if (selectedEffectId == null) {
			return;
		}
		EffectsConfig effects = workingConfig.effects;
		EffectColorOverride existing = effects.customEffectColors.get(selectedEffectId);
		if (existing == null) {
			String initialHex = ColorHexUtils.normalizeHex(hexEditBox != null ? hexEditBox.getValue() : null)
					.orElse(defaultHexForSelected());
			effects.customEffectColors.put(selectedEffectId, new EffectColorOverride(true, initialHex));
		} else {
			existing.enabled = !existing.enabled;
		}
		notifyConfigEdited();
		syncSelectedControls();
	}

	private void applyHexToSelected(String hex, boolean enableOverride) {
		if (selectedEffectId == null) {
			return;
		}
		Optional<String> normalized = ColorHexUtils.normalizeHex(hex);
		if (normalized.isEmpty()) {
			hexInputInvalid = true;
			return;
		}
		hexInputInvalid = false;
		EffectsConfig effects = workingConfig.effects;
		EffectColorOverride existing = effects.customEffectColors.get(selectedEffectId);
		if (existing == null) {
			effects.customEffectColors.put(selectedEffectId, new EffectColorOverride(enableOverride, normalized.get()));
		} else {
			existing.colorHex = normalized.get();
			if (enableOverride) {
				existing.enabled = true;
			}
		}
		notifyConfigEdited();
		syncSelectedControls();
	}

	private void applyCurrentHexBox() {
		if (hexEditBox == null) {
			return;
		}
		Optional<String> normalized = ColorHexUtils.normalizeHex(hexEditBox.getValue());
		if (normalized.isEmpty()) {
			hexInputInvalid = true;
			return;
		}
		applyHexToSelected(normalized.get(), true);
	}

	private void resetSelectedOverride() {
		if (selectedEffectId == null || workingConfig.effects == null) {
			return;
		}
		workingConfig.effects.customEffectColors.remove(selectedEffectId);
		hexInputInvalid = false;
		notifyConfigEdited();
		syncSelectedControls();
	}

	private void syncSelectedControls() {
		if (toggleOverrideBtn == null) {
			return;
		}
		EffectEntry sel = selectedEntry();
		EffectColorOverride ov = currentOverride();
		boolean hasOverride = ov != null;
		boolean enabled = ov != null && ov.enabled;

		Component toggleLabel;
		if (!hasOverride) {
			String autoHex = sel != null ? sel.autoHex() : "#E6EDF3";
			boolean nativeTint = sel != null && sel.hasNativeColor();
			toggleLabel = t(
					nativeTint ? "customizablehud.effect_colors.toggle_auto_native" : "customizablehud.effect_colors.toggle_auto_fallback",
					autoHex
			);
		} else if (enabled) {
			toggleLabel = t("customizablehud.effect_colors.toggle_on", ov.colorHex);
		} else {
			toggleLabel = t("customizablehud.effect_colors.toggle_off", ov.colorHex);
		}
		toggleOverrideBtn.setMessage(toggleLabel);
		toggleOverrideBtn.setTooltip(Tooltip.create(t(
				!hasOverride
						? "customizablehud.tooltip.effect_colors.toggle_auto"
						: (enabled ? "customizablehud.tooltip.effect_colors.toggle_on" : "customizablehud.tooltip.effect_colors.toggle_off")
		)));

		if (hexEditBox != null) {
			String nextVal = ov != null ? ov.colorHex : defaultHexForSelected();
			if (!hexEditBox.isFocused() && !nextVal.equals(hexEditBox.getValue())) {
				suppressHexResponder = true;
				hexEditBox.setValue(nextVal);
				suppressHexResponder = false;
				hexInputInvalid = false;
			}
		}
		if (resetColorBtn != null) {
			resetColorBtn.active = hasOverride;
		}
	}

	private EffectEntry selectedEntry() {
		for (EffectEntry entry : allEntries) {
			if (entry.id().equals(selectedEffectId)) {
				return entry;
			}
		}
		return null;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, HEADER_HEIGHT, this.width, this.height - FOOTER_HEIGHT, 0x66090D12);

		renderEffectList(graphics, mouseX, mouseY);

		if (!previewRect.isEmpty()) {
			graphics.fill(previewRect.x(), previewRect.y(), previewRect.right(), previewRect.bottom(), 0xAA0B1017);
			graphics.renderOutline(previewRect.x(), previewRect.y(), previewRect.width(), previewRect.height(), 0xFF334155);
			graphics.drawCenteredString(this.font, t("customizablehud.preview.title"), previewRect.centerX(), previewRect.y() + 4, 0xFF7FD4E6);
			EffectsHudRenderer.renderPreviewInBox(
					graphics,
					this.minecraft,
					workingConfig.effects,
					activeLang(),
					previewRect.x() + 4,
					previewRect.y() + 15,
					previewRect.width() - 8,
					Math.max(12, previewRect.height() - 18),
					workingConfig.effects.enabled ? 1.0f : 0.55f,
					selectedEffectId
			);
		}

		EffectEntry sel = selectedEntry();
		if (sel != null && toggleOverrideBtn != null) {
			int infoY = toggleOverrideBtn.getY() - 13;
			int sx = toggleOverrideBtn.getX();
			EffectColorOverride ov = currentOverride();
			int activeRgb = (ov != null && ov.enabled)
					? ColorHexUtils.parseRgb(ov.colorHex).orElse(sel.autoDisplayRgb())
					: sel.autoDisplayRgb();
			graphics.fill(sx, infoY, sx + 9, infoY + 9, 0xFF000000 | activeRgb);
			graphics.renderOutline(sx, infoY, 9, 9, (ov != null && ov.enabled) ? 0xFFFFFFFF : 0xFF94A3B8);

			String infoText = sel.displayName() + " (" + sel.id() + ")";
			int maxW = Math.max(40, toggleOverrideBtn.getWidth() - 14);
			if (this.font.width(infoText) > maxW) {
				infoText = this.font.plainSubstrByWidth(infoText, maxW - 8) + "...";
			}
			graphics.drawString(this.font, infoText, sx + 13, infoY + 1, 0xFFE2E8F0, false);
		}

		if (hexEditBox != null && hexInputInvalid) {
			graphics.renderOutline(hexEditBox.getX() - 1, hexEditBox.getY() - 1, hexEditBox.getWidth() + 2, hexEditBox.getHeight() + 2, 0xFFEF4444);
		} else if (hexEditBox != null && ColorHexUtils.isValidHex(hexEditBox.getValue())) {
			int validRgb = ColorHexUtils.parseRgb(hexEditBox.getValue()).orElse(0xFFFFFF);
			graphics.renderOutline(hexEditBox.getX() - 1, hexEditBox.getY() - 1, hexEditBox.getWidth() + 2, hexEditBox.getHeight() + 2, 0xFF000000 | validRgb);
		}

		renderHeaderAndFooterChrome(graphics, HEADER_HEIGHT, this.height - FOOTER_HEIGHT, t("customizablehud.screen.effect_colors.subtitle"));
		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void renderEffectList(GuiGraphics graphics, int mouseX, int mouseY) {
		if (listRect.isEmpty()) {
			return;
		}
		graphics.fill(listRect.x(), listRect.y(), listRect.right(), listRect.bottom(), 0xAA0B1017);
		graphics.renderOutline(listRect.x(), listRect.y(), listRect.width(), listRect.height(), 0xFF334155);

		if (filteredEntries.isEmpty()) {
			graphics.drawCenteredString(this.font, t("customizablehud.effect_colors.empty"), listRect.centerX(), listRect.y() + 12, 0xFF94A3B8);
			return;
		}

		graphics.enableScissor(listRect.x() + 1, listRect.y() + 1, listRect.right() - 1, listRect.bottom() - 1);
		try {
			for (int i = 0; i < filteredEntries.size(); i++) {
				EffectEntry entry = filteredEntries.get(i);
				int rowY = listRect.y() + 2 + i * LIST_ROW_HEIGHT - listScrollOffset;
				int rowBottom = rowY + LIST_ROW_HEIGHT - 2;
				if (rowBottom < listRect.y() || rowY > listRect.bottom()) {
					continue;
				}
				int rowW = listRect.width() - (maxListScroll() > 0 ? 10 : 4);
				int rowX = listRect.x() + 2;
				boolean selected = entry.id().equals(selectedEffectId);
				boolean hovered = mouseX >= rowX && mouseX <= rowX + rowW && mouseY >= rowY && mouseY <= rowBottom && listRect.contains(mouseX, mouseY);

				int rowBg = selected ? 0xCC1E3A5F : (hovered ? 0x881E293B : 0x55111827);
				graphics.fill(rowX, rowY, rowX + rowW, rowBottom, rowBg);
				if (selected) {
					graphics.renderOutline(rowX, rowY, rowW, LIST_ROW_HEIGHT - 2, 0xFF38BDF8);
				}

				int iconX = rowX + 3;
				int iconY = rowY + 3;
				drawSmallIcon(graphics, entry, iconX, iconY);

				int textX = iconX + 18;
				int swatchAreaW = 16;
				int maxTextW = Math.max(20, rowW - 24 - swatchAreaW);
				String title = entry.displayName();
				if (this.font.width(title) > maxTextW) {
					title = this.font.plainSubstrByWidth(title, maxTextW - 6) + "...";
				}
				graphics.drawString(this.font, title, textX, rowY + 2, 0xFFF1F5F9, false);

				String sub = entry.id() + (entry.registered() ? "" : " *");
				if (this.font.width(sub) > maxTextW) {
					sub = this.font.plainSubstrByWidth(sub, maxTextW - 6) + "...";
				}
				graphics.drawString(this.font, sub, textX, rowY + 12, 0xFF94A3B8, false);

				EffectColorOverride ov = workingConfig.effects != null && workingConfig.effects.customEffectColors != null
						? workingConfig.effects.customEffectColors.get(entry.id())
						: null;
				int sx = rowX + rowW - 13;
				int sy = rowY + 6;
				if (ov != null) {
					int rgb = ColorHexUtils.parseRgb(ov.colorHex).orElse(entry.autoDisplayRgb());
					if (ov.enabled) {
						graphics.fill(sx, sy, sx + 9, sy + 9, 0xFF000000 | rgb);
						graphics.renderOutline(sx, sy, 9, 9, 0xFFFFFFFF);
					} else {
						graphics.fill(sx, sy, sx + 9, sy + 9, 0xFF000000 | entry.autoDisplayRgb());
						graphics.renderOutline(sx, sy, 9, 9, 0xFF64748B);
					}
				} else {
					// Automatic Minecraft effect color indicator
					graphics.fill(sx, sy, sx + 9, sy + 9, 0xFF000000 | entry.autoDisplayRgb());
					graphics.renderOutline(sx, sy, 9, 9, 0xFF475569);
				}
			}
		} finally {
			graphics.disableScissor();
		}

		int maxScroll = maxListScroll();
		if (maxScroll > 0) {
			int trackX = listRect.right() - 6;
			graphics.fill(trackX, listRect.y() + 2, trackX + 4, listRect.bottom() - 2, 0x551A202C);
			int trackH = Math.max(10, listRect.height() - 4);
			int totalH = filteredEntries.size() * LIST_ROW_HEIGHT;
			int thumbH = Math.max(16, (int) ((listRect.height() / (float) Math.max(1, totalH)) * trackH));
			int thumbRange = Math.max(1, trackH - thumbH);
			int thumbY = listRect.y() + 2 + Math.round((listScrollOffset / (float) maxScroll) * thumbRange);
			graphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xCC7FD4E6);
		}
	}

	private static void drawSmallIcon(GuiGraphics graphics, EffectEntry entry, int x, int y) {
		if (entry.holder() != null && entry.holder().unwrapKey().isPresent()) {
			Identifier sprite = Gui.getMobEffectSprite(entry.holder());
			if (!MissingTextureAtlasSprite.getLocation().equals(sprite)) {
				graphics.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, 16, 16, ARGB.white(1.0f));
				return;
			}
		}
		graphics.fill(x + 2, y + 2, x + 14, y + 14, 0xFF000000 | (entry.autoDisplayRgb() & 0xFFFFFF));
		graphics.renderOutline(x + 2, y + 2, 12, 12, 0xFFFFFFFF);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (listRect.contains(mouseX, mouseY) && maxListScroll() > 0) {
			listScrollOffset = Mth.clamp((int) Math.round(listScrollOffset - scrollY * 18.0), 0, maxListScroll());
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		if (event.button() == 0 && listRect.contains(event.x(), event.y())) {
			if (maxListScroll() > 0 && event.x() >= listRect.right() - 9) {
				draggingListScrollbar = true;
				updateListScrollFromMouse(event.y());
				return true;
			}
			int relY = (int) Math.floor(event.y()) - (listRect.y() + 2) + listScrollOffset;
			int idx = relY / LIST_ROW_HEIGHT;
			if (idx >= 0 && idx < filteredEntries.size()) {
				if (searchBox != null) {
					searchBox.setFocused(false);
				}
				if (hexEditBox != null) {
					hexEditBox.setFocused(false);
				}
				selectedEffectId = filteredEntries.get(idx).id();
				hexInputInvalid = false;
				syncSelectedControls();
				return true;
			}
		}
		return super.mouseClicked(event, isDoubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingListScrollbar && event.button() == 0) {
			updateListScrollFromMouse(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0 && draggingListScrollbar) {
			draggingListScrollbar = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	private void updateListScrollFromMouse(double mouseY) {
		int max = maxListScroll();
		if (max <= 0) {
			return;
		}
		double frac = Mth.clamp((mouseY - listRect.y()) / (double) Math.max(1, listRect.height()), 0.0, 1.0);
		listScrollOffset = Mth.clamp((int) Math.round(frac * max), 0, max);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (hexEditBox != null && hexEditBox.isFocused() && (event.key() == 257 || event.key() == 335)) {
			applyCurrentHexBox();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		this.minecraft.setScreen(parent);
	}
}
