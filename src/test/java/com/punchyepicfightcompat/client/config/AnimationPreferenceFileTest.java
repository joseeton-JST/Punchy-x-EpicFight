package com.punchyepicfightcompat.client.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnimationPreferenceFileTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void writesAndReadsBooleanAnimationMap() throws Exception {
        Path path = temporaryDirectory.resolve("animations.toml");
        Map<String, Boolean> expected = new LinkedHashMap<>();
        expected.put("wom:biped/combat/torment_auto_1", false);
        expected.put("epicfight:biped/combat/sword_auto_1", true);

        AnimationPreferenceFile.write(path, expected);
        AnimationPreferenceFile.ReadResult result = AnimationPreferenceFile.read(path);

        assertEquals(expected, result.values());
        assertTrue(result.invalidLines().isEmpty());
    }

    @Test
    void ignoresOtherSectionsAndReportsMalformedAnimationEntries() throws Exception {
        Path path = temporaryDirectory.resolve("animations.toml");
        Files.writeString(path, """
            [unrelated]
            "example:value" = true

            [animations]
            "epicfight:valid/action" = false
            not_a_quoted_key = true
            "epicfight:also_valid" = true # comment
            """);

        AnimationPreferenceFile.ReadResult result = AnimationPreferenceFile.read(path);

        assertEquals(
            Map.of("epicfight:valid/action", false, "epicfight:also_valid", true),
            result.values()
        );
        assertEquals(1, result.invalidLines().size());
    }

    @Test
    void quotedKeysRoundTripEscapes() throws Exception {
        Path path = temporaryDirectory.resolve("animations.toml");
        Map<String, Boolean> expected = Map.of("test:path/with\\slash\"quote", false);

        AnimationPreferenceFile.write(path, expected);

        assertEquals(expected, AnimationPreferenceFile.read(path).values());
    }
}
