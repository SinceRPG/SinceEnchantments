package net.danh.sinceenchantments.gui;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PreviewGUI implements InventoryHolder {
    private final Inventory inventory;
    private final SinceEnchantments plugin;
    private final int page;
    private ItemStack weapon;
    private boolean isDummyWeapon = false;

    public PreviewGUI(SinceEnchantments plugin, ItemStack weapon, int page) {
        this(plugin, weapon, page, false);
    }

    public PreviewGUI(SinceEnchantments plugin, ItemStack weapon, int page, boolean isDummyWeapon) {
        this.plugin = plugin;
        this.weapon = weapon;
        this.page = page;
        this.isDummyWeapon = isDummyWeapon;
        int size = plugin.getGuiFile().getInt("gui.preview.size", 54);
        if (size % 9 != 0 || size < 27 || size > 54) size = 54;

        String titleRaw = plugin.getGuiFile().getString("gui.preview.title", "&8Enchantments Preview");
        Component title = ColorUtils.parse(titleRaw);

        this.inventory = Bukkit.createInventory(this, size, title);
        populate();
    }

    public void setWeapon(ItemStack weapon) {
        setWeapon(weapon, false);
    }

    public void setWeapon(ItemStack weapon, boolean isDummyWeapon) {
        this.weapon = weapon;
        this.isDummyWeapon = isDummyWeapon;
        populate();
    }

    public boolean isDummyWeapon() {
        return isDummyWeapon;
    }

    public ItemStack getWeapon() {
        return weapon;
    }

    public void populate() {
        inventory.clear();
        int size = inventory.getSize();
        int itemSlot = plugin.getGuiFile().getInt("gui.preview.item-slot", 10);
        
        List<Integer> innerSlots = plugin.getGuiFile().getConfig().getIntegerList("gui.preview.enchantment-slots");
        if (innerSlots.isEmpty()) {
            innerSlots = List.of(12, 13, 14, 15, 16, 21, 22, 23, 24, 25);
        }

        List<Integer> indicatorSlots = plugin.getGuiFile().getConfig().getIntegerList("gui.preview.indicator.slots");
        String indicatorMatStr = plugin.getGuiFile().getString("gui.preview.indicator.material", "LIME_STAINED_GLASS_PANE");
        Material indicatorMat = Material.matchMaterial(indicatorMatStr);
        if (indicatorMat == null) indicatorMat = Material.LIME_STAINED_GLASS_PANE;

        ItemStack indicator = new ItemStack(indicatorMat);
        ItemMeta indicatorMeta = indicator.getItemMeta();
        if (indicatorMeta != null) {
            indicatorMeta.displayName(ColorUtils.parse(plugin.getGuiFile().getString("gui.preview.indicator.name", " ")));
            indicator.setItemMeta(indicatorMeta);
        }

        String borderMatStr = plugin.getGuiFile().getString("gui.preview.border.material", "BLACK_STAINED_GLASS_PANE");
        Material borderMat = Material.matchMaterial(borderMatStr);
        if (borderMat == null) borderMat = Material.BLACK_STAINED_GLASS_PANE;

        ItemStack border = new ItemStack(borderMat);
        ItemMeta borderMeta = border.getItemMeta();
        if (borderMeta != null) {
            borderMeta.displayName(ColorUtils.parse(plugin.getGuiFile().getString("gui.preview.border.name", " ")));
            border.setItemMeta(borderMeta);
        }

        ConfigurationSection fillSec = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.fill-items");
        if (fillSec != null) {
            for (String key : fillSec.getKeys(false)) {
                ConfigurationSection itemCfg = fillSec.getConfigurationSection(key);
                if (itemCfg != null) {
                    List<Integer> slots = itemCfg.getIntegerList("slots");
                    String matStr = itemCfg.getString("material", "BLACK_STAINED_GLASS_PANE");
                    Material mat = Material.matchMaterial(matStr);
                    if (mat == null) mat = Material.BLACK_STAINED_GLASS_PANE;
                    ItemStack fillItem = new ItemStack(mat);
                    plugin.getItemFactory().applyItemMeta(fillItem, itemCfg, "&r");
                    for (int slot : slots) {
                        if (slot >= 0 && slot < inventory.getSize()) {
                            inventory.setItem(slot, fillItem);
                        }
                    }
                }
            }
        }

        ConfigurationSection backCfg = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.back-button");
        if (backCfg != null) {
            int slot = backCfg.getInt("slot", 36);
            String matStr = backCfg.getString("material", "BARRIER");
            Material mat = Material.matchMaterial(matStr);
            if (mat == null) mat = Material.BARRIER;
            ItemStack backItem = new ItemStack(mat);
            plugin.getItemFactory().applyItemMeta(backItem, backCfg, "&cClose Menu");
            if (slot >= 0 && slot < inventory.getSize()) {
                inventory.setItem(slot, backItem);
            }
        }

        int totalPages = 1;

        if (weapon != null && weapon.getType() != Material.AIR) {
            inventory.setItem(itemSlot, weapon);

            java.util.Map<String, Integer> currentEnchants = new java.util.HashMap<>();
            if (!isDummyWeapon) {
                currentEnchants = plugin.getEnchantManager().getAllEnchantsOnItem(weapon);
            }

            List<String> whitelisted = plugin.getEnchantManager().getWhitelistedEnchants(weapon);

            int maxItems = innerSlots.size();
            totalPages = (int) Math.ceil((double) whitelisted.size() / maxItems);
            if (totalPages == 0) totalPages = 1;

            int startIndex = page * maxItems;
            int endIndex = Math.min(startIndex + maxItems, whitelisted.size());
            int slotIndex = 0;

            for (int i = startIndex; i < endIndex; i++) {
                if (slotIndex >= innerSlots.size()) break;
                String id = whitelisted.get(i);
                int maxLevel = plugin.getEnchantManager().getMaxLevel(id);
                int currentLevel = currentEnchants.getOrDefault(id, 0);

                ConfigurationSection cfg;
                if (currentLevel == 0) {
                    cfg = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.not-applied-display-item");
                } else if (currentLevel >= maxLevel) {
                    cfg = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.maxed-display-item");
                } else {
                    cfg = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.display-item");
                }
                if (cfg == null) cfg = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.display-item");

                String matStr = cfg != null ? cfg.getString("material", "ENCHANTED_BOOK") : "ENCHANTED_BOOK";
                Material mat = Material.matchMaterial(matStr);
                if (mat == null) mat = Material.ENCHANTED_BOOK;

                ItemStack displayBook = new ItemStack(mat);
                String name = plugin.getEnchantManager().getEnchantName(id);
                String rarity = plugin.getEnchantManager().getRarity(id);
                String color = plugin.getSettingsFile().getString("rarities." + rarity, "&f");
                String type = plugin.getEnchantManager().getEnchantType(id);
                String target = plugin.getEnchantManager().getTarget(id);

                plugin.getItemFactory().applyItemMeta(displayBook, cfg, "&e" + name,
                    "%enchant_name%", name,
                    "%current_level%", String.valueOf(currentLevel),
                    "%max_level%", String.valueOf(maxLevel),
                    "%rarity_name%", rarity,
                    "%rarity_color%", color,
                    "%type%", type,
                    "%target%", target
                );

                ItemMeta meta = displayBook.getItemMeta();
                if (meta != null && meta.hasLore()) {
                    List<Component> newLore = new ArrayList<>();
                    for (Component c : meta.lore()) {
                        String plain = net.danh.sinceenchantments.utils.ColorUtils.toPlainText(c);
                        if (plain.contains("%description%")) {
                            for (String descLine : plugin.getEnchantManager().getDescription(id)) {
                                newLore.add(net.danh.sinceenchantments.utils.ColorUtils.parse(descLine).decoration(TextDecoration.ITALIC, false));
                            }
                        } else {
                            newLore.add(c);
                        }
                    }
                    meta.lore(newLore);
                    displayBook.setItemMeta(meta);
                }

                inventory.setItem(innerSlots.get(slotIndex++), displayBook);
            }
        } else {
            ConfigurationSection defaultItemsSec = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.default-items");
            if (defaultItemsSec != null) {
                List<String> keys = new ArrayList<>(defaultItemsSec.getKeys(false));
                int maxItems = innerSlots.size();
                totalPages = (int) Math.ceil((double) keys.size() / maxItems);
                if (totalPages == 0) totalPages = 1;

                int startIndex = page * maxItems;
                int endIndex = Math.min(startIndex + maxItems, keys.size());
                int slotIndex = 0;

                for (int i = startIndex; i < endIndex; i++) {
                    if (slotIndex >= innerSlots.size()) break;
                    String key = keys.get(i);
                    String path = "gui.preview.default-items." + key;
                    ConfigurationSection itemCfg = plugin.getGuiFile().getConfig().getConfigurationSection(path);
                    
                    String matStr = plugin.getGuiFile().getString(path + ".material", "DIRT");
                    Material mat = Material.matchMaterial(matStr);
                    if (mat == null) mat = Material.DIRT;
                    
                    ItemStack defaultItem = new ItemStack(mat);
                    plugin.getItemFactory().applyItemMeta(defaultItem, itemCfg, "&bDefault Item");
                    
                    ItemMeta meta = defaultItem.getItemMeta();
                    if (meta != null) {
                        NamespacedKey dummyKey = new NamespacedKey(plugin, "gui_dummy_item");
                        meta.getPersistentDataContainer().set(dummyKey, PersistentDataType.STRING, key);
                        
                        String customKey = plugin.getGuiFile().getString(path + ".custom-key");
                        if (customKey != null && !customKey.isEmpty()) {
                            NamespacedKey dummyCustomKey = new NamespacedKey(plugin, "gui_dummy_custom_key");
                            meta.getPersistentDataContainer().set(dummyCustomKey, PersistentDataType.STRING, customKey.toUpperCase());
                        }
                        
                        defaultItem.setItemMeta(meta);
                    }
                    
                    inventory.setItem(innerSlots.get(slotIndex++), defaultItem);
                }
            }
        }

        if (page > 0) {
            ConfigurationSection prevCfg = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.prev-page");
            if (prevCfg != null) {
                int prevSlot = prevCfg.getInt("slot", 17);
                Material prevMat = Material.matchMaterial(prevCfg.getString("material", "ARROW"));
                if (prevMat == null) prevMat = Material.ARROW;
                ItemStack prev = new ItemStack(prevMat);
                plugin.getItemFactory().applyItemMeta(prev, prevCfg, "&cPrevious Page", "%prev_page%", String.valueOf(page));
                inventory.setItem(prevSlot, prev);
            }
        }

        if (page < totalPages - 1) {
            ConfigurationSection nextCfg = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.next-page");
            if (nextCfg != null) {
                int nextSlot = nextCfg.getInt("slot", 26);
                Material nextMat = Material.matchMaterial(nextCfg.getString("material", "ARROW"));
                if (nextMat == null) nextMat = Material.ARROW;
                ItemStack next = new ItemStack(nextMat);
                plugin.getItemFactory().applyItemMeta(next, nextCfg, "&aNext Page", "%next_page%", String.valueOf(page + 2));
                inventory.setItem(nextSlot, next);
            }
        }
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
    
    public int getPage() {
        return page;
    }
}
