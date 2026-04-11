package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;

public class EnchantApplyListener implements Listener {

    private final SinceEnchantments plugin;
    private final EnchantManager manager;
    private final Random random = new Random();

    public EnchantApplyListener(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    private void sendMsg(Player p, String path, String... replacements) {
        String prefix = plugin.getMessagesFile().getString("prefix", "");
        String msg = plugin.getMessagesFile().getString(path, "");
        for (int i = 0; i < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        p.sendMessage(ColorUtils.parse(prefix + msg));
    }

    private void consumeCursor(Player player, ItemStack cursor) {
        if (cursor.getAmount() - 1 <= 0) {
            player.setItemOnCursor(null);
        } else {
            cursor.setAmount(cursor.getAmount() - 1);
            player.setItemOnCursor(cursor);
        }
    }

    @EventHandler
    public void onDragItem(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType() == Material.AIR) return;
        if (current == null || current.getType() == Material.AIR) return;

        ItemMeta cursorMeta = cursor.getItemMeta();
        ItemMeta currentMeta = current.getItemMeta();
        if (cursorMeta == null || currentMeta == null) return;

        // 1. SUCCESS CHARM
        if (cursorMeta.getPersistentDataContainer().has(manager.CHARM_BONUS_KEY, PersistentDataType.INTEGER) &&
                currentMeta.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) {
            event.setCancelled(true);
            if (current.getAmount() > 1) {
                sendMsg(player, "charm-need-unstack");
                return;
            }
            int currentSuccess = currentMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);
            if (currentSuccess >= 100) {
                sendMsg(player, "charm-max-reached");
                return;
            }
            int bonus = cursorMeta.getPersistentDataContainer().getOrDefault(manager.CHARM_BONUS_KEY, PersistentDataType.INTEGER, 1);
            int newSuccess = Math.min(100, currentSuccess + bonus);
            int currentDestroy = currentMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);
            int newDestroy = Math.max(0, currentDestroy - bonus);

            consumeCursor(player, cursor);

            String enchantId = currentMeta.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
            int level = currentMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);

            ItemStack newBook = manager.createEnchantBook(enchantId, level, newSuccess, newDestroy);
            event.setCurrentItem(newBook);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            sendMsg(player, "charm-apply-success", "%bonus%", String.valueOf(bonus));
            return;
        }

        // 2. SLOT GEM
        if (cursorMeta.getPersistentDataContainer().has(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER)) {
            event.setCancelled(true);
            if (current.getAmount() > 1) {
                sendMsg(player, "item-need-unstack");
                return;
            }
            if (manager.isLocked(current)) {
                sendMsg(player, "item-locked");
                return;
            }
            int gemModifier = cursorMeta.getPersistentDataContainer().get(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER);
            int currentModifier = currentMeta.getPersistentDataContainer().getOrDefault(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, 0);
            int maxAllowed = manager.getMaxSlotModifiersAllowed(current);

            if (gemModifier > 0 && currentModifier >= maxAllowed) {
                sendMsg(player, "slot-gem-max-reached", "%max%", String.valueOf(maxAllowed));
                return;
            }

            currentMeta.getPersistentDataContainer().set(manager.SLOT_MODIFIER_KEY, PersistentDataType.INTEGER, currentModifier + gemModifier);
            current.setItemMeta(currentMeta);

            consumeCursor(player, cursor);

            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_USE, 1f, 2f);
            sendMsg(player, "slot-gem-applied");
            return;
        }

        // 3. LOCK SCROLL
        if (cursorMeta.getPersistentDataContainer().has(manager.LOCK_SCROLL_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            if (current.getAmount() > 1) {
                sendMsg(player, "item-need-unstack");
                return;
            }
            manager.toggleLock(current);

            consumeCursor(player, cursor);

            player.playSound(player.getLocation(), Sound.BLOCK_CHEST_LOCKED, 1f, 1f);
            sendMsg(player, "item-lock-toggled");
            return;
        }

        // 4. PURGE SCROLL
        if (cursorMeta.getPersistentDataContainer().has(manager.PURGE_SCROLL_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            if (current.getAmount() > 1) {
                sendMsg(player, "item-need-unstack");
                return;
            }
            if (manager.isLocked(current)) {
                sendMsg(player, "item-locked");
                return;
            }
            boolean returnBooks = cursorMeta.getPersistentDataContainer().getOrDefault(manager.PURGE_RETURN_KEY, PersistentDataType.BYTE, (byte) 0) == 1;

            if (returnBooks) {
                Map<String, Integer> allEnchants = manager.getAllEnchantsOnItem(current);
                for (Map.Entry<String, Integer> entry : allEnchants.entrySet()) {
                    ItemStack book = manager.createEnchantBook(entry.getKey(), entry.getValue(), 100, 0);
                    if (!player.getInventory().addItem(book).isEmpty()) {
                        player.getWorld().dropItem(player.getLocation(), book);
                    }
                }
            }

            manager.setCustomEnchants(current, new HashMap<>());
            if (current.hasItemMeta() && current.getItemMeta().hasEnchants()) {
                ItemMeta m = current.getItemMeta();
                for (Enchantment e : new ArrayList<>(m.getEnchants().keySet())) {
                    m.removeEnchant(e);
                }
                current.setItemMeta(m);
            }

            consumeCursor(player, cursor);

            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
            sendMsg(player, "purge-applied");
            return;
        }

        // 5. RANDOMIZER STONE
        if (cursorMeta.getPersistentDataContainer().has(manager.RANDOMIZER_KEY, PersistentDataType.BYTE)) {
            if (!currentMeta.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) return;
            event.setCancelled(true);

            if (current.getAmount() > 1) {
                sendMsg(player, "item-need-unstack");
                return;
            }

            int newSuccess = random.nextInt(101);
            int newDestroy = random.nextInt(101);

            String enchantId = currentMeta.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
            int level = currentMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);

            ItemStack newBook = manager.createEnchantBook(enchantId, level, newSuccess, newDestroy);
            event.setCurrentItem(newBook);

            consumeCursor(player, cursor);

            player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            sendMsg(player, "randomizer-applied");
            return;
        }

        // 6. PROTECTION GEM
        if (cursorMeta.getPersistentDataContainer().has(manager.PROTECTOR_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            if (current.getAmount() > 1) {
                sendMsg(player, "item-need-unstack");
                return;
            }
            if (currentMeta.getPersistentDataContainer().has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) {
                sendMsg(player, "protector-already-applied");
                return;
            }

            currentMeta.getPersistentDataContainer().set(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            current.setItemMeta(currentMeta);

            consumeCursor(player, cursor);

            player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 1f, 1f);
            sendMsg(player, "protector-applied");
            return;
        }

        // 7. STAT TRACKER
        if (cursorMeta.getPersistentDataContainer().has(manager.TRACKER_KEY, PersistentDataType.BYTE)) {
            event.setCancelled(true);
            if (current.getAmount() > 1) {
                sendMsg(player, "item-need-unstack");
                return;
            }

            String matName = current.getType().name();
            boolean isApplicable = matName.endsWith("_SWORD") || matName.endsWith("_AXE") ||
                    matName.endsWith("_PICKAXE") || matName.endsWith("_SHOVEL") ||
                    matName.endsWith("_HOE") || matName.equals("BOW") ||
                    matName.equals("CROSSBOW") || matName.equals("TRIDENT") ||
                    matName.equals("MACE") || matName.equals("FISHING_ROD");

            if (!isApplicable) {
                sendMsg(player, "tracker-invalid-item");
                return;
            }

            if (currentMeta.getPersistentDataContainer().has(manager.TRACKER_KEY, PersistentDataType.BYTE)) {
                sendMsg(player, "tracker-already-applied");
                return;
            }

            currentMeta.getPersistentDataContainer().set(manager.TRACKER_KEY, PersistentDataType.BYTE, (byte) 1);
            current.setItemMeta(currentMeta);

            consumeCursor(player, cursor);

            player.playSound(player.getLocation(), Sound.BLOCK_SMITHING_TABLE_USE, 1f, 1f);
            sendMsg(player, "tracker-applied");
            return;
        }

        // 8. APPLY ENCHANTMENT BOOK
        if (!cursorMeta.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) return;
        event.setCancelled(true);

        if (current.getAmount() > 1) {
            sendMsg(player, "item-need-unstack");
            return;
        }

        if (manager.isLocked(current)) {
            sendMsg(player, "item-locked");
            return;
        }

        String enchantId = cursorMeta.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
        int enchantLevel = cursorMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
        int successRate = cursorMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);

        if (!manager.isApplicable(enchantId, current.getType())) {
            sendMsg(player, "enchant-wrong-target");
            return;
        }

        if (!manager.isWhitelisted(current, enchantId)) {
            sendMsg(player, "enchant-not-whitelisted");
            return;
        }

        List<String> missingReqs = manager.getMissingRequirements(enchantId, current);
        if (!missingReqs.isEmpty()) {
            sendMsg(player, "enchant-missing-requirements", "%requires%", String.join(", ", missingReqs));
            return;
        }

        if (manager.hasConflict(enchantId, current)) {
            sendMsg(player, "enchant-conflict");
            return;
        }

        int limit = manager.getMaxSlots(current);
        boolean isUpgrading = manager.getEnchantLevel(current, enchantId) > 0;

        if (!isUpgrading && manager.getAppliedEnchantsCount(current) >= limit) {
            sendMsg(player, "enchant-limit-reached", "%slots%", String.valueOf(limit));
            return;
        }

        int currentLevel = manager.getEnchantLevel(current, enchantId);
        int newLevel;

        if (currentLevel == 0) {
            newLevel = enchantLevel;
        } else if (enchantLevel > currentLevel) {
            newLevel = enchantLevel;
        } else if (currentLevel == enchantLevel) {
            newLevel = currentLevel + 1;
        } else {
            sendMsg(player, "enchant-lower-level");
            return;
        }

        if (currentLevel >= manager.getMaxLevel(enchantId) && newLevel > manager.getMaxLevel(enchantId)) {
            sendMsg(player, "enchant-max-level");
            return;
        }

        if (newLevel > manager.getMaxLevel(enchantId)) {
            newLevel = manager.getMaxLevel(enchantId);
        }

        consumeCursor(player, cursor);

        int roll = random.nextInt(100) + 1;
        if (roll <= successRate) {
            manager.addEnchant(current, enchantId, newLevel);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            sendMsg(player, "enchant-success");
        } else {
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
            sendMsg(player, "enchant-fail");
        }
    }
}