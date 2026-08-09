package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import su.nightexpress.excellentenchants.api.EnchantDefinition;
import su.nightexpress.excellentenchants.api.enchantment.CustomEnchantment;
import su.nightexpress.excellentenchants.api.item.ItemSet;
import su.nightexpress.excellentenchants.enchantment.EnchantRegistry;

import java.io.File;
import java.util.*;

public class ExcellentEnchantsHook {
    private final SinceEnchantments plugin;
    private final Map<String, YamlConfiguration> configCache = new HashMap<>();
    private boolean hooked = false;
    private Plugin excellentEnchants;

    public ExcellentEnchantsHook(SinceEnchantments plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        Plugin detected = Bukkit.getPluginManager().getPlugin("ExcellentEnchants");
        if (detected == null || !detected.isEnabled()) {
            log("log-ee-hook-missing", "ExcellentEnchants not detected. Skipping EE hook.");
            return;
        }

        this.excellentEnchants = detected;
        log("log-ee-hook-detected", "ExcellentEnchants detected (version %version%). Preparing EE API hook.", "%version%", detected.getPluginMeta().getVersion());

        try {
            hooked = true;
            log("log-ee-hook-success", "Successfully hooked into ExcellentEnchants API! Found %count% enchantments.", "%count%", String.valueOf(EnchantRegistry.getRegistered().size()));
        } catch (LinkageError | RuntimeException e) {
            hooked = false;
            logWarning("log-ee-hook-fail", "ExcellentEnchants detected, but failed to hook into API: %error%", "%error%", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public void loadExcellentEnchantments() {
        configCache.clear();
        if (!hooked) {
            log("log-ee-autoload-skip", "Skipping ExcellentEnchants auto-load because EE hook is not active.");
            return;
        }

        Set<CustomEnchantment> customs = EnchantRegistry.getRegistered();
        log("log-ee-autoload-start", "Scanning %total% ExcellentEnchants enchantments for missing SinceEnchantments entries.", "%total%", String.valueOf(customs.size()));

        int registered = 0;
        int skipped = 0;
        int failed = 0;

        List<String> defaultDescription = plugin.getSettingsFile().getStringList("settings.ee-default-description");
        if (defaultDescription == null || defaultDescription.isEmpty()) {
            defaultDescription = new ArrayList<>();
        }

        for (CustomEnchantment custom : customs) {
            try {
                Enchantment bukkitEnchant = custom.getBukkitEnchantment();
                if (bukkitEnchant == null) {
                    failed++;
                    continue;
                }

                String id = bukkitEnchant.getKey().toString().toLowerCase(Locale.ROOT);
                if (plugin.getEnchantsFile().getConfig().contains("custom-enchants." + id)) {
                    skipped++;
                    continue;
                }

                EnchantDefinition definition = custom.getDefinition();
                YamlConfiguration config = getEnchantConfig(custom.getId());
                plugin.getEnchantManager().registerDynamicEnchant(
                        id,
                        getDisplayName(custom, definition),
                        getMaxLevel(bukkitEnchant, definition),
                        getRarity(config),
                        getTarget(custom, bukkitEnchant, definition, config),
                        getDescription(custom, definition, config, defaultDescription)
                );
                registered++;
            } catch (LinkageError | RuntimeException e) {
                failed++;
                plugin.getLogger().warning("Failed to auto-register EE enchantment '" + safeId(custom) + "': " + e.getMessage());
            }
        }

        log("log-ee-autoload", "ExcellentEnchants auto-load finished: registered %count%, skipped %skipped%, failed %failed%.",
                "%count%", String.valueOf(registered),
                "%skipped%", String.valueOf(skipped),
                "%failed%", String.valueOf(failed));
    }

    public Map<String, Integer> getEnchants(ItemStack item) {
        Map<String, Integer> enchants = new HashMap<>();
        if (!hooked || item == null || !item.hasItemMeta()) return enchants;

        for (CustomEnchantment custom : EnchantRegistry.getRegistered()) {
            Enchantment enchantment = custom.getBukkitEnchantment();
            if (enchantment == null) continue;
            int level = item.getItemMeta().getEnchantLevel(enchantment);
            if (level > 0) enchants.put(enchantment.getKey().toString().toLowerCase(Locale.ROOT), level);
        }

        return enchants;
    }

    private String getDisplayName(CustomEnchantment custom, EnchantDefinition definition) {
        String displayName = definition == null ? custom.getDisplayName() : definition.getDisplayName();
        if (displayName == null || displayName.isBlank()) displayName = custom.getDisplayName();
        return ColorUtils.toPlainText(ColorUtils.parse(displayName)).trim();
    }

    private int getMaxLevel(Enchantment enchantment, EnchantDefinition definition) {
        if (definition != null) {
            return Math.max(1, definition.getMaxLevel());
        }
        return Math.max(1, enchantment.getMaxLevel());
    }

    private String getRarity(YamlConfiguration config) {
        String rarity = firstString(config, "Rarity", "Tier", "Group");
        return rarity == null || rarity.isBlank() ? "COMMON" : rarity.toUpperCase(Locale.ROOT);
    }

    private String getTarget(CustomEnchantment custom, Enchantment enchantment, EnchantDefinition definition, YamlConfiguration config) {
        String configured = firstString(config, "Target", "Item_Target", "ItemTarget");
        if (configured != null && !configured.isBlank()) return configured.toUpperCase(Locale.ROOT);

        List<String> itemSetValues = new ArrayList<>();
        addItemSetValues(itemSetValues, custom.getPrimaryItems());
        addItemSetValues(itemSetValues, custom.getSupportedItems());
        if (definition != null) {
            addItemSetValues(itemSetValues, definition.getPrimaryItemSet());
            addItemSetValues(itemSetValues, definition.getSupportedItemSet());
        }
        if (!itemSetValues.isEmpty()) return inferTargetFromStrings(itemSetValues);

        return inferTarget(enchantment);
    }

    private List<String> getDescription(CustomEnchantment custom, EnchantDefinition definition, YamlConfiguration config, List<String> fallback) {
        List<String> configured = firstStringList(config, "Description", "Lore", "Info.Description");
        if (!configured.isEmpty()) return configured;
        if (definition != null && definition.getDescription() != null && !definition.getDescription().isEmpty()) {
            return definition.getDescription();
        }
        if (custom.getDescription() != null && !custom.getDescription().isEmpty()) {
            return custom.getDescription();
        }
        return fallback;
    }

    private void addItemSetValues(List<String> values, ItemSet itemSet) {
        if (itemSet == null) return;
        values.add(itemSet.getId());
        values.add(itemSet.getDisplayName());
        values.addAll(itemSet.getMaterials());
    }

    private YamlConfiguration getEnchantConfig(String id) {
        if (excellentEnchants == null || id == null || id.isBlank()) return null;
        String plainId = id.toLowerCase(Locale.ROOT);
        if (configCache.containsKey(plainId)) return configCache.get(plainId);

        File root = excellentEnchants.getDataFolder();
        File direct = new File(new File(root, "enchants"), plainId + ".yml");
        File found = direct.isFile() ? direct : findConfigFile(new File(root, "enchants"), plainId + ".yml");
        if (found == null) {
            configCache.put(plainId, null);
            return null;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(found);
        configCache.put(plainId, config);
        return config;
    }

    private File findConfigFile(File folder, String fileName) {
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (File file : files) {
            if (file.isDirectory()) {
                File nested = findConfigFile(file, fileName);
                if (nested != null) return nested;
            } else if (file.getName().equalsIgnoreCase(fileName)) {
                return file;
            }
        }
        return null;
    }

    private String firstString(YamlConfiguration config, String... paths) {
        if (config == null) return null;
        for (String path : paths) {
            if (config.contains(path)) return config.getString(path);
        }
        return null;
    }

    private List<String> firstStringList(YamlConfiguration config, String... paths) {
        if (config == null) return new ArrayList<>();
        for (String path : paths) {
            if (config.isList(path)) return config.getStringList(path);
        }
        return new ArrayList<>();
    }

    private String inferTargetFromStrings(List<String> values) {
        String joined = String.join(" ", values).toUpperCase(Locale.ROOT);
        if (joined.contains("BOW") || joined.contains("CROSSBOW")) return "BOW";
        if (joined.contains("SWORD")) return "SWORD";
        if (joined.contains("WEAPON") || joined.contains("AXE") || joined.contains("TRIDENT") || joined.contains("MACE"))
            return "WEAPON";
        if (joined.contains("PICKAXE") || joined.contains("SHOVEL") || joined.contains("HOE") || joined.contains("TOOL") || joined.contains("MINING"))
            return "TOOL";
        if (joined.contains("HELMET") || joined.contains("CHESTPLATE") || joined.contains("LEGGINGS") || joined.contains("BOOTS") || joined.contains("ARMOR"))
            return "ARMOR";
        return "ALL";
    }

    private String inferTarget(Enchantment enchantment) {
        if (canEnchantAny(enchantment, Material.DIAMOND_SWORD, Material.NETHERITE_AXE, Material.TRIDENT, Material.MACE))
            return "WEAPON";
        if (canEnchantAny(enchantment, Material.BOW, Material.CROSSBOW)) return "BOW";
        if (canEnchantAny(enchantment, Material.DIAMOND_PICKAXE, Material.DIAMOND_SHOVEL, Material.DIAMOND_HOE))
            return "TOOL";
        if (canEnchantAny(enchantment, Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS))
            return "ARMOR";
        return "ALL";
    }

    private boolean canEnchantAny(Enchantment enchantment, Material... materials) {
        for (Material material : materials) {
            if (enchantment.canEnchantItem(new ItemStack(material))) return true;
        }
        return false;
    }

    private String safeId(CustomEnchantment custom) {
        try {
            return custom.getId();
        } catch (LinkageError | RuntimeException ignored) {
            return "unknown";
        }
    }

    private void log(String path, String fallback, String... replacements) {
        plugin.getLogger().info(formatMessage(path, fallback, replacements));
    }

    private void logWarning(String path, String fallback, String... replacements) {
        plugin.getLogger().warning(formatMessage(path, fallback, replacements));
    }

    private String formatMessage(String path, String fallback, String... replacements) {
        String message = plugin.getMessagesFile().getString(path, fallback);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
}
