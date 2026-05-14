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
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ExtractorGUI implements InventoryHolder {
    private final Inventory inventory;
    private final ItemStack weapon;
    private final int page;
    private boolean isCompleted = false;

    public ExtractorGUI(SinceEnchantments plugin, ItemStack weapon, int page) {
        this.weapon = weapon;
        this.page = page;
        int size = plugin.getGuiFile().getInt("gui.extractor.size", 54);
        if (size % 9 != 0 || size < 27 || size > 54) size = 54;

        String titleRaw = plugin.getGuiFile().getString("gui.extractor.title", "&8Select Enchant (Page %page%)");
        titleRaw = titleRaw.replace("%page%", String.valueOf(page + 1));
        Component title = ColorUtils.parse(titleRaw);

        this.inventory = Bukkit.createInventory(this, size, title);
        populate(plugin, size);
    }

    private void populate(SinceEnchantments plugin, int size) {
        List<Integer> innerSlots = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            if (i > 8 && i < size - 9 && i % 9 != 0 && i % 9 != 8) innerSlots.add(i);
        }

        String borderMatStr = plugin.getGuiFile().getString("gui.extractor.border.material", "BLACK_STAINED_GLASS_PANE");
        Material borderMat = Material.matchMaterial(borderMatStr);
        if (borderMat == null) borderMat = Material.BLACK_STAINED_GLASS_PANE;

        ItemStack border = new ItemStack(borderMat);
        ItemMeta borderMeta = border.getItemMeta();
        borderMeta.displayName(ColorUtils.parse(plugin.getGuiFile().getString("gui.extractor.border.name", " ")));
        borderMeta.getPersistentDataContainer().set(plugin.getEnchantManager().GUI_ACTION_KEY, PersistentDataType.STRING, "BORDER");
        border.setItemMeta(borderMeta);

        for (int i = 0; i < size; i++) {
            if (!innerSlots.contains(i)) inventory.setItem(i, border);
        }

        Map<String, Integer> allEnchantsMap = plugin.getEnchantManager().getAllEnchantsOnItem(weapon);
        List<Map.Entry<String, Integer>> allEnchants = new ArrayList<>(allEnchantsMap.entrySet());

        int maxItems = innerSlots.size();
        int totalPages = (int) Math.ceil((double) allEnchants.size() / maxItems);
        if (totalPages == 0) totalPages = 1;

        int startIndex = page * maxItems;
        int endIndex = Math.min(startIndex + maxItems, allEnchants.size());

        int slotIndex = 0;
        String matStr = plugin.getGuiFile().getString("gui.extractor.display-item.material", "ENCHANTED_BOOK");
        Material mat = Material.matchMaterial(matStr);
        if (mat == null) mat = Material.ENCHANTED_BOOK;

        String rawName = plugin.getGuiFile().getString("gui.extractor.display-item.name", "%rarity_color%%enchant_name% %level%");
        List<String> rawLore = plugin.getGuiFile().getStringList("gui.extractor.display-item.lore");

        for (int i = startIndex; i < endIndex; i++) {
            Map.Entry<String, Integer> entry = allEnchants.get(i);
            String id = entry.getKey();
            int level = entry.getValue();

            ItemStack displayBook = new ItemStack(mat);
            ItemMeta meta = displayBook.getItemMeta();

            String name = plugin.getEnchantManager().getEnchantName(id);
            String rarity = plugin.getEnchantManager().getRarity(id);
            String color = plugin.getSettingsFile().getString("rarities." + rarity, "&f");

            String parsedName = rawName.replace("%enchant_name%", name).replace("%level%", String.valueOf(level)).replace("%rarity_name%", rarity).replace("%rarity_color%", color);
            meta.displayName(ColorUtils.parse(parsedName).decoration(TextDecoration.ITALIC, false));

            List<Component> finalLore = new ArrayList<>();
            for (String line : rawLore) {
                String parsedLine = line.replace("%enchant_name%", name).replace("%level%", String.valueOf(level)).replace("%rarity_name%", rarity).replace("%rarity_color%", color);
                finalLore.add(ColorUtils.parse(parsedLine).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(finalLore);
            meta.getPersistentDataContainer().set(plugin.getEnchantManager().BOOK_ID_KEY, PersistentDataType.STRING, id);
            meta.getPersistentDataContainer().set(plugin.getEnchantManager().BOOK_LEVEL_KEY, PersistentDataType.INTEGER, level);

            displayBook.setItemMeta(meta);
            inventory.setItem(innerSlots.get(slotIndex++), displayBook);
        }

        if (page > 0) {
            int prevSlot = plugin.getGuiFile().getInt("gui.extractor.prev-page.slot", size - 9);
            Material pMat = Material.matchMaterial(plugin.getGuiFile().getString("gui.extractor.prev-page.material", "ARROW"));
            if (pMat == null) pMat = Material.ARROW;
            ItemStack prev = new ItemStack(pMat);
            ItemMeta pMeta = prev.getItemMeta();
            pMeta.displayName(ColorUtils.parse(plugin.getGuiFile().getString("gui.extractor.prev-page.name", "&cPrevious Page")).decoration(TextDecoration.ITALIC, false));
            pMeta.getPersistentDataContainer().set(plugin.getEnchantManager().GUI_ACTION_KEY, PersistentDataType.STRING, "PREV_PAGE");
            prev.setItemMeta(pMeta);
            inventory.setItem(prevSlot, prev);
        }

        if (page < totalPages - 1) {
            int nextSlot = plugin.getGuiFile().getInt("gui.extractor.next-page.slot", size - 1);
            Material nMat = Material.matchMaterial(plugin.getGuiFile().getString("gui.extractor.next-page.material", "ARROW"));
            if (nMat == null) nMat = Material.ARROW;
            ItemStack next = new ItemStack(nMat);
            ItemMeta nMeta = next.getItemMeta();
            nMeta.displayName(ColorUtils.parse(plugin.getGuiFile().getString("gui.extractor.next-page.name", "&aNext Page")).decoration(TextDecoration.ITALIC, false));
            nMeta.getPersistentDataContainer().set(plugin.getEnchantManager().GUI_ACTION_KEY, PersistentDataType.STRING, "NEXT_PAGE");
            next.setItemMeta(nMeta);
            inventory.setItem(nextSlot, next);
        }
    }

    public ItemStack getWeapon() {
        return weapon;
    }

    public int getPage() {
        return page;
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
