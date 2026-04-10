package net.danh.sinceenchantments.api;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

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
        mmoItemsWhitelist.clear();
        itemMaxSlots.clear();
        mmoItemsMaxSlots.clear();

        if (plugin.getConfigFile().getConfig().contains("custom-enchants")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("custom-enchants").getKeys(false)) {
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

        if (plugin.getConfigFile().getConfig().contains("vanilla-enchants")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("vanilla-enchants").getKeys(false)) {
                String path = "vanilla-enchants." + key;
                descriptions.put(key, plugin.getConfigFile().getStringList(path + ".description"));
                enchantNames.put(key, plugin.getConfigFile().getString(path + ".name", key));
            }
        }

        if (plugin.getConfigFile().getConfig().contains("item-whitelist")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("item-whitelist").getKeys(false)) {
                itemWhitelist.put(key.toUpperCase(), plugin.getConfigFile().getStringList("item-whitelist." + key));
            }
        }

        if (plugin.getConfigFile().getConfig().contains("mmoitems-whitelist")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("mmoitems-whitelist").getKeys(false)) {
                mmoItemsWhitelist.put(key.toUpperCase(), plugin.getConfigFile().getStringList("mmoitems-whitelist." + key));
            }
        }

        if (plugin.getConfigFile().getConfig().contains("item-max-slots")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("item-max-slots").getKeys(false)) {
                itemMaxSlots.put(key.toUpperCase(), plugin.getConfigFile().getInt("item-max-slots." + key));
            }
        }

        if (plugin.getConfigFile().getConfig().contains("mmoitems-max-slots")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("mmoitems-max-slots").getKeys(false)) {
                mmoItemsMaxSlots.put(key.toUpperCase(), plugin.getConfigFile().getInt("mmoitems-max-slots." + key));
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
        if (isBukkitEnchant(enchantId)) {
            Enchantment bukkitEnc = getBukkitRegistry().get(NamespacedKey.fromString(enchantId.toLowerCase()));
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
        if (item == null || !item.hasItemMeta()) return defaultSlots;

        int totalSlots = 0;
        boolean matched = false;

        NamespacedKey mmoTypeKey = new NamespacedKey("mmoitems", "type");
        NamespacedKey mmoIdKey = new NamespacedKey("mmoitems", "id");
        ItemMeta meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(mmoTypeKey, PersistentDataType.STRING) &&
                meta.getPersistentDataContainer().has(mmoIdKey, PersistentDataType.STRING)) {
            String mmoType = meta.getPersistentDataContainer().get(mmoTypeKey, PersistentDataType.STRING);
            String mmoId = meta.getPersistentDataContainer().get(mmoIdKey, PersistentDataType.STRING);
            String fullMmoKey = (mmoType + "." + mmoId).toUpperCase();

            for (Map.Entry<String, Integer> entry : mmoItemsMaxSlots.entrySet()) {
                if (isMatch(fullMmoKey, entry.getKey())) {
                    totalSlots += entry.getValue();
                    matched = true;
                }
            }
        }

        if (matched) return totalSlots;

        String matName = item.getType().name().toUpperCase();
        for (Map.Entry<String, Integer> entry : itemMaxSlots.entrySet()) {
            if (isMatch(matName, entry.getKey())) {
                totalSlots += entry.getValue();
                matched = true;
            }
        }

        return matched ? totalSlots : defaultSlots;
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

        NamespacedKey mmoTypeKey = new NamespacedKey("mmoitems", "type");
        NamespacedKey mmoIdKey = new NamespacedKey("mmoitems", "id");
        ItemMeta meta = item.getItemMeta();

        if (meta.getPersistentDataContainer().has(mmoTypeKey, PersistentDataType.STRING) &&
                meta.getPersistentDataContainer().has(mmoIdKey, PersistentDataType.STRING)) {
            String mmoType = meta.getPersistentDataContainer().get(mmoTypeKey, PersistentDataType.STRING);
            String mmoId = meta.getPersistentDataContainer().get(mmoIdKey, PersistentDataType.STRING);
            String fullMmoKey = (mmoType + "." + mmoId).toUpperCase();

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
            if (isBukkitEnchant(req)) {
                Enchantment bukkitEnc = getBukkitRegistry().get(NamespacedKey.fromString(req.toLowerCase()));
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
        if (isBukkitEnchant(enchantId)) {
            Enchantment bukkitEnc = getBukkitRegistry().get(NamespacedKey.fromString(enchantId.toLowerCase()));
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
        if (isBukkitEnchant(enchantId)) {
            Enchantment currentBukkit = getBukkitRegistry().get(NamespacedKey.fromString(enchantId.toLowerCase()));
            if (currentBukkit != null && item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
                for (Enchantment applied : item.getItemMeta().getEnchants().keySet()) {
                    if (currentBukkit.conflictsWith(applied)) return true;
                }
            }
            return false;
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

        if (isBukkitEnchant(enchantId)) {
            Enchantment bukkitEnc = getBukkitRegistry().get(NamespacedKey.fromString(enchantId.toLowerCase()));
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
        if (isBukkitEnchant(enchantId)) {
            Enchantment bukkitEnc = getBukkitRegistry().get(NamespacedKey.fromString(enchantId.toLowerCase()));
            if (bukkitEnc != null && item.hasItemMeta()) {
                ItemMeta meta = item.getItemMeta();
                meta.removeEnchant(bukkitEnc);
                item.setItemMeta(meta);
            }
        } else {
            Map<String, Integer> current = getCustomEnchants(item);
            if (current.containsKey(enchantId)) {
                current.remove(enchantId);
                setCustomEnchants(item, current);
            }
        }
    }

    public int getEnchantLevel(ItemStack item, String enchantId) {
        if (isBukkitEnchant(enchantId)) {
            Enchantment bukkitEnc = getBukkitRegistry().get(NamespacedKey.fromString(enchantId.toLowerCase()));
            if (bukkitEnc != null && item.hasItemMeta()) return item.getItemMeta().getEnchantLevel(bukkitEnc);
            return 0;
        }
        return getCustomEnchants(item).getOrDefault(enchantId, 0);
    }

    public ItemStack createEnchantBook(String enchantId, int level, int successRate, int destroyRate) {
        String matStr = plugin.getItemsFile().getString("enchant-book.material", "ENCHANTED_BOOK");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.ENCHANTED_BOOK;

        ItemStack book = new ItemStack(mat);
        ItemMeta meta = book.getItemMeta();

        String rawName = plugin.getItemsFile().getString("enchant-book.name", "Book: %enchant_name%");
        List<String> rawLore = plugin.getItemsFile().getStringList("enchant-book.lore");

        String eName = getEnchantName(enchantId);
        String rName = getRarity(enchantId);
        String rColor = plugin.getConfigFile().getString("rarities." + rName, "&f");
        List<String> description = getDescription(enchantId);

        rawName = rawName.replace("%enchant_name%", eName)
                .replace("%level%", String.valueOf(level))
                .replace("%rarity_name%", rName)
                .replace("%rarity_color%", rColor);
        meta.displayName(ColorUtils.parse(rawName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

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
        String matStr = plugin.getItemsFile().getString(path + ".material", "PAPER");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.PAPER;

        ItemStack extractor = new ItemStack(mat, amount);
        ItemMeta meta = extractor.getItemMeta();

        String name = plugin.getItemsFile().getString(path + ".name", "Extractor");
        List<String> lore = plugin.getItemsFile().getStringList(path + ".lore");

        meta.displayName(ColorUtils.parse(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        List<Component> compLore = new ArrayList<>();
        for (String line : lore) {
            compLore.add(ColorUtils.parse(line).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);

        meta.getPersistentDataContainer().set(EXTRACTOR_TYPE_KEY, PersistentDataType.STRING, type.toUpperCase());
        extractor.setItemMeta(meta);
        return extractor;
    }

    public ItemStack createSuccessCharm(int bonus, int amount) {
        String matStr = plugin.getItemsFile().getString("success-charm.material", "GLOWSTONE_DUST");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.GLOWSTONE_DUST;

        ItemStack charm = new ItemStack(mat, amount);
        ItemMeta meta = charm.getItemMeta();

        String name = plugin.getItemsFile().getString("success-charm.name", "Success Charm");
        List<String> lore = plugin.getItemsFile().getStringList("success-charm.lore");

        meta.displayName(ColorUtils.parse(name.replace("%bonus%", String.valueOf(bonus))).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<Component> compLore = new ArrayList<>();
        for (String line : lore) {
            compLore.add(ColorUtils.parse(line.replace("%bonus%", String.valueOf(bonus))).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);

        meta.getPersistentDataContainer().set(CHARM_BONUS_KEY, PersistentDataType.INTEGER, bonus);
        charm.setItemMeta(meta);
        return charm;
    }

    public ItemStack createSlotGem(int modifier, int amount) {
        String matStr = plugin.getItemsFile().getString("slot-gem.material", "EMERALD");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.EMERALD;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getItemsFile().getString("slot-gem.name", "Slot Gem");
        List<String> lore = plugin.getItemsFile().getStringList("slot-gem.lore");

        String modStr = (modifier >= 0 ? "+" : "") + modifier;
        meta.displayName(ColorUtils.parse(name.replace("%modifier%", modStr)).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<Component> compLore = new ArrayList<>();
        for (String line : lore) {
            compLore.add(ColorUtils.parse(line.replace("%modifier%", modStr)).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);
        meta.getPersistentDataContainer().set(SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, modifier);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createLockScroll(int amount) {
        String matStr = plugin.getItemsFile().getString("lock-scroll.material", "PAPER");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.PAPER;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getItemsFile().getString("lock-scroll.name", "Lock Scroll");
        List<String> lore = plugin.getItemsFile().getStringList("lock-scroll.lore");

        meta.displayName(ColorUtils.parse(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<Component> compLore = new ArrayList<>();
        for (String line : lore) {
            compLore.add(ColorUtils.parse(line).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);
        meta.getPersistentDataContainer().set(LOCK_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
        item.setItemMeta(meta);
        return item;
    }

    public ItemStack createPurgeScroll(boolean returnBooks, int amount) {
        String matStr = plugin.getItemsFile().getString("purge-scroll.material", "PAPER");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.PAPER;

        ItemStack item = new ItemStack(mat, amount);
        ItemMeta meta = item.getItemMeta();

        String name = plugin.getItemsFile().getString("purge-scroll.name", "Purge Scroll");
        List<String> lore = plugin.getItemsFile().getStringList("purge-scroll.lore");

        meta.displayName(ColorUtils.parse(name).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<Component> compLore = new ArrayList<>();
        for (String line : lore) {
            compLore.add(ColorUtils.parse(line.replace("%returns%", returnBooks ? "True" : "False")).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);
        meta.getPersistentDataContainer().set(PURGE_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(PURGE_RETURN_KEY, PersistentDataType.BYTE, (byte) (returnBooks ? 1 : 0));
        item.setItemMeta(meta);
        return item;
    }
}