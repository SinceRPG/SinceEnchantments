package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.Bukkit;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class AdvancedEnchantmentsHook {
    private final SinceEnchantments plugin;
    private boolean hooked = false;
    private Method getAllEnchantmentsMethod;
    private Method getHighestEnchantmentLevelMethod;

    public AdvancedEnchantmentsHook(SinceEnchantments plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        if (Bukkit.getPluginManager().isPluginEnabled("AdvancedEnchantments")) {
            try {
                Class<?> aeApiClass = Class.forName("net.advancedplugins.ae.api.AEAPI");
                getAllEnchantmentsMethod = aeApiClass.getMethod("getAllEnchantments");
                getHighestEnchantmentLevelMethod = aeApiClass.getMethod("getHighestEnchantmentLevel", String.class);

                hooked = true;
                plugin.getLogger().info("Successfully hooked into AdvancedEnchantments API!");
            } catch (Exception e) {
                plugin.getLogger().warning("AdvancedEnchantments detected, but failed to hook into API.");
            }
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    @SuppressWarnings("unchecked")
    public void loadAEEnchantments() {
        if (!hooked || getAllEnchantmentsMethod == null) return;

        try {
            List<String> aeEnchants = (List<String>) getAllEnchantmentsMethod.invoke(null);
            int count = 0;

            List<String> defaultDescription = plugin.getSettingsFile().getStringList("settings.ae-default-description");
            if (defaultDescription == null || defaultDescription.isEmpty()) {
                defaultDescription = new ArrayList<>();
                defaultDescription.add("&7(AdvancedEnchantments effect)");
            }

            for (String aeName : aeEnchants) {
                String id = "ae:" + aeName.toLowerCase();

                if (plugin.getEnchantsFile().getConfig().contains("custom-enchants." + id)) {
                    continue;
                }

                int maxLevel = 3;
                if (getHighestEnchantmentLevelMethod != null) {
                    try {
                        maxLevel = (int) getHighestEnchantmentLevelMethod.invoke(null, aeName);
                    } catch (Exception ignored) {
                    }
                }

                String displayName = aeName.substring(0, 1).toUpperCase() + aeName.substring(1).toLowerCase();

                plugin.getEnchantManager().registerDynamicEnchant(
                        id,
                        displayName,
                        maxLevel,
                        "EPIC",
                        "ALL",
                        defaultDescription
                );
                count++;
            }

            if (count > 0) {
                plugin.getLogger().info("Auto-registered " + count + " missing enchantments from AdvancedEnchantments API!");
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error while auto-loading enchantments from AdvancedEnchantments!");
            e.printStackTrace();
        }
    }
}