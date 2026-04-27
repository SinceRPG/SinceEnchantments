package net.danh.sinceenchantments.api;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.ConfigUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

@SuppressWarnings({"BooleanMethodIsAlwaysInverted", "unused", "UnusedReturnValue"})
public class EnchantManager {

    public final NamespacedKey ENCHANT_KEY;
    public final NamespacedKey BOOK_ID_KEY;
    public final NamespacedKey BOOK_LEVEL_KEY;
    public final NamespacedKey BOOK_SUCCESS_KEY;
    public final NamespacedKey BOOK_DESTROY_KEY;
    public final NamespacedKey EXTRACTOR_TYPE_KEY;
    public final NamespacedKey CHARM_BONUS_KEY;
    public final NamespacedKey GUI_ACTION_KEY;
    public final NamespacedKey SLOT_GEM_KEY;
    public final NamespacedKey SLOT_MODIFIER_KEY;
    public final NamespacedKey LOCKED_KEY;
    public final NamespacedKey LOCK_SCROLL_KEY;
    public final NamespacedKey PURGE_SCROLL_KEY;
    public final NamespacedKey PURGE_RETURN_KEY;
    public final NamespacedKey RANDOMIZER_KEY;
    public final NamespacedKey PROTECTOR_KEY;
    public final NamespacedKey PROTECTED_ITEM_KEY;
    public final NamespacedKey TRACKER_ITEM_KEY;
    public final NamespacedKey TRACKER_KEY;
    public final NamespacedKey STAT_BLOCKS_KEY;
    public final NamespacedKey STAT_MOBS_KEY;
    public final NamespacedKey STAT_PLAYERS_KEY;
    public final NamespacedKey STAT_FISH_KEY;

    private final SinceEnchantments plugin;
    private final Map<String, Integer> maxLevels = new HashMap<>();
    private final Map<String, String> rarities = new HashMap<>();
    private final Map<String, String> enchantNames = new HashMap<>();
    private final Map<String, String> targets = new HashMap<>();
    private final Map<String, List<String>> conflicts = new HashMap<>();
    private final Map<String, List<String>> requires = new HashMap<>();
    private final Map<String, List<String>> descriptions = new HashMap<>();
    private final Map<String, List<String>> itemWhitelist = new HashMap<>();
    private final Map<String, List<String>> customItemsWhitelist = new HashMap<>();
    private final Map<String, Integer> itemMaxSlots = new HashMap<>();
    private final Map<String, Integer> customItemsMaxSlots = new HashMap<>();
    private final Map<String, Integer> itemMaxSlotModifiers = new HashMap<>();
    private final Map<String, Integer> customItemsMaxSlotModifiers = new HashMap<>();

    private final List<String> aePlainNames = new ArrayList<>();

    public EnchantManager(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.ENCHANT_KEY = new NamespacedKey(plugin, "custom_enchants");
        this.BOOK_ID_KEY = new NamespacedKey(plugin, "book_enchant_id");
        this.BOOK_LEVEL_KEY = new NamespacedKey(plugin, "book_enchant_level");
        this.BOOK_SUCCESS_KEY = new NamespacedKey(plugin, "book_success_rate");
        this.BOOK_DESTROY_KEY = new NamespacedKey(plugin, "book_destroy_rate");
        this.EXTRACTOR_TYPE_KEY = new NamespacedKey(plugin, "extractor_type");
        this.CHARM_BONUS_KEY = new NamespacedKey(plugin, "charm_bonus");
        this.GUI_ACTION_KEY = new NamespacedKey(plugin, "gui_action");
        this.SLOT_GEM_KEY = new NamespacedKey(plugin, "slot_gem_item");
        this.SLOT_MODIFIER_KEY = new NamespacedKey(plugin, "slot_modifier");
        this.LOCKED_KEY = new NamespacedKey(plugin, "item_locked");
        this.LOCK_SCROLL_KEY = new NamespacedKey(plugin, "lock_scroll");
        this.PURGE_SCROLL_KEY = new NamespacedKey(plugin, "purge_scroll");
        this.PURGE_RETURN_KEY = new NamespacedKey(plugin, "purge_return_books");
        this.RANDOMIZER_KEY = new NamespacedKey(plugin, "randomizer_stone");
        this.PROTECTOR_KEY = new NamespacedKey(plugin, "protection_gem");
        this.PROTECTED_ITEM_KEY = new NamespacedKey(plugin, "item_is_protected");
        this.TRACKER_ITEM_KEY = new NamespacedKey(plugin, "stat_tracker_item");
        this.TRACKER_KEY = new NamespacedKey(plugin, "stat_tracker_applied");
        this.STAT_BLOCKS_KEY = new NamespacedKey(plugin, "stat_blocks_mined");
        this.STAT_MOBS_KEY = new NamespacedKey(plugin, "stat_mobs_killed");
        this.STAT_PLAYERS_KEY = new NamespacedKey(plugin, "stat_players_killed");
        this.STAT_FISH_KEY = new NamespacedKey(plugin, "stat_fish_caught");

        loadEnchantsFromConfig();
    }

    public void loadEnchantsFromConfig() {
        maxLevels.clear();
        rarities.clear();
        enchantNames.clear();
        targets.clear();
        conflicts.clear();
        requires.clear();
        descriptions.clear();
        itemWhitelist.clear();
        customItemsWhitelist.clear();
        itemMaxSlots.clear();
        customItemsMaxSlots.clear();
        itemMaxSlotModifiers.clear();
        customItemsMaxSlotModifiers.clear();
        aePlainNames.clear();

        ConfigurationSection customEnchSec = plugin.getEnchantsFile().getConfig().getConfigurationSection("custom-enchants");
        if (customEnchSec != null) {
            for (String key : customEnchSec.getKeys(false)) {
                String path = "custom-enchants." + key;
                maxLevels.put(key, plugin.getEnchantsFile().getInt(path + ".max-level", 1));
                rarities.put(key, plugin.getEnchantsFile().getString(path + ".rarity", "COMMON"));
                enchantNames.put(key, plugin.getEnchantsFile().getString(path + ".name", key));
                targets.put(key, plugin.getEnchantsFile().getString(path + ".target", "ALL").toUpperCase());
                conflicts.put(key, plugin.getEnchantsFile().getStringList(path + ".conflicts"));
                requires.put(key, plugin.getEnchantsFile().getStringList(path + ".requires"));
                descriptions.put(key, plugin.getEnchantsFile().getStringList(path + ".description"));

                if (key.startsWith("ae:")) {
                    String plainName = ColorUtils.toPlainText(ColorUtils.parse(enchantNames.get(key))).trim();
                    if (!plainName.isEmpty() && !aePlainNames.contains(plainName)) aePlainNames.add(plainName);
                }
            }
        }

        ConfigurationSection vanillaEnchSec = plugin.getEnchantsFile().getConfig().getConfigurationSection("vanilla-enchants");
        if (vanillaEnchSec != null) {
            for (String key : vanillaEnchSec.getKeys(false)) {
                String path = "vanilla-enchants." + key;
                descriptions.put(key, plugin.getEnchantsFile().getStringList(path + ".description"));
                enchantNames.put(key, plugin.getEnchantsFile().getString(path + ".name", key));

                if (plugin.getEnchantsFile().getConfig().contains(path + ".max-level")) {
                    maxLevels.put(key, plugin.getEnchantsFile().getInt(path + ".max-level"));
                }
                if (plugin.getEnchantsFile().getConfig().contains(path + ".rarity")) {
                    rarities.put(key, plugin.getEnchantsFile().getString(path + ".rarity", "COMMON"));
                }
                if (plugin.getEnchantsFile().getConfig().contains(path + ".target")) {
                    targets.put(key, plugin.getEnchantsFile().getString(path + ".target").toUpperCase());
                }
            }
        }

        ConfigurationSection itemWlSec = plugin.getLimitsFile().getConfig().getConfigurationSection("item-whitelist");
        if (itemWlSec != null) {
            for (String key : itemWlSec.getKeys(false)) {
                itemWhitelist.put(key.toUpperCase(), plugin.getLimitsFile().getStringList("item-whitelist." + key));
            }
        }

        ConfigurationSection customWlSec = plugin.getLimitsFile().getConfig().getConfigurationSection("custom-item-whitelist");
        if (customWlSec != null) {
            for (String key : customWlSec.getKeys(false)) {
                customItemsWhitelist.put(key.toUpperCase(), plugin.getLimitsFile().getStringList("custom-item-whitelist." + key));
            }
        }
        ConfigurationSection mmoWlSec = plugin.getLimitsFile().getConfig().getConfigurationSection("mmoitems-whitelist");
        if (mmoWlSec != null) {
            for (String key : mmoWlSec.getKeys(false)) {
                customItemsWhitelist.put(key.toUpperCase(), plugin.getLimitsFile().getStringList("mmoitems-whitelist." + key));
            }
        }

        ConfigurationSection itemSlotSec = plugin.getLimitsFile().getConfig().getConfigurationSection("item-max-slots");
        if (itemSlotSec != null) {
            for (String key : itemSlotSec.getKeys(false)) {
                itemMaxSlots.put(key.toUpperCase(), plugin.getLimitsFile().getInt("item-max-slots." + key));
            }
        }

        ConfigurationSection customSlotSec = plugin.getLimitsFile().getConfig().getConfigurationSection("custom-item-max-slots");
        if (customSlotSec != null) {
            for (String key : customSlotSec.getKeys(false)) {
                customItemsMaxSlots.put(key.toUpperCase(), plugin.getLimitsFile().getInt("custom-item-max-slots." + key));
            }
        }
        ConfigurationSection mmoSlotSec = plugin.getLimitsFile().getConfig().getConfigurationSection("mmoitems-max-slots");
        if (mmoSlotSec != null) {
            for (String key : mmoSlotSec.getKeys(false)) {
                customItemsMaxSlots.put(key.toUpperCase(), plugin.getLimitsFile().getInt("mmoitems-max-slots." + key));
            }
        }

        ConfigurationSection itemModSec = plugin.getLimitsFile().getConfig().getConfigurationSection("item-max-slot-modifiers");
        if (itemModSec != null) {
            for (String key : itemModSec.getKeys(false)) {
                itemMaxSlotModifiers.put(key.toUpperCase(), plugin.getLimitsFile().getInt("item-max-slot-modifiers." + key));
            }
        }

        ConfigurationSection customModSec = plugin.getLimitsFile().getConfig().getConfigurationSection("custom-item-max-slot-modifiers");
        if (customModSec != null) {
            for (String key : customModSec.getKeys(false)) {
                customItemsMaxSlotModifiers.put(key.toUpperCase(), plugin.getLimitsFile().getInt("custom-item-max-slot-modifiers." + key));
            }
        }
        ConfigurationSection mmoModSec = plugin.getLimitsFile().getConfig().getConfigurationSection("mmoitems-max-slot-modifiers");
        if (mmoModSec != null) {
            for (String key : mmoModSec.getKeys(false)) {
                customItemsMaxSlotModifiers.put(key.toUpperCase(), plugin.getLimitsFile().getInt("mmoitems-max-slot-modifiers." + key));
            }
        }
    }

    public void registerDynamicEnchant(String id, String name, int maxLevel, String rarity, String target, List<String> description) {
        enchantNames.put(id, name);
        maxLevels.put(id, maxLevel);
        rarities.put(id, rarity);
        targets.put(id, target.toUpperCase());
        descriptions.put(id, description);
        conflicts.putIfAbsent(id, new ArrayList<>());
        requires.putIfAbsent(id, new ArrayList<>());

        if (id.startsWith("ae:")) {
            String plainName = ColorUtils.toPlainText(ColorUtils.parse(name)).trim();
            if (!plainName.isEmpty() && !aePlainNames.contains(plainName)) {
                aePlainNames.add(plainName);
            }
        }
    }

    public boolean isMatch(String text, String pattern) {
        if (pattern.equals("*")) return true;
        if (pattern.startsWith("*") && pattern.endsWith("*") && pattern.length() >= 2) {
            return text.contains(pattern.substring(1, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) return text.endsWith(pattern.substring(1));
        if (pattern.endsWith("*")) return text.startsWith(pattern.substring(0, pattern.length() - 1));
        return text.equals(pattern);
    }

    public boolean isItemInCategory(ItemStack item, String category) {
        if (item == null || item.getType() == Material.AIR) return false;
        String configPath = "settings.stat-tracker-categories." + category;
        ConfigUtils settings = plugin.getSettingsFile();
        String customKey = getCustomItemKey(item);
        if (customKey != null) {
            List<String> mmoPatterns = settings.getStringList(configPath + ".mmoitems");
            for (String pattern : mmoPatterns) {
                if (isMatch(customKey, pattern.toUpperCase())) return true;
            }
        }
        String matName = item.getType().name().toUpperCase();
        List<String> vanillaPatterns = settings.getStringList(configPath + ".vanilla");
        for (String pattern : vanillaPatterns) {
            if (isMatch(matName, pattern.toUpperCase())) return true;
        }
        return false;
    }

    public String getCustomItemKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        ConfigurationSection hooksSec = plugin.getCustomItemsFile().getConfig().getConfigurationSection("hooks");
        if (hooksSec != null) {
            for (String hookKey : hooksSec.getKeys(false)) {
                List<String> pdcKeys = plugin.getCustomItemsFile().getStringList("hooks." + hookKey + ".pdc-keys");
                String format = plugin.getCustomItemsFile().getString("hooks." + hookKey + ".format");
                if (pdcKeys.isEmpty() || format == null) continue;

                for (String keyStr : pdcKeys) {
                    String[] parts = keyStr.split(":", 2);
                    if (parts.length == 2) {
                        NamespacedKey key = new NamespacedKey(parts[0], parts[1]);
                        if (pdc.has(key, PersistentDataType.STRING)) {
                            String value = pdc.get(key, PersistentDataType.STRING);
                            if (value != null) {
                                return format.replace("{id}", value).toUpperCase();
                            }
                        }
                    }
                }
            }
        }

        NamespacedKey typeKey = new NamespacedKey("mmoitems", "type");
        NamespacedKey idKey = new NamespacedKey("mmoitems", "id");
        if (pdc.has(typeKey, PersistentDataType.STRING) && pdc.has(idKey, PersistentDataType.STRING)) {
            String type = pdc.get(typeKey, PersistentDataType.STRING);
            String id = pdc.get(idKey, PersistentDataType.STRING);
            if (type != null && id != null) return (type + ":" + id).toUpperCase();
        }
        return plugin.getMythicLibHook().getMMOItemKey(item);
    }

    public void cleanItemLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean changed = false;

        NamespacedKey startKey = new NamespacedKey(plugin, "lore_start");
        NamespacedKey countKey = new NamespacedKey(plugin, "lore_count");
        NamespacedKey placeholderKey = new NamespacedKey(plugin, "lore_placeholder");

        if (pdc.has(countKey, PersistentDataType.INTEGER)) {
            int start = pdc.getOrDefault(startKey, PersistentDataType.INTEGER, -1);
            int count = pdc.getOrDefault(countKey, PersistentDataType.INTEGER, 0);
            boolean hadPlaceholder = pdc.getOrDefault(placeholderKey, PersistentDataType.BYTE, (byte) 0) == 1;

            if (start != -1 && count > 0) {
                List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
                for (int i = 0; i < count; i++) {
                    if (start < lore.size()) lore.remove(start);
                }
                if (hadPlaceholder) {
                    String placeholderStr = plugin.getSettingsFile().getString("settings.placeholder", "#enchants#");
                    if (start <= lore.size()) {
                        lore.add(start, ColorUtils.parse(placeholderStr).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                    } else {
                        lore.add(ColorUtils.parse(placeholderStr).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                    }
                }
                meta.lore(lore);
            }
            pdc.remove(startKey);
            pdc.remove(countKey);
            pdc.remove(placeholderKey);
            changed = true;
        }

        if (meta.hasLore() && !aePlainNames.isEmpty()) {
            List<Component> lore = new ArrayList<>(meta.lore());
            boolean strippedAe = false;

            for (int i = lore.size() - 1; i >= 0; i--) {
                String plainLine = ColorUtils.toPlainText(lore.get(i)).trim();
                for (String aeName : aePlainNames) {
                    if (plainLine.startsWith(aeName)) {
                        lore.remove(i);
                        strippedAe = true;
                        break;
                    }
                }
            }

            if (strippedAe) {
                meta.lore(lore);
                changed = true;
            }
        }

        if (changed) {
            item.setItemMeta(meta);
        }
    }

    private Registry<Enchantment> getBukkitRegistry() {
        return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT);
    }

    public boolean isBukkitEnchant(String id) {
        NamespacedKey key = NamespacedKey.fromString(id.toLowerCase());
        return key != null && getBukkitRegistry().get(key) != null;
    }

    public boolean enchantExists(String id) {
        return plugin.getEnchantRegistry().getEnchant(id) != null || isBukkitEnchant(id);
    }

    public int getMaxLevel(String enchantId) {
        if (maxLevels.containsKey(enchantId)) {
            return maxLevels.get(enchantId);
        }

        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment bukkitEnc = getBukkitRegistry().get(key);
            if (bukkitEnc != null) return bukkitEnc.getMaxLevel();
        }

        return 1;
    }

    public String getRarity(String enchantId) {
        if (rarities.containsKey(enchantId)) {
            return rarities.get(enchantId);
        }

        if (isBukkitEnchant(enchantId)) return "COMMON";
        return "COMMON";
    }

    public String getEnchantName(String enchantId) {
        return enchantNames.getOrDefault(enchantId, enchantId);
    }

    public List<String> getDescription(String enchantId) {
        return descriptions.getOrDefault(enchantId, new ArrayList<>());
    }

    public int getRawMaxSlots(ItemStack item) {
        int defaultSlots = plugin.getSettingsFile().getInt("settings.default-max-custom-enchants-per-item", 5);
        return getMaxSlot(item, defaultSlots, customItemsMaxSlots, itemMaxSlots);
    }

    public int getMaxSlotModifiersAllowed(ItemStack item) {
        int defaultAllowed = plugin.getSettingsFile().getInt("settings.max-slot-modifiers-allowed", 5);
        return getMaxSlot(item, defaultAllowed, customItemsMaxSlotModifiers, itemMaxSlotModifiers);
    }

    private int getMaxSlot(ItemStack item, int defaultAllowed, Map<String, Integer> customMap, Map<String, Integer> itemMap) {
        if (item == null || item.getType() == Material.AIR) return defaultAllowed;
        int maxAllowed = 0;
        boolean matched = false;
        String customKey = getCustomItemKey(item);
        if (customKey != null) {
            for (Map.Entry<String, Integer> entry : customMap.entrySet()) {
                if (isMatch(customKey, entry.getKey())) {
                    maxAllowed += entry.getValue();
                    matched = true;
                }
            }
        }
        if (matched) return maxAllowed;
        String matName = item.getType().name().toUpperCase();
        for (Map.Entry<String, Integer> entry : itemMap.entrySet()) {
            if (isMatch(matName, entry.getKey())) {
                maxAllowed += entry.getValue();
                matched = true;
            }
        }
        return matched ? maxAllowed : defaultAllowed;
    }

    public int getMaxSlots(ItemStack item) {
        if (item == null || item.getType() == Material.AIR)
            return plugin.getSettingsFile().getInt("settings.default-max-custom-enchants-per-item", 5);

        int raw = getRawMaxSlots(item);
        int modifier = 0;

        if (item.hasItemMeta()) {
            PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
            modifier = pdc.getOrDefault(SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, 0);
        }

        return Math.max(0, raw + modifier);
    }

    public int getAppliedEnchantsCount(ItemStack item) {
        int count = getCustomEnchants(item).size();
        if (plugin.getSettingsFile().getBoolean("settings.override-vanilla-enchants", true)) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                count += item.getItemMeta().getEnchants().size();
            }
        }
        return count;
    }

    public boolean isLocked(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().getOrDefault(LOCKED_KEY, PersistentDataType.BYTE, (byte) 0) == 1;
    }

    public void toggleLock(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        boolean locked = isLocked(item);
        meta.getPersistentDataContainer().set(LOCKED_KEY, PersistentDataType.BYTE, (byte) (locked ? 0 : 1));
        item.setItemMeta(meta);
    }

    public List<String> getWhitelistedEnchants(ItemStack item) {
        List<String> allowed = new ArrayList<>();
        if (item == null || item.getType() == Material.AIR) return allowed;
        Set<String> mergedWhitelist = new HashSet<>();
        boolean matched = false;
        String customKey = getCustomItemKey(item);
        if (customKey != null) {
            for (Map.Entry<String, List<String>> entry : customItemsWhitelist.entrySet()) {
                if (isMatch(customKey, entry.getKey())) {
                    mergedWhitelist.addAll(entry.getValue());
                    matched = true;
                }
            }
        }
        if (matched) {
            allowed.addAll(mergedWhitelist);
            return allowed;
        }
        String matName = item.getType().name().toUpperCase();
        for (Map.Entry<String, List<String>> entry : itemWhitelist.entrySet()) {
            if (isMatch(matName, entry.getKey())) {
                mergedWhitelist.addAll(entry.getValue());
                matched = true;
            }
        }
        if (matched) {
            allowed.addAll(mergedWhitelist);
        }
        return allowed;
    }

    public boolean isWhitelisted(ItemStack item, String enchantId) {
        List<String> allowed = getWhitelistedEnchants(item);
        if (allowed.isEmpty()) return true;
        return allowed.contains(enchantId);
    }

    public List<String> getMissingRequirements(String enchantId, ItemStack item) {
        List<String> missing = new ArrayList<>();
        List<String> reqs = requires.getOrDefault(enchantId, new ArrayList<>());
        if (reqs.isEmpty()) return missing;
        Map<String, Integer> currentCustoms = getCustomEnchants(item);
        for (String req : reqs) {
            NamespacedKey key = NamespacedKey.fromString(req.toLowerCase());
            if (key != null && getBukkitRegistry().get(key) != null) {
                Enchantment bukkitEnc = getBukkitRegistry().get(key);
                if (bukkitEnc == null || !item.hasItemMeta() || item.getItemMeta().getEnchantLevel(bukkitEnc) == 0) {
                    missing.add(getEnchantName(req));
                }
            } else {
                if (!currentCustoms.containsKey(req)) missing.add(getEnchantName(req));
            }
        }
        return missing;
    }

    public boolean isApplicable(String enchantId, Material mat) {
        if (targets.containsKey(enchantId)) {
            String target = targets.get(enchantId);
            if (target.equals("ALL")) return true;

            String name = mat.name().toUpperCase();
            List<String> patterns = plugin.getSettingsFile().getStringList("settings.enchant-targets." + target);

            if (patterns != null && !patterns.isEmpty()) {
                for (String pattern : patterns) {
                    if (isMatch(name, pattern.toUpperCase())) {
                        return true;
                    }
                }
                return false;
            }
            return true;
        }

        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment bukkitEnc = getBukkitRegistry().get(key);
            if (bukkitEnc != null) return bukkitEnc.canEnchantItem(new ItemStack(mat));
        }

        return true;
    }

    public boolean hasConflict(String enchantId, ItemStack item) {
        List<String> conflictList = conflicts.getOrDefault(enchantId, new ArrayList<>());
        Map<String, Integer> currentApplied = getAllEnchantsOnItem(item);
        for (String conf : conflictList) {
            if (currentApplied.containsKey(conf)) return true;
        }
        ConfigurationSection vanillaOverride = plugin.getEnchantsFile().getConfig().getConfigurationSection("vanilla-enchants." + enchantId);

        if (vanillaOverride == null) {
            NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
            if (key != null) {
                Enchantment currentBukkit = getBukkitRegistry().get(key);
                if (currentBukkit != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                    for (Enchantment applied : item.getItemMeta().getEnchants().keySet()) {
                        if (currentBukkit.conflictsWith(applied)) return true;
                    }
                }
            }
        }

        return false;
    }

    public Map<String, Integer> getCustomEnchants(ItemStack item) {
        Map<String, Integer> enchants = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return enchants;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        String rawData = pdc.get(ENCHANT_KEY, PersistentDataType.STRING);
        if (rawData != null && !rawData.isEmpty()) {
            for (String pair : rawData.split(";")) {
                String[] split = pair.split(",");
                if (split.length == 2) {
                    try {
                        enchants.put(split[0], Integer.parseInt(split[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }

        for (NamespacedKey key : pdc.getKeys()) {
            if (key.getNamespace().equals("advancedenchantments") && key.getKey().startsWith("ae_enchantment-")) {
                String aeId = "ae:" + key.getKey().substring(15);
                Integer level = pdc.get(key, PersistentDataType.INTEGER);
                if (level != null) {
                    enchants.put(aeId, level);
                }
            }
        }

        return enchants;
    }

    public void setCustomEnchants(ItemStack item, Map<String, Integer> enchants) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        Map<String, Integer> sinceEnchants = new HashMap<>();
        Map<String, Integer> aeEnchants = new HashMap<>();

        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            if (entry.getKey().startsWith("ae:")) {
                aeEnchants.put(entry.getKey(), entry.getValue());
            } else {
                sinceEnchants.put(entry.getKey(), entry.getValue());
            }
        }

        if (sinceEnchants.isEmpty()) {
            pdc.remove(ENCHANT_KEY);
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : sinceEnchants.entrySet()) {
                sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
            }
            pdc.set(ENCHANT_KEY, PersistentDataType.STRING, sb.toString());
        }

        for (NamespacedKey key : pdc.getKeys()) {
            if (key.getNamespace().equals("advancedenchantments") && key.getKey().startsWith("ae_enchantment-")) {
                pdc.remove(key);
            }
        }

        for (Map.Entry<String, Integer> entry : aeEnchants.entrySet()) {
            String aeName = entry.getKey().substring(3);
            NamespacedKey aeKey = new NamespacedKey("advancedenchantments", "ae_enchantment-" + aeName);
            pdc.set(aeKey, PersistentDataType.INTEGER, entry.getValue());
        }

        NamespacedKey aeSlotTrackerKey = new NamespacedKey("advancedenchantments", "slots");
        if (aeEnchants.isEmpty()) {
            pdc.remove(aeSlotTrackerKey);
        } else {
            pdc.set(aeSlotTrackerKey, PersistentDataType.INTEGER, aeEnchants.size());
        }

        item.setItemMeta(meta);
    }

    public Map<String, Integer> getAllEnchantsOnItem(ItemStack item) {
        Map<String, Integer> allEnchants = new HashMap<>(getCustomEnchants(item));
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
                allEnchants.put(entry.getKey().getKey().toString(), entry.getValue());
            }
        }
        return allEnchants;
    }

    public boolean addEnchant(ItemStack item, String enchantId, int level) {
        int maxLvl = getMaxLevel(enchantId);
        int finalLevel = Math.min(level, maxLvl);
        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment bukkitEnc = getBukkitRegistry().get(key);
            if (bukkitEnc != null) {
                ItemMeta meta = item.getItemMeta();
                if (meta == null) return false;
                int currentLvl = meta.getEnchantLevel(bukkitEnc);
                if (currentLvl >= maxLvl && level <= maxLvl) return false;
                meta.addEnchant(bukkitEnc, finalLevel, true);
                item.setItemMeta(meta);
                return true;
            }
        }
        Map<String, Integer> current = getCustomEnchants(item);
        int currentLevel = current.getOrDefault(enchantId, 0);
        if (currentLevel >= maxLvl && level <= maxLvl) return false;
        current.put(enchantId, finalLevel);
        setCustomEnchants(item, current);
        return true;
    }

    public void removeEnchant(ItemStack item, String enchantId) {
        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment bukkitEnc = getBukkitRegistry().get(key);
            if (bukkitEnc != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                meta.removeEnchant(bukkitEnc);
                item.setItemMeta(meta);
                return;
            }
        }
        Map<String, Integer> current = getCustomEnchants(item);
        if (current.containsKey(enchantId)) {
            current.remove(enchantId);
            setCustomEnchants(item, current);
        }
    }

    public int getEnchantLevel(ItemStack item, String enchantId) {
        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment bukkitEnc = getBukkitRegistry().get(key);
            if (bukkitEnc != null && item.hasItemMeta()) return item.getItemMeta().getEnchantLevel(bukkitEnc);
        }
        return getCustomEnchants(item).getOrDefault(enchantId, 0);
    }
}