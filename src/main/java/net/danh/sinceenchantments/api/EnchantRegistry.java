package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
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

    /**
     * Đăng ký một Custom Enchant mới vào hệ thống
     */
    public void register(SinceEnchant enchant) {
        registeredEnchants.put(enchant.getId(), enchant);
        // Tự động đăng ký Event Listener cho Enchant này luôn! Quá tiện lợi!
        Bukkit.getPluginManager().registerEvents(enchant, plugin);
        plugin.getLogger().info("Da dang ky Custom Enchant: " + enchant.getId());
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