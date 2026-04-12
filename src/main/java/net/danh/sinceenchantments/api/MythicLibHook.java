package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Method;

public class MythicLibHook {
    private final SinceEnchantments plugin;
    private boolean hooked = false;
    private Method nbtItemGetMethod;
    private Method nbtItemHasTypeMethod;
    private Method nbtItemGetTypeMethod;
    private Method nbtItemGetStringMethod;

    public MythicLibHook(SinceEnchantments plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().isPluginEnabled("MythicLib") || Bukkit.getPluginManager().isPluginEnabled("MMOItems")) {
            try {
                Class<?> nbtItemClass = Class.forName("io.lumine.mythic.lib.api.item.NBTItem");
                nbtItemGetMethod = nbtItemClass.getMethod("get", ItemStack.class);
                nbtItemHasTypeMethod = nbtItemClass.getMethod("hasType");
                nbtItemGetTypeMethod = nbtItemClass.getMethod("getType");
                nbtItemGetStringMethod = nbtItemClass.getMethod("getString", String.class);
                hooked = true;
                plugin.getLogger().info("Successfully hooked into MythicLib/MMOItems NBT API!");
            } catch (Exception e) {
                plugin.getLogger().warning("MythicLib or MMOItems detected, but failed to hook into API. Falling back to Vanilla/PDC.");
            }
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public String getMMOItemKey(ItemStack item) {
        if (!hooked || item == null || item.getType().isAir()) return null;
        try {
            Object nbtItem = nbtItemGetMethod.invoke(null, item);
            if ((boolean) nbtItemHasTypeMethod.invoke(nbtItem)) {
                String type = (String) nbtItemGetTypeMethod.invoke(nbtItem);
                String id = (String) nbtItemGetStringMethod.invoke(nbtItem, "MMOITEMS_ITEM_ID");
                if (type != null && id != null && !type.isEmpty() && !id.isEmpty()) {
                    return (type + ":" + id).toUpperCase();
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}