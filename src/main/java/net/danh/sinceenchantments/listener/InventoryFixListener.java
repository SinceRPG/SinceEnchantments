package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class InventoryFixListener implements Listener {

    private final EnchantManager manager;

    public InventoryFixListener(SinceEnchantments plugin) {
        this.manager = plugin.getEnchantManager();
    }

    private boolean needsUpdate(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return false;
        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();

        if (pdc.has(manager.BOOK_ID_KEY, PersistentDataType.STRING) || pdc.has(manager.ENCHANT_KEY, PersistentDataType.STRING))
            return true;
        if (pdc.has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) return true;
        if (pdc.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) return true;
        if (manager.isLocked(item)) return true;

        return !manager.getWhitelistedEnchants(item).isEmpty();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            ItemStack current = event.getCurrentItem();
            ItemStack cursor = event.getCursor();

            if (needsUpdate(current) || needsUpdate(cursor)) {
                Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            ItemStack cursor = event.getOldCursor();
            if (needsUpdate(cursor)) {
                Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
            }
        }
    }
}