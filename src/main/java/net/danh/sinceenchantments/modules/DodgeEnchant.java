package net.danh.sinceenchantments.modules;

import net.danh.sinceenchantments.api.SinceEnchant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.Random;

public class DodgeEnchant extends SinceEnchant {
    private final Random r = new Random();

    public DodgeEnchant() {
        super("since:dodge");
    }

    @EventHandler
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            int level = getLevel(p.getInventory().getChestplate());
            if (level > 0 && r.nextInt(100) < (level * getInt("chance-per-level", 5))) {
                event.setCancelled(true);
                sendMessage(p, "activate");
            }
        }
    }
}