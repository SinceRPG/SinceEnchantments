package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class InventoryFixListener implements Listener {
    private final EnchantManager manager;

    public InventoryFixListener(SinceEnchantments plugin) {
        this.manager = plugin.getEnchantManager();
    }

    private boolean needsUpdate(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        ItemMeta meta = item.getItemMeta();

        if (meta.hasLore()) return true;

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (pdc.has(manager.BOOK_ID_KEY, PersistentDataType.STRING) || pdc.has(manager.ENCHANT_KEY, PersistentDataType.STRING))
            return true;

        for (NamespacedKey key : pdc.getKeys()) {
            if (key.getNamespace().equals("advancedenchantments")) return true;
        }

        if (pdc.has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) return true;
        if (pdc.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) return true;
        if (manager.isLocked(item)) return true;
        return !manager.getWhitelistedEnchants(item).isEmpty();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            boolean update = false;

            ItemStack current = event.getCurrentItem();
            if (needsUpdate(current)) {
                manager.cleanItemLore(current);
                update = true;
            }

            ItemStack cursor = event.getCursor();
            if (needsUpdate(cursor)) {
                manager.cleanItemLore(cursor);
                update = true;
            }

            if (update) {
                Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            boolean update = false;
            ItemStack cursor = event.getOldCursor();

            if (needsUpdate(cursor)) {
                manager.cleanItemLore(cursor);
                update = true;
            }

            if (update) {
                Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
            }
        }
    }
}