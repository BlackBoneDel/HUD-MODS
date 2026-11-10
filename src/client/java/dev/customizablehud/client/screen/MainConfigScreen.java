package dev.customizablehud.client.screen;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.PresetCatalog;
import dev.customizablehud.config.PresetId;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

public final class MainConfigScreen extends AbstractScrollableConfigScreen {
	private PresetId selectedPreset = PresetId.VANILLA_COMPACT;

	public MainConfigScreen(Screen parent) {
		this(parent, CustomizableHudClient.getConfig().copy());
	}

	private MainConfigScreen(Screen parent, HudConfig workingConfig) {
		super(Component.translatable("customizablehud.screen.main.title"), parent, workingConfig);
	}

	@Override
	protected Component subtitleText() {
		return Component.translatable("customizablehud.screen.main.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		int panelW = Math.max(180, Math.min(360, this.width - 28));
		int leftX = (this.width - panelW) / 2;
		int halfW = (panelW - 6) / 2;
		int rightX = leftX + halfW + 6;
		int y = 4;

		addSectionLabel(Component.translatable("customizablehud.section.modules"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						workingConfig.armor.enabled
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.module.armor"), (btn, value) -> workingConfig.armor.enabled = value),
				y
		);
		addScrollWidget(
				Button.builder(
						Component.translatable("customizablehud.button.configure_armor"),
						btn -> this.minecraft.setScreen(new ArmorConfigScreen(this, workingConfig))
				).bounds(rightX, 0, halfW, 20).build(),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						workingConfig.effects.enabled
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.module.effects"), (btn, value) -> workingConfig.effects.enabled = value),
				y
		);
		addScrollWidget(
				Button.builder(
						Component.translatable("customizablehud.button.configure_effects"),
						btn -> this.minecraft.setScreen(new EffectsConfigScreen(this, workingConfig))
				).bounds(rightX, 0, halfW, 20).build(),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				Button.builder(
								Component.translatable("customizablehud.button.open_editor"),
								btn -> this.minecraft.setScreen(new HudEditorScreen(this, workingConfig.copy(), updated -> {
									workingConfig.armor = updated.armor.copy();
									workingConfig.effects = updated.effects.copy();
									workingConfig.editorSnapToGrid = updated.editorSnapToGrid;
									workingConfig.editorGridSize = updated.editorGridSize;
									workingConfig.editorShowGuides = updated.editorShowGuides;
								}))
						)
						.bounds(leftX, 0, panelW, 20)
						.tooltip(Tooltip.create(Component.translatable("customizablehud.tooltip.open_editor")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(Component.translatable("customizablehud.section.presets"), leftX, y);
		y += 16;

		int applyBtnW = 92;
		int presetW = panelW - applyBtnW - 6;
		addScrollWidget(
				CycleButton.builder((PresetId p) -> Component.translatable(p.translationKey()), () -> selectedPreset)
						.withValues(List.of(PresetId.values()))
						.create(leftX, 0, presetW, 20, Component.translatable("customizablehud.option.preset"), (btn, value) -> selectedPreset = value),
				y
		);
		addScrollWidget(
				Button.builder(
						Component.translatable("customizablehud.button.apply_preset"),
						btn -> {
							PresetCatalog.applyPreset(workingConfig, selectedPreset);
							this.rebuildWidgets();
						}
				).bounds(leftX + presetW + 6, 0, applyBtnW, 20)
						.tooltip(Tooltip.create(Component.translatable("customizablehud.tooltip.apply_preset")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(Component.translatable("customizablehud.section.visibility"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						workingConfig.visibility.hideInInventory
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.hide_inventory"), (btn, value) -> workingConfig.visibility.hideInInventory = value),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						workingConfig.visibility.hideInChat
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.option.hide_chat"), (btn, value) -> workingConfig.visibility.hideInChat = value),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						workingConfig.visibility.hideInPauseScreen
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.hide_pause"), (btn, value) -> workingConfig.visibility.hideInPauseScreen = value),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						workingConfig.editorShowGuides
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.option.editor_guides"), (btn, value) -> workingConfig.editorShowGuides = value),
				y
		);

		syncScrollPositions();

		int footerY = this.height - FOOTER_HEIGHT + 7;
		int btnGap = 6;
		int btnW = Math.min(116, (panelW - btnGap * 2) / 3);
		int totalFooterW = btnW * 3 + btnGap * 2;
		int footerX = (this.width - totalFooterW) / 2;

		addRenderableWidget(
				Button.builder(
						Component.translatable("customizablehud.button.reset_all"),
						btn -> {
							HudConfig defaults = HudConfig.createDefault();
							workingConfig.armor = defaults.armor;
							workingConfig.effects = defaults.effects;
							workingConfig.visibility = defaults.visibility;
							workingConfig.editorSnapToGrid = defaults.editorSnapToGrid;
							workingConfig.editorGridSize = defaults.editorGridSize;
							workingConfig.editorShowGuides = defaults.editorShowGuides;
							this.rebuildWidgets();
						}
				).bounds(footerX, footerY, btnW, 20).build()
		);

		addRenderableWidget(
				Button.builder(CommonComponents.GUI_CANCEL, btn -> onClose())
						.bounds(footerX + btnW + btnGap, footerY, btnW, 20)
						.build()
		);

		addRenderableWidget(
				Button.builder(Component.translatable("customizablehud.button.save_close"), btn -> saveAndClose())
						.bounds(footerX + (btnW + btnGap) * 2, footerY, btnW, 20)
						.build()
		);
	}

	private void saveAndClose() {
		CustomizableHudClient.getConfigManager().setConfig(workingConfig);
		CustomizableHudClient.getConfigManager().save();
		onClose();
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
