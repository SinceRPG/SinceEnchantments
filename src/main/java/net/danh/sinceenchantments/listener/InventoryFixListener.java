package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * Cleans items of injected lore when moved within inventories.
 * Highly optimized to fast-fail and prevent updateInventory() lag spikes.
 */
public class InventoryFixListener implements Listener {
    private final EnchantManager manager;

    public InventoryFixListener(SinceEnchantments plugin) {
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            boolean update = false;

            // Only trigger an update if the item actually HAD injected lore stripped from it
            if (manager.cleanItemLore(event.getCurrentItem())) update = true;
            if (manager.cleanItemLore(event.getCursor())) update = true;

            // Specifically handle hotkey swapping (F key and Numbers 1-9)
            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                if (manager.cleanItemLore(p.getInventory().getItemInOffHand())) update = true;
            } else if (event.getClick() == ClickType.NUMBER_KEY) {
                if (manager.cleanItemLore(p.getInventory().getItem(event.getHotbarButton()))) update = true;
            }

            if (update) {
                Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            if (manager.cleanItemLore(event.getOldCursor())) {
                Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
            }
        }
    }
}