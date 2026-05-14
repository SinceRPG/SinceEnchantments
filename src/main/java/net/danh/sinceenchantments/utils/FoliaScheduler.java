package net.danh.sinceenchantments.utils;

import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 * Routes scheduled work through Folia's entity scheduler when Folia is present.
 * Paper keeps the standard Bukkit scheduler path so behavior stays unchanged there.
 */
public final class FoliaScheduler {
    private FoliaScheduler() {
    }

    public static void run(SinceEnchantments plugin, Runnable task) {
        if (ServerVersion.isFolia()) {
            Bukkit.getGlobalRegionScheduler().run(plugin, scheduledTask -> task.run());
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runForPlayer(SinceEnchantments plugin, Player player, Runnable task) {
        if (ServerVersion.isFolia()) {
            player.getScheduler().run(plugin, scheduledTask -> task.run(), null);
            return;
        }
        Bukkit.getScheduler().runTask(plugin, task);
    }

    public static void runForPlayerLater(SinceEnchantments plugin, Player player, Runnable task, long delayTicks) {
        if (ServerVersion.isFolia()) {
            player.getScheduler().runDelayed(plugin, scheduledTask -> task.run(), null, delayTicks);
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, task, delayTicks);
    }
}
