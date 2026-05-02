package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

/**
 * INVENTORY FIX LISTENER
 *
 * Functionality:
 * This class monitors inventory interactions to prevent packet-injected visual lore
 * from being accidentally saved to the server's permanent item data. It intercepts
 * clicks and drags, stripping the fake lore before Bukkit processes the event.
 *
 * Lag Optimization Applied:
 * Removed manual `updateInventory()` calls. Bukkit naturally sends a highly-optimized
 * SET_SLOT packet for items modified during an InventoryClickEvent. Forcing a manual
 * inventory update caused massive WINDOW_ITEMS packet storms, severely dropping TPS
 * during rapid clicking. By directly modifying the event items, we maintain perfect
 * server sync with zero performance penalty.
 */
public class InventoryFixListener implements Listener {
    private final EnchantManager manager;

    public InventoryFixListener(SinceEnchantments plugin) {
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onClick(InventoryClickEvent event) {
        if (event.getWhoClicked() instanceof Player p) {

            // Stripping fake lore directly modifies the item reference in the event.
            // Bukkit will naturally sync this slot back to the client.
            manager.cleanItemLore(event.getCurrentItem());
            manager.cleanItemLore(event.getCursor());

            if (event.getClick() == ClickType.SWAP_OFFHAND) {
                manager.cleanItemLore(p.getInventory().getItemInOffHand());
            } else if (event.getClick() == ClickType.NUMBER_KEY) {
                manager.cleanItemLore(p.getInventory().getItem(event.getHotbarButton()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDrag(InventoryDragEvent event) {
        if (event.getWhoClicked() instanceof Player p) {
            manager.cleanItemLore(event.getOldCursor());
        }
    }
}