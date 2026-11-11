package dev.customizablehud.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.function.LongSupplier;

public final class ConfigManager {
	public enum SaveStatus {
		SAVED("customizablehud.save_status.saved"),
		UNSAVED("customizablehud.save_status.unsaved"),
		FAILED("customizablehud.save_status.failed");

		private final String translationKey;

		SaveStatus(String translationKey) {
			this.translationKey = translationKey;
		}

		public String translationKey() {
			return translationKey;
		}
	}

	public static final long DEFAULT_AUTO_SAVE_DEBOUNCE_MS = 450L;
	private static final long FAILURE_BACKOFF_MS = 5000L;
	private static final long LOG_COOLDOWN_MS = 10000L;

	private static final System.Logger LOGGER = System.getLogger("CustomizableHUD/Config");
	private static final Gson GSON = new GsonBuilder()
			.setPrettyPrinting()
			.disableHtmlEscaping()
			.create();

	private final Path configPath;
	private final LongSupplier clockMs;
	private final long debounceMs;

	private HudConfig config;
	private boolean recoveredFromCorruptFile;
	private Path lastCorruptBackupPath;

	private boolean dirty;
	private SaveStatus saveStatus = SaveStatus.SAVED;
	private long autoSaveDeadlineMs;
	private String lastSaveError;
	private String lastLoggedErrorKey;
	private long lastLoggedErrorAtMs = Long.MIN_VALUE;

	public ConfigManager(Path configPath) {
		this(configPath, System::currentTimeMillis, DEFAULT_AUTO_SAVE_DEBOUNCE_MS);
	}

	public ConfigManager(Path configPath, LongSupplier clockMs, long debounceMs) {
		this.configPath = Objects.requireNonNull(configPath, "configPath");
		this.clockMs = Objects.requireNonNull(clockMs, "clockMs");
		this.debounceMs = Math.max(0L, debounceMs);
		this.config = HudConfig.createDefault();
	}

	public synchronized HudConfig getConfig() {
		return config;
	}

	public synchronized void setConfig(HudConfig newConfig) {
		ConfigValidator.ValidationResult validated = ConfigValidator.validateAndSanitize(newConfig);
		this.config = validated.config();
	}

	public synchronized void applyAndScheduleSave(HudConfig updated) {
		setConfig(updated);
		markDirty();
	}

	public synchronized boolean applyAndSaveNow(HudConfig updated) {
		setConfig(updated);
		this.dirty = true;
		return saveNow();
	}

	public synchronized void markDirty() {
		ConfigValidator.ValidationResult validated = ConfigValidator.validateAndSanitize(this.config);
		this.config = validated.config();
		this.dirty = true;
		this.saveStatus = SaveStatus.UNSAVED;
		this.lastSaveError = null;
		this.autoSaveDeadlineMs = clockMs.getAsLong() + debounceMs;
	}

	public synchronized boolean tickAutoSave() {
		if (!dirty) {
			return true;
		}
		if (clockMs.getAsLong() < autoSaveDeadlineMs) {
			return false;
		}
		return saveNow();
	}

	public synchronized boolean flushPendingSaves() {
		if (!dirty) {
			return saveStatus != SaveStatus.FAILED;
		}
		return saveNow();
	}

	public synchronized boolean isDirty() {
		return dirty;
	}

	public synchronized SaveStatus getSaveStatus() {
		return saveStatus;
	}

	public synchronized String getLastSaveError() {
		return lastSaveError;
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
			this.dirty = true;
			saveNow();
			return this.config;
		}

		try {
			String raw = Files.readString(configPath, StandardCharsets.UTF_8);
			if (raw == null || raw.isBlank()) {
				throw new JsonParseException("Configuration file is empty");
			}
			JsonElement element = JsonParser.parseString(raw);
			if (element == null || !element.isJsonObject()) {
				throw new JsonParseException("Configuration root must be a JSON object");
			}
			JsonObject rootObj = element.getAsJsonObject();
			HudConfig parsed = GSON.fromJson(rootObj, HudConfig.class);
			if (parsed == null) {
				throw new JsonParseException("Parsed configuration was null");
			}

			boolean migrated = ConfigMigrator.migrate(rootObj, parsed);
			ConfigValidator.ValidationResult validated = ConfigValidator.validateAndSanitize(parsed);
			this.config = validated.config();

			if (migrated || validated.modified()) {
				this.dirty = true;
				saveNow();
			} else {
				this.dirty = false;
				this.saveStatus = SaveStatus.SAVED;
				this.lastSaveError = null;
			}
			return this.config;
		} catch (IOException | RuntimeException ex) {
			LOGGER.log(System.Logger.Level.WARNING, "Failed to read HUD config; backing up corrupt file and restoring defaults", ex);
			backupCorruptFile();
			this.config = HudConfig.createDefault();
			this.recoveredFromCorruptFile = true;
			this.dirty = true;
			saveNow();
			return this.config;
		}
	}

	public synchronized boolean save() {
		return saveNow();
	}

	public synchronized boolean saveNow() {
		ConfigValidator.ValidationResult validated = ConfigValidator.validateAndSanitize(this.config);
		this.config = validated.config();
		HudConfig immutableSnapshot = this.config.copy();

		Path tempPath = null;
		try {
			Path parent = configPath.getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			String json = GSON.toJson(immutableSnapshot) + System.lineSeparator();
			tempPath = configPath.resolveSibling(configPath.getFileName() + ".tmp");
			Files.writeString(tempPath, json, StandardCharsets.UTF_8);
			try {
				Files.move(tempPath, configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException ignored) {
				Files.move(tempPath, configPath, StandardCopyOption.REPLACE_EXISTING);
			}
			this.dirty = false;
			this.saveStatus = SaveStatus.SAVED;
			this.lastSaveError = null;
			this.autoSaveDeadlineMs = 0L;
			return true;
		} catch (IOException | RuntimeException ex) {
			this.dirty = true;
			this.saveStatus = SaveStatus.FAILED;
			String detail = ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : ex.getClass().getSimpleName();
			this.lastSaveError = detail;
			long now = clockMs.getAsLong();
			this.autoSaveDeadlineMs = now + FAILURE_BACKOFF_MS;
			logSaveFailureWithoutSpam(detail, ex, now);
			return false;
		} finally {
			if (tempPath != null) {
				try {
					Files.deleteIfExists(tempPath);
				} catch (IOException ignored) {
					// Best-effort temp cleanup
				}
			}
		}
	}

	private void logSaveFailureWithoutSpam(String detail, Throwable ex, long nowMs) {
		String errorKey = ex.getClass().getName() + ":" + detail;
		boolean sameError = errorKey.equals(lastLoggedErrorKey);
		if (!sameError || lastLoggedErrorAtMs == Long.MIN_VALUE || (nowMs - lastLoggedErrorAtMs) >= LOG_COOLDOWN_MS) {
			LOGGER.log(System.Logger.Level.ERROR, "Failed to save HUD config to " + configPath + ": " + detail, ex);
			lastLoggedErrorKey = errorKey;
			lastLoggedErrorAtMs = nowMs;
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
