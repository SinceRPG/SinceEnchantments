package net.danh.sinceenchantments.modules;

import net.danh.sinceenchantments.api.SinceEnchant;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;

public class ExcavatorEnchant extends SinceEnchant {
    private boolean processing = false;

    public ExcavatorEnchant() {
        super("since:excavator");
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        if (processing) return;
        int level = getLevel(event.getPlayer().getInventory().getItemInMainHand());
        if (level > 0) {
            processing = true;
            int radius = getInt("radius", 1);
            Block center = event.getBlock();
            for (int x = -radius; x <= radius; x++)
                for (int y = -radius; y <= radius; y++)
                    for (int z = -radius; z <= radius; z++) {
                        Block b = center.getRelative(x, y, z);
                        if (b.getType() != Material.AIR && b.getType().getHardness() > 0)
                            b.breakNaturally(event.getPlayer().getInventory().getItemInMainHand());
                    }
            processing = false;
            sendMessage(event.getPlayer(), "activate");
        }
    }
}