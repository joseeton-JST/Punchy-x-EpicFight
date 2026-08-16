package com.punchyepicfightcompat.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnifiedConfigMigrationTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void mergesBothLegacyFilesAndKeepsRecoverableBackups() throws Exception {
        Path client = temporaryDirectory.resolve("punchyepicfightcompat-client.toml");
        Path animations = temporaryDirectory.resolve("punchyepicfightcompat-animations.toml");
        Files.writeString(client, """
            [mode]
            forceEpicFightMode = false
            [rendering]
            epicWeaponNamespaces = ["epicfight", "wom"]
            forceEpicItems = ["minecraft:diamond_sword"]
            forcePunchyItems = ["minecraft:stick"]
            """);
        Map<String, Boolean> oldAnimations = new LinkedHashMap<>();
        oldAnimations.put("epicparcool:biped/hang_down", true);
        oldAnimations.put("epicfight:biped/living/idle", false);
        AnimationPreferenceFile.write(animations, oldAnimations);

        UnifiedConfigMigration.Result result = UnifiedConfigMigration.migrate(client, animations);
        UnifiedConfigFile.Settings migrated = UnifiedConfigFile.read(client).settings();

        assertTrue(result.migrated());
        assertEquals(2, result.backups().size());
        assertTrue(result.backups().stream().allMatch(Files::isRegularFile));
        assertFalse(Files.exists(animations));
        assertFalse(migrated.forceEpicFightMode());
        assertEquals(oldAnimations, migrated.animations());
        assertEquals(Map.of("epicparcool:biped/hang_down", true, "epicfight:biped/living/idle", false), migrated.animations());
        assertEquals(java.util.List.of("epicfight", "wom"), migrated.epicWeaponNamespaces());
    }

    @Test
    void currentUnifiedFileIsNotMigratedAgain() throws Exception {
        Path client = temporaryDirectory.resolve("punchyepicfightcompat-client.toml");
        UnifiedConfigFile.write(client, UnifiedConfigFile.Settings.defaults());

        UnifiedConfigMigration.Result result = UnifiedConfigMigration.migrate(
            client,
            temporaryDirectory.resolve("missing-animations.toml")
        );

        assertFalse(result.migrated());
        assertTrue(result.backups().isEmpty());
    }
}
