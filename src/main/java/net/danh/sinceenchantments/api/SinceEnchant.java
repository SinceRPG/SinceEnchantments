package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

/**
 * Base abstract class for all custom enchantments.
 * Extend this class to create a new custom enchantment module.
 */
public abstract class SinceEnchant implements Listener {

    protected final String id;
    protected final String configPath;

    /**
     * @param id The NamespacedKey-like identifier (e.g., "since:lifesteal").
     */
    public SinceEnchant(String id) {
        this.id = id;
        this.configPath = "custom-enchants." + id;
    }

    public String getId() {
        return id;
    }

    protected int getInt(String key, int def) {
        return SinceEnchantments.getInstance().getConfigFile().getInt(configPath + ".settings." + key, def);
    }

    protected double getDouble(String key, double def) {
        return SinceEnchantments.getInstance().getConfigFile().getDouble(configPath + ".settings." + key, def);
    }

    protected String getString(String key, String def) {
        return SinceEnchantments.getInstance().getConfigFile().getString(configPath + ".settings." + key, def);
    }

    protected boolean getBoolean(String key, boolean def) {
        return SinceEnchantments.getInstance().getConfigFile().getBoolean(configPath + ".settings." + key, def);
    }

    /**
     * Sends a formatted message to the player from the enchant's message config.
     *
     * @param p            The target player.
     * @param messageKey   The configuration key under 'messages'.
     * @param replacements Alternate key-value pairs for placeholders (e.g., "%amount%", "5").
     */
    protected void sendMessage(Player p, String messageKey, String... replacements) {
        String rawMsg = SinceEnchantments.getInstance().getConfigFile().getString(configPath + ".messages." + messageKey);
        if (rawMsg == null || rawMsg.isEmpty()) return;

        for (int i = 0; i < replacements.length; i += 2) {
            rawMsg = rawMsg.replace(replacements[i], replacements[i + 1]);
        }

        String prefix = SinceEnchantments.getInstance().getMessagesFile().getString("prefix", "");
        p.sendMessage(ColorUtils.parse(prefix + rawMsg));
    }

    /**
     * Retrieves the level of this enchantment on the given item.
     *
     * @param item The ItemStack to check.
     * @return The level of the enchant, or 0 if not present.
     */
    public int getLevel(ItemStack item) {
        return SinceEnchantments.getInstance().getEnchantManager().getCustomEnchants(item).getOrDefault(id, 0);
    }

    public String getName() {
        return SinceEnchantments.getInstance().getEnchantManager().getEnchantName(id);
    }

    public int getMaxLevel() {
        return SinceEnchantments.getInstance().getEnchantManager().getMaxLevel(id);
    }

    public String getRarity() {
        return SinceEnchantments.getInstance().getEnchantManager().getRarity(id);
    }

    public boolean isApplicable(Material material) {
        return SinceEnchantments.getInstance().getEnchantManager().isApplicable(id, material);
    }

    public boolean hasConflict(ItemStack item) {
        return SinceEnchantments.getInstance().getEnchantManager().hasConflict(id, item);
    }
}