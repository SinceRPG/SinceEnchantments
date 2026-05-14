package net.danh.sinceenchantments.modules;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class ThunderEnchant extends SinceEnchant {
    private final Random r = new Random();
    private final Set<UUID> isStriking = new HashSet<>();

    public ThunderEnchant() {
        super("since:thunder");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onHit(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player p && event.getEntity() instanceof LivingEntity target) {
            if (isStriking.contains(p.getUniqueId())) return;
            int level = getLevel(p.getInventory().getItemInMainHand());
            if (level > 0 && r.nextInt(100) < (level * getInt("chance-per-level", 8))) {
                isStriking.add(p.getUniqueId());
                try {
                    target.getWorld().strikeLightningEffect(target.getLocation());
                    target.damage(getDouble("extra-damage", 3.0), p);
                    sendMessage(p, "activate");
                } finally {
                    isStriking.remove(p.getUniqueId());
                }
            }
        }
    }
}
