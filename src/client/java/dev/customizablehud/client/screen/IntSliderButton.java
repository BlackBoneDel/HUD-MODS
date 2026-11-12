package dev.customizablehud.client.screen;

import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class IntSliderButton extends AbstractSliderButton {
	private final Component prefix;
	private final String suffix;
	private final int min;
	private final int max;
	private final IntConsumer onChanged;
	private final IntFunction<Component> tooltipProvider;
	private boolean suppressCallback;

	public IntSliderButton(
			int x,
			int y,
			int width,
			int height,
			Component prefix,
			String suffix,
			int min,
			int max,
			int initialValue,
			IntConsumer onChanged
	) {
		this(x, y, width, height, prefix, suffix, min, max, initialValue, onChanged, null);
	}

	public IntSliderButton(
			int x,
			int y,
			int width,
			int height,
			Component prefix,
			String suffix,
			int min,
			int max,
			int initialValue,
			IntConsumer onChanged,
			IntFunction<Component> tooltipProvider
	) {
		super(x, y, width, height, Component.empty(), toNormalized(initialValue, min, max));
		this.prefix = prefix;
		this.suffix = suffix == null ? "" : suffix;
		this.min = min;
		this.max = max;
		this.onChanged = onChanged;
		this.tooltipProvider = tooltipProvider;
		updateMessage();
		refreshDynamicTooltip();
	}

	public int getIntValue() {
		return Mth.clamp((int) Math.round(min + value * (max - min)), min, max);
	}

	public void setIntValue(int intValue) {
		suppressCallback = true;
		setValue(toNormalized(intValue, min, max));
		suppressCallback = false;
		refreshDynamicTooltip();
	}

	private void refreshDynamicTooltip() {
		if (tooltipProvider != null) {
			Component tip = tooltipProvider.apply(getIntValue());
			if (tip != null) {
				setTooltip(Tooltip.create(tip));
			}
		}
	}

	@Override
	protected void updateMessage() {
		setMessage(Component.empty().append(prefix).append(": ").append(getIntValue() + suffix));
	}

	@Override
	protected void applyValue() {
		refreshDynamicTooltip();
		if (!suppressCallback && onChanged != null) {
			onChanged.accept(getIntValue());
		}
	}

	private static double toNormalized(int value, int min, int max) {
		if (max <= min) {
			return 0.0;
		}
		return Mth.clamp((value - min) / (double) (max - min), 0.0, 1.0);
	}
}
