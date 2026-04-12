package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

/**
 * Factory class responsible for creating all custom items used in the plugin.
 */
public class ItemFactory {

    private final SinceEnchantments plugin;
    private final EnchantManager manager;

    public ItemFactory(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
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
        meta.displayName(ColorUtils.parse(name).decoration(TextDecoration.ITALIC, false));
        List<String> rawLore = plugin.getItemsFile().getStringList(configPath + ".lore");
        List<Component> compLore = new ArrayList<>();
        for (String line : rawLore) {
            for (int i = 0; i < replacements.length; i += 2) {
                line = line.replace(replacements[i], replacements[i + 1]);
            }
            compLore.add(ColorUtils.parse(line).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(compLore);
        item.setItemMeta(meta);
    }

    public ItemStack createEnchantBook(String enchantId, int level, int successRate, int destroyRate) {
        ItemStack book = buildItem("enchant-book", "ENCHANTED_BOOK", 1);
        String eName = manager.getEnchantName(enchantId);
        String rName = manager.getRarity(enchantId);
        String rColor = plugin.getSettingsFile().getString("rarities." + rName, "&f");
        List<String> description = manager.getDescription(enchantId);
        ItemMeta meta = book.getItemMeta();
        String rawName = plugin.getItemsFile().getString("enchant-book.name", "Book: %enchant_name%");
        rawName = rawName.replace("%enchant_name%", eName).replace("%level%", String.valueOf(level))
                .replace("%rarity_name%", rName).replace("%rarity_color%", rColor);
        meta.displayName(ColorUtils.parse(rawName).decoration(TextDecoration.ITALIC, false));

        List<String> rawLore = plugin.getItemsFile().getStringList("enchant-book.lore");
        List<Component> finalLore = new ArrayList<>();
        for (String line : rawLore) {
            if (line.contains("%description%")) {
                for (String descLine : description) {
                    finalLore.add(ColorUtils.parse(descLine).decoration(TextDecoration.ITALIC, false));
                }
                continue;
            }
            String parsedLine = line.replace("%enchant_name%", eName).replace("%level%", String.valueOf(level))
                    .replace("%success%", String.valueOf(successRate)).replace("%destroy%", String.valueOf(destroyRate))
                    .replace("%rarity_name%", rName).replace("%rarity_color%", rColor);
            finalLore.add(ColorUtils.parse(parsedLine).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(finalLore);
        meta.getPersistentDataContainer().set(manager.BOOK_ID_KEY, PersistentDataType.STRING, enchantId);
        meta.getPersistentDataContainer().set(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, level);
        meta.getPersistentDataContainer().set(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, successRate);
        meta.getPersistentDataContainer().set(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, destroyRate);
        book.setItemMeta(meta);
        return book;
    }

    public ItemStack createExtractor(String type, int amount) {
        String path = type.toLowerCase() + "-extractor";
        ItemStack item = buildItem(path, "PAPER", amount);
        applyItemMeta(item, path, "Extractor");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.EXTRACTOR_TYPE_KEY, PersistentDataType.STRING, type.toUpperCase());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSuccessCharm(int bonus, int amount) {
        ItemStack item = buildItem("success-charm", "GLOWSTONE_DUST", amount);
        applyItemMeta(item, "success-charm", "Success Charm", "%bonus%", String.valueOf(bonus));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.CHARM_BONUS_KEY, PersistentDataType.INTEGER, bonus);
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
            meta.getPersistentDataContainer().set(manager.SLOT_GEM_KEY, PersistentDataType.INTEGER, modifier);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createLockScroll(int amount) {
        ItemStack item = buildItem("lock-scroll", "PAPER", amount);
        applyItemMeta(item, "lock-scroll", "Lock Scroll");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.LOCK_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createPurgeScroll(boolean returnBooks, int amount) {
        ItemStack item = buildItem("purge-scroll", "PAPER", amount);
        applyItemMeta(item, "purge-scroll", "Purge Scroll", "%returns%", returnBooks ? "True" : "False");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.PURGE_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(manager.PURGE_RETURN_KEY, PersistentDataType.BYTE, (byte) (returnBooks ? 1 : 0));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createRandomizer(int amount) {
        ItemStack item = buildItem("randomizer-stone", "MAGMA_CREAM", amount);
        applyItemMeta(item, "randomizer-stone", "Randomizer Stone");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.RANDOMIZER_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createProtector(int amount) {
        ItemStack item = buildItem("protection-gem", "NETHER_STAR", amount);
        applyItemMeta(item, "protection-gem", "Protection Gem");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.PROTECTOR_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createTracker(int amount) {
        ItemStack item = buildItem("stat-tracker", "CLOCK", amount);
        applyItemMeta(item, "stat-tracker", "Stat Tracker");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.TRACKER_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}