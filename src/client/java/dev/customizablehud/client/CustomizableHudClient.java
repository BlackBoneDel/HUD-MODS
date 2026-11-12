package dev.customizablehud.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.customizablehud.client.hud.ArmorHudRenderer;
import dev.customizablehud.client.hud.EffectsHudRenderer;
import dev.customizablehud.client.hud.LiveDataCollector;
import dev.customizablehud.client.hud.VisibilityRules;
import dev.customizablehud.client.i18n.ModI18n;
import dev.customizablehud.client.screen.HudEditorScreen;
import dev.customizablehud.client.screen.MainConfigScreen;
import dev.customizablehud.config.ConfigManager;
import dev.customizablehud.config.HudConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import org.lwjgl.glfw.GLFW;

public final class CustomizableHudClient implements ClientModInitializer {
	public static final String MOD_ID = "customizablehud";

	private static ConfigManager configManager;

	private static KeyMapping openConfigKey;
	private static KeyMapping openEditorKey;
	private static KeyMapping toggleArmorKey;
	private static KeyMapping toggleEffectsKey;

	public static synchronized ConfigManager getConfigManager() {
		if (configManager == null) {
			configManager = new ConfigManager(FabricLoader.getInstance().getConfigDir().resolve("customizablehud.json"));
			configManager.load();
		}
		return configManager;
	}

	public static HudConfig getConfig() {
		return getConfigManager().getConfig();
	}

	@Override
	public void onInitializeClient() {
		getConfigManager();

		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloader(
				Identifier.fromNamespaceAndPath(MOD_ID, "i18n_reloader"),
				(ResourceManagerReloadListener) ModI18n::reloadFromResources
		);

		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> getConfigManager().flushPendingSaves());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
			LiveDataCollector.clearLiveEffectTracking();
			getConfigManager().flushPendingSaves();
		});

		KeyMapping.Category keyCategory = KeyMapping.Category.register(
				Identifier.fromNamespaceAndPath(MOD_ID, "general")
		);

		openConfigKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.customizablehud.open_config",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_O,
				keyCategory
		));

		openEditorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.customizablehud.open_editor",
				InputConstants.Type.KEYSYM,
				GLFW.GLFW_KEY_H,
				keyCategory
		));

		toggleArmorKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.customizablehud.toggle_armor",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				keyCategory
		));

		toggleEffectsKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
				"key.customizablehud.toggle_effects",
				InputConstants.Type.KEYSYM,
				InputConstants.UNKNOWN.getValue(),
				keyCategory
		));

		ClientTickEvents.END_CLIENT_TICK.register(this::onEndClientTick);

		HudElementRegistry.replaceElement(
				VanillaHudElements.STATUS_EFFECTS,
				vanilla -> (graphics, tickCounter) -> {
					if (VisibilityRules.shouldRenderVanillaEffects(getConfig())) {
						vanilla.render(graphics, tickCounter);
					}
				}
		);

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.HOTBAR,
				Identifier.fromNamespaceAndPath(MOD_ID, "armor_hud"),
				ArmorHudRenderer::renderLive
		);

		HudElementRegistry.attachElementAfter(
				VanillaHudElements.STATUS_EFFECTS,
				Identifier.fromNamespaceAndPath(MOD_ID, "effects_hud"),
				EffectsHudRenderer::renderLive
		);
	}

	private void onEndClientTick(Minecraft client) {
		if (!(client.screen instanceof HudEditorScreen editor && editor.isInteracting())) {
			getConfigManager().tickAutoSave();
		}

		if (client.player != null && getConfigManager().consumeRecoveryNotice()) {
			client.player.displayClientMessage(
					ModI18n.text("customizablehud.message.config_recovered"),
					false
			);
		}

		// Never process mod hotkeys while any GUI/text screen is open.
		if (client.screen != null) {
			drainClicks();
			return;
		}

		while (openConfigKey.consumeClick()) {
			client.setScreen(new MainConfigScreen(null));
			return;
		}

		while (openEditorKey.consumeClick()) {
			client.setScreen(new HudEditorScreen(null, getConfig().copy(), null));
			return;
		}

		while (toggleArmorKey.consumeClick()) {
			HudConfig cfg = getConfig();
			cfg.armor.enabled = !cfg.armor.enabled;
			getConfigManager().markDirty();
			getConfigManager().saveNow();
			if (client.player != null) {
				client.player.displayClientMessage(
						ModI18n.text(cfg.armor.enabled
								? "customizablehud.message.armor_enabled"
								: "customizablehud.message.armor_disabled"),
						true
				);
			}
		}

		while (toggleEffectsKey.consumeClick()) {
			HudConfig cfg = getConfig();
			cfg.effects.enabled = !cfg.effects.enabled;
			getConfigManager().markDirty();
			getConfigManager().saveNow();
			if (client.player != null) {
				client.player.displayClientMessage(
						ModI18n.text(cfg.effects.enabled
								? "customizablehud.message.effects_enabled"
								: "customizablehud.message.effects_disabled"),
						true
				);
			}
		}
	}

	private static void drainClicks() {
		while (openConfigKey.consumeClick()) {
			// drained
		}
		while (openEditorKey.consumeClick()) {
			// drained
		}
		while (toggleArmorKey.consumeClick()) {
			// drained
		}
		while (toggleEffectsKey.consumeClick()) {
			// drained
		}
	}
}
