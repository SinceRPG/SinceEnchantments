package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

public abstract class SinceEnchant implements Listener {

    private final String id;

    public SinceEnchant(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * Hàm tiện ích giúp bạn kiểm tra xem vật phẩm này có enchant này không, và ở cấp độ mấy.
     */
    public int getLevel(ItemStack item) {
        return SinceEnchantments.getInstance().getEnchantManager().getCustomEnchants(item).getOrDefault(id, 0);
    }

    // Lấy thông tin từ Config
    public String getName() {
        return SinceEnchantments.getInstance().getEnchantManager().getEnchantName(id);
    }

    public int getMaxLevel() {
        return SinceEnchantments.getInstance().getEnchantManager().getMaxLevel(id);
    }

    public String getRarity() {
        return SinceEnchantments.getInstance().getEnchantManager().getRarity(id);
    }
}