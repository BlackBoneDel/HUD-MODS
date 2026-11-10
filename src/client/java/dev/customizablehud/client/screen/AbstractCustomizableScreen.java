package dev.customizablehud.client.screen;

import dev.customizablehud.config.HudConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public abstract class AbstractCustomizableScreen extends Screen {
	protected final Screen parent;
	protected final HudConfig workingConfig;

	protected AbstractCustomizableScreen(Component title, Screen parent, HudConfig workingConfig) {
		super(title);
		this.parent = parent;
		this.workingConfig = workingConfig;
	}

	protected void renderHeaderAndFooterChrome(GuiGraphics graphics, int headerBottom, int footerTop, Component subtitle) {
		graphics.fill(0, 0, this.width, headerBottom, 0xCC0D1117);
		graphics.fill(0, headerBottom - 1, this.width, headerBottom, 0xFF2D3748);

		graphics.fill(0, footerTop, this.width, this.height, 0xCC0D1117);
		graphics.fill(0, footerTop, this.width, footerTop + 1, 0xFF2D3748);

		int titleY = subtitle != null ? 8 : 14;
		graphics.drawCenteredString(this.font, this.title, this.width / 2, titleY, 0xFFF7FAFC);
		if (subtitle != null) {
			graphics.drawCenteredString(this.font, subtitle, this.width / 2, 22, 0xFFA0AEC0);
		}
	}
}
