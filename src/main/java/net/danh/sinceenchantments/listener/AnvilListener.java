package net.danh.sinceenchantments.listener;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.api.ItemFactory;
import net.danh.sinceenchantments.utils.ConfigUtils;
import net.danh.sinceenchantments.utils.FoliaScheduler;
import net.danh.sinceenchantments.utils.ServerVersion;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.inventory.AnvilInventory;
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
    private final ItemFactory itemFactory;

    public AnvilListener(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
        this.itemFactory = plugin.getItemFactory();
    }

    private Enchantment getBukkitEnchantment(String id) {
        NamespacedKey key = NamespacedKey.fromString(id.toLowerCase());
        if (key == null) return null;

        if (ServerVersion.isAtLeast(1, 21, 0)) {
            return RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(key);
        } else {
            return Enchantment.getByKey(key);
        }
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

        if (isBook2 && slot1.hasItemMeta() && slot1.getItemMeta().getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) {
            String id2 = meta2.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
            int lvl2 = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
            int success2 = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);
            int destroy2 = meta2.getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);

            ItemMeta meta1 = slot1.getItemMeta();
            String id1 = meta1.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
            int lvl1 = meta1.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
            int success1 = meta1.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);
            int destroy1 = meta1.getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);

            if (id1 != null && id1.equals(id2) && lvl1 == lvl2) {
                int nextLvl = lvl1 + 1;
                if (nextLvl <= manager.getMaxLevel(id1)) {
                    int avgSuccess = (success1 + success2) / 2;
                    int avgDestroy = (destroy1 + destroy2) / 2;
                    ItemStack resultBook = itemFactory.createEnchantBook(id1, nextLvl, avgSuccess, avgDestroy);
                    event.setResult(resultBook);
                    anvilView.setRepairCost(nextLvl * costCombine);
                    return;
                }
            }
            event.setResult(null);
            return;
        }

        ItemStack result;
        if (event.getResult() != null && event.getResult().getType() != Material.AIR) {
            result = event.getResult().clone();
        } else {
            if (slot1.getType() == slot2.getType() || slot2.getType() == Material.ENCHANTED_BOOK) {
                result = slot1.clone();
            } else return;
        }

        Map<String, Integer> allEnchants1 = manager.getAllEnchantsOnItem(slot1);
        Map<String, Integer> allEnchants2 = manager.getAllEnchantsOnItem(slot2);

        Map<String, Integer> merged = new HashMap<>(allEnchants1);
        int costIncrease = 0;
        boolean modified = false;

        for (Map.Entry<String, Integer> entry2 : allEnchants2.entrySet()) {
            String id = entry2.getKey();
            int lvl2 = entry2.getValue();
            int lvl1 = merged.getOrDefault(id, 0);
            int maxLvl = manager.getMaxLevel(id);

            int finalLvl = 0;
            if (lvl1 == 0) {
                if (!manager.isApplicable(id, result.getType())) continue;
                if (!manager.isWhitelisted(result, id)) continue;
                if (manager.hasConflict(id, result)) continue;
                if (!manager.getMissingRequirements(id, result).isEmpty()) continue;
                if (manager.getAppliedEnchantsCount(result) >= manager.getMaxSlots(result)) continue;

                finalLvl = Math.min(lvl2, maxLvl);
                costIncrease += finalLvl * costApply;
            } else if (lvl1 == lvl2) {
                finalLvl = Math.min(maxLvl, lvl1 + 1);
                if (finalLvl > lvl1) costIncrease += finalLvl * costCombine;
            } else if (lvl2 > lvl1) {
                finalLvl = Math.min(lvl2, maxLvl);
                costIncrease += (finalLvl - lvl1) * costApply;
            }

            if (finalLvl > 0 && finalLvl != lvl1) {
                merged.put(id, finalLvl);
                modified = true;
            }
        }

        if (!modified && (event.getResult() == null || event.getResult().getType() == Material.AIR)) return;

        ItemMeta rMeta = result.getItemMeta();
        if (rMeta == null) return;

        Map<String, Integer> customToApply = new HashMap<>();

        for (Map.Entry<String, Integer> e : merged.entrySet()) {
            String id = e.getKey();
            int level = e.getValue();

            if (manager.isBukkitEnchant(id)) {
                Enchantment bEnc = getBukkitEnchantment(id);
                if (bEnc != null) rMeta.addEnchant(bEnc, level, true);
            } else {
                customToApply.put(id, level);
            }
        }

        result.setItemMeta(rMeta);
        manager.setCustomEnchants(result, customToApply);

        PersistentDataContainer pdc1 = slot1.hasItemMeta() ? slot1.getItemMeta().getPersistentDataContainer() : null;
        PersistentDataContainer pdc2 = slot2.hasItemMeta() ? slot2.getItemMeta().getPersistentDataContainer() : null;

        if (pdc1 != null || pdc2 != null) {
            ItemMeta finalMeta = result.getItemMeta();
            PersistentDataContainer finalPdc = finalMeta.getPersistentDataContainer();

            int mod1 = pdc1 != null ? pdc1.getOrDefault(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, 0) : 0;
            int mod2 = pdc2 != null ? pdc2.getOrDefault(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, 0) : 0;
            if (mod1 + mod2 != 0) finalPdc.set(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, mod1 + mod2);

            if (manager.isLocked(slot1) || manager.isLocked(slot2))
                finalPdc.set(manager.LOCKED_KEY, PersistentDataType.BYTE, (byte) 1);

            boolean prot1 = pdc1 != null && pdc1.has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE);
            boolean prot2 = pdc2 != null && pdc2.has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE);
            if (prot1 || prot2) finalPdc.set(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);

            if ((pdc1 != null && pdc1.has(manager.TRACKER_KEY, PersistentDataType.BYTE)) || (pdc2 != null && pdc2.has(manager.TRACKER_KEY, PersistentDataType.BYTE))) {
                finalPdc.set(manager.TRACKER_KEY, PersistentDataType.BYTE, (byte) 1);
                transferStat(pdc1, pdc2, finalPdc, manager.STAT_BLOCKS_KEY);
                transferStat(pdc1, pdc2, finalPdc, manager.STAT_MOBS_KEY);
                transferStat(pdc1, pdc2, finalPdc, manager.STAT_PLAYERS_KEY);
                transferStat(pdc1, pdc2, finalPdc, manager.STAT_FISH_KEY);
            }
            result.setItemMeta(finalMeta);
        }

        event.setResult(result);

        int vanillaCost = anvilView.getRepairCost();
        if (event.getResult() == null || event.getResult().getType() == Material.AIR) vanillaCost = 1;

        anvilView.setRepairCost(vanillaCost + costIncrease);
    }

    private void transferStat(PersistentDataContainer p1, PersistentDataContainer p2, PersistentDataContainer target, NamespacedKey key) {
        int v1 = p1 != null ? p1.getOrDefault(key, PersistentDataType.INTEGER, 0) : 0;
        int v2 = p2 != null ? p2.getOrDefault(key, PersistentDataType.INTEGER, 0) : 0;
        if (v1 + v2 > 0) target.set(key, PersistentDataType.INTEGER, v1 + v2);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAnvilTakeResult(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (!(event.getInventory() instanceof AnvilInventory anvil)) return;
        if (event.getRawSlot() != 2) return;

        ItemStack result = event.getCurrentItem();
        if (result == null || result.getType() == Material.AIR) return;

        boolean isOurOperation = result.hasItemMeta() && (
                result.getItemMeta().getPersistentDataContainer().has(manager.ENCHANT_KEY, PersistentDataType.STRING) ||
                        result.getItemMeta().getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)
        );

        if (!isOurOperation) return;

        if (plugin.getMMOCoreHook().isHooked()) {
            FoliaScheduler.runForPlayerLater(plugin, p, () -> {
                if (p.isOnline()) plugin.getMMOCoreHook().syncLevelFromVanilla(p);
            }, 1L);
        }
    }
}
