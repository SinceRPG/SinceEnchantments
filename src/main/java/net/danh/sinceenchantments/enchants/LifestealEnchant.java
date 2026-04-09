package net.danh.sinceenchantments.enchants;

import net.danh.sinceenchantments.api.SinceEnchant;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

public class LifestealEnchant extends SinceEnchant {

    public LifestealEnchant() {
        super("since:lifesteal");
    }

    @EventHandler
    public void onAttack(EntityDamageByEntityEvent event) {
        // Condition: Kẻ tấn công phải là người chơi
        if (!(event.getDamager() instanceof Player p)) return;

        ItemStack weapon = p.getInventory().getItemInMainHand();

        // Lấy cấp độ của enchant since:lifesteal trên vũ khí này
        int level = getLevel(weapon);

        if (level > 0) {
            // Logic Hút Máu: Mỗi cấp độ hồi 1 Máu (0.5 Tim)
            double maxHealth = p.getAttribute(Attribute.MAX_HEALTH).getValue();
            double healAmount = level * 1.0;
            p.setHealth(Math.min(maxHealth, p.getHealth() + healAmount));
        }
    }
}