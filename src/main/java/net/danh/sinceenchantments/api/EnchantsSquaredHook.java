package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnchantsSquaredHook {
    private final SinceEnchantments plugin;
    private final boolean hooked;
    private final NamespacedKey esKey;

    public EnchantsSquaredHook(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.esKey = new NamespacedKey("enchantssquared", "es_enchantments");
        this.hooked = Bukkit.getPluginManager().getPlugin("EnchantsSquared") != null;

        if (this.hooked) {
            plugin.getLogger().info("Successfully hooked into EnchantsSquared PDC format!");
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public void loadEnchantsSquared() {
        if (!hooked) {
            plugin.getLogger().info("Skipping EnchantsSquared auto-load because ES hook is not active.");
            return;
        }

        try {
            Class<?> managerClass = Class.forName("me.athlaeos.enchantssquared.managers.CustomEnchantManager");
            Method getInstanceMethod = managerClass.getMethod("getInstance");
            Object managerInstance = getInstanceMethod.invoke(null);

            Method getAllEnchantsMethod = managerClass.getMethod("getAllEnchants");
            Object biMap = getAllEnchantsMethod.invoke(managerInstance);

            Method valuesMethod = Map.class.getMethod("values");
            Collection<?> enchants = (Collection<?>) valuesMethod.invoke(biMap);

            Class<?> customEnchantClass = Class.forName("me.athlaeos.enchantssquared.enchantments.CustomEnchant");
            Method getIdMethod = customEnchantClass.getMethod("getId");
            Method getDisplayMethod = customEnchantClass.getMethod("getDisplayEnchantment");
            Method getMaxLevelMethod = customEnchantClass.getMethod("getMaxLevel");
            Method getDescriptionMethod = customEnchantClass.getMethod("getDescription");

            int count = 0;
            int skipped = 0;

            for (Object customEnchant : enchants) {
                try {
                    int id = (int) getIdMethod.invoke(customEnchant);
                    String displayName = (String) getDisplayMethod.invoke(customEnchant);
                    int maxLevel = (int) getMaxLevelMethod.invoke(customEnchant);
                    String description = (String) getDescriptionMethod.invoke(customEnchant);

                    String enchantId = "es:" + id;

                    if (plugin.getEnchantsFile().getConfig().contains("custom-enchants." + enchantId)) {
                        skipped++;
                        continue;
                    }

                    List<String> descList = new ArrayList<>();
                    if (description != null && !description.isEmpty()) {
                        descList.add(description);
                    }

                    plugin.getEnchantManager().registerDynamicEnchant(
                            enchantId,
                            displayName,
                            Math.max(1, maxLevel),
                            "EPIC",
                            "ALL",
                            descList
                    );
                    count++;
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to auto-register an ES enchantment: " + e.getMessage());
                }
            }

            plugin.getLogger().info("EnchantsSquared auto-load finished: registered " + count + ", skipped " + skipped + ".");
        } catch (Exception e) {
            plugin.getLogger().warning("Error while auto-loading enchantments from EnchantsSquared: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getEnchants(ItemStack item) {
        Map<String, Integer> enchants = new HashMap<>();
        if (!hooked || item == null || !item.hasItemMeta()) return enchants;

        PersistentDataContainer pdc = item.getItemMeta().getPersistentDataContainer();
        String rawData = pdc.get(esKey, PersistentDataType.STRING);

        if (rawData != null && !rawData.isEmpty()) {
            for (String pair : rawData.split(";")) {
                if (pair.isEmpty()) continue;
                String[] split = pair.split(":");
                if (split.length == 2) {
                    try {
                        enchants.put("es:" + split[0], Integer.parseInt(split[1]));
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        }
        if (plugin.getSettingsFile().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("[DEBUG] getEnchants returned: " + enchants.toString() + " from rawData: " + rawData);
        }
        return enchants;
    }

    public boolean applyEnchant(ItemStack item, String id, int level) {
        if (!hooked || item == null || !id.startsWith("es:")) return false;

        String internalId = id.substring(3); // Remove "es:" prefix
        ItemMeta meta = item.getItemMeta();
        
        Map<String, Integer> currentEnchants = getEnchants(item);
        currentEnchants.put("es:" + internalId, level);
        
        saveEnchants(meta, currentEnchants);
        item.setItemMeta(meta);
        if (plugin.getSettingsFile().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("[DEBUG] applyEnchant saved to PDC. New PDC string: " + meta.getPersistentDataContainer().get(esKey, PersistentDataType.STRING));
        }
        return true;
    }

    public boolean removeEnchant(ItemStack item, String id) {
        if (!hooked || item == null || !id.startsWith("es:")) return false;

        ItemMeta meta = item.getItemMeta();
        Map<String, Integer> currentEnchants = getEnchants(item);
        
        if (!currentEnchants.containsKey(id)) return false;
        currentEnchants.remove(id);
        
        saveEnchants(meta, currentEnchants);
        item.setItemMeta(meta);
        return true;
    }

    private void saveEnchants(ItemMeta meta, Map<String, Integer> enchants) {
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (enchants.isEmpty()) {
            pdc.remove(esKey);
            return;
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> entry : enchants.entrySet()) {
            if (!entry.getKey().startsWith("es:")) continue;
            String internalId = entry.getKey().substring(3);
            sb.append(internalId).append(":").append(entry.getValue()).append(";");
        }
        if (plugin.getSettingsFile().getBoolean("settings.debug", false)) {
            plugin.getLogger().info("[DEBUG] saveEnchants writing string: " + sb.toString());
        }
        pdc.set(esKey, PersistentDataType.STRING, sb.toString());
    }
}
