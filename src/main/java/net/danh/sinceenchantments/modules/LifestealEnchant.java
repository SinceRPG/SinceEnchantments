package net.danh.sinceenchantments.modules;

import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public class LifestealEnchant extends SinceEnchant {
    public LifestealEnchant() {
        super("since:lifesteal");
    }

    @EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
    public void onAttack(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Player p)) return;
        if (p.isDead()) return;
        if (!(event.getEntity() instanceof LivingEntity target) || target.isDead()) return;

        int level = getLevel(p.getInventory().getItemInMainHand());
        if (level > 0) {
            double multiplier = getDouble("heal-multiplier", 1.0);
            double heal = level * multiplier;
            AttributeInstance maxHealthAttr = p.getAttribute(Attribute.MAX_HEALTH);
            double maxHealth = (maxHealthAttr != null) ? maxHealthAttr.getValue() : 20.0;
            p.setHealth(Math.min(maxHealth, p.getHealth() + heal));
            sendMessage(p, "action", "%amount%", String.valueOf(heal));
        }
    }
}