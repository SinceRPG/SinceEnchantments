package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.FoliaScheduler;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class AdvancedEnchantmentsHook {
    private static final int MAX_LOAD_ATTEMPTS = 5;
    private static final long RETRY_DELAY_TICKS = 40L;

    private final SinceEnchantments plugin;
    private boolean hooked = false;
    private Plugin advancedEnchantments;
    private Method getAllEnchantmentsMethod;
    private Method getHighestEnchantmentLevelMethod;
    private Method getEnchantmentsOnItemMethod;
    private Method applyEnchantMethod;
    private Method removeEnchantmentMethod;
    private File enchantmentsFile;
    private YamlConfiguration enchantmentsConfig;

    public AdvancedEnchantmentsHook(SinceEnchantments plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        Plugin detected = Bukkit.getPluginManager().getPlugin("AdvancedEnchantments");
        if (detected == null || !detected.isEnabled()) {
            log("log-ae-hook-missing", "AdvancedEnchantments not detected. Skipping AE hook.");
            return;
        }

        this.advancedEnchantments = detected;
        log("log-ae-hook-detected", "AdvancedEnchantments detected (version %version%). Preparing AE API hook.", "%version%", detected.getPluginMeta().getVersion());

        try {
            Class<?> aeApiClass = Class.forName("net.advancedplugins.ae.api.AEAPI");
            getAllEnchantmentsMethod = aeApiClass.getMethod("getAllEnchantments");
            getHighestEnchantmentLevelMethod = aeApiClass.getMethod("getHighestEnchantmentLevel", String.class);
            getEnchantmentsOnItemMethod = aeApiClass.getMethod("getEnchantmentsOnItem", ItemStack.class);
            applyEnchantMethod = aeApiClass.getMethod("applyEnchant", String.class, int.class, ItemStack.class);
            removeEnchantmentMethod = aeApiClass.getMethod("removeEnchantment", ItemStack.class, String.class);

            hooked = true;
            int count = getAllEnchantmentsFromApi().size();
            if (count > 0) {
                log("log-ae-hook-success", "Successfully hooked into AdvancedEnchantments API! Found %count% enchantments.", "%count%", String.valueOf(count));
            } else {
                log("log-ae-hook-empty", "AdvancedEnchantments API hook is ready, but the enchantment list is currently empty. Auto-load will retry after AE finishes loading.");
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            logWarning("log-ae-hook-fail", "AdvancedEnchantments detected, but failed to hook into API: %error%", "%error%", formatError(e));
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public void loadAEEnchantments() {
        enchantmentsConfig = null;
        loadAEEnchantments(1);
    }

    private void loadAEEnchantments(int attempt) {
        if (!hooked) {
            log("log-ae-autoload-skip", "Skipping AdvancedEnchantments auto-load because AE hook is not active.");
            return;
        }

        List<String> aeEnchants = getAllEnchantmentsFromApi();
        if (aeEnchants.isEmpty() && attempt < MAX_LOAD_ATTEMPTS) {
            log("log-ae-autoload-retry", "AdvancedEnchantments API returned 0 enchantments on attempt %attempt%/%max%. Retrying after %delay% ticks.",
                    "%attempt%", String.valueOf(attempt),
                    "%max%", String.valueOf(MAX_LOAD_ATTEMPTS),
                    "%delay%", String.valueOf(RETRY_DELAY_TICKS));
            FoliaScheduler.runLater(plugin, () -> loadAEEnchantments(attempt + 1), RETRY_DELAY_TICKS);
            return;
        }

        String source = "api";
        if (aeEnchants.isEmpty()) {
            aeEnchants = getAllEnchantmentsFromConfig();
            source = "config";
        }

        log("log-ae-autoload-start", "Scanning %total% AdvancedEnchantments enchantments from %source% for missing SinceEnchantments entries.",
                "%total%", String.valueOf(aeEnchants.size()),
                "%source%", source);

        int registered = 0;
        int skipped = 0;
        int failed = 0;

        List<String> defaultDescription = plugin.getSettingsFile().getStringList("settings.ae-default-description");
        if (defaultDescription == null || defaultDescription.isEmpty()) {
            defaultDescription = new ArrayList<>();
            defaultDescription.add("&7(AdvancedEnchantments effect)");
        }

        for (String aeName : aeEnchants) {
            String id = toEnchantId(aeName);

            if (plugin.getEnchantsFile().getConfig().contains("custom-enchants." + id)) {
                skipped++;
                continue;
            }

            try {
                plugin.getEnchantManager().registerDynamicEnchant(
                        id,
                        getDisplayName(aeName),
                        getMaxLevel(aeName),
                        "EPIC",
                        "ALL",
                        defaultDescription
                );
                registered++;
            } catch (LinkageError | RuntimeException e) {
                failed++;
                plugin.getLogger().warning("Failed to auto-register AE enchantment '" + aeName + "': " + e.getMessage());
            }
        }

        log("log-ae-autoload", "AdvancedEnchantments auto-load finished from %source%: registered %count%, skipped %skipped%, failed %failed%.",
                "%source%", source,
                "%count%", String.valueOf(registered),
                "%skipped%", String.valueOf(skipped),
                "%failed%", String.valueOf(failed));
    }

    public Map<String, Integer> getEnchants(ItemStack item) {
        Map<String, Integer> enchants = new HashMap<>();
        if (!hooked || item == null) return enchants;

        try {
            Object raw = getEnchantmentsOnItemMethod.invoke(null, item);
            if (raw instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> entry : map.entrySet()) {
                    if (entry.getKey() instanceof String name && entry.getValue() instanceof Number level && level.intValue() > 0) {
                        enchants.put(toEnchantId(name), level.intValue());
                    }
                }
            }
        } catch (IllegalAccessException | InvocationTargetException | LinkageError | RuntimeException ignored) {
            for (String aeName : getAllEnchantmentsFromApi()) {
                int level = getAppliedLevel(item, aeName);
                if (level > 0) {
                    enchants.put(toEnchantId(aeName), level);
                }
            }
        }

        return enchants;
    }

    public boolean applyEnchant(ItemStack item, String enchantId, int level) {
        if (!hooked || item == null || !enchantId.startsWith("ae:")) return false;

        String aeName = enchantId.substring(3);
        try {
            Object raw = applyEnchantMethod.invoke(null, aeName, level, item);
            if (!(raw instanceof ItemStack updated) || updated == item) return true;
            item.setItemMeta(updated.getItemMeta());
            return true;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public boolean removeEnchant(ItemStack item, String enchantId) {
        if (!hooked || item == null || !enchantId.startsWith("ae:")) return false;

        String aeName = enchantId.substring(3);
        try {
            Object raw = removeEnchantmentMethod.invoke(null, item, aeName);
            if (!(raw instanceof ItemStack updated) || updated == item) return true;
            item.setItemMeta(updated.getItemMeta());
            return true;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private List<String> getAllEnchantmentsFromApi() {
        if (getAllEnchantmentsMethod == null) return new ArrayList<>();
        try {
            Object raw = getAllEnchantmentsMethod.invoke(null);
            if (!(raw instanceof Iterable<?> iterable)) return new ArrayList<>();

            List<String> names = new ArrayList<>();
            for (Object value : iterable) {
                if (value instanceof String name && !name.isBlank()) {
                    names.add(name.toLowerCase(Locale.ROOT));
                }
            }
            return names;
        } catch (IllegalAccessException | InvocationTargetException | LinkageError | RuntimeException e) {
            plugin.getLogger().warning("Failed to read AE enchantments from API: " + formatError(e));
            return new ArrayList<>();
        }
    }

    private List<String> getAllEnchantmentsFromConfig() {
        if (advancedEnchantments == null) return new ArrayList<>();

        YamlConfiguration config = getEnchantmentsConfig();
        if (config == null) {
            String filePath = enchantmentsFile == null ? "plugins/AdvancedEnchantments/enchantments.yml" : enchantmentsFile.getPath();
            logWarning("log-ae-config-missing", "AdvancedEnchantments fallback config not found at %file%.", "%file%", filePath);
            return new ArrayList<>();
        }

        Set<String> names = new HashSet<>();
        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section != null && section.isConfigurationSection("levels")) {
                names.add(key.toLowerCase(Locale.ROOT));
            }
        }

        log("log-ae-config-fallback", "AdvancedEnchantments API returned 0 enchantments. Fallback config scan found %count% entries in %file%.",
                "%count%", String.valueOf(names.size()),
                "%file%", enchantmentsFile.getPath());
        return new ArrayList<>(names);
    }

    private int getAppliedLevel(ItemStack item, String aeName) {
        String pdcName = aeName.toLowerCase(Locale.ROOT);
        if (item.hasItemMeta()) {
            var pdc = item.getItemMeta().getPersistentDataContainer();
            var key = new org.bukkit.NamespacedKey(PersistentKeyNames.ADVANCED_ENCHANTMENTS_NAMESPACE, PersistentKeyNames.AE_ENCHANTMENT_PREFIX + pdcName);
            Integer level = pdc.get(key, org.bukkit.persistence.PersistentDataType.INTEGER);
            if (level != null) return level;
        }
        return 0;
    }

    private int getMaxLevel(String aeName) {
        if (getHighestEnchantmentLevelMethod != null) {
            try {
                Object raw = getHighestEnchantmentLevelMethod.invoke(null, aeName);
                if (raw instanceof Number number) {
                    return Math.max(1, number.intValue());
                }
            } catch (IllegalAccessException | InvocationTargetException | LinkageError | RuntimeException ignored) {
            }
        }

        ConfigurationSection section = getConfigEnchantSection(aeName);
        if (section != null) {
            ConfigurationSection levels = section.getConfigurationSection("levels");
            if (levels != null) {
                int max = 1;
                for (String level : levels.getKeys(false)) {
                    try {
                        max = Math.max(max, Integer.parseInt(level));
                    } catch (NumberFormatException ignored) {
                    }
                }
                return max;
            }
        }
        return 3;
    }

    private String getDisplayName(String aeName) {
        ConfigurationSection section = getConfigEnchantSection(aeName);
        if (section != null) {
            String display = section.getString("display");
            if (display != null && !display.isBlank()) {
                return ColorUtils.toPlainText(ColorUtils.parse(display.replace("%group-color%", ""))).trim();
            }
        }
        return formatDisplayName(aeName);
    }

    private ConfigurationSection getConfigEnchantSection(String aeName) {
        YamlConfiguration config = getEnchantmentsConfig();
        if (config == null) return null;
        return config.getConfigurationSection(aeName.toLowerCase(Locale.ROOT));
    }

    private YamlConfiguration getEnchantmentsConfig() {
        if (advancedEnchantments == null) return null;
        if (enchantmentsConfig != null) return enchantmentsConfig;

        enchantmentsFile = new File(advancedEnchantments.getDataFolder(), "enchantments.yml");
        if (!enchantmentsFile.isFile()) return null;

        enchantmentsConfig = YamlConfiguration.loadConfiguration(enchantmentsFile);
        return enchantmentsConfig;
    }

    private String toEnchantId(String aeName) {
        return "ae:" + aeName.toLowerCase(Locale.ROOT);
    }

    private String formatDisplayName(String rawName) {
        if (rawName == null || rawName.isBlank()) return "Advanced Enchantment";
        String spaced = rawName.replace('_', ' ').replace('-', ' ');
        String[] words = spaced.split("\\s+");
        StringBuilder formatted = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            if (!formatted.isEmpty()) formatted.append(' ');
            formatted.append(Character.toUpperCase(word.charAt(0)));
            if (word.length() > 1) formatted.append(word.substring(1).toLowerCase(Locale.ROOT));
        }
        return formatted.toString();
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

    private String formatError(Throwable throwable) {
        Throwable cause = throwable instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : throwable;
        return cause.getClass().getSimpleName() + ": " + cause.getMessage();
    }
}
