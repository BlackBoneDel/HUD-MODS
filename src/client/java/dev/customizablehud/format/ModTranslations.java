package dev.customizablehud.format;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.customizablehud.config.ModLanguage;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.IllegalFormatException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class ModTranslations {
	private static final Gson GSON = new Gson();
	private static final Map<ModLanguage, Map<String, String>> BUNDLED_CACHE = new EnumMap<>(ModLanguage.class);

	private ModTranslations() {
	}

	public static synchronized Map<String, String> getBundledTranslations(ModLanguage language) {
		ModLanguage safeLang = language != null ? language : ModLanguage.ENGLISH;
		return BUNDLED_CACHE.computeIfAbsent(safeLang, ModTranslations::loadBundledFor);
	}

	private static Map<String, String> loadBundledFor(ModLanguage language) {
		String resourcePath = "/assets/customizablehud/lang/" + language.code() + ".json";
		try (InputStream stream = ModTranslations.class.getResourceAsStream(resourcePath)) {
			if (stream == null) {
				return Collections.emptyMap();
			}
			return parseJsonStream(stream);
		} catch (IOException | RuntimeException ex) {
			return Collections.emptyMap();
		}
	}

	public static Map<String, String> parseJsonStream(InputStream stream) {
		if (stream == null) {
			return Collections.emptyMap();
		}
		try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			JsonElement parsed = JsonParser.parseReader(reader);
			if (parsed == null || !parsed.isJsonObject()) {
				return Collections.emptyMap();
			}
			JsonObject obj = parsed.getAsJsonObject();
			Map<String, String> result = new LinkedHashMap<>();
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				if (entry.getKey() != null && entry.getValue() != null && entry.getValue().isJsonPrimitive()) {
					result.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
			return Collections.unmodifiableMap(result);
		} catch (IOException | RuntimeException ex) {
			return Collections.emptyMap();
		}
	}

	public static String translateBundled(ModLanguage language, String key, Object... args) {
		if (key == null) {
			return "";
		}
		Map<String, String> selected = getBundledTranslations(language);
		String template = selected.get(key);
		if (template == null && language != ModLanguage.ENGLISH) {
			template = getBundledTranslations(ModLanguage.ENGLISH).get(key);
		}
		if (template == null) {
			template = key;
		}
		return safeFormat(template, args);
	}

	public static String resolveWithFallbacks(
			String key,
			Map<String, String> preferredTranslations,
			Map<String, String> gameLanguageFallback,
			Map<String, String> englishFallback,
			String readableFallbackId,
			Object... args
	) {
		String template = null;
		if (key != null) {
			if (preferredTranslations != null) {
				template = preferredTranslations.get(key);
			}
			if (template == null && gameLanguageFallback != null) {
				template = gameLanguageFallback.get(key);
			}
			if (template == null && englishFallback != null) {
				template = englishFallback.get(key);
			}
		}
		if (template != null) {
			return safeFormat(template, args);
		}
		if (readableFallbackId != null && !readableFallbackId.isBlank()) {
			return humanizeIdentifier(readableFallbackId);
		}
		return key != null ? humanizeTranslationKey(key) : "";
	}

	public static String safeFormat(String template, Object... args) {
		if (template == null) {
			return "";
		}
		if (args == null || args.length == 0) {
			return template;
		}
		try {
			return String.format(Locale.ROOT, template, args);
		} catch (IllegalFormatException ex) {
			return template;
		}
	}

	public static String humanizeIdentifier(String rawId) {
		if (rawId == null || rawId.isBlank()) {
			return "";
		}
		String trimmed = rawId.trim();
		int colon = trimmed.indexOf(':');
		String path = colon >= 0 ? trimmed.substring(colon + 1) : trimmed;
		int slash = path.lastIndexOf('/');
		if (slash >= 0) {
			path = path.substring(slash + 1);
		}
		return titleCaseWords(path.replace('_', ' ').replace('-', ' ').replace('.', ' '));
	}

	public static String humanizeTranslationKey(String key) {
		if (key == null || key.isBlank()) {
			return "";
		}
		int lastDot = key.lastIndexOf('.');
		String tail = lastDot >= 0 && lastDot + 1 < key.length() ? key.substring(lastDot + 1) : key;
		return titleCaseWords(tail.replace('_', ' ').replace('-', ' '));
	}

	private static String titleCaseWords(String text) {
		String[] parts = text.trim().split("\\s+");
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (part.isEmpty()) {
				continue;
			}
			if (!sb.isEmpty()) {
				sb.append(' ');
			}
			sb.append(Character.toUpperCase(part.charAt(0)));
			if (part.length() > 1) {
				sb.append(part.substring(1));
			}
		}
		return sb.toString();
	}
}
