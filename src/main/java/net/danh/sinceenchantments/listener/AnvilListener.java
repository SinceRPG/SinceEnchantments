package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.utils.ConfigUtils;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.view.AnvilView;
import org.bukkit.persistence.PersistentDataType;

public class AnvilListener implements Listener {

    private final SinceEnchantments plugin;
    private final EnchantManager manager;

    public AnvilListener(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        ItemStack slot1 = event.getInventory().getFirstItem();
        ItemStack slot2 = event.getInventory().getSecondItem();

        if (slot1 == null || slot2 == null) return;
        if (slot1.getType() == Material.AIR || slot2.getType() == Material.AIR) return;

        if (slot1.getAmount() > 1) return;

        ItemMeta meta2 = slot2.getItemMeta();
        if (meta2 == null || !meta2.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) {
            return;
        }

        String enchantId = meta2.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
        int bookLvl = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
        int successRate = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);
        int destroyRate = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);

        ConfigUtils config = plugin.getConfigFile();
        int costCombine = config.getInt("settings.anvil-xp-combine-books", 5);
        int costApply = config.getInt("settings.anvil-xp-apply-book", 3);

        if (!(event.getView() instanceof AnvilView anvilView)) return;

        anvilView.setMaximumRepairCost(999999999);

        if (slot1.hasItemMeta() && slot1.getItemMeta().getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) {
            String id1 = slot1.getItemMeta().getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
            int lvl1 = slot1.getItemMeta().getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);

            if (id1.equals(enchantId) && lvl1 == bookLvl) {
                int nextLvl = lvl1 + 1;
                if (nextLvl <= manager.getMaxLevel(enchantId)) {
                    ItemStack resultBook = manager.createEnchantBook(enchantId, nextLvl, successRate, destroyRate);
                    event.setResult(resultBook);
                    anvilView.setRepairCost(nextLvl * costCombine);
                    return;
                }
            }
            return;
        }

        if (manager.isLocked(slot1)) return;
        if (!manager.isApplicable(enchantId, slot1.getType())) return;
        if (!manager.isWhitelisted(slot1, enchantId)) return;
        if (!manager.getMissingRequirements(enchantId, slot1).isEmpty()) return;
        if (manager.hasConflict(enchantId, slot1)) return;

        int limit = manager.getMaxSlots(slot1);
        boolean isUpgrading = manager.getEnchantLevel(slot1, enchantId) > 0;

        if (!isUpgrading && manager.getAppliedEnchantsCount(slot1) >= limit) {
            return;
        }

        int currentLvl = manager.getEnchantLevel(slot1, enchantId);
        int newLvl = currentLvl;

        if (currentLvl == 0) {
            newLvl = bookLvl;
        } else if (currentLvl == bookLvl) {
            newLvl = currentLvl + 1;
        } else if (bookLvl > currentLvl) {
            newLvl = bookLvl;
        }

        if (newLvl > manager.getMaxLevel(enchantId)) newLvl = manager.getMaxLevel(enchantId);

        if (newLvl > currentLvl) {
            ItemStack resultItem = slot1.clone();
            manager.addEnchant(resultItem, enchantId, newLvl);
            event.setResult(resultItem);
            anvilView.setRepairCost(newLvl * costApply);
        }
    }
}