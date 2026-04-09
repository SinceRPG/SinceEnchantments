package net.danh.sinceenchantments.gui;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Custom InventoryHolder to identify the Specific Extractor GUI.
 */
public class ExtractorGUI implements InventoryHolder {

    private final Inventory inventory;
    private final ItemStack weapon;
    private boolean isCompleted = false;

    public ExtractorGUI(SinceEnchantments plugin, ItemStack weapon) {
        this.weapon = weapon;

        String titleRaw = plugin.getConfigFile().getString("gui.extractor.title", "&8Select Enchant to Extract");
        int size = plugin.getConfigFile().getInt("gui.extractor.size", 27);
        if (size % 9 != 0 || size < 9 || size > 54) size = 27;

        Component title = ColorUtils.parse(titleRaw);
        this.inventory = Bukkit.createInventory(this, size, title);

        populate(plugin);
    }

    private void populate(SinceEnchantments plugin) {
        Map<String, Integer> allEnchants = plugin.getEnchantManager().getAllEnchantsOnItem(weapon);

        int slot = 0;
        int maxSize = inventory.getSize();

        String matStr = plugin.getConfigFile().getString("gui.extractor.display-item.material", "ENCHANTED_BOOK");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.ENCHANTED_BOOK;

        String rawName = plugin.getConfigFile().getString("gui.extractor.display-item.name", "%rarity_color%%enchant_name% %level%");
        List<String> rawLore = plugin.getConfigFile().getStringList("gui.extractor.display-item.lore");

        for (Map.Entry<String, Integer> entry : allEnchants.entrySet()) {
            if (slot >= maxSize) break;

            String id = entry.getKey();
            int level = entry.getValue();

            ItemStack displayBook = new ItemStack(mat);
            ItemMeta meta = displayBook.getItemMeta();

            String name = plugin.getEnchantManager().getEnchantName(id);
            String rarity = plugin.getEnchantManager().getRarity(id);
            String color = plugin.getConfigFile().getString("rarities." + rarity, "&f");

            String parsedName = rawName.replace("%enchant_name%", name)
                    .replace("%level%", String.valueOf(level))
                    .replace("%rarity_name%", rarity)
                    .replace("%rarity_color%", color);
            meta.displayName(ColorUtils.parse(parsedName).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));

            List<Component> finalLore = new ArrayList<>();
            for (String line : rawLore) {
                String parsedLine = line.replace("%enchant_name%", name)
                        .replace("%level%", String.valueOf(level))
                        .replace("%rarity_name%", rarity)
                        .replace("%rarity_color%", color);
                finalLore.add(ColorUtils.parse(parsedLine).decoration(net.kyori.adventure.text.format.TextDecoration.ITALIC, false));
            }
            meta.lore(finalLore);

            meta.getPersistentDataContainer().set(plugin.getEnchantManager().BOOK_ID_KEY, PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(plugin.getEnchantManager().BOOK_LEVEL_KEY, PersistentDataType.INTEGER, level);

            displayBook.setItemMeta(meta);
            inventory.setItem(slot++, displayBook);
        }
    }

    public ItemStack getWeapon() {
        return weapon;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}