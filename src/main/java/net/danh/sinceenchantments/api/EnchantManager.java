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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Core manager handling NBT data, validation, and Bukkit integration.
 */
@SuppressWarnings("UnstableApiUsage")
public class EnchantManager {

    public final NamespacedKey ENCHANT_KEY;
    public final NamespacedKey BOOK_ID_KEY;
    public final NamespacedKey BOOK_LEVEL_KEY;
    public final NamespacedKey BOOK_SUCCESS_KEY;
    public final NamespacedKey BOOK_DESTROY_KEY;
    public final NamespacedKey EXTRACTOR_TYPE_KEY; // NEW: Identifies Extractor Type

    private final SinceEnchantments plugin;
    private final Map<String, Integer> maxLevels = new HashMap<>();
    private final Map<String, String> rarities = new HashMap<>();
    private final Map<String, String> enchantNames = new HashMap<>();
    private final Map<String, String> targets = new HashMap<>();
    private final Map<String, List<String>> conflicts = new HashMap<>();
    private final Map<String, List<String>> descriptions = new HashMap<>();

    public EnchantManager(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.ENCHANT_KEY = new NamespacedKey(plugin, "custom_enchants");
        this.BOOK_ID_KEY = new NamespacedKey(plugin, "book_enchant_id");
        this.BOOK_LEVEL_KEY = new NamespacedKey(plugin, "book_enchant_level");
        this.BOOK_SUCCESS_KEY = new NamespacedKey(plugin, "book_success_rate");
        this.BOOK_DESTROY_KEY = new NamespacedKey(plugin, "book_destroy_rate");
        this.EXTRACTOR_TYPE_KEY = new NamespacedKey(plugin, "extractor_type");
        loadEnchantsFromConfig();
    }

    public void loadEnchantsFromConfig() {
        maxLevels.clear();
        rarities.clear();
        enchantNames.clear();
        targets.clear();
        conflicts.clear();
        descriptions.clear();

        if (plugin.getConfigFile().getConfig().contains("custom-enchants")) {
            for (String key : plugin.getConfigFile().getConfig().getConfigurationSection("custom-enchants").getKeys(false)) {
                String path = "custom-enchants." + key;
                maxLevels.put(key, plugin.getConfigFile().getInt(path + ".max-level", 1));
                rarities.put(key, plugin.getConfigFile().getString(path + ".rarity", "COMMON"));
                enchantNames.put(key, plugin.getConfigFile().getString(path + ".name", key));
                targets.put(key, plugin.getConfigFile().getString(path + ".target", "ALL").toUpperCase());
                conflicts.put(key, plugin.getConfigFile().getStringList(path + ".conflicts"));
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
                if (split.length == 2) enchants.put(split[0], Integer.parseInt(split[1]));
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

    /**
     * Gets all enchantments (both Vanilla and Custom) currently on the item.
     */
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

    /**
     * Safely removes an enchantment from the item (Vanilla or Custom).
     */
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

    /**
     * Creates an Extractor item (Random or Specific).
     *
     * @param type   "random" or "specific"
     * @param amount The amount to give.
     */
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
}