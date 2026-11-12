package dev.customizablehud.client.i18n;

import dev.customizablehud.client.CustomizableHudClient;
import dev.customizablehud.config.HudConfig;
import dev.customizablehud.config.ModLanguage;
import dev.customizablehud.format.ModTranslations;
import java.io.InputStream;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

public final class ModI18n {
	private static final Map<ModLanguage, Map<String, String>> RESOURCE_TRANSLATIONS = new EnumMap<>(ModLanguage.class);
	private static volatile boolean loadedFromResources = false;

	private ModI18n() {
	}

	public static ModLanguage activeLanguage() {
		HudConfig config = CustomizableHudClient.getConfig();
		return config != null && config.language != null ? config.language : ModLanguage.ENGLISH;
	}

	public static synchronized void reloadFromResources(ResourceManager manager) {
		for (ModLanguage lang : ModLanguage.values()) {
			Map<String, String> merged = new HashMap<>(ModTranslations.getBundledTranslations(lang));
			if (manager != null) {
				String path = "lang/" + lang.code() + ".json";
				for (String namespace : manager.getNamespaces()) {
					try {
						Identifier id = Identifier.fromNamespaceAndPath(namespace, path);
						List<Resource> stack = manager.getResourceStack(id);
						for (Resource resource : stack) {
							try (InputStream stream = resource.open()) {
								Language.loadFromJson(stream, merged::put);
							}
						}
					} catch (Exception ignored) {
						// Ignore malformed or unreadable external language file without crashing
					}
				}
			}
			RESOURCE_TRANSLATIONS.put(lang, Collections.unmodifiableMap(merged));
		}
		loadedFromResources = true;
	}

	private static void ensureLoaded() {
		if (loadedFromResources) {
			return;
		}
		Minecraft client = Minecraft.getInstance();
		if (client != null && client.getResourceManager() != null) {
			reloadFromResources(client.getResourceManager());
		}
	}

	private static Map<String, String> mapFor(ModLanguage lang) {
		ensureLoaded();
		ModLanguage safeLang = lang != null ? lang : ModLanguage.ENGLISH;
		Map<String, String> loaded = RESOURCE_TRANSLATIONS.get(safeLang);
		if (loaded != null && !loaded.isEmpty()) {
			return loaded;
		}
		return ModTranslations.getBundledTranslations(safeLang);
	}

	public static String string(String key, Object... args) {
		return string(activeLanguage(), key, args);
	}

	public static String string(ModLanguage lang, String key, Object... args) {
		if (key == null) {
			return "";
		}
		Map<String, String> primary = mapFor(lang);
		String template = primary.get(key);
		if (template == null && lang != ModLanguage.ENGLISH) {
			template = mapFor(ModLanguage.ENGLISH).get(key);
		}
		if (template == null) {
			Language mcLang = Language.getInstance();
			if (mcLang != null && mcLang.has(key)) {
				template = mcLang.getOrDefault(key);
			}
		}
		if (template == null) {
			template = key;
		}
		return ModTranslations.safeFormat(template, args);
	}

	public static Component text(String key, Object... args) {
		return Component.literal(string(key, args));
	}

	public static Component text(ModLanguage lang, String key, Object... args) {
		return Component.literal(string(lang, key, args));
	}

	public static String translateExternal(
			ModLanguage lang,
			String translationKey,
			Component gameFallbackComponent,
			String fallbackId
	) {
		ModLanguage safeLang = lang != null ? lang : ModLanguage.ENGLISH;
		if (translationKey != null && !translationKey.isBlank()) {
			Map<String, String> selected = mapFor(safeLang);
			String found = selected.get(translationKey);
			if (found != null && !found.isBlank()) {
				return found;
			}
		}

		Language mcLang = Language.getInstance();
		if (translationKey != null && mcLang != null && mcLang.has(translationKey)) {
			String mcValue = mcLang.getOrDefault(translationKey);
			if (mcValue != null && !mcValue.isBlank() && !mcValue.equals(translationKey)) {
				return mcValue;
			}
		}

		if (gameFallbackComponent != null) {
			String gameStr = gameFallbackComponent.getString();
			if (gameStr != null && !gameStr.isBlank() && !gameStr.equals(translationKey)) {
				return gameStr;
			}
		}

		if (translationKey != null && safeLang != ModLanguage.ENGLISH) {
			String enFallback = mapFor(ModLanguage.ENGLISH).get(translationKey);
			if (enFallback != null && !enFallback.isBlank()) {
				return enFallback;
			}
		}

		if (fallbackId != null && !fallbackId.isBlank()) {
			return ModTranslations.humanizeIdentifier(fallbackId);
		}
		return translationKey != null ? ModTranslations.humanizeTranslationKey(translationKey) : "";
	}
}
