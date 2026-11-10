package dev.customizablehud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigManager {
	private static final System.Logger LOGGER = System.getLogger("CustomizableHUD/Config");
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private final Path configPath;
	private HudConfig config;
	private boolean recoveredFromCorruptFile;
	private Path lastCorruptBackupPath;

	public ConfigManager(Path configPath) {
		this.configPath = configPath;
		this.config = HudConfig.createDefault();
	}

	public synchronized HudConfig getConfig() {
		return config;
	}

	public synchronized void setConfig(HudConfig newConfig) {
		ConfigValidator.ValidationResult validated = ConfigValidator.validateAndSanitize(newConfig);
		this.config = validated.config();
	}

	public synchronized boolean consumeRecoveryNotice() {
		boolean recovered = recoveredFromCorruptFile;
		recoveredFromCorruptFile = false;
		return recovered;
	}

	public synchronized Path getLastCorruptBackupPath() {
		return lastCorruptBackupPath;
	}

	public synchronized Path getConfigPath() {
		return configPath;
	}

	public synchronized HudConfig load() {
		if (!Files.exists(configPath)) {
			this.config = HudConfig.createDefault();
			save();
			return this.config;
		}

		try {
			String raw = Files.readString(configPath, StandardCharsets.UTF_8);
			if (raw == null || raw.isBlank()) {
				throw new JsonParseException("Configuration file is empty");
			}
			HudConfig parsed = GSON.fromJson(raw, HudConfig.class);
			if (parsed == null) {
				throw new JsonParseException("Parsed configuration was null");
			}
			ConfigValidator.ValidationResult validated = ConfigValidator.validateAndSanitize(parsed);
			this.config = validated.config();
			if (validated.modified()) {
				save();
			}
			return this.config;
		} catch (JsonParseException | IOException | RuntimeException ex) {
			LOGGER.log(System.Logger.Level.WARNING, "Failed to read HUD config; backing up corrupt file and restoring defaults", ex);
			backupCorruptFile();
			this.config = HudConfig.createDefault();
			this.recoveredFromCorruptFile = true;
			save();
			return this.config;
		}
	}

	public synchronized void save() {
		ConfigValidator.ValidationResult validated = ConfigValidator.validateAndSanitize(this.config);
		this.config = validated.config();
		try {
			Path parent = configPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			String json = GSON.toJson(this.config) + System.lineSeparator();
			Path tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
			Files.writeString(tempPath, json, StandardCharsets.UTF_8);
			try {
				Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException ex) {
			LOGGER.log(System.Logger.Level.ERROR, "Failed to save HUD config to " + configPath, ex);
		}
	}

	private void backupCorruptFile() {
		try {
			if (!Files.exists(configPath)) {
				return;
			}
			String backupName = configPath.getFileName() + ".corrupt-" + System.currentTimeMillis() + ".bak";
			Path backupPath = configPath.resolveSibling(backupName);
			Files.copy(configPath, backupPath, StandardCopyOption.REPLACE_EXISTING);
			this.lastCorruptBackupPath = backupPath;
		} catch (IOException ex) {
			LOGGER.log(System.Logger.Level.ERROR, "Failed to back up corrupt HUD config", ex);
		}
	}
}
