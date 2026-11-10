package dev.customizablehud.client.screen;

import dev.customizablehud.config.HudConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public abstract class AbstractScrollableConfigScreen extends AbstractCustomizableScreen {
	protected static final int HEADER_HEIGHT = 38;
	protected static final int FOOTER_HEIGHT = 34;
	protected static final int ROW_HEIGHT = 24;

	private record ScrollWidgetEntry(AbstractWidget widget, int baseY) {
	}

	private record SectionLabelEntry(Component text, int x, int baseY) {
	}

	private final List<ScrollWidgetEntry> scrollWidgets = new ArrayList<>();
	private final List<SectionLabelEntry> sectionLabels = new ArrayList<>();
	private int scrollOffset = 0;
	private int contentHeight = 0;
	private boolean draggingScrollbar = false;

	protected AbstractScrollableConfigScreen(Component title, Screen parent, HudConfig workingConfig) {
		super(title, parent, workingConfig);
	}

	protected void resetScrollContainer() {
		scrollWidgets.clear();
		sectionLabels.clear();
		contentHeight = 0;
		clampScroll();
	}

	protected int bodyTop() {
		return HEADER_HEIGHT + 4;
	}

	protected int bodyBottom() {
		return this.height - FOOTER_HEIGHT - 4;
	}

	protected int bodyHeight() {
		return Math.max(20, bodyBottom() - bodyTop());
	}

	protected int maxScroll() {
		return Math.max(0, contentHeight - bodyHeight());
	}

	protected <T extends AbstractWidget> T addScrollWidget(T widget, int baseY) {
		this.addWidget(widget);
		scrollWidgets.add(new ScrollWidgetEntry(widget, baseY));
		contentHeight = Math.max(contentHeight, baseY + widget.getHeight() + 8);
		widget.setY(bodyTop() + baseY - scrollOffset);
		return widget;
	}

	protected void addSectionLabel(Component label, int x, int baseY) {
		sectionLabels.add(new SectionLabelEntry(label, x, baseY));
		contentHeight = Math.max(contentHeight, baseY + 14);
	}

	protected void syncScrollPositions() {
		clampScroll();
		int top = bodyTop();
		for (ScrollWidgetEntry entry : scrollWidgets) {
			entry.widget().setY(top + entry.baseY() - scrollOffset);
		}
	}

	private void clampScroll() {
		scrollOffset = Mth.clamp(scrollOffset, 0, maxScroll());
	}

	@Override
	public Optional<GuiEventListener> getChildAt(double mouseX, double mouseY) {
		boolean inBody = mouseY >= bodyTop() && mouseY <= bodyBottom();
		for (GuiEventListener child : this.children()) {
			boolean isScrollable = isScrollWidget(child);
			if (isScrollable && !inBody) {
				continue;
			}
			if (child.isMouseOver(mouseX, mouseY)) {
				return Optional.of(child);
			}
		}
		return Optional.empty();
	}

	private boolean isScrollWidget(GuiEventListener listener) {
		for (ScrollWidgetEntry entry : scrollWidgets) {
			if (entry.widget() == listener) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (super.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
			return true;
		}
		if (maxScroll() > 0 && mouseY >= bodyTop() && mouseY <= bodyBottom()) {
			scrollOffset = Mth.clamp((int) Math.round(scrollOffset - scrollY * 18.0), 0, maxScroll());
			syncScrollPositions();
			return true;
		}
		return false;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean isDoubleClick) {
		if (event.button() == 0 && maxScroll() > 0) {
			int trackX = scrollbarX();
			if (event.x() >= trackX - 2 && event.x() <= trackX + 8 && event.y() >= bodyTop() && event.y() <= bodyBottom()) {
				draggingScrollbar = true;
				updateScrollFromMouseY(event.y());
				return true;
			}
		}
		return super.mouseClicked(event, isDoubleClick);
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
		if (draggingScrollbar && event.button() == 0) {
			updateScrollFromMouseY(event.y());
			return true;
		}
		return super.mouseDragged(event, dx, dy);
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		if (event.button() == 0 && draggingScrollbar) {
			draggingScrollbar = false;
			return true;
		}
		return super.mouseReleased(event);
	}

	private void updateScrollFromMouseY(double mouseY) {
		int max = maxScroll();
		if (max <= 0) {
			return;
		}
		double frac = Mth.clamp((mouseY - bodyTop()) / (double) bodyHeight(), 0.0, 1.0);
		scrollOffset = Mth.clamp((int) Math.round(frac * max), 0, max);
		syncScrollPositions();
	}

	private int scrollbarX() {
		return this.width - 10;
	}

	protected Component subtitleText() {
		return null;
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		int top = bodyTop();
		int bottom = bodyBottom();
		graphics.fill(0, top, this.width, bottom, 0x55090D12);
		boolean mouseInBody = mouseY >= top && mouseY <= bottom;
		int bodyMouseX = mouseInBody ? mouseX : -10000;
		int bodyMouseY = mouseInBody ? mouseY : -10000;

		graphics.enableScissor(0, top, this.width, bottom);
		for (SectionLabelEntry label : sectionLabels) {
			int drawY = top + label.baseY() - scrollOffset;
			if (drawY + 12 >= top && drawY <= bottom) {
				graphics.drawString(this.font, label.text(), label.x(), drawY, 0xFF7FD4E6, true);
				graphics.fill(label.x(), drawY + 10, Math.min(this.width - 18, label.x() + 260), drawY + 11, 0x447FD4E6);
			}
		}
		for (ScrollWidgetEntry entry : scrollWidgets) {
			AbstractWidget widget = entry.widget();
			if (widget.getBottom() >= top && widget.getY() <= bottom) {
				widget.render(graphics, bodyMouseX, bodyMouseY, partialTick);
			}
		}
		graphics.disableScissor();

		if (maxScroll() > 0) {
			int trackX = scrollbarX();
			graphics.fill(trackX, top, trackX + 4, bottom, 0x551A202C);
			int thumbH = Math.max(18, (int) ((bodyHeight() / (float) contentHeight) * bodyHeight()));
			int thumbRange = Math.max(1, bodyHeight() - thumbH);
			int thumbY = top + Math.round((scrollOffset / (float) maxScroll()) * thumbRange);
			graphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xCC7FD4E6);
		}

		renderHeaderAndFooterChrome(graphics, HEADER_HEIGHT, this.height - FOOTER_HEIGHT, subtitleText());
		super.render(graphics, mouseX, mouseY, partialTick);
	}
}
