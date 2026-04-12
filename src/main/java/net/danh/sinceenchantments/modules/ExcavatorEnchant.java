package net.danh.sinceenchantments.modules;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ExcavatorEnchant extends SinceEnchant {
    private final Set<UUID> activePlayers = new HashSet<>();

    public ExcavatorEnchant() {
        super("since:excavator");
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player p = event.getPlayer();
        if (activePlayers.contains(p.getUniqueId())) return;
        ItemStack item = p.getInventory().getItemInMainHand();
        int level = getLevel(item);

        if (level > 0) {
            activePlayers.add(p.getUniqueId());
            int radius = getInt("radius", 1);
            Block center = event.getBlock();
            int blocksBroken = 0;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block b = center.getRelative(x, y, z);
                        if (!b.getType().isAir() && b.getType().getHardness() >= 0 && !b.equals(center)) {
                            BlockBreakEvent breakEvent = new BlockBreakEvent(b, p);
                            Bukkit.getPluginManager().callEvent(breakEvent);
                            if (!breakEvent.isCancelled()) {
                                if (b.breakNaturally(item)) blocksBroken++;
                            }
                        }
                    }
                }
            }

            if (blocksBroken > 0 && item.getItemMeta() instanceof Damageable damageable) {
                int unbreakingLvl = item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.UNBREAKING);
                int effectiveDamage = 0;
                for (int i = 0; i < blocksBroken; i++) {
                    if (unbreakingLvl == 0 || (100.0 / (unbreakingLvl + 1)) > Math.random() * 100) {
                        effectiveDamage++;
                    }
                }
                if (effectiveDamage > 0) {
                    damageable.setDamage(damageable.getDamage() + effectiveDamage);
                    item.setItemMeta(damageable);
                    if (damageable.getDamage() >= item.getType().getMaxDurability()) {
                        p.getInventory().setItemInMainHand(null);
                        p.playSound(p.getLocation(), org.bukkit.Sound.ENTITY_ITEM_BREAK, 1f, 1f);
                    }
                }
            }
            activePlayers.remove(p.getUniqueId());
            sendMessage(p, "activate");
        }
    }
}