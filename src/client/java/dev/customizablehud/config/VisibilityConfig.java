package dev.customizablehud.config;

public final class VisibilityConfig {
	public boolean hideInInventory = true;
	public boolean hideInChat = false;
	public boolean hideInPauseScreen = true;
	public boolean requireActiveWorld = true;

	public VisibilityConfig copy() {
		VisibilityConfig copy = new VisibilityConfig();
		copy.hideInInventory = hideInInventory;
		copy.hideInChat = hideInChat;
		copy.hideInPauseScreen = hideInPauseScreen;
		copy.requireActiveWorld = requireActiveWorld;
		return copy;
	}
}
