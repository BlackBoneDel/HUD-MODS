package dev.customizablehud.config;

public final class PointOffset {
	public int x;
	public int y;

	public PointOffset() {
		this(0, 0);
	}

	public PointOffset(int x, int y) {
		this.x = x;
		this.y = y;
	}

	public PointOffset copy() {
		return new PointOffset(x, y);
	}
}
