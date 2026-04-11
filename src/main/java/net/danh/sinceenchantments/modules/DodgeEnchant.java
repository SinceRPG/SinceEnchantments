package net.danh.sinceenchantments.modules;

import net.danh.sinceenchantments.api.SinceEnchant;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Random;

public class DodgeEnchant extends SinceEnchant {
    private final Random r = new Random();

    public DodgeEnchant() {
        super("since:dodge");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntity() instanceof Player p) {
            int totalLevel = 0;

            for (ItemStack armorItem : p.getInventory().getArmorContents()) {
                totalLevel += getLevel(armorItem);
            }

            if (totalLevel > 0 && r.nextInt(100) < (totalLevel * getInt("chance-per-level", 5))) {
                event.setCancelled(true);
                sendMessage(p, "activate");
            }
        }
    }
}