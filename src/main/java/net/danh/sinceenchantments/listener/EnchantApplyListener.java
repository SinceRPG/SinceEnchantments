package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Map;
import java.util.Random;

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

        // ===============================================
        // ACTION 1: APPLY SUCCESS CHARM TO ENCHANTMENT BOOK
        // ===============================================
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

            // Consume 1 Charm
            cursor.setAmount(cursor.getAmount() - 1);
            player.setItemOnCursor(cursor);

            String enchantId = currentMeta.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
            int level = currentMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
            int destroyRate = currentMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, 0);

            // Create updated book with new success rate
            ItemStack newBook = manager.createEnchantBook(enchantId, level, newSuccess, destroyRate);
            event.setCurrentItem(newBook);

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            sendMsg(player, "charm-apply-success", "%bonus%", String.valueOf(bonus));
            return;
        }

        // ===============================================
        // ACTION 2: APPLY ENCHANTMENT BOOK TO ITEM
        // ===============================================
        if (!cursorMeta.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) return;

        event.setCancelled(true);

        String enchantId = cursorMeta.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
        int enchantLevel = cursorMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);
        int successRate = cursorMeta.getPersistentDataContainer().getOrDefault(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, 100);

        if (!manager.isApplicable(enchantId, current.getType())) {
            sendMsg(player, "enchant-wrong-target");
            return;
        }

        if (manager.hasConflict(enchantId, current)) {
            sendMsg(player, "enchant-conflict");
            return;
        }

        if (!manager.isBukkitEnchant(enchantId)) {
            Map<String, Integer> currentCustomEnchants = manager.getCustomEnchants(current);
            int limit = plugin.getConfigFile().getInt("settings.max-custom-enchants-per-item", 5);

            if (!currentCustomEnchants.containsKey(enchantId) && currentCustomEnchants.size() >= limit) {
                sendMsg(player, "enchant-limit-reached", "%limit%", String.valueOf(limit));
                return;
            }
        }

        int currentLevel = manager.getEnchantLevel(current, enchantId);
        if (currentLevel >= manager.getMaxLevel(enchantId)) {
            sendMsg(player, "enchant-max-level");
            return;
        }

        // Always consume the book first
        cursor.setAmount(cursor.getAmount() - 1);
        player.setItemOnCursor(cursor);

        int roll = random.nextInt(100) + 1;
        if (roll <= successRate) {
            manager.addEnchant(current, enchantId, enchantLevel);
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
            sendMsg(player, "enchant-success");
        } else {
            // Failed: Book is already consumed. Target item is untouched.
            player.playSound(player.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, 1f, 1f);
            sendMsg(player, "enchant-fail");
        }
    }
}