package dev.customizablehud.client.hud;

import dev.customizablehud.client.screen.AbstractCustomizableScreen;
import dev.customizablehud.client.screen.HudEditorScreen;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.VanillaEffectsMode;
import dev.customizablehud.config.VisibilityConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;

public final class VisibilityRules {
	private VisibilityRules() {
	}

	public static boolean shouldRenderLiveHud(Minecraft client, VisibilityConfig config) {
		if (client == null || config == null) {
			return false;
		}
		if (client.options.hideGui) {
			return false;
		}
		if (config.requireActiveWorld && (client.player == null || client.level == null)) {
			return false;
		}
		if (client.screen instanceof HudEditorScreen || client.screen instanceof AbstractCustomizableScreen) {
			return false;
		}
		if (config.hideInInventory && client.screen instanceof AbstractContainerScreen<?>) {
			return false;
		}
		if (config.hideInChat && client.screen instanceof ChatScreen) {
			return false;
		}
		if (config.hideInPauseScreen && client.screen instanceof PauseScreen) {
			return false;
		}
		return true;
	}

	public static boolean shouldRenderVanillaEffects(HudConfig config) {
		if (config == null || config.effects == null || !config.effects.enabled) {
			// Disabling our Effects HUD always leaves the vanilla HUD intact.
			return true;
		}
		return config.effects.vanillaEffectsMode != VanillaEffectsMode.CUSTOM_ONLY;
	}

	public static boolean shouldRenderCustomEffects(HudConfig config) {
		if (config == null || config.effects == null || !config.effects.enabled) {
			return false;
		}
		return config.effects.vanillaEffectsMode != VanillaEffectsMode.VANILLA_ONLY;
	}
}
