package dev.customizablehud.client.screen;

import dev.customizablehud.config.ConfigValidator;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

public final class ScaleSliderButton extends AbstractSliderButton {
	private final Component prefix;
	private final float min;
	private final float max;
	private final Consumer<Float> onChanged;
	private boolean suppressCallback;

	public ScaleSliderButton(int x, int y, int width, int height, Component prefix, float initialValue, Consumer<Float> onChanged) {
		this(x, y, width, height, prefix, ConfigValidator.MIN_SCALE, ConfigValidator.MAX_SCALE, initialValue, onChanged);
	}

	public ScaleSliderButton(
			int x,
			int y,
			int width,
			int height,
			Component prefix,
			float min,
			float max,
			float initialValue,
			Consumer<Float> onChanged
	) {
		super(x, y, width, height, Component.empty(), toNormalized(initialValue, min, max));
		this.prefix = prefix;
		this.min = min;
		this.max = max;
		this.onChanged = onChanged;
		updateMessage();
	}

	public float getScaleValue() {
		float raw = (float) (min + value * (max - min));
		return Math.round(raw * 20.0f) / 20.0f;
	}

	public void setScaleValue(float scale) {
		suppressCallback = true;
		setValue(toNormalized(scale, min, max));
		suppressCallback = false;
	}

	@Override
	protected void updateMessage() {
		String formatted = String.format(Locale.ROOT, "%.2fx", getScaleValue());
		setMessage(Component.empty().append(prefix).append(": ").append(formatted));
	}

	@Override
	protected void applyValue() {
		if (!suppressCallback && onChanged != null) {
			onChanged.accept(getScaleValue());
		}
	}

	private static double toNormalized(float scale, float min, float max) {
		if (max <= min) {
			return 0.0;
		}
		return Mth.clamp((scale - min) / (max - min), 0.0, 1.0);
	}
}
