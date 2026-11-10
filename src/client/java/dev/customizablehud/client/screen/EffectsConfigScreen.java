package dev.customizablehud.client.screen;

import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.EffectSortMode;
import dev.customizablehud.config.EffectsConfig;
import dev.customizablehud.config.EffectsVisualStyle;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.config.LevelFormat;
import dev.customizablehud.config.VanillaEffectsMode;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class EffectsConfigScreen extends AbstractScrollableConfigScreen {
	public EffectsConfigScreen(Screen parent, HudConfig workingConfig) {
		super(Component.translatable("customizablehud.screen.effects.title"), parent, workingConfig);
	}

	@Override
	protected Component subtitleText() {
		return Component.translatable("customizablehud.screen.effects.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		EffectsConfig effects = workingConfig.effects;
		int panelW = Math.max(180, Math.min(380, this.width - 28));
		int leftX = (this.width - panelW) / 2;
		int halfW = (panelW - 6) / 2;
		int rightX = leftX + halfW + 6;
		int y = 4;

		addSectionLabel(Component.translatable("customizablehud.section.general"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.enabled
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.enabled"), (btn, val) -> effects.enabled = val),
				y
		);
		addScrollWidget(
				CycleButton.builder((VanillaEffectsMode m) -> Component.translatable(m.translationKey()), effects.vanillaEffectsMode)
						.withValues(List.of(VanillaEffectsMode.values()))
						.withTooltip(val -> Tooltip.create(Component.translatable("customizablehud.tooltip.vanilla_mode")))
						.create(rightX, 0, halfW, 20, Component.translatable("customizablehud.effects.vanilla_mode"), (btn, val) -> effects.vanillaEffectsMode = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((EffectsVisualStyle s) -> Component.translatable(s.translationKey()), effects.style)
						.withValues(List.of(EffectsVisualStyle.values()))
						.create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.style"), (btn, val) -> effects.style = val),
				y
		);
		addScrollWidget(
				CycleButton.builder((HudAnchor a) -> Component.translatable(a.translationKey()), effects.anchor)
						.withValues(List.of(HudAnchor.values()))
						.create(rightX, 0, halfW, 20, Component.translatable("customizablehud.option.anchor"), (btn, val) -> {
							effects.anchor = val;
							if (val != HudAnchor.CUSTOM) {
								effects.offsetX = 0;
								effects.offsetY = 0;
							}
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((HudOrientation o) -> Component.translatable(o.translationKey()), effects.orientation)
						.withValues(List.of(HudOrientation.values()))
						.create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.orientation"), (btn, val) -> effects.orientation = val),
				y
		);
		addScrollWidget(
				new ScaleSliderButton(
						rightX,
						0,
						halfW,
						20,
						Component.translatable("customizablehud.option.scale"),
						effects.scale,
						val -> effects.scale = val
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
						Component.translatable("customizablehud.option.spacing"),
						" px",
						ConfigValidator.MIN_SPACING,
						ConfigValidator.MAX_SPACING,
						effects.rowSpacing,
						val -> effects.rowSpacing = val
				),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(Component.translatable("customizablehud.section.effects_elements"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.showIcon
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.show_icon"), (btn, val) -> effects.showIcon = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.showName
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.effects.show_name"), (btn, val) -> effects.showName = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.showDuration
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.effects.show_duration"), (btn, val) -> effects.showDuration = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.showLevel
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.effects.show_level"), (btn, val) -> effects.showLevel = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.hideLevelOne
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.effects.hide_level_one"), (btn, val) -> effects.hideLevelOne = val),
				y
		);
		addScrollWidget(
				CycleButton.builder((LevelFormat f) -> Component.translatable(f.translationKey()), effects.levelFormat)
						.withValues(List.of(LevelFormat.values()))
						.create(rightX, 0, halfW, 20, Component.translatable("customizablehud.effects.level_format"), (btn, val) -> effects.levelFormat = val),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(Component.translatable("customizablehud.section.effects_behavior"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.showEffectsWithoutParticles
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.effects.without_particles"), (btn, val) -> effects.showEffectsWithoutParticles = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.groupByEffectCategory
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.effects.group_category"), (btn, val) -> effects.groupByEffectCategory = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((EffectSortMode s) -> Component.translatable(s.translationKey()), effects.sortMode)
						.withValues(List.of(EffectSortMode.values()))
						.create(leftX, 0, panelW, 20, Component.translatable("customizablehud.effects.sort_mode"), (btn, val) -> effects.sortMode = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.fadeOnLowDuration
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.effects.fade_low"), (btn, val) -> effects.fadeOnLowDuration = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						effects.colorDurationByRemaining
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.effects.color_duration"), (btn, val) -> effects.colorDurationByRemaining = val),
				y
		);

		syncScrollPositions();

		int footerY = this.height - FOOTER_HEIGHT + 7;
		int btnGap = 6;
		int btnW = Math.min(120, (panelW - btnGap * 2) / 3);
		int totalFooterW = btnW * 3 + btnGap * 2;
		int footerX = (this.width - totalFooterW) / 2;

		addRenderableWidget(
				Button.builder(
						Component.translatable("customizablehud.button.open_editor"),
						btn -> this.minecraft.setScreen(new HudEditorScreen(this, workingConfig.copy(), updated -> {
							workingConfig.armor = updated.armor.copy();
							workingConfig.effects = updated.effects.copy();
							workingConfig.editorSnapToGrid = updated.editorSnapToGrid;
							workingConfig.editorGridSize = updated.editorGridSize;
							workingConfig.editorShowGuides = updated.editorShowGuides;
						}, HudEditorScreen.ModuleTarget.EFFECTS))
				).bounds(footerX, footerY, btnW, 20).build()
		);

		addRenderableWidget(
				Button.builder(
						Component.translatable("customizablehud.button.reset_module"),
						btn -> {
							workingConfig.effects = new EffectsConfig();
							this.rebuildWidgets();
						}
				).bounds(footerX + btnW + btnGap, footerY, btnW, 20).build()
		);

		addRenderableWidget(
				Button.builder(Component.translatable("customizablehud.button.back"), btn -> onClose())
						.bounds(footerX + (btnW + btnGap) * 2, footerY, btnW, 20)
						.build()
		);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
