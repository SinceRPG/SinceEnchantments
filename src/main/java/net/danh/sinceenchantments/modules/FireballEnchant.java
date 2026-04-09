package net.danh.sinceenchantments.modules;

import net.danh.sinceenchantments.api.SinceEnchant;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;
import java.util.UUID;

public class FireballEnchant extends SinceEnchant {
    private final HashMap<UUID, Long> cd = new HashMap<>();

    public FireballEnchant() {
        super("since:fireball");
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.LEFT_CLICK_AIR) return;
        Player p = event.getPlayer();
        int level = getLevel(p.getInventory().getItemInMainHand());

        if (level > 0) {
            long now = System.currentTimeMillis();
            long cooldown = getInt("cooldown", 5000);

            if (cd.containsKey(p.getUniqueId()) && cd.get(p.getUniqueId()) > now) {
                long remaining = (cd.get(p.getUniqueId()) - now) / 1000;
                sendMessage(p, "on-cooldown", "%time%", String.valueOf(remaining));
                return;
            }

            Fireball f = p.launchProjectile(Fireball.class);
            f.setYield((float) getDouble("explosion-power", 2.0));
            cd.put(p.getUniqueId(), now + cooldown);
            sendMessage(p, "launch");
        }
    }
}