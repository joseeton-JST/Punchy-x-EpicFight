package com.punchyepicfightcompat.client.config;

import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.electronwill.nightconfig.toml.TomlParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/** Reads and writes the complete client configuration, including dynamic animation IDs. */
public final class UnifiedConfigFile {
    public static final int CURRENT_VERSION = 3;
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9/._-]+");

    private UnifiedConfigFile() {}

    public static ReadResult read(Path path) throws IOException {
        Settings defaults = Settings.defaults();
        if (!Files.isRegularFile(path)) {
            return new ReadResult(defaults, List.of());
        }

        UnmodifiableConfig root;
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            root = new TomlParser().parse(reader);
        }

        List<String> problems = new ArrayList<>();
        int version = number(root.getRaw("configVersion"), 1, "configVersion", problems);
        boolean forceMode = bool(root.getRaw("mode.forceEpicFightMode"), true, "mode.forceEpicFightMode", problems);
        boolean emptyHandBlockBreaking = bool(
            root.getRaw("fixes.emptyHandBlockBreaking"),
            true,
            "fixes.emptyHandBlockBreaking",
            problems
        );
        List<String> namespaces = strings(
            root.getRaw("rendering.epicWeaponNamespaces"),
            defaults.epicWeaponNamespaces(),
            "rendering.epicWeaponNamespaces",
            problems
        );
        List<String> forceEpic = strings(
            root.getRaw("rendering.forceEpicItems"),
            defaults.forceEpicItems(),
            "rendering.forceEpicItems",
            problems
        );
        List<String> forcePunchy = strings(
            root.getRaw("rendering.forcePunchyItems"),
            defaults.forcePunchyItems(),
            "rendering.forcePunchyItems",
            problems
        );

        Map<String, Boolean> animations = new LinkedHashMap<>();
        Object animationRoot = root.getRaw("animations");
        if (animationRoot instanceof UnmodifiableConfig animationModules) {
            for (Map.Entry<String, Object> namespaceEntry : animationModules.valueMap().entrySet()) {
                String namespace = namespaceEntry.getKey();
                if (!NAMESPACE.matcher(namespace).matches() || !(namespaceEntry.getValue() instanceof UnmodifiableConfig entries)) {
                    problems.add("animations." + namespace);
                    continue;
                }
                for (Map.Entry<String, Object> animationEntry : entries.valueMap().entrySet()) {
                    String animationPath = animationEntry.getKey();
                    Object value = animationEntry.getValue();
                    if (!PATH.matcher(animationPath).matches() || !(value instanceof Boolean booleanValue)) {
                        problems.add("animations.\"" + namespace + "\".\"" + animationPath + "\"");
                        continue;
                    }
                    animations.put(namespace + ":" + animationPath, booleanValue);
                }
            }
        } else if (animationRoot != null) {
            problems.add("animations");
        }

        return new ReadResult(
            new Settings(version, forceMode, emptyHandBlockBreaking, namespaces, forceEpic, forcePunchy, animations),
            List.copyOf(problems)
        );
    }

    public static void write(Path path, Settings settings) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        StringBuilder output = new StringBuilder(65536);
        output.append("# Punchy x Epic Fight Compatibility - client-only configuration.\n")
            .append("# Animation booleans affect only first-person rendering:\n")
            .append("# true = Epic Fight renderer; false = Punchy renderer.\n")
            .append("# Animation sections are grouped by mod namespace only for organization.\n\n")
            .append("configVersion = ").append(CURRENT_VERSION).append("\n\n")
            .append("[mode]\n")
            .append("# Keep the player in Epic Fight battle mode and block switching to vanilla mode.\n")
            .append("forceEpicFightMode = ").append(settings.forceEpicFightMode()).append("\n\n")
            .append("[fixes]\n")
            .append("# With an empty main hand, mine a reachable targeted block through vanilla when no attackable living entity is nearby.\n")
            .append("emptyHandBlockBreaking = ").append(settings.emptyHandBlockBreaking()).append("\n\n")
            .append("[rendering]\n")
            .append("# Namespaces eligible for automatic Epic Fight weapon rendering.\n");
        appendList(output, "epicWeaponNamespaces", settings.epicWeaponNamespaces());
        output.append("\n# Exact item IDs or item tags forced to Epic outside configured animations.\n");
        appendList(output, "forceEpicItems", settings.forceEpicItems());
        output.append("\n# Exact item IDs or item tags forced to Punchy outside configured animations.\n");
        appendList(output, "forcePunchyItems", settings.forcePunchyItems());

        Map<String, Map<String, Boolean>> modules = new TreeMap<>();
        settings.animations().forEach((id, value) -> {
            int separator = id.indexOf(':');
            if (separator > 0 && separator < id.length() - 1) {
                modules.computeIfAbsent(id.substring(0, separator), ignored -> new TreeMap<>())
                    .put(id.substring(separator + 1), value);
            }
        });

        for (Map.Entry<String, Map<String, Boolean>> module : modules.entrySet()) {
            output.append("\n\n# ============================================================\n")
                .append("# ANIMATIONS: ").append(module.getKey()).append("\n")
                .append("# ============================================================\n")
                .append("[animations.\"").append(escape(module.getKey())).append("\"]\n");
            module.getValue().forEach((animation, useEpic) -> output
                .append('"').append(escape(animation)).append("\" = ").append(useEpic).append('\n'));
        }

        Path temporary = path.resolveSibling(path.getFileName() + ".tmp");
        Files.writeString(temporary, output.toString(), StandardCharsets.UTF_8);
        try {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicMoveFailure) {
            Files.move(temporary, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void appendList(StringBuilder output, String name, List<String> values) {
        output.append(name).append(" = [");
        if (!values.isEmpty()) {
            output.append('\n');
            for (String value : values) {
                output.append("    \"").append(escape(value)).append("\",\n");
            }
        }
        output.append("]\n");
    }

    private static int number(Object value, int fallback, String path, List<String> problems) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        problems.add(path);
        return fallback;
    }

    private static boolean bool(Object value, boolean fallback, String path, List<String> problems) {
        if (value == null) {
            return fallback;
        }
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        problems.add(path);
        return fallback;
    }

    private static List<String> strings(Object value, List<String> fallback, String path, List<String> problems) {
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof List<?> list)) {
            problems.add(path);
            return fallback;
        }
        List<String> result = new ArrayList<>();
        for (Object entry : list) {
            if (entry instanceof String string) {
                result.add(string);
            } else {
                problems.add(path);
            }
        }
        return List.copyOf(result);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record Settings(
        int configVersion,
        boolean forceEpicFightMode,
        boolean emptyHandBlockBreaking,
        List<String> epicWeaponNamespaces,
        List<String> forceEpicItems,
        List<String> forcePunchyItems,
        Map<String, Boolean> animations
    ) {
        private static final List<String> DEFAULT_EPIC_WEAPON_NAMESPACES = List.of(
            "epicfight", "efn", "wom", "epicfightx", "invincible", "epicfight_awaken",
            "epicawaken_grappling_hook", "epic_fight_avalon", "combat_evolution", "epicparcool",
            "epictweaks", "epicfightcombocontinuity", "epicfight_curios_compat",
            "epicfightdragonfix", "visualhealth_epicfight", "epicfightparaglidercompat"
        );

        public Settings {
            epicWeaponNamespaces = List.copyOf(epicWeaponNamespaces);
            forceEpicItems = List.copyOf(forceEpicItems);
            forcePunchyItems = List.copyOf(forcePunchyItems);
            animations = Map.copyOf(animations);
        }

        public static Settings defaults() {
            return new Settings(CURRENT_VERSION, true, true, DEFAULT_EPIC_WEAPON_NAMESPACES, List.of(), List.of(), Map.of());
        }

        public Settings withAnimations(Map<String, Boolean> updatedAnimations) {
            return new Settings(
                CURRENT_VERSION,
                forceEpicFightMode,
                emptyHandBlockBreaking,
                epicWeaponNamespaces,
                forceEpicItems,
                forcePunchyItems,
                updatedAnimations
            );
        }

        public Settings withCurrentVersion() {
            return new Settings(
                CURRENT_VERSION,
                forceEpicFightMode,
                emptyHandBlockBreaking,
                epicWeaponNamespaces,
                forceEpicItems,
                forcePunchyItems,
                animations
            );
        }
    }

    public record ReadResult(Settings settings, List<String> problems) {}
}
