package dev.customizablehud.layout;

public record HudRect(int x, int y, int width, int height) {
	public static final HudRect EMPTY = new HudRect(0, 0, 0, 0);

	public int right() {
		return x + width;
	}

	public int bottom() {
		return y + height;
	}

	public int centerX() {
		return x + width / 2;
	}

	public int centerY() {
		return y + height / 2;
	}

	public boolean isEmpty() {
		return width <= 0 || height <= 0;
	}

	public boolean contains(double px, double py) {
		return !isEmpty() && px >= x && px <= right() && py >= y && py <= bottom();
	}

	public boolean intersects(HudRect other) {
		if (other == null || isEmpty() || other.isEmpty()) {
			return false;
		}
		return x < other.right() && right() > other.x && y < other.bottom() && bottom() > other.y;
	}

	public HudRect clampToScreen(int screenWidth, int screenHeight, int margin) {
		int safeMargin = Math.max(0, margin);
		int minX = width + 2 * safeMargin <= screenWidth ? safeMargin : 0;
		int minY = height + 2 * safeMargin <= screenHeight ? safeMargin : 0;
		int maxX = Math.max(minX, screenWidth - width - minX);
		int maxY = Math.max(minY, screenHeight - height - minY);
		int clampedX = Math.max(minX, Math.min(maxX, x));
		int clampedY = Math.max(minY, Math.min(maxY, y));
		return new HudRect(clampedX, clampedY, width, height);
	}

	public static HudRect union(HudRect a, HudRect b) {
		if (a == null || a.isEmpty()) {
			return b == null ? EMPTY : b;
		}
		if (b == null || b.isEmpty()) {
			return a;
		}
		int minX = Math.min(a.x, b.x);
		int minY = Math.min(a.y, b.y);
		int maxX = Math.max(a.right(), b.right());
		int maxY = Math.max(a.bottom(), b.bottom());
		return new HudRect(minX, minY, maxX - minX, maxY - minY);
	}
}
