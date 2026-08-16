package com.punchyepicfightcompat.client.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** One-time, recoverable migration from the former Forge config plus animation catalogue. */
public final class UnifiedConfigMigration {
    private UnifiedConfigMigration() {}

    public static Result migrate(Path unifiedPath, Path legacyAnimationsPath) throws IOException {
        UnifiedConfigFile.ReadResult existing = UnifiedConfigFile.read(unifiedPath);
        boolean legacyAnimationsExist = Files.isRegularFile(legacyAnimationsPath);
        if (existing.settings().configVersion() >= UnifiedConfigFile.CURRENT_VERSION && !legacyAnimationsExist) {
            return new Result(false, List.of());
        }

        Map<String, Boolean> animations = new LinkedHashMap<>(existing.settings().animations());
        if (legacyAnimationsExist) {
            AnimationPreferenceFile.ReadResult legacy = AnimationPreferenceFile.read(legacyAnimationsPath);
            animations.putAll(legacy.values());
        }

        List<Path> backups = new ArrayList<>();
        if (Files.isRegularFile(unifiedPath)) {
            Path backup = availableBackup(unifiedPath);
            Files.copy(unifiedPath, backup, StandardCopyOption.COPY_ATTRIBUTES);
            backups.add(backup);
        }

        UnifiedConfigFile.write(unifiedPath, existing.settings().withAnimations(animations));

        if (legacyAnimationsExist) {
            Path backup = availableBackup(legacyAnimationsPath);
            Files.move(legacyAnimationsPath, backup);
            backups.add(backup);
        }
        return new Result(true, List.copyOf(backups));
    }

    private static Path availableBackup(Path original) {
        String fileName = original.getFileName().toString();
        int extension = fileName.lastIndexOf('.');
        String base = extension < 0 ? fileName : fileName.substring(0, extension);
        String suffix = extension < 0 ? "" : fileName.substring(extension);
        Path candidate = original.resolveSibling(base + ".before-unified-config" + suffix);
        int index = 1;
        while (Files.exists(candidate)) {
            candidate = original.resolveSibling(base + ".before-unified-config." + index + suffix);
            index++;
        }
        return candidate;
    }

    public record Result(boolean migrated, List<Path> backups) {}
}
