package dev.customizablehud.client.screen;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.client.i18n.ModI18n;
import dev.customizablehud.config.ConfigManager;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.ModLanguage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractCustomizableScreen extends Screen {
	protected final Screen parent;
	protected final HudConfig workingConfig;
	private final String titleKey;

	protected AbstractCustomizableScreen(String titleKey, Screen parent, HudConfig workingConfig) {
		super(ModI18n.text(workingConfig != null ? workingConfig.language : ModLanguage.ENGLISH, titleKey));
		this.titleKey = titleKey;
		this.parent = parent;
		this.workingConfig = workingConfig;
	}

	@Override
	public Component getTitle() {
		return ModI18n.text(activeLang(), titleKey);
	}

	protected ModLanguage activeLang() {
		return workingConfig != null && workingConfig.language != null
				? workingConfig.language
				: ModLanguage.ENGLISH;
	}

	protected Component t(String key, Object... args) {
		return ModI18n.text(activeLang(), key, args);
	}

	protected String s(String key, Object... args) {
		return ModI18n.string(activeLang(), key, args);
	}

	protected void notifyConfigEdited() {
		CustomizableHudClient.getConfigManager().applyAndScheduleSave(workingConfig);
	}

	protected boolean saveConfigNow() {
		ConfigManager manager = CustomizableHudClient.getConfigManager();
		boolean saved = manager.applyAndSaveNow(workingConfig);
		syncWorkingFromManager(manager.getConfig());
		return saved;
	}

	protected void flushPendingOnExit() {
		CustomizableHudClient.getConfigManager().flushPendingSaves();
	}

	protected void syncWorkingFromManager(HudConfig source) {
		if (source == null || workingConfig == null) {
			return;
		}
		workingConfig.schemaVersion = source.schemaVersion;
		workingConfig.language = source.language;
		workingConfig.armor = source.armor.copy();
		workingConfig.effects = source.effects.copy();
		workingConfig.visibility = source.visibility.copy();
		workingConfig.editorSnapToGrid = source.editorSnapToGrid;
		workingConfig.editorGridSize = source.editorGridSize;
		workingConfig.editorShowGuides = source.editorShowGuides;
	}

	protected void renderHeaderAndFooterChrome(GuiGraphics graphics, int headerBottom, int footerTop, Component subtitle) {
		graphics.fill(0, 0, this.width, headerBottom, 0xCC0D1117);
		graphics.fill(0, headerBottom - 1, this.width, headerBottom, 0xFF2D3748);

		graphics.fill(0, footerTop, this.width, this.height, 0xCC0D1117);
		graphics.fill(0, footerTop, this.width, footerTop + 1, 0xFF2D3748);

		Component currentTitle = getTitle();
		int titleY = subtitle != null ? 8 : 14;
		graphics.drawCenteredString(this.font, currentTitle, this.width / 2, titleY, 0xFFF7FAFC);
		if (subtitle != null) {
			graphics.drawCenteredString(this.font, subtitle, this.width / 2, 22, 0xFFA0AEC0);
		}

		renderSaveStatusBadge(graphics, 8, 6);
	}

	protected void renderSaveStatusBadge(GuiGraphics graphics, int rightMargin, int y) {
		ConfigManager manager = CustomizableHudClient.getConfigManager();
		ConfigManager.SaveStatus status = manager.getSaveStatus();
		Component badgeText = t(status.translationKey());
		int textColor = switch (status) {
			case SAVED -> 0xFF4ADE80;
			case UNSAVED -> 0xFFFBBF24;
			case FAILED -> 0xFFF87171;
		};
		int bgColor = switch (status) {
			case SAVED -> 0xAA064E3B;
			case UNSAVED -> 0xAA78350F;
			case FAILED -> 0xAA7F1D1D;
		};
		int borderColor = switch (status) {
			case SAVED -> 0xFF10B981;
			case UNSAVED -> 0xFFF59E0B;
			case FAILED -> 0xFFEF4444;
		};
		int badgeW = this.font.width(badgeText) + 10;
		int badgeX = Math.max(4, this.width - badgeW - rightMargin);
		if (badgeX <= this.width / 2 + 40 && this.width < 320) {
			return;
		}
		graphics.fill(badgeX, y, badgeX + badgeW, y + 13, bgColor);
		graphics.renderOutline(badgeX, y, badgeW, 13, borderColor);
		graphics.drawString(this.font, badgeText, badgeX + 5, y + 3, textColor, false);
	}
}
