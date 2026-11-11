package dev.customizablehud.client.screen;

import dev.customizablehud.client.hud.ArmorHudRenderer;
import dev.customizablehud.client.hud.EffectsHudRenderer;
import dev.customizablehud.config.ArmorConfig;
import dev.customizablehud.config.ArmorSlotKind;
import dev.customizablehud.config.ConfigValidator;
import dev.customizablehud.config.HudAnchor;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.PointOffset;
import dev.customizablehud.layout.ArmorLayoutResult;
import dev.customizablehud.layout.ArmorSlotPlacement;
import dev.customizablehud.layout.EffectsLayoutResult;
import dev.customizablehud.layout.GuideLine;
import dev.customizablehud.layout.HudLayoutEngine;
import dev.customizablehud.layout.HudRect;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class HudEditorScreen extends AbstractCustomizableScreen {
	public enum ModuleTarget {
		ARMOR,
		EFFECTS
	}

	private static final int ROW_STEP = 22;
	private static final int PANEL_PAD = 4;

	private final HudConfig entrySnapshot;
	private final Consumer<HudConfig> onSyncParent;
	private ModuleTarget selectedModule;
	private ArmorSlotKind selectedArmorSlot = ArmorSlotKind.HELMET;
	private boolean dockAtTop = false;

	private boolean draggingElement = false;
	private int dragOffsetX = 0;
	private int dragOffsetY = 0;
	private List<GuideLine> activeGuides = List.of();

	private ArmorLayoutResult lastArmorLayout;
	private EffectsLayoutResult lastEffectsLayout;

	private Button selectTargetBtn;
	private Button toggleEnabledBtn;
	private ScaleSliderButton scaleSlider;
	private EditBox scaleEditBox;
	private Button individualModeBtn;
	private Button autoLayoutBtn;
	private Button snapBtn;
	private Button dockBtn;
	private Button cancelBtn;
	private Button saveLayoutBtn;
	private Button doneBtn;
	private boolean suppressEditBoxSync;

	private int computedToolbarHeight = 52;

	public HudEditorScreen(Screen parent, HudConfig workingConfig, Consumer<HudConfig> onSyncParent) {
		this(parent, workingConfig, onSyncParent, ModuleTarget.ARMOR);
	}

	public HudEditorScreen(Screen parent, HudConfig workingConfig, Consumer<HudConfig> onSyncParent, ModuleTarget initialTarget) {
		super("customizablehud.screen.editor.title", parent, workingConfig);
		this.entrySnapshot = workingConfig.copy();
		this.onSyncParent = onSyncParent;
		this.selectedModule = initialTarget != null ? initialTarget : ModuleTarget.ARMOR;
	}

	public boolean isDragging() {
		return draggingElement;
	}

	private int toolbarWidth() {
		return Math.max(180, Math.min(500, this.width - 12));
	}

	private int toolbarX(int panelW) {
		return (this.width - panelW) / 2;
	}

	private int toolbarY() {
		return dockAtTop ? 2 : Math.max(2, this.height - computedToolbarHeight - 2);
	}

	private boolean isInsideToolbar(double x, double y) {
		int panelW = toolbarWidth();
		int panelX = toolbarX(panelW);
		int panelY = toolbarY();
		return x >= panelX && x <= panelX + panelW && y >= panelY && y <= panelY + computedToolbarHeight;
	}

	@Override
	protected void init() {
		int panelW = toolbarWidth();
		int panelX = toolbarX(panelW);
		int innerW = panelW - PANEL_PAD * 2;
		int gap = 3;

		selectTargetBtn = Button.builder(Component.empty(), btn -> {
			selectedModule = selectedModule == ModuleTarget.ARMOR ? ModuleTarget.EFFECTS : ModuleTarget.ARMOR;
			syncControlsFromSelection();
		}).bounds(0, 0, 56, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.editor.target")))
				.build();

		toggleEnabledBtn = Button.builder(Component.empty(), btn -> {
			if (selectedModule == ModuleTarget.ARMOR) {
				workingConfig.armor.enabled = !workingConfig.armor.enabled;
			} else {
				workingConfig.effects.enabled = !workingConfig.effects.enabled;
			}
			notifyEditorChanged();
			syncControlsFromSelection();
		}).bounds(0, 0, 62, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.editor.toggle_enabled")))
				.build();

		scaleSlider = new ScaleSliderButton(
				0,
				0,
				80,
				20,
				t("customizablehud.option.scale"),
				currentSelectedScale(),
				val -> {
					applyScale(val);
					updateScaleEditBoxText(val);
					notifyEditorChanged();
				},
				val -> t("customizablehud.tooltip.editor.scale", String.format(Locale.ROOT, "%.2fx", val))
		);

		scaleEditBox = new EditBox(this.font, 0, 0, 38, 18, t("customizablehud.option.scale"));
		scaleEditBox.setMaxLength(5);
		scaleEditBox.setTooltip(Tooltip.create(t("customizablehud.tooltip.editor.scale_input")));
		scaleEditBox.setFilter(text -> text.matches("\\d*(\\.\\d*)?"));
		scaleEditBox.setResponder(text -> {
			if (suppressEditBoxSync || text == null || text.isBlank() || ".".equals(text)) {
				return;
			}
			try {
				float parsed = Float.parseFloat(text);
				if (parsed >= ConfigValidator.MIN_SCALE && parsed <= ConfigValidator.MAX_SCALE) {
					float clamped = ConfigValidator.clampScale(parsed);
					applyScale(clamped);
					scaleSlider.setScaleValue(clamped);
					notifyEditorChanged();
				}
			} catch (NumberFormatException ignored) {
				// Wait for valid number input
			}
		});

		dockBtn = Button.builder(Component.empty(), btn -> {
			dockAtTop = !dockAtTop;
			this.rebuildWidgets();
		}).bounds(0, 0, 56, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.move_toolbar")))
				.build();

		individualModeBtn = Button.builder(Component.empty(), btn -> {
			toggleIndividualArmorMode();
			notifyEditorChanged();
		}).bounds(0, 0, 64, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.editor.individual")))
				.build();

		autoLayoutBtn = Button.builder(t("customizablehud.editor.auto_layout"), btn -> {
			resetSelectedLayout();
			notifyEditorChanged();
		}).bounds(0, 0, 60, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.auto_layout")))
				.build();

		snapBtn = Button.builder(Component.empty(), btn -> {
			workingConfig.editorSnapToGrid = !workingConfig.editorSnapToGrid;
			notifyEditorChanged();
			syncControlsFromSelection();
		}).bounds(0, 0, 52, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.editor.snap")))
				.build();

		cancelBtn = Button.builder(t("customizablehud.button.cancel"), btn -> cancelAndRevert())
				.bounds(0, 0, 54, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.editor.cancel")))
				.build();

		saveLayoutBtn = Button.builder(t("customizablehud.editor.save_layout"), btn -> {
			boolean ok = saveConfigNow();
			if (ok && onSyncParent != null) {
				onSyncParent.accept(workingConfig.copy());
			}
		}).bounds(0, 0, 74, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.editor.save_layout")))
				.build();

		doneBtn = Button.builder(t("customizablehud.button.done"), btn -> onClose())
				.bounds(0, 0, 48, 20)
				.tooltip(Tooltip.create(t("customizablehud.tooltip.editor.done")))
				.build();

		record ControlSlot(AbstractWidget widget, int minW, int weight, boolean isEditBox) {
		}

		List<List<ControlSlot>> rows = new ArrayList<>();
		if (innerW >= 380) {
			rows.add(List.of(
					new ControlSlot(selectTargetBtn, 52, 16, false),
					new ControlSlot(toggleEnabledBtn, 58, 18, false),
					new ControlSlot(scaleSlider, 70, 34, false),
					new ControlSlot(scaleEditBox, 38, 10, true),
					new ControlSlot(dockBtn, 52, 18, false)
			));
			rows.add(List.of(
					new ControlSlot(individualModeBtn, 62, 18, false),
					new ControlSlot(autoLayoutBtn, 56, 17, false),
					new ControlSlot(snapBtn, 48, 14, false),
					new ControlSlot(cancelBtn, 48, 15, false),
					new ControlSlot(saveLayoutBtn, 68, 22, false),
					new ControlSlot(doneBtn, 42, 14, false)
			));
		} else if (innerW >= 245) {
			rows.add(List.of(
					new ControlSlot(selectTargetBtn, 50, 22, false),
					new ControlSlot(toggleEnabledBtn, 54, 24, false),
					new ControlSlot(scaleSlider, 62, 30, false),
					new ControlSlot(scaleEditBox, 36, 12, true)
			));
			rows.add(List.of(
					new ControlSlot(individualModeBtn, 56, 26, false),
					new ControlSlot(autoLayoutBtn, 52, 24, false),
					new ControlSlot(snapBtn, 44, 22, false),
					new ControlSlot(dockBtn, 48, 24, false)
			));
			rows.add(List.of(
					new ControlSlot(cancelBtn, 54, 30, false),
					new ControlSlot(saveLayoutBtn, 74, 42, false),
					new ControlSlot(doneBtn, 48, 28, false)
			));
		} else {
			rows.add(List.of(
					new ControlSlot(selectTargetBtn, 50, 34, false),
					new ControlSlot(toggleEnabledBtn, 54, 34, false),
					new ControlSlot(dockBtn, 48, 32, false)
			));
			rows.add(List.of(
					new ControlSlot(scaleSlider, 70, 75, false),
					new ControlSlot(scaleEditBox, 36, 25, true)
			));
			rows.add(List.of(
					new ControlSlot(individualModeBtn, 54, 36, false),
					new ControlSlot(autoLayoutBtn, 52, 34, false),
					new ControlSlot(snapBtn, 44, 30, false)
			));
			rows.add(List.of(
					new ControlSlot(cancelBtn, 48, 30, false),
					new ControlSlot(saveLayoutBtn, 64, 42, false),
					new ControlSlot(doneBtn, 42, 28, false)
			));
		}

		computedToolbarHeight = PANEL_PAD * 2 + rows.size() * 20 + Math.max(0, rows.size() - 1) * 2;
		int startY = toolbarY() + PANEL_PAD;

		for (int r = 0; r < rows.size(); r++) {
			List<ControlSlot> row = rows.get(r);
			int rowY = startY + r * ROW_STEP;
			int avail = innerW - gap * (row.size() - 1);
			int totalWeight = 0;
			for (ControlSlot slot : row) {
				totalWeight += slot.weight();
			}
			int[] widths = new int[row.size()];
			int used = 0;
			for (int i = 0; i < row.size(); i++) {
				ControlSlot slot = row.get(i);
				int w = (i == row.size() - 1)
						? Math.max(24, avail - used)
						: Math.max(24, Math.round((slot.weight() / (float) Math.max(1, totalWeight)) * avail));
				widths[i] = w;
				used += w;
			}
			int cx = panelX + PANEL_PAD;
			for (int i = 0; i < row.size(); i++) {
				ControlSlot slot = row.get(i);
				AbstractWidget w = slot.widget();
				w.setX(cx);
				w.setY(slot.isEditBox() ? rowY + 1 : rowY);
				w.setWidth(widths[i]);
				addRenderableWidget(w);
				cx += widths[i] + gap;
			}
		}

		syncControlsFromSelection();
	}

	private void notifyEditorChanged() {
		notifyConfigEdited();
		if (onSyncParent != null) {
			onSyncParent.accept(workingConfig.copy());
		}
	}

	private void cancelAndRevert() {
		syncWorkingFromManager(entrySnapshot);
		saveConfigNow();
		if (onSyncParent != null) {
			onSyncParent.accept(workingConfig.copy());
		}
		this.minecraft.setScreen(parent);
	}

	private void toggleIndividualArmorMode() {
		ArmorConfig armor = workingConfig.armor;
		if (!armor.individualPositionsEnabled) {
			List<ArmorSlotKind> visibleKinds = ArmorHudRenderer.captureIndividualPositionsFromAuto(
					this.minecraft,
					armor,
					activeLang(),
					this.width,
					this.height
			);
			armor.individualPositionsEnabled = true;
			if (!visibleKinds.isEmpty()) {
				selectedArmorSlot = visibleKinds.getFirst();
			}
		} else {
			armor.individualPositionsEnabled = false;
		}
		syncControlsFromSelection();
	}

	private void resetSelectedLayout() {
		if (selectedModule == ModuleTarget.ARMOR) {
			workingConfig.armor.individualPositionsEnabled = false;
			workingConfig.armor.anchor = HudAnchor.HOTBAR_LEFT;
			workingConfig.armor.offsetX = 0;
			workingConfig.armor.offsetY = 0;
			workingConfig.armor.scale = 1.0f;
			workingConfig.armor.individualOffsets = ArmorConfig.createDefaultIndividualOffsets();
		} else {
			workingConfig.effects.anchor = HudAnchor.TOP_LEFT;
			workingConfig.effects.offsetX = 0;
			workingConfig.effects.offsetY = 0;
			workingConfig.effects.scale = 1.0f;
		}
		syncControlsFromSelection();
	}

	private float currentSelectedScale() {
		return selectedModule == ModuleTarget.ARMOR ? workingConfig.armor.scale : workingConfig.effects.scale;
	}

	private void applyScale(float scale) {
		float clamped = ConfigValidator.clampScale(scale);
		if (selectedModule == ModuleTarget.ARMOR) {
			workingConfig.armor.scale = clamped;
		} else {
			workingConfig.effects.scale = clamped;
		}
	}

	private void updateScaleEditBoxText(float scale) {
		if (scaleEditBox == null) {
			return;
		}
		suppressEditBoxSync = true;
		scaleEditBox.setValue(String.format(Locale.ROOT, "%.2f", scale));
		suppressEditBoxSync = false;
	}

	private void syncControlsFromSelection() {
		if (selectTargetBtn == null) {
			return;
		}
		selectTargetBtn.setMessage(t(
				selectedModule == ModuleTarget.ARMOR ? "customizablehud.editor.target_armor" : "customizablehud.editor.target_effects"
		));
		boolean enabled = selectedModule == ModuleTarget.ARMOR ? workingConfig.armor.enabled : workingConfig.effects.enabled;
		toggleEnabledBtn.setMessage(t(
				enabled ? "customizablehud.state.enabled" : "customizablehud.state.disabled"
		));
		float scale = currentSelectedScale();
		scaleSlider.setScaleValue(scale);
		updateScaleEditBoxText(scale);

		individualModeBtn.active = selectedModule == ModuleTarget.ARMOR;
		individualModeBtn.setMessage(t(
				workingConfig.armor.individualPositionsEnabled
						? "customizablehud.editor.individual_on"
						: "customizablehud.editor.individual_off"
		));
		snapBtn.setMessage(t(
				workingConfig.editorSnapToGrid ? "customizablehud.editor.snap_on" : "customizablehud.editor.snap_off"
		));
		dockBtn.setMessage(t(
				dockAtTop ? "customizablehud.editor.dock_bottom" : "customizablehud.editor.dock_top"
		));
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int backdrop = (this.minecraft != null && this.minecraft.level == null) ? 0xE610151D : 0x40000000;
		graphics.fill(0, 0, this.width, this.height, backdrop);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		if (workingConfig.editorSnapToGrid) {
			int step = Math.max(8, workingConfig.editorGridSize * 4);
			for (int gx = step; gx < this.width; gx += step) {
				for (int gy = step; gy < this.height; gy += step) {
					graphics.fill(gx, gy, gx + 1, gy + 1, 0x24FFFFFF);
				}
			}
		}

		// Reference silhouette for vanilla hotbar & offhand slot
		HudRect hotbarRef = HudLayoutEngine.vanillaHotbarRect(this.width, this.height);
		HudRect offhandRef = HudLayoutEngine.vanillaOffhandLeftRect(this.width, this.height);
		graphics.fill(hotbarRef.x(), hotbarRef.y(), hotbarRef.right(), hotbarRef.bottom(), 0x26000000);
		graphics.renderOutline(hotbarRef.x(), hotbarRef.y(), hotbarRef.width(), hotbarRef.height(), 0x5594A3B8);
		graphics.fill(offhandRef.x(), offhandRef.y(), offhandRef.right(), offhandRef.bottom(), 0x22000000);
		graphics.renderOutline(offhandRef.x(), offhandRef.y(), offhandRef.width(), offhandRef.height(), 0x4494A3B8);
		graphics.drawCenteredString(
				this.font,
				t("customizablehud.editor.hotbar_ref"),
				hotbarRef.centerX(),
				hotbarRef.y() + 7,
				0x77CBD5E1
		);

		lastArmorLayout = ArmorHudRenderer.renderForEditor(
				graphics,
				this.minecraft,
				workingConfig.armor,
				activeLang(),
				workingConfig.armor.enabled ? 1.0f : 0.38f
		);
		lastEffectsLayout = EffectsHudRenderer.renderForEditor(
				graphics,
				this.minecraft,
				workingConfig.effects,
				activeLang(),
				workingConfig.effects.enabled ? 1.0f : 0.38f
		);

		drawModuleOverlay(graphics, ModuleTarget.ARMOR, lastArmorLayout != null ? lastArmorLayout.bounds() : HudRect.EMPTY, workingConfig.armor.enabled);
		drawModuleOverlay(graphics, ModuleTarget.EFFECTS, lastEffectsLayout != null ? lastEffectsLayout.bounds() : HudRect.EMPTY, workingConfig.effects.enabled);

		if (workingConfig.armor.individualPositionsEnabled && lastArmorLayout != null) {
			for (ArmorSlotPlacement placement : lastArmorLayout.slots()) {
				HudRect r = placement.screenBounds();
				boolean isSel = selectedModule == ModuleTarget.ARMOR && placement.kind() == selectedArmorSlot;
				int outlineColor = isSel ? 0xFF38BDF8 : 0x7794A3B8;
				graphics.renderOutline(r.x() - 1, r.y() - 1, r.width() + 2, r.height() + 2, outlineColor);
			}
		}

		for (GuideLine guide : activeGuides) {
			if (guide.vertical()) {
				graphics.vLine(guide.coordinate(), 0, this.height, 0xCC38BDF8);
			} else {
				graphics.hLine(0, this.width, guide.coordinate(), 0xCC38BDF8);
			}
		}

		// Overlap warning indicator
		if (detectEditorOverlap(hotbarRef)) {
			Component warn = t("customizablehud.editor.overlap_warning");
			int warnW = this.font.width(warn) + 14;
			int warnX = (this.width - warnW) / 2;
			int warnY = dockAtTop ? (computedToolbarHeight + 10) : 8;
			graphics.fill(warnX, warnY, warnX + warnW, warnY + 14, 0xCC78350F);
			graphics.renderOutline(warnX, warnY, warnW, 14, 0xFFF59E0B);
			graphics.drawCenteredString(this.font, warn, this.width / 2, warnY + 3, 0xFFFDE68A);
		}

		// Toolbar backdrop
		int panelW = toolbarWidth();
		int panelX = toolbarX(panelW);
		int panelY = toolbarY();
		graphics.fill(panelX, panelY, panelX + panelW, panelY + computedToolbarHeight, 0xD80D1117);
		graphics.renderOutline(panelX, panelY, panelW, computedToolbarHeight, 0xFF334155);

		Component hint = t("customizablehud.editor.hint");
		int hintY = dockAtTop ? (panelY + computedToolbarHeight + 3) : (panelY - 11);
		if (!detectEditorOverlap(hotbarRef) || !dockAtTop) {
			graphics.drawCenteredString(this.font, hint, this.width / 2, hintY, 0xCC94A3B8);
		}

		renderSaveStatusBadge(graphics, 8, dockAtTop ? (this.height - 19) : 6);

		super.render(graphics, mouseX, mouseY, partialTick);
	}

	private void drawModuleOverlay(GuiGraphics graphics, ModuleTarget target, HudRect bounds, boolean enabled) {
		if (bounds == null || bounds.isEmpty()) {
			return;
		}
		boolean selected = selectedModule == target;
		int borderColor;
		if (selected) {
			borderColor = enabled ? 0xFF38BDF8 : 0xFFF87171;
		} else {
			borderColor = enabled ? 0x77CBD5E1 : 0x77EF4444;
		}
		graphics.renderOutline(bounds.x() - 2, bounds.y() - 2, bounds.width() + 4, bounds.height() + 4, borderColor);

		if (selected) {
			int handle = 0xFF38BDF8;
			graphics.fill(bounds.x() - 3, bounds.y() - 3, bounds.x(), bounds.y(), handle);
			graphics.fill(bounds.right(), bounds.y() - 3, bounds.right() + 3, bounds.y(), handle);
			graphics.fill(bounds.x() - 3, bounds.bottom(), bounds.x(), bounds.bottom() + 3, handle);
			graphics.fill(bounds.right(), bounds.bottom(), bounds.right() + 3, bounds.bottom() + 3, handle);
		}

		Component badge = buildBadgeText(target, enabled);
		int badgeW = this.font.width(badge) + 8;
		int badgeX = Math.max(2, Math.min(this.width - badgeW - 2, bounds.x()));
		int badgeY = bounds.y() >= 16 ? (bounds.y() - 14) : (bounds.bottom() + 4);
		int badgeBg = enabled ? (selected ? 0xDD0F172A : 0xAA0F172A) : 0xDD7F1D1D;
		int badgeText = enabled ? 0xFFE2E8F0 : 0xFFFECACA;
		graphics.fill(badgeX, badgeY, badgeX + badgeW, badgeY + 12, badgeBg);
		graphics.renderOutline(badgeX, badgeY, badgeW, 12, borderColor);
		graphics.drawString(this.font, badge, badgeX + 4, badgeY + 2, badgeText, false);
	}

	private Component buildBadgeText(ModuleTarget target, boolean enabled) {
		Component base = target == ModuleTarget.ARMOR
				? (workingConfig.armor.individualPositionsEnabled && selectedModule == ModuleTarget.ARMOR
				? t(selectedArmorSlot.translationKey())
				: t("customizablehud.module.armor"))
				: t("customizablehud.module.effects");
		if (enabled) {
			return base;
		}
		return Component.empty().append(base).append(" (").append(t("customizablehud.badge.disabled")).append(")");
	}

	private boolean detectEditorOverlap(HudRect hotbarRef) {
		List<HudRect> rects = new ArrayList<>();
		if (workingConfig.armor.enabled && lastArmorLayout != null) {
			if (workingConfig.armor.individualPositionsEnabled) {
				for (ArmorSlotPlacement placement : lastArmorLayout.slots()) {
					rects.add(placement.screenBounds());
				}
			} else if (!lastArmorLayout.bounds().isEmpty()) {
				rects.add(lastArmorLayout.bounds());
			}
		}
		if (workingConfig.effects.enabled && lastEffectsLayout != null && !lastEffectsLayout.bounds().isEmpty()) {
			rects.add(lastEffectsLayout.bounds());
		}
		if (rects.isEmpty()) {
			return false;
		}
		rects.add(hotbarRef);
		return HudLayoutEngine.hasAnyOverlap(rects);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		if (super.mouseClicked(event, isDoubleClick)) {
			if (scaleEditBox != null && !scaleEditBox.isMouseOver(event.x(), event.y())) {
				scaleEditBox.setFocused(false);
			}
			return true;
		}
		if (scaleEditBox != null) {
			scaleEditBox.setFocused(false);
		}
		this.setFocused(null);

		if (isInsideToolbar(event.x(), event.y())) {
			return true;
		}

		if (event.button() == 0) {
			if (workingConfig.armor.individualPositionsEnabled && lastArmorLayout != null) {
				for (int i = lastArmorLayout.slots().size() - 1; i >= 0; i--) {
					ArmorSlotPlacement placement = lastArmorLayout.slots().get(i);
					if (expandedContains(placement.screenBounds(), event.x(), event.y())) {
						selectedModule = ModuleTarget.ARMOR;
						selectedArmorSlot = placement.kind();
						draggingElement = true;
						dragOffsetX = (int) Math.round(event.x()) - placement.screenBounds().x();
						dragOffsetY = (int) Math.round(event.y()) - placement.screenBounds().y();
						syncControlsFromSelection();
						return true;
					}
				}
			}

			HudRect armorBounds = lastArmorLayout != null ? lastArmorLayout.bounds() : HudRect.EMPTY;
			HudRect effectsBounds = lastEffectsLayout != null ? lastEffectsLayout.bounds() : HudRect.EMPTY;
			boolean hitArmor = expandedContains(armorBounds, event.x(), event.y());
			boolean hitEffects = expandedContains(effectsBounds, event.x(), event.y());

			ModuleTarget hitTarget = null;
			if (hitArmor && hitEffects) {
				hitTarget = selectedModule;
			} else if (hitArmor) {
				hitTarget = ModuleTarget.ARMOR;
			} else if (hitEffects) {
				hitTarget = ModuleTarget.EFFECTS;
			}

			if (hitTarget != null) {
				selectedModule = hitTarget;
				HudRect hitRect = hitTarget == ModuleTarget.ARMOR ? armorBounds : effectsBounds;
				draggingElement = true;
				dragOffsetX = (int) Math.round(event.x()) - hitRect.x();
				dragOffsetY = (int) Math.round(event.y()) - hitRect.y();
				syncControlsFromSelection();
				return true;
			}
		}

		return false;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingElement && event.button() == 0) {
			int rawX = (int) Math.round(event.x()) - dragOffsetX;
			int rawY = (int) Math.round(event.y()) - dragOffsetY;
			applyDraggedPosition(rawX, rawY, true);
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (draggingElement && event.button() == 0) {
			draggingElement = false;
			activeGuides = List.of();
			notifyEditorChanged();
			return true;
		}
		return super.mouseReleased(event);
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		if (scaleEditBox != null && scaleEditBox.isFocused()) {
			if (event.isEscape()) {
				scaleEditBox.setFocused(false);
				this.setFocused(null);
				return true;
			}
			return super.keyPressed(event);
		}
		if (event.isEscape()) {
			onClose();
			return true;
		}
		int step = event.hasShiftDown() ? 5 : 1;
		int dx = 0;
		int dy = 0;
		if (event.isLeft()) {
			dx = -step;
		} else if (event.isRight()) {
			dx = step;
		} else if (event.isUp()) {
			dy = -step;
		} else if (event.isDown()) {
			dy = step;
		}
		if (dx != 0 || dy != 0) {
			nudgeSelected(dx, dy);
			notifyEditorChanged();
			return true;
		}
		return super.keyPressed(event);
	}

	private void applyDraggedPosition(int rawX, int rawY, boolean allowSnap) {
		HudRect currentRect = activeSelectionRect();
		if (currentRect.isEmpty()) {
			return;
		}
		List<HudRect> refs = buildAlignmentReferences();
		HudLayoutEngine.SnapResult snapped = allowSnap
				? HudLayoutEngine.snapPosition(
						rawX,
						rawY,
						currentRect.width(),
						currentRect.height(),
						this.width,
						this.height,
						workingConfig.editorGridSize,
						workingConfig.editorSnapToGrid,
						workingConfig.editorShowGuides,
						refs
				)
				: clampAsSnap(rawX, rawY, currentRect);

		activeGuides = snapped.guides();
		commitScreenPosition(snapped.x(), snapped.y());
	}

	private void nudgeSelected(int dx, int dy) {
		HudRect currentRect = activeSelectionRect();
		if (currentRect.isEmpty()) {
			return;
		}
		activeGuides = List.of();
		HudRect clamped = new HudRect(currentRect.x() + dx, currentRect.y() + dy, currentRect.width(), currentRect.height())
				.clampToScreen(this.width, this.height, 2);
		if (selectedModule == ModuleTarget.ARMOR && workingConfig.armor.individualPositionsEnabled && lastArmorLayout != null) {
			PointOffset current = workingConfig.armor.individualOffsets.getOrDefault(selectedArmorSlot, new PointOffset());
			float safeScale = Math.max(0.5f, workingConfig.armor.scale);
			int nextUx = current.x;
			int nextUy = current.y;
			if (clamped.x() != currentRect.x()) {
				int deltaUx = dx > 0 ? Math.max(1, Math.round(dx / safeScale)) : Math.min(-1, Math.round(dx / safeScale));
				nextUx += deltaUx;
			}
			if (clamped.y() != currentRect.y()) {
				int deltaUy = dy > 0 ? Math.max(1, Math.round(dy / safeScale)) : Math.min(-1, Math.round(dy / safeScale));
				nextUy += deltaUy;
			}
			int minX = currentRect.width() + 4 <= this.width ? 2 : 0;
			int minY = currentRect.height() + 4 <= this.height ? 2 : 0;
			int maxX = Math.max(minX, this.width - currentRect.width() - minX);
			int maxY = Math.max(minY, this.height - currentRect.height() - minY);
			int rawScreenX = lastArmorLayout.originX() + Math.round(nextUx * safeScale);
			int rawScreenY = lastArmorLayout.originY() + Math.round(nextUy * safeScale);
			if (rawScreenX < minX || rawScreenX > maxX) {
				int clampedX = Math.max(minX, Math.min(maxX, rawScreenX));
				nextUx = Math.round((clampedX - lastArmorLayout.originX()) / safeScale);
			}
			if (rawScreenY < minY || rawScreenY > maxY) {
				int clampedY = Math.max(minY, Math.min(maxY, rawScreenY));
				nextUy = Math.round((clampedY - lastArmorLayout.originY()) / safeScale);
			}
			workingConfig.armor.individualOffsets.put(selectedArmorSlot, new PointOffset(nextUx, nextUy));
			return;
		}
		commitScreenPosition(clamped.x(), clamped.y());
	}

	private void commitScreenPosition(int targetX, int targetY) {
		if (selectedModule == ModuleTarget.ARMOR) {
			ArmorConfig armor = workingConfig.armor;
			if (armor.individualPositionsEnabled && lastArmorLayout != null) {
				float safeScale = Math.max(0.5f, armor.scale);
				int ux = Math.round((targetX - lastArmorLayout.originX()) / safeScale);
				int uy = Math.round((targetY - lastArmorLayout.originY()) / safeScale);
				armor.individualOffsets.put(selectedArmorSlot, new PointOffset(ux, uy));
			} else {
				armor.anchor = HudAnchor.CUSTOM;
				armor.offsetX = targetX;
				armor.offsetY = targetY;
			}
		} else {
			workingConfig.effects.anchor = HudAnchor.CUSTOM;
			workingConfig.effects.offsetX = targetX;
			workingConfig.effects.offsetY = targetY;
		}
	}

	private HudRect activeSelectionRect() {
		if (selectedModule == ModuleTarget.ARMOR && lastArmorLayout != null) {
			if (workingConfig.armor.individualPositionsEnabled) {
				for (ArmorSlotPlacement placement : lastArmorLayout.slots()) {
					if (placement.kind() == selectedArmorSlot) {
						return placement.screenBounds();
					}
				}
			}
			return lastArmorLayout.bounds();
		}
		if (selectedModule == ModuleTarget.EFFECTS && lastEffectsLayout != null) {
			return lastEffectsLayout.bounds();
		}
		return HudRect.EMPTY;
	}

	private List<HudRect> buildAlignmentReferences() {
		List<HudRect> refs = new ArrayList<>();
		refs.add(HudLayoutEngine.vanillaHotbarRect(this.width, this.height));
		if (selectedModule == ModuleTarget.ARMOR) {
			if (lastEffectsLayout != null && !lastEffectsLayout.bounds().isEmpty()) {
				refs.add(lastEffectsLayout.bounds());
			}
			if (workingConfig.armor.individualPositionsEnabled && lastArmorLayout != null) {
				for (ArmorSlotPlacement placement : lastArmorLayout.slots()) {
					if (placement.kind() != selectedArmorSlot) {
						refs.add(placement.screenBounds());
					}
				}
			}
		} else {
			if (lastArmorLayout != null && !lastArmorLayout.bounds().isEmpty()) {
				refs.add(lastArmorLayout.bounds());
			}
		}
		return refs;
	}

	private HudLayoutEngine.SnapResult clampAsSnap(int x, int y, HudRect rect) {
		HudRect clamped = new HudRect(x, y, rect.width(), rect.height()).clampToScreen(this.width, this.height, 2);
		return new HudLayoutEngine.SnapResult(clamped.x(), clamped.y(), List.of());
	}

	private static boolean expandedContains(HudRect rect, double x, double y) {
		if (rect == null || rect.isEmpty()) {
			return false;
		}
		return x >= rect.x() - 3 && x <= rect.right() + 3 && y >= rect.y() - 3 && y <= rect.bottom() + 3;
	}

	@Override
	public void onClose() {
		flushPendingOnExit();
		if (onSyncParent != null) {
			onSyncParent.accept(workingConfig.copy());
		}
		this.minecraft.setScreen(parent);
	}
}
