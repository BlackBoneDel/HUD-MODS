package dev.customizablehud.client.screen;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.ModLanguage;
import dev.customizablehud.config.PresetCatalog;
import dev.customizablehud.config.PresetId;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class MainConfigScreen extends AbstractScrollableConfigScreen {
	private PresetId selectedPreset = PresetId.VANILLA_COMPACT;

	public MainConfigScreen(Screen parent) {
		this(parent, CustomizableHudClient.getConfigManager().getConfig().copy());
	}

	private MainConfigScreen(Screen parent, HudConfig workingConfig) {
		super("customizablehud.screen.main.title", parent, workingConfig);
	}

	@Override
	protected Component subtitleText() {
		return t("customizablehud.screen.main.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		int panelW = contentPanelWidth();
		int leftX = contentLeftX();
		int halfW = (panelW - 6) / 2;
		int rightX = leftX + halfW + 6;
		int y = 4;

		addSectionLabel(t("customizablehud.section.language"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.builder((ModLanguage lang) -> Component.literal(lang.displayName()), activeLang())
						.withValues(List.of(ModLanguage.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, panelW, 20, t("customizablehud.option.language"), (btn, value) -> {
							workingConfig.language = value;
							notifyConfigEdited();
							this.rebuildWidgets();
						}),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.modules"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						workingConfig.armor.enabled
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.module.armor_on"
						: "customizablehud.tooltip.module.armor_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.module.armor"), (btn, value) -> {
							workingConfig.armor.enabled = value;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				Button.builder(
						t("customizablehud.button.configure_armor"),
						btn -> this.minecraft.setScreen(new ArmorConfigScreen(this, workingConfig))
				).bounds(rightX, 0, halfW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.configure_armor")))
						.build(),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						workingConfig.effects.enabled
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.module.effects_on"
						: "customizablehud.tooltip.module.effects_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.module.effects"), (btn, value) -> {
							workingConfig.effects.enabled = value;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				Button.builder(
						t("customizablehud.button.configure_effects"),
						btn -> this.minecraft.setScreen(new EffectsConfigScreen(this, workingConfig))
				).bounds(rightX, 0, halfW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.configure_effects")))
						.build(),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				Button.builder(
								t("customizablehud.button.open_editor"),
								btn -> this.minecraft.setScreen(new HudEditorScreen(this, workingConfig, updated -> {
									syncWorkingFromManager(updated);
								}))
						)
						.bounds(leftX, 0, panelW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.open_editor")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.presets"), leftX, y);
		y += 16;

		int applyBtnW = 92;
		int presetW = panelW - applyBtnW - 6;
		addScrollWidget(
				CycleButton.builder((PresetId p) -> t(p.translationKey()), selectedPreset)
						.withValues(List.of(PresetId.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, presetW, 20, t("customizablehud.option.preset"), (btn, value) -> selectedPreset = value),
				y
		);
		addScrollWidget(
				Button.builder(
						t("customizablehud.button.apply_preset"),
						btn -> {
							PresetCatalog.applyPreset(workingConfig, selectedPreset);
							notifyConfigEdited();
							this.rebuildWidgets();
						}
				).bounds(leftX + presetW + 6, 0, applyBtnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.apply_preset")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.visibility"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						workingConfig.visibility.hideInInventory
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.hide_inventory_on"
						: "customizablehud.tooltip.hide_inventory_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.hide_inventory"), (btn, value) -> {
							workingConfig.visibility.hideInInventory = value;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						workingConfig.visibility.hideInChat
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.hide_chat_on"
						: "customizablehud.tooltip.hide_chat_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.option.hide_chat"), (btn, value) -> {
							workingConfig.visibility.hideInChat = value;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						workingConfig.visibility.hideInPauseScreen
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.hide_pause_on"
						: "customizablehud.tooltip.hide_pause_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.hide_pause"), (btn, value) -> {
							workingConfig.visibility.hideInPauseScreen = value;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						workingConfig.editorShowGuides
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.editor_guides_on"
						: "customizablehud.tooltip.editor_guides_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.option.editor_guides"), (btn, value) -> {
							workingConfig.editorShowGuides = value;
							notifyConfigEdited();
						}),
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
						t("customizablehud.button.reset_all"),
						btn -> {
							HudConfig defaults = HudConfig.createDefault();
							syncWorkingFromManager(defaults);
							notifyConfigEdited();
							this.rebuildWidgets();
						}
				).bounds(footerX, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.reset_all")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("customizablehud.button.save"), btn -> saveConfigNow())
						.bounds(footerX + btnW + btnGap, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.save_now")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("customizablehud.button.close"), btn -> onClose())
						.bounds(footerX + (btnW + btnGap) * 2, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.close_screen")))
						.build()
		);
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		this.minecraft.setScreen(parent);
	}
}
