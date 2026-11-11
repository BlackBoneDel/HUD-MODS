package dev.customizablehud.client.screen;

import dev.customizablehud.client.hud.ArmorHudRenderer;
import dev.customizablehud.config.ArmorBorderStyle;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorMetricPosition;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.ColorHexUtils;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import dev.customizablehud.layout.HudRect;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArmorConfigScreen extends AbstractScrollableConfigScreen {
	private IntSliderButton warningSlider;
	private IntSliderButton criticalSlider;

	public ArmorConfigScreen(Screen parent, HudConfig workingConfig) {
		super("customizablehud.screen.armor.title", parent, workingConfig);
	}

	@Override
	protected boolean hasPreviewPane() {
		return true;
	}

	@Override
	protected Component subtitleText() {
		return t("customizablehud.screen.armor.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		ArmorConfig armor = workingConfig.armor;
		int panelW = contentPanelWidth();
		int leftX = contentLeftX();
		int halfW = (panelW - 6) / 2;
		int rightX = leftX + halfW + 6;
		int thirdW = (panelW - 12) / 3;
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
						armor.enabled
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.enabled_on"
						: "customizablehud.tooltip.armor.enabled_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.enabled"), (btn, val) -> {
							armor.enabled = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((ArmorVisualStyle st) -> t(st.translationKey()), armor.style)
						.withValues(List.of(ArmorVisualStyle.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("customizablehud.option.style"), (btn, val) -> {
							armor.style = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((ArmorBorderStyle bs) -> t(bs.translationKey()), armor.borderStyle)
						.withValues(List.of(ArmorBorderStyle.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("customizablehud.armor.border_style"), (btn, val) -> {
							armor.borderStyle = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((ArmorMetricPosition mp) -> t(mp.translationKey()), armor.metricPosition)
						.withValues(List.of(ArmorMetricPosition.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("customizablehud.armor.metric_pos"), (btn, val) -> {
							armor.metricPosition = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((HudAnchor a) -> t(a.translationKey()), armor.anchor)
						.withValues(List.of(HudAnchor.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.anchor"), (btn, val) -> {
							armor.anchor = val;
							if (val != HudAnchor.CUSTOM) {
								armor.offsetX = 0;
								armor.offsetY = 0;
							}
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((HudOrientation o) -> t(o.translationKey()), armor.orientation)
						.withValues(List.of(HudOrientation.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(rightX, 0, halfW, 20, t("customizablehud.option.orientation"), (btn, val) -> {
							armor.orientation = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.hideEmptySlots
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.hide_empty_on"
						: "customizablehud.tooltip.armor.hide_empty_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.armor.hide_empty"), (btn, val) -> {
							armor.hideEmptySlots = val;
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
						armor.scale,
						val -> {
							armor.scale = val;
							notifyConfigEdited();
						},
						val -> t("customizablehud.tooltip.armor.scale", String.format(Locale.ROOT, "%.2fx", val))
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
						ConfigValidator.MIN_ARMOR_SPACING,
						ConfigValidator.MAX_SPACING,
						armor.slotSpacing,
						val -> {
							armor.slotSpacing = val;
							notifyConfigEdited();
						},
						val -> t("customizablehud.tooltip.armor.spacing", val)
				),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.armor_display"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.showIcon
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.show_icon_on"
						: "customizablehud.tooltip.armor.show_icon_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.option.show_icon"), (btn, val) -> {
							armor.showIcon = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.showDurabilityBar
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.show_bar_on"
						: "customizablehud.tooltip.armor.show_bar_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.armor.show_bar"), (btn, val) -> {
							armor.showDurabilityBar = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((DurabilityDisplayMode m) -> t(m.translationKey()), armor.durabilityDisplayMode)
						.withValues(List.of(DurabilityDisplayMode.values()))
						.withTooltip(val -> Tooltip.create(t(val.tooltipKey())))
						.create(leftX, 0, halfW, 20, t("customizablehud.armor.durability_mode"), (btn, val) -> {
							armor.durabilityDisplayMode = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.showMaxDurability
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.show_max_on"
						: "customizablehud.tooltip.armor.show_max_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.armor.show_max"), (btn, val) -> {
							armor.showMaxDurability = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.showItemName
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.show_name_on"
						: "customizablehud.tooltip.armor.show_name_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.armor.show_name"), (btn, val) -> {
							armor.showItemName = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.colorByDurability
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.color_range_on"
						: "customizablehud.tooltip.armor.color_range_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.armor.color_range"), (btn, val) -> {
							armor.colorByDurability = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		warningSlider = addScrollWidget(
				new IntSliderButton(
						leftX,
						0,
						halfW,
						20,
						t("customizablehud.armor.warning_threshold"),
						"%",
						ConfigValidator.MIN_WARNING_THRESHOLD,
						ConfigValidator.MAX_WARNING_THRESHOLD,
						armor.warningThresholdPercent,
						val -> {
							armor.warningThresholdPercent = val;
							if (armor.criticalThresholdPercent >= val) {
								armor.criticalThresholdPercent = Math.max(ConfigValidator.MIN_CRITICAL_THRESHOLD, val - 1);
								if (criticalSlider != null) {
									criticalSlider.setIntValue(armor.criticalThresholdPercent);
								}
							}
							notifyConfigEdited();
						},
						val -> t("customizablehud.tooltip.armor.warning_threshold", val, armor.criticalThresholdPercent)
				),
				y
		);
		criticalSlider = addScrollWidget(
				new IntSliderButton(
						rightX,
						0,
						halfW,
						20,
						t("customizablehud.armor.critical_threshold"),
						"%",
						ConfigValidator.MIN_CRITICAL_THRESHOLD,
						ConfigValidator.MAX_CRITICAL_THRESHOLD,
						armor.criticalThresholdPercent,
						val -> {
							armor.criticalThresholdPercent = val;
							if (armor.warningThresholdPercent <= val) {
								armor.warningThresholdPercent = Math.min(ConfigValidator.MAX_WARNING_THRESHOLD, val + 1);
								if (warningSlider != null) {
									warningSlider.setIntValue(armor.warningThresholdPercent);
								}
							}
							notifyConfigEdited();
						},
						val -> t("customizablehud.tooltip.armor.critical_threshold", val, armor.warningThresholdPercent)
				),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				Button.builder(buildColorLabel("customizablehud.armor.color_healthy", armor.healthyColorHex, 0x6EEB83), btn -> {
							armor.healthyColorHex = ColorHexUtils.nextPresetColor(armor.healthyColorHex, ColorHexUtils.HEALTHY_COLOR_PRESETS);
							btn.setMessage(buildColorLabel("customizablehud.armor.color_healthy", armor.healthyColorHex, 0x6EEB83));
							notifyConfigEdited();
						})
						.bounds(leftX, 0, thirdW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.color_healthy")))
						.build(),
				y
		);
		addScrollWidget(
				Button.builder(buildColorLabel("customizablehud.armor.color_warning", armor.warningColorHex, 0xFFE066), btn -> {
							armor.warningColorHex = ColorHexUtils.nextPresetColor(armor.warningColorHex, ColorHexUtils.WARNING_COLOR_PRESETS);
							btn.setMessage(buildColorLabel("customizablehud.armor.color_warning", armor.warningColorHex, 0xFFE066));
							notifyConfigEdited();
						})
						.bounds(leftX + thirdW + 6, 0, thirdW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.color_warning")))
						.build(),
				y
		);
		addScrollWidget(
				Button.builder(buildColorLabel("customizablehud.armor.color_critical", armor.criticalColorHex, 0xFF5555), btn -> {
							armor.criticalColorHex = ColorHexUtils.nextPresetColor(armor.criticalColorHex, ColorHexUtils.CRITICAL_COLOR_PRESETS);
							btn.setMessage(buildColorLabel("customizablehud.armor.color_critical", armor.criticalColorHex, 0xFF5555));
							notifyConfigEdited();
						})
						.bounds(leftX + (thirdW + 6) * 2, 0, thirdW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.color_critical")))
						.build(),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.lowDurabilityWarning
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.low_warning_on"
						: "customizablehud.tooltip.armor.low_warning_off")))
						.create(leftX, 0, panelW, 20, t("customizablehud.armor.low_warning"), (btn, val) -> {
							armor.lowDurabilityWarning = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.hands"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.showMainHand
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.main_hand_on"
						: "customizablehud.tooltip.armor.main_hand_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.armor.main_hand"), (btn, val) -> {
							armor.showMainHand = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.showOffHand
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.off_hand_on"
						: "customizablehud.tooltip.armor.off_hand_off")))
						.create(rightX, 0, halfW, 20, t("customizablehud.armor.off_hand"), (btn, val) -> {
							armor.showOffHand = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.showHandItemName
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.hand_name_on"
						: "customizablehud.tooltip.armor.hand_name_off")))
						.create(leftX, 0, panelW, 20, t("customizablehud.armor.hand_name"), (btn, val) -> {
							armor.showHandItemName = val;
							notifyConfigEdited();
						}),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(t("customizablehud.section.armor_order"), leftX, y);
		y += 16;

		addScrollWidget(
				Button.builder(buildOrderText(armor), btn -> {
							Collections.reverse(armor.armorOrder);
							btn.setMessage(buildOrderText(armor));
							notifyConfigEdited();
						})
						.bounds(leftX, 0, panelW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.reverse_order")))
						.build(),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				Button.builder(t("customizablehud.armor.swap_1_2"), btn -> {
					swapArmorPair(armor, 0, 1);
					notifyConfigEdited();
					this.rebuildWidgets();
				}).bounds(leftX, 0, thirdW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.swap_pair")))
						.build(),
				y
		);
		addScrollWidget(
				Button.builder(t("customizablehud.armor.swap_2_3"), btn -> {
					swapArmorPair(armor, 1, 2);
					notifyConfigEdited();
					this.rebuildWidgets();
				}).bounds(leftX + thirdW + 6, 0, thirdW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.swap_pair")))
						.build(),
				y
		);
		addScrollWidget(
				Button.builder(t("customizablehud.armor.swap_3_4"), btn -> {
					swapArmorPair(armor, 2, 3);
					notifyConfigEdited();
					this.rebuildWidgets();
				}).bounds(leftX + (thirdW + 6) * 2, 0, thirdW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.swap_pair")))
						.build(),
				y
		);
		y += ROW_HEIGHT + 4;

		addScrollWidget(
				CycleButton.booleanBuilder(
						t("customizablehud.state.on"),
						t("customizablehud.state.off"),
						armor.individualPositionsEnabled
				).withTooltip(val -> Tooltip.create(t(val
						? "customizablehud.tooltip.armor.individual_on"
						: "customizablehud.tooltip.armor.individual_off")))
						.create(leftX, 0, halfW, 20, t("customizablehud.armor.individual_mode"), (btn, val) -> {
							if (val && !armor.individualPositionsEnabled) {
								captureIndividualFromAuto(armor);
							}
							armor.individualPositionsEnabled = val;
							notifyConfigEdited();
						}),
				y
		);
		addScrollWidget(
				Button.builder(t("customizablehud.armor.reset_individual"), btn -> {
					armor.individualPositionsEnabled = false;
					armor.anchor = HudAnchor.HOTBAR_LEFT;
					armor.offsetX = 0;
					armor.offsetY = 0;
					armor.individualOffsets = ArmorConfig.createDefaultIndividualOffsets();
					notifyConfigEdited();
					this.rebuildWidgets();
				}).bounds(rightX, 0, halfW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.reset_individual")))
						.build(),
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
						}, HudEditorScreen.ModuleTarget.ARMOR))
				).bounds(footerX, footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.open_editor")))
						.build()
		);

		addRenderableWidget(
				Button.builder(
						t("customizablehud.button.reset_module"),
						btn -> {
							workingConfig.armor = new ArmorConfig();
							notifyConfigEdited();
							this.rebuildWidgets();
						}
				).bounds(footerX + (btnW + btnGap), footerY, btnW, 20)
						.tooltip(Tooltip.create(t("customizablehud.tooltip.armor.reset_module")))
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
		Component label = workingConfig.armor.enabled
				? t("customizablehud.preview.title")
				: t("customizablehud.preview.disabled");
		graphics.drawCenteredString(this.font, label, previewBox.centerX(), previewBox.y() + 4,
				workingConfig.armor.enabled ? 0xFF7FD4E6 : 0xFFFBBF24);

		int contentTop = previewBox.y() + 15;
		int contentH = Math.max(12, previewBox.height() - 18);
		float alpha = workingConfig.armor.enabled ? 1.0f : 0.55f;
		ArmorHudRenderer.renderPreviewInBox(
				graphics,
				this.minecraft,
				workingConfig.armor,
				activeLang(),
				previewBox.x() + 4,
				contentTop,
				previewBox.width() - 8,
				contentH,
				alpha
		);
	}

	private Component buildColorLabel(String labelKey, String hex, int fallbackRgb) {
		int rgb = ColorHexUtils.parseRgb(hex).orElse(fallbackRgb);
		return Component.literal("■ ").withColor(rgb).append(t(labelKey));
	}

	private void captureIndividualFromAuto(ArmorConfig armor) {
		ArmorHudRenderer.captureIndividualPositionsFromAuto(this.minecraft, armor, activeLang(), this.width, this.height);
	}

	private static void swapArmorPair(ArmorConfig armor, int i, int j) {
		if (armor.armorOrder != null && armor.armorOrder.size() == 4) {
			Collections.swap(armor.armorOrder, i, j);
		}
	}

	private Component buildOrderText(ArmorConfig armor) {
		StringBuilder sb = new StringBuilder();
		List<ArmorSlotKind> order = armor.armorOrder != null && armor.armorOrder.size() == 4
				? armor.armorOrder
				: ArmorSlotKind.DEFAULT_ARMOR_ORDER;
		for (int i = 0; i < order.size(); i++) {
			if (i > 0) {
				sb.append(" → ");
			}
			sb.append(s(order.get(i).translationKey()));
		}
		return t("customizablehud.armor.order_display", sb.toString());
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		this.minecraft.setScreen(parent);
	}
}
