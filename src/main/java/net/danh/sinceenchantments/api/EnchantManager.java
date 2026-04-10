package net.danh.sinceenchantments.api;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
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

    public final NamespacedKey SLOT_MODIFIER_KEY;
    public final NamespacedKey LOCKED_KEY;
    public final NamespacedKey LOCK_SCROLL_KEY;
    public final NamespacedKey PURGE_SCROLL_KEY;
    public final NamespacedKey PURGE_RETURN_KEY;

    private final SinceEnchantments plugin;
    private final Map<String, Integer> maxLevels = new HashMap<>();
    private final Map<String, String> rarities = new HashMap<>();
    private final Map<String, String> enchantNames = new HashMap<>();
    private final Map<String, String> targets = new HashMap<>();
    private final Map<String, List<String>> conflicts = new HashMap<>();
    private final Map<String, List<String>> requires = new HashMap<>();
    private final Map<String, List<String>> descriptions = new HashMap<>();

    private final Map<String, List<String>> itemWhitelist = new HashMap<>();
    private final Map<String, List<String>> mmoItemsWhitelist = new HashMap<>();

    private final Map<String, Integer> itemMaxSlots = new HashMap<>();
    private final Map<String, Integer> mmoItemsMaxSlots = new HashMap<>();

    private final Map<String, Integer> itemMaxSlotModifiers = new HashMap<>();
    private final Map<String, Integer> mmoItemsMaxSlotModifiers = new HashMap<>();

    private Method nbtItemGetMethod;
    private Method nbtItemHasTypeMethod;
    private Method nbtItemGetTypeMethod;
    private Method nbtItemGetStringMethod;
    private boolean mmoItemsHooked = false;

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

        this.SLOT_MODIFIER_KEY = new NamespacedKey(plugin, "slot_modifier");
        this.LOCKED_KEY = new NamespacedKey(plugin, "item_locked");
        this.LOCK_SCROLL_KEY = new NamespacedKey(plugin, "lock_scroll");
        this.PURGE_SCROLL_KEY = new NamespacedKey(plugin, "purge_scroll");
        this.PURGE_RETURN_KEY = new NamespacedKey(plugin, "purge_return_books");

        setupMMOItemsHook();
        loadEnchantsFromConfig();
    }

    private void setupMMOItemsHook() {
        if (Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            try {
                Class<?> nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
                nbtItemGetMethod = nbtItemClass.getMethod("get", ItemStack.class);
                nbtItemHasTypeMethod = nbtItemClass.getMethod("hasType");
                nbtItemGetTypeMethod = nbtItemClass.getMethod("getType");
                nbtItemGetStringMethod = nbtItemClass.getMethod("getString", String.class);
                mmoItemsHooked = true;
                plugin.getLogger().info("Successfully hooked into MMOItems NBT API!");
            } catch (Exception e) {
                plugin.getLogger().warning("MMOItems detected, but failed to hook into MythicLib API.");
            }
        }
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
        mmoItemsWhitelist.clear();
        itemMaxSlots.clear();
        mmoItemsMaxSlots.clear();
        itemMaxSlotModifiers.clear();
        mmoItemsMaxSlotModifiers.clear();

        ConfigurationSection customEnchSec = plugin.getConfigFile().getConfig().getConfigurationSection("custom-enchants");
        if (customEnchSec != null) {
            for (String key : customEnchSec.getKeys(false)) {
                String path = "custom-enchants." + key;
                maxLevels.put(key, plugin.getConfigFile().getInt(path + ".max-level", 1));
                rarities.put(key, plugin.getConfigFile().getString(path + ".rarity", "COMMON"));
                enchantNames.put(key, plugin.getConfigFile().getString(path + ".name", key));
                targets.put(key, plugin.getConfigFile().getString(path + ".target", "ALL").toUpperCase());
                conflicts.put(key, plugin.getConfigFile().getStringList(path + ".conflicts"));
                requires.put(key, plugin.getConfigFile().getStringList(path + ".requires"));
                descriptions.put(key, plugin.getConfigFile().getStringList(path + ".description"));
            }
        }

        ConfigurationSection vanillaEnchSec = plugin.getConfigFile().getConfig().getConfigurationSection("vanilla-enchants");
        if (vanillaEnchSec != null) {
            for (String key : vanillaEnchSec.getKeys(false)) {
                String path = "vanilla-enchants." + key;
                descriptions.put(key, plugin.getConfigFile().getStringList(path + ".description"));
                enchantNames.put(key, plugin.getConfigFile().getString(path + ".name", key));
            }
        }

        ConfigurationSection itemWlSec = plugin.getConfigFile().getConfig().getConfigurationSection("item-whitelist");
        if (itemWlSec != null) {
            for (String key : itemWlSec.getKeys(false)) {
                itemWhitelist.put(key.toUpperCase(), plugin.getConfigFile().getStringList("item-whitelist." + key));
            }
        }

        ConfigurationSection mmoWlSec = plugin.getConfigFile().getConfig().getConfigurationSection("mmoitems-whitelist");
        if (mmoWlSec != null) {
            for (String key : mmoWlSec.getKeys(false)) {
                mmoItemsWhitelist.put(key.toUpperCase(), plugin.getConfigFile().getStringList("mmoitems-whitelist." + key));
            }
        }

        ConfigurationSection itemSlotSec = plugin.getConfigFile().getConfig().getConfigurationSection("item-max-slots");
        if (itemSlotSec != null) {
            for (String key : itemSlotSec.getKeys(false)) {
                itemMaxSlots.put(key.toUpperCase(), plugin.getConfigFile().getInt("item-max-slots." + key));
            }
        }

        ConfigurationSection mmoSlotSec = plugin.getConfigFile().getConfig().getConfigurationSection("mmoitems-max-slots");
        if (mmoSlotSec != null) {
            for (String key : mmoSlotSec.getKeys(false)) {
                mmoItemsMaxSlots.put(key.toUpperCase(), plugin.getConfigFile().getInt("mmoitems-max-slots." + key));
            }
        }

        ConfigurationSection itemModSec = plugin.getConfigFile().getConfig().getConfigurationSection("item-max-slot-modifiers");
        if (itemModSec != null) {
            for (String key : itemModSec.getKeys(false)) {
                itemMaxSlotModifiers.put(key.toUpperCase(), plugin.getConfigFile().getInt("item-max-slot-modifiers." + key));
            }
        }

        ConfigurationSection mmoModSec = plugin.getConfigFile().getConfig().getConfigurationSection("mmoitems-max-slot-modifiers");
        if (mmoModSec != null) {
            for (String key : mmoModSec.getKeys(false)) {
                mmoItemsMaxSlotModifiers.put(key.toUpperCase(), plugin.getConfigFile().getInt("mmoitems-max-slot-modifiers." + key));
            }
        }
    }

    private boolean isMatch(String text, String pattern) {
        if (pattern.equals("*")) return true;
        if (pattern.startsWith("*") && pattern.endsWith("*") && pattern.length() >= 2) {
            return text.contains(pattern.substring(1, pattern.length() - 1));
        }
        if (pattern.startsWith("*")) {
            return text.endsWith(pattern.substring(1));
        }
        if (pattern.endsWith("*")) {
            return text.startsWith(pattern.substring(0, pattern.length() - 1));
        }
        return text.equals(pattern);
    }

    public String getMMOItemKey(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        NamespacedKey typeKey = new NamespacedKey("mmoitems", "type");
        NamespacedKey idKey = new NamespacedKey("mmoitems", "id");

        if (pdc.has(typeKey, PersistentDataType.STRING) && pdc.has(idKey, PersistentDataType.STRING)) {
            String type = pdc.get(typeKey, PersistentDataType.STRING);
            String id = pdc.get(idKey, PersistentDataType.STRING);
            if (type != null && id != null) return (type + ":" + id).toUpperCase();
        }

        if (!mmoItemsHooked) return null;
        try {
            Object nbtItem = nbtItemGetMethod.invoke(null, item);
            if ((boolean) nbtItemHasTypeMethod.invoke(nbtItem)) {
                String type = (String) nbtItemGetTypeMethod.invoke(nbtItem);
                String id = (String) nbtItemGetStringMethod.invoke(nbtItem, "MMOITEMS_ITEM_ID");
                if (type != null && id != null && !type.isEmpty() && !id.isEmpty()) {
                    return (type + ":" + id).toUpperCase();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public void cleanItemLore(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
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
                    if (start < lore.size()) {
                        lore.remove(start);
                    }
                }

                if (hadPlaceholder) {
                    String placeholderStr = plugin.getConfigFile().getString("settings.placeholder", "#enchants#");
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
        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment bukkitEnc = getBukkitRegistry().get(key);
            if (bukkitEnc != null) return bukkitEnc.getMaxLevel();
        }
        return maxLevels.getOrDefault(enchantId, 1);
    }

    public String getRarity(String enchantId) {
        if (isBukkitEnchant(enchantId)) return "COMMON";
        return rarities.getOrDefault(enchantId, "COMMON");
    }

    public String getEnchantName(String enchantId) {
        return enchantNames.getOrDefault(enchantId, enchantId);
    }

    public List<String> getDescription(String enchantId) {
        return descriptions.getOrDefault(enchantId, new ArrayList<>());
    }

    public int getRawMaxSlots(ItemStack item) {
        int defaultSlots = plugin.getConfigFile().getInt("settings.default-max-custom-enchants-per-item", 5);
        return getMaxSlot(item, defaultSlots, mmoItemsMaxSlots, itemMaxSlots);
    }

    public int getMaxSlotModifiersAllowed(ItemStack item) {
        int defaultAllowed = plugin.getConfigFile().getInt("settings.default-max-slot-modifiers-allowed", 5);
        return getMaxSlot(item, defaultAllowed, mmoItemsMaxSlotModifiers, itemMaxSlotModifiers);
    }

    private int getMaxSlot(ItemStack item, int defaultAllowed, Map<String, Integer> mmoItemsMaxSlotModifiers, Map<String, Integer> itemMaxSlotModifiers) {
        if (item == null || !item.hasItemMeta()) return defaultAllowed;

        int maxAllowed = 0;
        boolean matched = false;

        String fullMmoKey = getMMOItemKey(item);
        if (fullMmoKey != null) {
            for (Map.Entry<String, Integer> entry : mmoItemsMaxSlotModifiers.entrySet()) {
                if (isMatch(fullMmoKey, entry.getKey())) {
                    maxAllowed += entry.getValue();
                    matched = true;
                }
            }
        }

        if (matched) return maxAllowed;

        String matName = item.getType().name().toUpperCase();
        for (Map.Entry<String, Integer> entry : itemMaxSlotModifiers.entrySet()) {
            if (isMatch(matName, entry.getKey())) {
                maxAllowed += entry.getValue();
                matched = true;
            }
        }

        return matched ? maxAllowed : defaultAllowed;
    }

    public int getMaxSlots(ItemStack item) {
        if (item == null || !item.hasItemMeta())
            return plugin.getConfigFile().getInt("settings.default-max-custom-enchants-per-item", 5);
        int raw = getRawMaxSlots(item);
        int modifier = item.getItemMeta().getPersistentDataContainer().getOrDefault(SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, 0);
        return Math.max(0, raw + modifier);
    }

    public int getAppliedEnchantsCount(ItemStack item) {
        int count = getCustomEnchants(item).size();
        if (plugin.getConfigFile().getBoolean("settings.override-vanilla-enchants", true)) {
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
        if (item == null || !item.hasItemMeta()) return allowed;

        Set<String> mergedWhitelist = new HashSet<>();
        boolean matched = false;

        String fullMmoKey = getMMOItemKey(item);
        if (fullMmoKey != null) {
            for (Map.Entry<String, List<String>> entry : mmoItemsWhitelist.entrySet()) {
                if (isMatch(fullMmoKey, entry.getKey())) {
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
        if (allowed.isEmpty()) {
            return true;
        }
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
        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment bukkitEnc = getBukkitRegistry().get(key);
            if (bukkitEnc != null) return bukkitEnc.canEnchantItem(new ItemStack(mat));
        }
        String target = targets.getOrDefault(enchantId, "ALL");
        String name = mat.name();
        return switch (target) {
            case "WEAPON" ->
                    name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("TRIDENT") || name.equals("MACE");
            case "ARMOR" ->
                    name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
            case "SWORD" -> name.endsWith("_SWORD");
            case "BOW" -> name.equals("BOW") || name.equals("CROSSBOW");
            case "TOOL" ->
                    name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE") || name.endsWith("_HOE");
            default -> true;
        };
    }

    public boolean hasConflict(String enchantId, ItemStack item) {
        NamespacedKey key = NamespacedKey.fromString(enchantId.toLowerCase());
        if (key != null) {
            Enchantment currentBukkit = getBukkitRegistry().get(key);
            if (currentBukkit != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                for (Enchantment applied : item.getItemMeta().getEnchants().keySet()) {
                    if (currentBukkit.conflictsWith(applied)) return true;
                }
                return false;
            }
        }

        List<String> conflictList = conflicts.getOrDefault(enchantId, new ArrayList<>());
        if (conflictList.isEmpty()) return false;

        Map<String, Integer> currentCustoms = getCustomEnchants(item);
        for (String conf : conflictList) {
            if (currentCustoms.containsKey(conf)) return true;
        }

        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            for (Enchantment vanilla : item.getItemMeta().getEnchants().keySet()) {
                String vId = vanilla.getKey().toString();
                if (conflictList.contains(vId)) return true;
            }
        }
        return false;
    }

    public Map<String, Integer> getCustomEnchants(ItemStack item) {
        Map<String, Integer> enchants = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return enchants;

        String rawData = item.getItemMeta().getPersistentDataContainer().get(ENCHANT_KEY, PersistentDataType.STRING);
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
        return enchants;
    }

    public void setCustomEnchants(ItemStack item, Map<String, Integer> enchants) {
        if (item == null || !item.hasItemMeta()) return;
        ItemMeta meta = item.getItemMeta();

        if (enchants.isEmpty()) {
            meta.getPersistentDataContainer().remove(ENCHANT_KEY);
        } else {
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
                sb.append(entry.getKey()).append(",").append(entry.getValue()).append(";");
            }
            meta.getPersistentDataContainer().set(ENCHANT_KEY, PersistentDataType.STRING, sb.toString());
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

    private ItemStack buildItem(String configPath, String defMat, int amount) {
        String matStr = plugin.getItemsFile().getString(configPath + ".material", defMat);
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.valueOf(defMat);
        return new ItemStack(mat, amount);
    }

    private void applyItemMeta(ItemStack item, String configPath, String defName, String... replacements) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        String name = plugin.getItemsFile().getString(configPath + ".name", defName);
        for (int i = 0; i < replacements.length; i += 2) {
            name = name.replace(replacements[i], replacements[i + 1]);
        }
        meta.displayName(ColorUtils.parse(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<String> rawLore = plugin.getItemsFile().getStringList(configPath + ".lore");
        List<Component> compLore = new ArrayList<>();
        for (String line : rawLore) {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace(replacements[i], replacements[i + 1]);
            }
            compLore.add(ColorUtils.parse(line).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);
        item.setItemMeta(meta);
    }

    public ItemStack createEnchantBook(String enchantId, int level, int successRate, int destroyRate) {
        ItemStack book = buildItem("enchant-book", "ENCHANTED_BOOK", 1);
        String eName = getEnchantName(enchantId);
        String rName = getRarity(enchantId);
        String rColor = plugin.getConfigFile().getString("rarities." + rName, "&f");
        List<String> description = getDescription(enchantId);

        ItemMeta meta = book.getItemMeta();
        String rawName = plugin.getItemsFile().getString("enchant-book.name", "Book: %enchant_name%");
        rawName = rawName.replace("%enchant_name%", eName)
                .replace("%level%", String.valueOf(level))
                .replace("%rarity_name%", rName)
                .replace("%rarity_color%", rColor);
        meta.displayName(ColorUtils.parse(rawName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<String> rawLore = plugin.getItemsFile().getStringList("enchant-book.lore");
        List<Component> finalLore = new ArrayList<>();
        for (String line : rawLore) {
            if (line.contains("%description%")) {
                for (String descLine : description) {
                    finalLore.add(ColorUtils.parse(descLine).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
                }
                continue;
            }
            String parsedLine = line.replace("%enchant_name%", eName)
                    .replace("%level%", String.valueOf(level))
                    .replace("%success%", String.valueOf(successRate))
                    .replace("%destroy%", String.valueOf(destroyRate))
                    .replace("%rarity_name%", rName)
                    .replace("%rarity_color%", rColor);
            finalLore.add(ColorUtils.parse(parsedLine).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(finalLore);

        meta.getPersistentDataContainer().set(BOOK_ID_KEY, PersistentDataType.STRING, enchantId);
        meta.getPersistentDataContainer().set(BOOK_LEVEL_KEY, PersistentDataType.INTEGER, level);
        meta.getPersistentDataContainer().set(BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, successRate);
        meta.getPersistentDataContainer().set(BOOK_DESTROY_KEY, PersistentDataType.INTEGER, destroyRate);
        book.setItemMeta(meta);
        return book;
    }

    public ItemStack createExtractor(String type, int amount) {
        String path = type.toLowerCase() + "-extractor";
        ItemStack item = buildItem(path, "PAPER", amount);
        applyItemMeta(item, path, "Extractor");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(EXTRACTOR_TYPE_KEY, PersistentDataType.STRING, type.toUpperCase());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSuccessCharm(int bonus, int amount) {
        ItemStack item = buildItem("success-charm", "GLOWSTONE_DUST", amount);
        applyItemMeta(item, "success-charm", "Success Charm", "%bonus%", String.valueOf(bonus));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(CHARM_BONUS_KEY, PersistentDataType.INTEGER, bonus);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSlotGem(int modifier, int amount) {
        ItemStack item = buildItem("slot-gem", "EMERALD", amount);
        String modStr = (modifier >= 0 ? "+" : "") + modifier;
        applyItemMeta(item, "slot-gem", "Slot Gem", "%modifier%", modStr);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, modifier);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createLockScroll(int amount) {
        ItemStack item = buildItem("lock-scroll", "PAPER", amount);
        applyItemMeta(item, "lock-scroll", "Lock Scroll");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(LOCK_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createPurgeScroll(boolean returnBooks, int amount) {
        ItemStack item = buildItem("purge-scroll", "PAPER", amount);
        applyItemMeta(item, "purge-scroll", "Purge Scroll", "%returns%", returnBooks ? "True" : "False");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(PURGE_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(PURGE_RETURN_KEY, PersistentDataType.BYTE, (byte) (returnBooks ? 1 : 0));
            item.setItemMeta(meta);
        }
        return item;
    }
}