package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class InventoryFixListener implements Listener {

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            Bukkit.getScheduler().runTaskLater(SinceEnchantments.getInstance(), p::updateInventory, 1L);
        }
    }
}