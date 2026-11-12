package dev.customizablehud.client.screen;

import dev.customizablehud.client.hud.EffectsHudRenderer;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.EffectSortMode;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.EffectsVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.LevelFormat;
import dev.customizablehud.config.PotionHudMode;
import dev.customizablehud.config.PotionMiniDisplayMode;
import dev.customizablehud.config.VanillaEffectsMode;
import dev.customizablehud.layout.HudRect;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EffectsConfigScreen extends AbstractScrollableConfigScreen {
	public EffectsConfigScreen(Screen parent, HudConfig workingConfig) {
		super("customizablehud.screen.effects.title", parent, workingConfig);
	}

	@Override
	protected boolean hasPreviewPane() {
		return true;
	}

	@Override
	protected Component subtitleText() {
		return t("customizablehud.screen.effects.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		EffectsConfig effects = workingConfig.effects;
		int panelW = contentPanelWidth();
		int leftX = contentLeftX();
		int halfW = (panelW - 6) / 2;
		int rightX = leftX + halfW + 6;
		int y = 4;

		if (this.width < 540 && this.height < 245) {
			addRenderableWidget(
					Button.builder(
							t(compactPreviewExpanded ? "customizablehud.preview.hide" : "customizablehud.preview.show"),
							btn -> {
								compactPreviewExpanded = !compactPreviewExpanded;
								this.rebuildWidgets();
							}
					).bounds(6, 4, 58, 15)
							.tooltip(Tooltip.create(t("customizablehud.tooltip.toggle_preview")))
							.build()
			);
		}

		addSectionLabel(t("customizablehud.section.general"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.enabled
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.enabled_on"
						: "customizablehud.tooltip.effects.enabled_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.enabled"), (btn, val) -> {
							effects.enabled = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((VanillaEffectsMode m) -> t(m.translationKey()), effects.vanillaEffectsMode)
						.withValues(List.of(VanillaEffectsMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.vanilla_mode"), (btn, val) -> {
							effects.vanillaEffectsMode = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((EffectsVisualStyle st) -> t(st.translationKey()), effects.style)
						.withValues(List.of(EffectsVisualStyle.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.style"), (btn, val) -> {
							effects.style = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((HudAnchor a) -> t(a.translationKey()), effects.anchor)
						.withValues(List.of(HudAnchor.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("customizablehud.option.anchor"), (btn, val) -> {
							effects.anchor = val;
							if (val != HudAnchor.CUSTOM) {
								effects.offsetX = 0;
								effects.offsetY = 0;
							}
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((HudOrientation o) -> t(o.translationKey()), effects.orientation)
						.withValues(List.of(HudOrientation.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.orientation"), (btn, val) -> {
							effects.orientation = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				new ScaleSliderButton(
						rightX,
						0,
						halfW,
						20,
						t("customizablehud.option.scale"),
						effects.scale,
						val -> {
							effects.scale = val;
							notifyConfigEdited();
						},
						val -> t("customizablehud.tooltip.effects.scale", String.format(Locale.ROOT, "%.2fx", val))
				),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				new IntSliderButton(
						leftX,
						0,
						panelW,
						20,
						t("customizablehud.option.spacing"),
						" px",
						ConfigValidator.MIN_SPACING,
						ConfigValidator.MAX_SPACING,
						effects.rowSpacing,
						val -> {
							effects.rowSpacing = val;
							notifyConfigEdited();
						},
						val -> t("customizablehud.tooltip.effects.spacing", val)
				),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.effects_appearance"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.showEffectBackground
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.show_background_on"
						: "customizablehud.tooltip.effects.show_background_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.effects.show_background"), (btn, val) -> {
							effects.showEffectBackground = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.showEffectBorder
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.show_border_on"
						: "customizablehud.tooltip.effects.show_border_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.show_border"), (btn, val) -> {
							effects.showEffectBorder = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				new IntSliderButton(
						leftX,
						0,
						panelW,
						20,
						t("customizablehud.effects.bg_opacity"),
						"%",
						ConfigValidator.MIN_OPACITY_PERCENT,
						ConfigValidator.MAX_OPACITY_PERCENT,
						effects.backgroundOpacityPercent,
						val -> {
							effects.backgroundOpacityPercent = val;
							notifyConfigEdited();
						},
						val -> t("customizablehud.tooltip.effects.bg_opacity", val)
				),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((PotionHudMode pm) -> t(pm.translationKey()), effects.potionHudMode)
						.withValues(List.of(PotionHudMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("customizablehud.effects.potion_mode"), (btn, val) -> {
							effects.potionHudMode = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((PotionMiniDisplayMode md) -> t(md.translationKey()), effects.potionMiniDisplay)
						.withValues(List.of(PotionMiniDisplayMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.mini_display"), (btn, val) -> {
							effects.potionMiniDisplay = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				Button.builder(
						t("customizablehud.button.effect_colors"),
						btn -> this.minecraft.setScreen(new EffectColorsScreen(this, workingConfig))
				).bounds(leftX, 0, panelW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.effect_colors")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.effects_elements"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.showIcon
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.show_icon_on"
						: "customizablehud.tooltip.effects.show_icon_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.show_icon"), (btn, val) -> {
							effects.showIcon = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.showName
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.show_name_on"
						: "customizablehud.tooltip.effects.show_name_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.show_name"), (btn, val) -> {
							effects.showName = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.showDuration
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.show_duration_on"
						: "customizablehud.tooltip.effects.show_duration_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.effects.show_duration"), (btn, val) -> {
							effects.showDuration = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.showLevel
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.show_level_on"
						: "customizablehud.tooltip.effects.show_level_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.show_level"), (btn, val) -> {
							effects.showLevel = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.hideLevelOne
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.hide_level_one_on"
						: "customizablehud.tooltip.effects.hide_level_one_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.effects.hide_level_one"), (btn, val) -> {
							effects.hideLevelOne = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((LevelFormat f) -> t(f.translationKey()), effects.levelFormat)
						.withValues(List.of(LevelFormat.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.level_format"), (btn, val) -> {
							effects.levelFormat = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.effects_behavior"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.showEffectsWithoutParticles
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.without_particles_on"
						: "customizablehud.tooltip.effects.without_particles_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.effects.without_particles"), (btn, val) -> {
							effects.showEffectsWithoutParticles = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.groupByEffectCategory
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.group_category_on"
						: "customizablehud.tooltip.effects.group_category_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.group_category"), (btn, val) -> {
							effects.groupByEffectCategory = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((EffectSortMode sm) -> t(sm.translationKey()), effects.sortMode)
						.withValues(List.of(EffectSortMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, panelW, 20, t("customizablehud.effects.sort_mode"), (btn, val) -> {
							effects.sortMode = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.fadeOnLowDuration
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.fade_low_on"
						: "customizablehud.tooltip.effects.fade_low_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.effects.fade_low"), (btn, val) -> {
							effects.fadeOnLowDuration = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						effects.colorDurationByRemaining
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.effects.color_duration_on"
						: "customizablehud.tooltip.effects.color_duration_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.effects.color_duration"), (btn, val) -> {
							effects.colorDurationByRemaining = val;
							notifyConfigEdited();
						}),
				y
		);

		syncScrollPositions();

		int footerY = this.height - FOOTER_HEIGHT + 7;
		int totalFooterW = Math.min(420, this.width - 20);
		int btnGap = 5;
		int btnW = (totalFooterW - btnGap * 3) / 4;
		int footerX = (this.width - (btnW * 4 + btnGap * 3)) / 2;

		addRenderableWidget(
				Button.builder(
						t("customizablehud.button.open_editor_short"),
						btn -> this.minecraft.setScreen(new HudEditorScreen(this, workingConfig, updated -> {
							syncWorkingFromManager(updated);
						}, HudEditorScreen.ModuleTarget.EFFECTS))
				).bounds(footerX, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.open_editor")))
						.build()
		);

		addRenderableWidget(
				Button.builder(
						t("customizablehud.button.reset_module"),
						btn -> {
							workingConfig.effects = new EffectsConfig();
							notifyConfigEdited();
							this.rebuildWidgets();
						}
				).bounds(footerX + (btnW + btnGap), footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.effects.reset_module")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("customizablehud.button.save"), btn -> saveConfigNow())
						.bounds(footerX + (btnW + btnGap) * 2, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.save_now")))
						.build()
		);

		addRenderableWidget(
				Button.builder(t("customizablehud.button.back"), btn -> onClose())
						.bounds(footerX + (btnW + btnGap) * 3, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.back_screen")))
						.build()
		);
	}

	@Override
	protected void renderPreviewPanel(GuiGraphics graphics, HudRect previewBox) {
		Component label = workingConfig.effects.enabled
				? t("customizablehud.preview.title")
				: t("customizablehud.preview.disabled");
		graphics.drawCenteredString(this.font, label, previewBox.centerX(), previewBox.y() + 4,
				workingConfig.effects.enabled ? 0xFF7FD4E6 : 0xFFFBBF24);

		int contentTop = previewBox.y() + 15;
		int contentH = Math.max(12, previewBox.height() - 18);
		float alpha = workingConfig.effects.enabled ? 1.0f : 0.55f;
		EffectsHudRenderer.renderPreviewInBox(
				graphics,
				this.minecraft,
				workingConfig.effects,
				activeLang(),
				previewBox.x() + 4,
				contentTop,
				previewBox.width() - 8,
				contentH,
				alpha,
				null
		);
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		this.minecraft.setScreen(parent);
	}
}
