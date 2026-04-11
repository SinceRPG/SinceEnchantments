package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

public class StatTrackerListener implements Listener {

    private final EnchantManager manager;

    public StatTrackerListener(SinceEnchantments plugin) {
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        ItemStack item = p.getInventory().getItemInMainHand();
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer pdc = meta.getPersistentDataContainer();

        if (pdc.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) {
            int blocks = pdc.getOrDefault(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER, 0);
            pdc.set(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER, blocks + 1);
            item.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player p = event.getEntity().getKiller();
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item == null || !item.hasItemMeta()) return;

            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            if (pdc.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) {
                if (event.getEntity() instanceof Player) {
                    int players = pdc.getOrDefault(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER, 0);
                    pdc.set(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER, players + 1);
                } else {
                    int mobs = pdc.getOrDefault(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER, 0);
                    pdc.set(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER, mobs + 1);
                }
                item.setItemMeta(meta);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            Player p = event.getPlayer();
            ItemStack item = p.getInventory().getItemInMainHand();
            if (item == null || !item.hasItemMeta()) return;

            ItemMeta meta = item.getItemMeta();
            PersistentDataContainer pdc = meta.getPersistentDataContainer();

            if (pdc.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) {
                int fish = pdc.getOrDefault(manager.STAT_FISH_KEY, PersistentDataType.INTEGER, 0);
                pdc.set(manager.STAT_FISH_KEY, PersistentDataType.INTEGER, fish + 1);
                item.setItemMeta(meta);
            }
        }
    }
}