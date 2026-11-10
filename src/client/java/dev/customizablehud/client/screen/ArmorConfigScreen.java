package dev.customizablehud.client.screen;

import dev.customizablehud.client.hud.ArmorHudRenderer;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ArmorVisualStyle;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.DurabilityDisplayMode;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.HudOrientation;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class ArmorConfigScreen extends AbstractScrollableConfigScreen {
	public ArmorConfigScreen(Screen parent, HudConfig workingConfig) {
		super(Component.translatable("customizablehud.screen.armor.title"), parent, workingConfig);
	}

	@Override
	protected Component subtitleText() {
		return Component.translatable("customizablehud.screen.armor.subtitle");
	}

	@Override
	protected void init() {
		resetScrollContainer();

		ArmorConfig armor = workingConfig.armor;
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
						armor.enabled
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.enabled"), (btn, val) -> armor.enabled = val),
				y
		);
		addScrollWidget(
				CycleButton.builder((ArmorVisualStyle s) -> Component.translatable(s.translationKey()), () -> armor.style)
						.withValues(List.of(ArmorVisualStyle.values()))
						.create(rightX, 0, halfW, 20, Component.translatable("customizablehud.option.style"), (btn, val) -> armor.style = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((HudAnchor a) -> Component.translatable(a.translationKey()), () -> armor.anchor)
						.withValues(List.of(HudAnchor.values()))
						.create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.anchor"), (btn, val) -> {
							armor.anchor = val;
							if (val != HudAnchor.CUSTOM) {
								armor.offsetX = 0;
								armor.offsetY = 0;
							}
						}),
				y
		);
		addScrollWidget(
				CycleButton.builder((HudOrientation o) -> Component.translatable(o.translationKey()), () -> armor.orientation)
						.withValues(List.of(HudOrientation.values()))
						.create(rightX, 0, halfW, 20, Component.translatable("customizablehud.option.orientation"), (btn, val) -> armor.orientation = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.hideEmptySlots
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.armor.hide_empty"), (btn, val) -> armor.hideEmptySlots = val),
				y
		);
		addScrollWidget(
				new ScaleSliderButton(
						rightX,
						0,
						halfW,
						20,
						Component.translatable("customizablehud.option.scale"),
						armor.scale,
						val -> armor.scale = val
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
						armor.slotSpacing,
						val -> armor.slotSpacing = val
				),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(Component.translatable("customizablehud.section.armor_display"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.showIcon
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.option.show_icon"), (btn, val) -> armor.showIcon = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.showDurabilityBar
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.armor.show_bar"), (btn, val) -> armor.showDurabilityBar = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.builder((DurabilityDisplayMode m) -> Component.translatable(m.translationKey()), () -> armor.durabilityDisplayMode)
						.withValues(List.of(DurabilityDisplayMode.values()))
						.create(leftX, 0, halfW, 20, Component.translatable("customizablehud.armor.durability_mode"), (btn, val) -> armor.durabilityDisplayMode = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.showMaxDurability
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.armor.show_max"), (btn, val) -> armor.showMaxDurability = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.showItemName
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.armor.show_name"), (btn, val) -> armor.showItemName = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.colorByDurability
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.armor.color_range"), (btn, val) -> armor.colorByDurability = val),
				y
		);
		y += ROW_HEIGHT;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.lowDurabilityWarning
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.armor.low_warning"), (btn, val) -> armor.lowDurabilityWarning = val),
				y
		);
		addScrollWidget(
				new IntSliderButton(
						rightX,
						0,
						halfW,
						20,
						Component.translatable("customizablehud.armor.low_threshold"),
						"%",
						ConfigValidator.MIN_THRESHOLD,
						ConfigValidator.MAX_THRESHOLD,
						armor.lowDurabilityThresholdPercent,
						val -> armor.lowDurabilityThresholdPercent = val
				),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(Component.translatable("customizablehud.section.hands"), leftX, y);
		y += 16;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.showMainHand
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.armor.main_hand"), (btn, val) -> armor.showMainHand = val),
				y
		);
		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.showOffHand
				).create(rightX, 0, halfW, 20, Component.translatable("customizablehud.armor.off_hand"), (btn, val) -> armor.showOffHand = val),
				y
		);
		y += ROW_HEIGHT + 6;

		addSectionLabel(Component.translatable("customizablehud.section.armor_order"), leftX, y);
		y += 16;

		addScrollWidget(
				Button.builder(buildOrderText(armor), btn -> {
							Collections.reverse(armor.armorOrder);
							btn.setMessage(buildOrderText(armor));
						})
						.bounds(leftX, 0, panelW, 20)
						.tooltip(Tooltip.create(Component.translatable("customizablehud.tooltip.reverse_order")))
						.build(),
				y
		);
		y += ROW_HEIGHT;

		int swapW = (panelW - 12) / 3;
		addScrollWidget(
				Button.builder(Component.translatable("customizablehud.armor.swap_1_2"), btn -> {
					swapArmorPair(armor, 0, 1);
					this.rebuildWidgets();
				}).bounds(leftX, 0, swapW, 20).build(),
				y
		);
		addScrollWidget(
				Button.builder(Component.translatable("customizablehud.armor.swap_2_3"), btn -> {
					swapArmorPair(armor, 1, 2);
					this.rebuildWidgets();
				}).bounds(leftX + swapW + 6, 0, swapW, 20).build(),
				y
		);
		addScrollWidget(
				Button.builder(Component.translatable("customizablehud.armor.swap_3_4"), btn -> {
					swapArmorPair(armor, 2, 3);
					this.rebuildWidgets();
				}).bounds(leftX + (swapW + 6) * 2, 0, swapW, 20).build(),
				y
		);
		y += ROW_HEIGHT + 4;

		addScrollWidget(
				CycleButton.booleanBuilder(
						Component.translatable("customizablehud.state.on"),
						Component.translatable("customizablehud.state.off"),
						armor.individualPositionsEnabled
				).create(leftX, 0, halfW, 20, Component.translatable("customizablehud.armor.individual_mode"), (btn, val) -> {
					if (val && !armor.individualPositionsEnabled) {
						captureIndividualFromAuto(armor);
					}
					armor.individualPositionsEnabled = val;
				}),
				y
		);
		addScrollWidget(
				Button.builder(Component.translatable("customizablehud.armor.reset_individual"), btn -> {
					armor.individualPositionsEnabled = false;
					armor.anchor = HudAnchor.HOTBAR_LEFT;
					armor.offsetX = 0;
					armor.offsetY = 0;
					armor.individualOffsets = ArmorConfig.createDefaultIndividualOffsets();
					this.rebuildWidgets();
				}).bounds(rightX, 0, halfW, 20).build(),
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
						}, HudEditorScreen.ModuleTarget.ARMOR))
				).bounds(footerX, footerY, btnW, 20).build()
		);

		addRenderableWidget(
				Button.builder(
						Component.translatable("customizablehud.button.reset_module"),
						btn -> {
							workingConfig.armor = new ArmorConfig();
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

	private void captureIndividualFromAuto(ArmorConfig armor) {
		ArmorHudRenderer.captureIndividualPositionsFromAuto(this.minecraft, armor, this.width, this.height);
	}

	private static void swapArmorPair(ArmorConfig armor, int i, int j) {
		if (armor.armorOrder != null && armor.armorOrder.size() == 4) {
			Collections.swap(armor.armorOrder, i, j);
		}
	}

	private static Component buildOrderText(ArmorConfig armor) {
		StringBuilder sb = new StringBuilder();
		List<ArmorSlotKind> order = armor.armorOrder != null && armor.armorOrder.size() == 4
				? armor.armorOrder
				: ArmorSlotKind.DEFAULT_ARMOR_ORDER;
		for (int i = 0; i < order.size(); i++) {
			if (i > 0) {
				sb.append(" → ");
			}
			sb.append(Component.translatable(order.get(i).translationKey()).getString());
		}
		return Component.translatable("customizablehud.armor.order_display", sb.toString());
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(parent);
	}
}
