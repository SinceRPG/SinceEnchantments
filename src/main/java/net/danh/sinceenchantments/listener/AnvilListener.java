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
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;

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

        if (!(event.getView() instanceof AnvilView anvilView)) return;
        anvilView.setMaximumRepairCost(999999999);

        ConfigUtils config = plugin.getSettingsFile();
        int costCombine = config.getInt("settings.anvil-xp-combine-books", 5);
        int costApply = config.getInt("settings.anvil-xp-apply-book", 3);

        ItemMeta meta2 = slot2.getItemMeta();
        boolean isBook2 = meta2 != null && meta2.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING);

        if (isBook2) {
            if (slot1.hasItemMeta() && slot1.getItemMeta().getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) {
                String enchantId = meta2.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
                int bookLvl = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
                int successRate2 = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);
                int destroyRate2 = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);

                String id1 = slot1.getItemMeta().getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
                int lvl1 = slot1.getItemMeta().getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
                int successRate1 = slot1.getItemMeta().getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);
                int destroyRate1 = slot1.getItemMeta().getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);

                if (id1.equals(enchantId) && lvl1 == bookLvl) {
                    int nextLvl = lvl1 + 1;
                    if (nextLvl <= manager.getMaxLevel(enchantId)) {
                        int avgSuccess = (successRate1 + successRate2) / 2;
                        int avgDestroy = (destroyRate1 + destroyRate2) / 2;

                        ItemStack resultBook = manager.createEnchantBook(enchantId, nextLvl, avgSuccess, avgDestroy);
                        event.setResult(resultBook);
                        anvilView.setRepairCost(nextLvl * costCombine);
                    }
                }
            } else {
                event.setResult(null);
            }
            return;
        }

        ItemStack result = event.getResult();
        boolean vanillaHasNoResult = (result == null || result.getType() == Material.AIR);

        if (vanillaHasNoResult) {
            if (slot1.getType() == slot2.getType() && slot1.getType() != Material.ENCHANTED_BOOK) {
                result = slot1.clone();
            } else {
                return;
            }
        } else {
            result = result.clone();
        }

        Map<String, Integer> enchants1 = manager.getCustomEnchants(slot1);
        Map<String, Integer> enchants2 = manager.getCustomEnchants(slot2);

        Map<String, Integer> merged = new HashMap<>(enchants1);
        int costIncrease = 0;
        boolean customMerged = false;

        int vanillaCount = 0;
        if (config.getBoolean("settings.override-vanilla-enchants", true) && result.hasItemMeta() && result.getItemMeta().hasEnchants()) {
            vanillaCount = result.getItemMeta().getEnchants().size();
        }

        for (Map.Entry<String, Integer> e2 : enchants2.entrySet()) {
            String id = e2.getKey();
            int lvl2 = e2.getValue();
            int lvl1 = merged.getOrDefault(id, 0);

            if (lvl1 == 0) {
                manager.setCustomEnchants(result, merged);
                if (!manager.isApplicable(id, result.getType())) continue;
                if (!manager.isWhitelisted(result, id)) continue;
                if (manager.hasConflict(id, result)) continue;
                if (!manager.getMissingRequirements(id, result).isEmpty()) continue;

                if ((merged.size() + vanillaCount) < manager.getMaxSlots(result)) {
                    merged.put(id, lvl2);
                    costIncrease += lvl2 * costApply;
                    customMerged = true;
                }
            } else if (lvl1 == lvl2) {
                int nextLvl = lvl1 + 1;
                if (nextLvl <= manager.getMaxLevel(id)) {
                    merged.put(id, nextLvl);
                    costIncrease += nextLvl * costCombine;
                    customMerged = true;
                }
            } else if (lvl2 > lvl1) {
                merged.put(id, lvl2);
                costIncrease += (lvl2 - lvl1) * costApply;
                customMerged = true;
            }
        }

        if (vanillaHasNoResult && !customMerged) {
            event.setResult(null);
            return;
        }

        manager.setCustomEnchants(result, merged);

        ItemMeta rMeta = result.getItemMeta();
        PersistentDataContainer rPdc = rMeta.getPersistentDataContainer();

        PersistentDataContainer pdc1 = slot1.hasItemMeta() ? slot1.getItemMeta().getPersistentDataContainer() : null;
        PersistentDataContainer pdc2 = slot2.hasItemMeta() ? slot2.getItemMeta().getPersistentDataContainer() : null;

        if (pdc1 != null && pdc1.has(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER)) {
            rPdc.set(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, pdc1.get(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER));
        } else if (pdc2 != null && pdc2.has(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER)) {
            rPdc.set(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, pdc2.get(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER));
        }

        if (pdc1 != null && manager.isLocked(slot1)) {
            rPdc.set(manager.LOCKED_KEY, PersistentDataType.BYTE, (byte) 1);
        } else if (pdc2 != null && manager.isLocked(slot2)) {
            rPdc.set(manager.LOCKED_KEY, PersistentDataType.BYTE, (byte) 1);
        }

        if (pdc1 != null && pdc1.has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) {
            rPdc.set(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE, pdc1.get(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE));
        } else if (pdc2 != null && pdc2.has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) {
            rPdc.set(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE, pdc2.get(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE));
        }

        if (pdc1 != null && pdc1.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) {
            rPdc.set(manager.TRACKER_KEY, PersistentDataType.BYTE, pdc1.get(manager.TRACKER_KEY, PersistentDataType.BYTE));
            if (pdc1.has(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER, pdc1.get(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER));
            if (pdc1.has(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER, pdc1.get(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER));
            if (pdc1.has(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER, pdc1.get(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER));
            if (pdc1.has(manager.STAT_FISH_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_FISH_KEY, PersistentDataType.INTEGER, pdc1.get(manager.STAT_FISH_KEY, PersistentDataType.INTEGER));
        } else if (pdc2 != null && pdc2.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) {
            rPdc.set(manager.TRACKER_KEY, PersistentDataType.BYTE, pdc2.get(manager.TRACKER_KEY, PersistentDataType.BYTE));
            if (pdc2.has(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER, pdc2.get(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER));
            if (pdc2.has(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER, pdc2.get(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER));
            if (pdc2.has(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER, pdc2.get(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER));
            if (pdc2.has(manager.STAT_FISH_KEY, PersistentDataType.INTEGER)) rPdc.set(manager.STAT_FISH_KEY, PersistentDataType.INTEGER, pdc2.get(manager.STAT_FISH_KEY, PersistentDataType.INTEGER));
        }

        result.setItemMeta(rMeta);
        event.setResult(result);

        int finalCost = anvilView.getRepairCost() + costIncrease;
        if (vanillaHasNoResult) finalCost += 1;
        anvilView.setRepairCost(finalCost);
    }
}