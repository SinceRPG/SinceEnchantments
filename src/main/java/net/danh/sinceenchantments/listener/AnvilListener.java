package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class AnvilListener implements Listener {

    private final EnchantManager manager;

    public AnvilListener(SinceEnchantments plugin) {
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack slot1 = event.getInventory().getItem(0); // Vật phẩm chính
        ItemStack slot2 = event.getInventory().getItem(1); // Sách ép / Vật phẩm phụ

        if (slot1 == null || slot2 == null) return;
        if (slot1.getType() == Material.AIR || slot2.getType() == Material.AIR) return;

        ItemMeta meta2 = slot2.getItemMeta();
        if (meta2 == null || !meta2.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) {
            return; // Nếu ô 2 không phải sách custom thì kệ vanilla xử lý
        }

        String enchantId = meta2.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
        int bookLvl = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
        int successRate = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);
        int destroyRate = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);

        // TRƯỜNG HỢP 1: SÁCH + SÁCH = SÁCH NÂNG CẤP
        if (slot1.hasItemMeta() && slot1.getItemMeta().getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) {
            String id1 = slot1.getItemMeta().getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
            int lvl1 = slot1.getItemMeta().getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);

            // Phải cùng loại enchant và cùng cấp độ mới được cộng dồn (Giống Vanilla)
            if (id1.equals(enchantId) && lvl1 == bookLvl) {
                int nextLvl = lvl1 + 1;
                if (nextLvl <= manager.getMaxLevel(enchantId)) {
                    ItemStack resultBook = manager.createEnchantBook(enchantId, nextLvl, successRate, destroyRate);
                    event.setResult(resultBook);
                    event.getInventory().setRepairCost(nextLvl * 5); // Tốn XP Level tương ứng
                    return;
                }
            }
            return;
        }

        // TRƯỜNG HỢP 2: VŨ KHÍ + SÁCH
        if (!manager.isApplicable(enchantId, slot1.getType())) return; // Sai loại
        if (manager.hasConflict(enchantId, slot1)) return; // Khắc hệ

        int currentLvl = manager.getCustomEnchants(slot1).getOrDefault(enchantId, 0);
        int newLvl = currentLvl;

        if (currentLvl == 0) {
            newLvl = bookLvl; // Ép mới
        } else if (currentLvl == bookLvl) {
            newLvl = currentLvl + 1; // Nâng cấp (Ghép 2 cái cùng cấp)
        } else if (bookLvl > currentLvl) {
            newLvl = bookLvl; // Lấy cấp cao hơn
        }

        if (newLvl > manager.getMaxLevel(enchantId)) newLvl = manager.getMaxLevel(enchantId);

        if (newLvl > currentLvl) {
            ItemStack resultItem = slot1.clone();
            manager.addEnchant(resultItem, enchantId, newLvl);
            event.setResult(resultItem);
            event.getInventory().setRepairCost(newLvl * 3); // Tốn XP
        }
    }
}