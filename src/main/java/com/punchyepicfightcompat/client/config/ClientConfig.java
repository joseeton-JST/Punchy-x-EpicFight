package com.punchyepicfightcompat.client.config;

import com.punchyepicfightcompat.PunchyEpicFightCompat;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import net.minecraftforge.fml.loading.FMLPaths;

/** Runtime owner of the single, dynamically extensible client TOML. */
public final class ClientConfig {
    public static final String FILE_NAME = PunchyEpicFightCompat.MOD_ID + "-client.toml";
    public static final String LEGACY_ANIMATION_FILE_NAME = PunchyEpicFightCompat.MOD_ID + "-animations.toml";
    private static final long RELOAD_INTERVAL_TICKS = 100L;
    private static final Object LOCK = new Object();

    private static volatile UnifiedConfigFile.Settings settings = UnifiedConfigFile.Settings.defaults();
    private static boolean loaded;
    private static boolean warnedInvalid;
    private static long lastCheckTick = Long.MIN_VALUE;
    private static Object lastPlayer;
    private static FileTime knownModifiedTime;

    private ClientConfig() {}

    public static void tick() {
        net.minecraft.client.Minecraft minecraft = net.minecraft.client.Minecraft.getInstance();
        if (minecraft.player == null) {
            lastPlayer = null;
            lastCheckTick = Long.MIN_VALUE;
            return;
        }

        if (lastPlayer != minecraft.player) {
            lastPlayer = minecraft.player;
            lastCheckTick = Long.MIN_VALUE;
        }

        long tick = minecraft.player.tickCount;
        if (lastCheckTick != Long.MIN_VALUE && tick - lastCheckTick < RELOAD_INTERVAL_TICKS) {
            return;
        }
        lastCheckTick = tick;

        synchronized (LOCK) {
            Path path = path();
            if (!loaded) {
                migrateLegacyFiles(path, legacyAnimationPath());
            }
            FileTime modified = modifiedTime(path);
            if (!loaded || !Objects.equals(modified, knownModifiedTime)) {
                reload(path);
            }
        }
    }

    public static UnifiedConfigFile.Settings current() {
        if (!loaded) {
            synchronized (LOCK) {
                if (!loaded) {
                    Path path = path();
                    migrateLegacyFiles(path, legacyAnimationPath());
                    reload(path);
                }
            }
        }
        return settings;
    }

    public static void addDiscoveredAnimations(Map<String, Boolean> discovered) {
        if (discovered.isEmpty()) {
            return;
        }
        synchronized (LOCK) {
            UnifiedConfigFile.Settings before = current();
            Map<String, Boolean> merged = new LinkedHashMap<>(before.animations());
            boolean changed = false;
            for (Map.Entry<String, Boolean> entry : discovered.entrySet()) {
                if (!merged.containsKey(entry.getKey())) {
                    merged.put(entry.getKey(), entry.getValue());
                    changed = true;
                }
            }
            if (!changed) {
                return;
            }

            UnifiedConfigFile.Settings updated = before.withAnimations(merged);
            try {
                UnifiedConfigFile.write(path(), updated);
                settings = updated;
                loaded = true;
                knownModifiedTime = modifiedTime(path());
                PunchyEpicFightCompat.LOGGER.info(
                    "Unified client config contains {} Epic Fight animation choices at {}",
                    merged.size(),
                    path()
                );
            } catch (IOException exception) {
                PunchyEpicFightCompat.LOGGER.warn("Could not update unified client config {}", path(), exception);
            }
        }
    }

    private static void migrateLegacyFiles(Path unifiedPath, Path legacyAnimationsPath) {
        try {
            UnifiedConfigMigration.Result result = UnifiedConfigMigration.migrate(unifiedPath, legacyAnimationsPath);
            if (result.migrated()) {
                PunchyEpicFightCompat.LOGGER.info(
                    "Migrated Punchy x Epic Fight settings into {} (backups: {})",
                    unifiedPath,
                    result.backups()
                );
            }
        } catch (IOException | RuntimeException exception) {
            PunchyEpicFightCompat.LOGGER.warn(
                "Could not migrate legacy Punchy x Epic Fight config files; existing files were left recoverable",
                exception
            );
        }
    }

    private static void reload(Path path) {
        try {
            UnifiedConfigFile.ReadResult result = UnifiedConfigFile.read(path);
            settings = result.settings();
            if (!result.problems().isEmpty() && !warnedInvalid) {
                warnedInvalid = true;
                PunchyEpicFightCompat.LOGGER.warn(
                    "Ignoring invalid entry in {} ({}). The remaining settings were loaded.",
                    path,
                    result.problems().get(0)
                );
            }
            if (!Files.isRegularFile(path) || settings.configVersion() != UnifiedConfigFile.CURRENT_VERSION) {
                settings = settings.withCurrentVersion();
                UnifiedConfigFile.write(path, settings);
            }
        } catch (IOException | RuntimeException exception) {
            PunchyEpicFightCompat.LOGGER.warn("Could not load unified client config {}; using last valid values", path, exception);
        }
        loaded = true;
        knownModifiedTime = modifiedTime(path);
    }

    private static Path path() {
        return FMLPaths.CONFIGDIR.get().resolve(FILE_NAME);
    }

    private static Path legacyAnimationPath() {
        return FMLPaths.CONFIGDIR.get().resolve(LEGACY_ANIMATION_FILE_NAME);
    }

    private static FileTime modifiedTime(Path path) {
        try {
            return Files.isRegularFile(path) ? Files.getLastModifiedTime(path) : null;
        } catch (IOException exception) {
            return null;
        }
    }
}
