package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantManager {

    private final SinceEnchantments plugin;
    public final NamespacedKey ENCHANT_KEY;

    public final NamespacedKey BOOK_ID_KEY;
    public final NamespacedKey BOOK_LEVEL_KEY;
    public final NamespacedKey BOOK_SUCCESS_KEY;
    public final NamespacedKey BOOK_DESTROY_KEY;

    private final Map<String, Integer> maxLevels = new HashMap<>();
    private final Map<String, String> rarities = new HashMap<>();
    private final Map<String, String> enchantNames = new HashMap<>();

    // Lưu trữ Target và Conflict
    private final Map<String, String> targets = new HashMap<>();
    private final Map<String, List<String>> conflicts = new HashMap<>();

    public EnchantManager(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.ENCHANT_KEY = new NamespacedKey(plugin, "custom_enchants");
        this.BOOK_ID_KEY = new NamespacedKey(plugin, "book_enchant_id");
        this.BOOK_LEVEL_KEY = new NamespacedKey(plugin, "book_enchant_level");
        this.BOOK_SUCCESS_KEY = new NamespacedKey(plugin, "book_success_rate");
        this.BOOK_DESTROY_KEY = new NamespacedKey(plugin, "book_destroy_rate");
        loadEnchantsFromConfig();
    }

    public void loadEnchantsFromConfig() {
        maxLevels.clear();
        rarities.clear();
        enchantNames.clear();
        targets.clear();
        conflicts.clear();

        if (plugin.getConfigFile().getConfig().contains("custom-enchants")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("custom-enchants").getKeys(false)) {
                String path = "custom-enchants." + key;
                maxLevels.put(key, plugin.getConfigFile().getInt(path + ".max-level", 1));
                rarities.put(key, plugin.getConfigFile().getString(path + ".rarity", "COMMON"));
                enchantNames.put(key, plugin.getConfigFile().getString(path + ".name", key));
                targets.put(key, plugin.getConfigFile().getString(path + ".target", "ALL").toUpperCase());
                conflicts.put(key, plugin.getConfigFile().getStringList(path + ".conflicts"));
            }
        }
    }

    public int getMaxLevel(String enchantId) { return maxLevels.getOrDefault(enchantId, 1); }
    public String getRarity(String enchantId) { return rarities.getOrDefault(enchantId, "COMMON"); }
    public String getEnchantName(String enchantId) { return enchantNames.getOrDefault(enchantId, enchantId); }

    // ===============================================
    // KIỂM TRA TARGET VÀ CONFLICTS
    // ===============================================
    public boolean isApplicable(String enchantId, Material mat) {
        String target = targets.getOrDefault(enchantId, "ALL");
        String name = mat.name();
        switch (target) {
            case "WEAPON": return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.equals("TRIDENT") || name.equals("MACE");
            case "ARMOR": return name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS");
            case "SWORD": return name.endsWith("_SWORD");
            case "BOW": return name.equals("BOW") || name.equals("CROSSBOW");
            case "TOOL": return name.endsWith("_PICKAXE") || name.endsWith("_SHOVEL") || name.endsWith("_AXE") || name.endsWith("_HOE");
            case "ALL": return true;
            default: return true;
        }
    }

    public boolean hasConflict(String enchantId, ItemStack item) {
        List<String> conflictList = conflicts.getOrDefault(enchantId, new ArrayList<>());
        if (conflictList.isEmpty()) return false;

        // Kiểm tra với Custom Enchants đang có
        Map<String, Integer> currentCustoms = getCustomEnchants(item);
        for (String conf : conflictList) {
            if (currentCustoms.containsKey(conf)) return true;
        }

        // Kiểm tra với Vanilla Enchants đang có
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            for (Enchantment vanilla : item.getItemMeta().getEnchants().keySet()) {
                String vId = vanilla.getKey().toString(); // Trả về dạng minecraft:smite
                if (conflictList.contains(vId)) return true;
            }
        }
        return false;
    }

    // ===============================================
    // HỆ THỐNG NBT CHO VŨ KHÍ / GIÁP
    // ===============================================
    public Map<String, Integer> getCustomEnchants(ItemStack item) {
        Map<String, Integer> enchants = new HashMap<>();
        if (item == null || !item.hasItemMeta()) return enchants;
        ItemMeta meta = item.getItemMeta();

        String rawData = meta.getPersistentDataContainer().get(ENCHANT_KEY, PersistentDataType.STRING);
        if (rawData != null && !rawData.isEmpty()) {
            String[] pairs = rawData.split(";");
            for (String pair : pairs) {
                String[] split = pair.split(",");
                if (split.length == 2) {
                    enchants.put(split[0], Integer.parseInt(split[1]));
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

    public boolean addEnchant(ItemStack item, String enchantId, int level) {
        Map<String, Integer> current = getCustomEnchants(item);
        int currentLevel = current.getOrDefault(enchantId, 0);
        int maxLvl = getMaxLevel(enchantId);

        if (currentLevel >= maxLvl && level <= maxLvl) return false;
        current.put(enchantId, Math.min(level, maxLvl));

        setCustomEnchants(item, current);
        return true;
    }

    // ===============================================
    // TẠO SÁCH ENCHANT TỪ ITEMS.YML
    // ===============================================
    public ItemStack createEnchantBook(String enchantId, int level, int successRate, int destroyRate) {
        String matStr = plugin.getItemsFile().getString("enchant-book.material", "ENCHANTED_BOOK");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.ENCHANTED_BOOK;

        ItemStack book = new ItemStack(mat);
        ItemMeta meta = book.getItemMeta();

        String rawName = plugin.getItemsFile().getString("enchant-book.name", "Sách: %enchant_name%");
        List<String> rawLore = plugin.getItemsFile().getStringList("enchant-book.lore");

        String eName = getEnchantName(enchantId);
        String rName = getRarity(enchantId);
        String rColor = plugin.getConfigFile().getString("rarities." + rName, "&f");

        rawName = rawName.replace("%enchant_name%", eName)
                .replace("%level%", String.valueOf(level))
                .replace("%rarity_name%", rName)
                .replace("%rarity_color%", rColor);
        meta.displayName(ColorUtils.parse(rawName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

        List<Component> finalLore = new ArrayList<>();
        for (String line : rawLore) {
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
}