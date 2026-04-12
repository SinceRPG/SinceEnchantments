package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.modules.SinceEnchant;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class EnchantRegistry {
    private final Map<String, SinceEnchant> registeredEnchants = new HashMap<>();
    private final SinceEnchantments plugin;

    public EnchantRegistry(SinceEnchantments plugin) {
        this.plugin = plugin;
    }

    public void register(SinceEnchant enchant) {
        registeredEnchants.put(enchant.getId(), enchant);
        Bukkit.getPluginManager().registerEvents(enchant, plugin);
        plugin.getLogger().info("Registered Custom Enchant: " + enchant.getId());
    }

    public SinceEnchant getEnchant(String id) {
        return registeredEnchants.get(id);
    }

    public Set<String> getRegisteredIds() {
        return registeredEnchants.keySet();
    }

    public Collection<SinceEnchant> getAllEnchants() {
        return registeredEnchants.values();
    }
}