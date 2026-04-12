package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * Handles soft-depend hooking into MMOCore to prevent EXP desyncs.
 */
public class MMOCoreHook {

    private final SinceEnchantments plugin;
    private boolean hooked = false;

    private Method getPlayerDataMethod;
    private Method setLevelMethod;
    private Object unknownReason;

    public MMOCoreHook(SinceEnchantments plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().isPluginEnabled("MMOCore")) {
            try {
                Class<?> playerDataClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
                getPlayerDataMethod = playerDataClass.getMethod("get", UUID.class);

                Class<?> reasonEnumClass = Class.forName("net.Indyuce.mmocore.api.event.PlayerLevelChangeEvent$Reason");

                for (Object obj : reasonEnumClass.getEnumConstants()) {
                    if (obj.toString().equals("UNKNOWN")) {
                        unknownReason = obj;
                        break;
                    }
                }

                setLevelMethod = playerDataClass.getMethod("setLevel", int.class, reasonEnumClass);

                hooked = true;
                plugin.getLogger().info("Successfully hooked into MMOCore API (Latest Version) for Anvil EXP sync!");

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to hook into latest MMOCore API. Attempting legacy hook...");
                try {
                    Class<?> playerDataClass = Class.forName("net.Indyuce.mmocore.api.player.PlayerData");
                    getPlayerDataMethod = playerDataClass.getMethod("get", UUID.class);
                    setLevelMethod = playerDataClass.getMethod("setLevel", int.class);
                    hooked = true;
                    plugin.getLogger().info("Successfully hooked into MMOCore API (Legacy Version)!");
                } catch (Exception ex) {
                    plugin.getLogger().warning("Completely failed to hook into MMOCore. EXP might desync in Anvil.");
                }
            }
        }
    }

    public boolean isHooked() {
        return hooked;
    }


    public void syncLevelFromVanilla(Player player) {
        if (!hooked) return;
        try {
            Object playerData = getPlayerDataMethod.invoke(null, player.getUniqueId());
            int currentVanillaLevel = player.getLevel();

            if (setLevelMethod.getParameterCount() == 2) {
                if (unknownReason != null) {
                    setLevelMethod.invoke(playerData, currentVanillaLevel, unknownReason);
                }
            } else {
                setLevelMethod.invoke(playerData, currentVanillaLevel);
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to sync MMOCore level for " + player.getName());
            e.printStackTrace();
        }
    }
}