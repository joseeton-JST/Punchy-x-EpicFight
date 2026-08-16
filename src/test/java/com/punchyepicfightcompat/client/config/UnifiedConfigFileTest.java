package com.punchyepicfightcompat.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UnifiedConfigFileTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsAllSettingsAndGroupsAnimationsByNamespace() throws Exception {
        Path path = temporaryDirectory.resolve("punchyepicfightcompat-client.toml");
        Map<String, Boolean> animations = new LinkedHashMap<>();
        animations.put("epicparcool:biped/hang_down", true);
        animations.put("epicfight:biped/living/idle", false);
        UnifiedConfigFile.Settings expected = new UnifiedConfigFile.Settings(
            UnifiedConfigFile.CURRENT_VERSION,
            false,
            true,
            List.of("epicfight", "wom"),
            List.of("minecraft:diamond_sword"),
            List.of("minecraft:stick"),
            animations
        );

        UnifiedConfigFile.write(path, expected);
        UnifiedConfigFile.ReadResult result = UnifiedConfigFile.read(path);

        assertEquals(expected, result.settings());
        assertTrue(result.problems().isEmpty());
        String written = Files.readString(path);
        assertTrue(written.contains("[animations.\"epicfight\"]"));
        assertTrue(written.contains("[animations.\"epicparcool\"]"));
        assertFalse(written.contains("enabled ="));
    }

    @Test
    void eachAnimationBooleanKeepsItsExactRendererMeaning() throws Exception {
        Path path = temporaryDirectory.resolve("config.toml");
        Files.writeString(path, """
            configVersion = 2
            [animations."epicparcool"]
            "biped/hang_down" = true
            "biped/crawl" = false
            """);

        Map<String, Boolean> values = UnifiedConfigFile.read(path).settings().animations();

        assertEquals(true, values.get("epicparcool:biped/hang_down"));
        assertEquals(false, values.get("epicparcool:biped/crawl"));
        assertTrue(UnifiedConfigFile.read(path).settings().emptyHandBlockBreaking());
    }

    @Test
    void invalidAnimationEntriesAreReportedAndIgnored() throws Exception {
        Path path = temporaryDirectory.resolve("config.toml");
        Files.writeString(path, """
            configVersion = 2
            [animations."epicparcool"]
            "biped/hang_down" = true
            "Invalid Path" = false
            "biped/not_boolean" = "Epic"
            """);

        UnifiedConfigFile.ReadResult result = UnifiedConfigFile.read(path);

        assertEquals(Map.of("epicparcool:biped/hang_down", true), result.settings().animations());
        assertEquals(2, result.problems().size());
    }
}
