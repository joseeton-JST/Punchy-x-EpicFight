package com.punchyepicfightcompat.client.config;

import com.punchyepicfightcompat.PunchyEpicFightCompat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

public final class ItemOverrideMatcher {
    private static volatile Snapshot cached = Snapshot.empty();
    private static volatile int cachedNamespaceHash;
    private static volatile int cachedEpicHash;
    private static volatile int cachedPunchyHash;

    private ItemOverrideMatcher() {}

    public static Snapshot current() {
        UnifiedConfigFile.Settings config = ClientConfig.current();
        List<? extends String> namespaceEntries = config.epicWeaponNamespaces();
        List<? extends String> epicEntries = config.forceEpicItems();
        List<? extends String> punchyEntries = config.forcePunchyItems();
        int namespaceHash = namespaceEntries.hashCode();
        int epicHash = epicEntries.hashCode();
        int punchyHash = punchyEntries.hashCode();

        Snapshot result = cached;
        if (namespaceHash != cachedNamespaceHash || epicHash != cachedEpicHash || punchyHash != cachedPunchyHash) {
            synchronized (ItemOverrideMatcher.class) {
                if (namespaceHash != cachedNamespaceHash || epicHash != cachedEpicHash || punchyHash != cachedPunchyHash) {
                    result = new Snapshot(
                        parseNamespaces(namespaceEntries),
                        parse(epicEntries, "forceEpicItems"),
                        parse(punchyEntries, "forcePunchyItems")
                    );
                    cached = result;
                    cachedNamespaceHash = namespaceHash;
                    cachedEpicHash = epicHash;
                    cachedPunchyHash = punchyHash;
                } else {
                    result = cached;
                }
            }
        }
        return result;
    }

    private static Set<String> parseNamespaces(List<? extends String> entries) {
        Set<String> namespaces = new HashSet<>();
        Set<String> warned = new HashSet<>();

        for (String raw : entries) {
            String namespace = raw == null ? "" : raw.trim();
            ResourceLocation probe = ResourceLocation.tryParse(namespace + ":compat_probe");
            if (namespace.isEmpty() || namespace.indexOf(':') >= 0 || probe == null || !probe.getNamespace().equals(namespace)) {
                warnInvalid("epicWeaponNamespaces", raw, warned);
            } else {
                namespaces.add(namespace);
            }
        }

        return Set.copyOf(namespaces);
    }

    private static MatchSet parse(List<? extends String> entries, String listName) {
        Set<ResourceLocation> itemIds = new HashSet<>();
        Set<TagKey<Item>> tags = new HashSet<>();
        Set<String> warned = new HashSet<>();

        for (String raw : entries) {
            String entry = raw == null ? "" : raw.trim();
            boolean tag = entry.startsWith("#");
            String idText = tag ? entry.substring(1) : entry;
            ResourceLocation id = ResourceLocation.tryParse(idText);

            if (entry.isEmpty() || id == null) {
                warnInvalid(listName, raw, warned);
                continue;
            }

            if (tag) {
                tags.add(TagKey.create(Registries.ITEM, id));
            } else if (ForgeRegistries.ITEMS.containsKey(id)) {
                itemIds.add(id);
            } else {
                warnInvalid(listName, raw, warned);
            }
        }

        return new MatchSet(Set.copyOf(itemIds), Set.copyOf(tags));
    }

    private static void warnInvalid(String listName, String entry, Set<String> warned) {
        String printable = String.valueOf(entry);
        if (warned.add(printable)) {
            PunchyEpicFightCompat.LOGGER.warn("Ignoring invalid {} entry: {}", listName, printable);
        }
    }

    public record Snapshot(Set<String> epicWeaponNamespaces, MatchSet forceEpic, MatchSet forcePunchy) {
        private static Snapshot empty() {
            return new Snapshot(Set.of(), MatchSet.empty(), MatchSet.empty());
        }

        public boolean namespaceAllows(ItemStack stack) {
            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            return itemId != null && epicWeaponNamespaces.contains(itemId.getNamespace());
        }
    }

    public record MatchSet(Set<ResourceLocation> itemIds, Set<TagKey<Item>> tags) {
        private static MatchSet empty() {
            return new MatchSet(Set.of(), Set.of());
        }

        public boolean matches(ItemStack stack) {
            if (stack.isEmpty()) {
                return false;
            }

            ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (itemId != null && itemIds.contains(itemId)) {
                return true;
            }

            for (TagKey<Item> tag : tags) {
                if (stack.is(tag)) {
                    return true;
                }
            }
            return false;
        }
    }
}
