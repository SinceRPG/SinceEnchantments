package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.api.ItemFactory;
import net.danh.sinceenchantments.gui.ExtractorDialog;
import net.danh.sinceenchantments.gui.ExtractorGUI;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.FoliaScheduler;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * EXTRACTOR LISTENER
 * <p>
 * Functionality:
 * Handles interactions regarding the Extractor item. Listens for drag-and-drop actions
 * on items to extract custom enchantments and manages the legacy chest GUI interactions.
 * <p>
 * Optimization Applied:
 * Added state validation (gui.isCompleted()) to prevent click-spamming in the GUI.
 * This immediately returns the function, preventing logic overlap, server bottlenecking,
 * and potential item duplication.
 */
public class ExtractorListener implements Listener {
    private final SinceEnchantments plugin;
    private final EnchantManager manager;
    private final ItemFactory itemFactory;
    private final Random random = new Random();

    public ExtractorListener(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
        this.itemFactory = plugin.getItemFactory();
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
        if (cursor.getAmount() - 1 <= 0) player.setItemOnCursor(null);
        else {
            cursor.setAmount(cursor.getAmount() - 1);
            player.setItemOnCursor(cursor);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onDragExtractor(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player p)) return;

        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor == null || cursor.getType() == Material.AIR) return;
        if (current == null || current.getType() == Material.AIR) return;

        ItemMeta cursorMeta = cursor.getItemMeta();
        if (cursorMeta == null || !cursorMeta.getPersistentDataContainer().has(manager.EXTRACTOR_TYPE_KEY, PersistentDataType.STRING))
            return;

        String extractorType = cursorMeta.getPersistentDataContainer().get(manager.EXTRACTOR_TYPE_KEY, PersistentDataType.STRING);
        if (extractorType == null) return;

        event.setCancelled(true);

        if (current.getAmount() > 1) {
            sendMsg(p, "item-need-unstack");
            return;
        }
        if (manager.isLocked(current)) {
            sendMsg(p, "item-locked");
            return;
        }

        Map<String, Integer> allEnchants = manager.getAllEnchantsOnItem(current);
        if (allEnchants.isEmpty()) {
            sendMsg(p, "extract-no-enchants");
            return;
        }

        if (extractorType.equals("RANDOM")) {
            consumeCursor(p, cursor);
            List<String> keys = new ArrayList<>(allEnchants.keySet());
            String removedId = keys.get(random.nextInt(keys.size()));
            int removedLevel = allEnchants.get(removedId);

            manager.removeEnchant(current, removedId);
            ItemStack book = itemFactory.createEnchantBook(removedId, removedLevel, 100, 0);
            if (!p.getInventory().addItem(book).isEmpty()) {
                p.getWorld().dropItem(p.getLocation(), book);
            }

            p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            sendMsg(p, "extract-success", "%enchant%", manager.getEnchantName(removedId), "%level%", String.valueOf(removedLevel));

        } else if (extractorType.equals("SPECIFIC")) {
            int amountLeft = cursor.getAmount() - 1;
            p.setItemOnCursor(null);
            if (amountLeft > 0) {
                ItemStack leftover = cursor.clone();
                leftover.setAmount(amountLeft);
                if (!p.getInventory().addItem(leftover).isEmpty()) {
                    p.getWorld().dropItem(p.getLocation(), leftover);
                }
            }

            String mode = plugin.getSettingsFile().getString("settings.extractor-mode", "DIALOG").toUpperCase();
            if (mode.equals("GUI")) {
                ExtractorGUI gui = new ExtractorGUI(plugin, current, 0);
                FoliaScheduler.runForPlayer(plugin, p, () -> p.openInventory(gui.getInventory()));
            } else {
                FoliaScheduler.runForPlayer(plugin, p, () -> ExtractorDialog.open(plugin, p, current));
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDragInGUI(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ExtractorGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClickGUI(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ExtractorGUI gui)) return;
        event.setCancelled(true);

        // Anti-spam bottleneck safeguard
        if (gui.isCompleted()) return;

        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui.getInventory())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR || !clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();

        if (meta.getPersistentDataContainer().has(manager.GUI_ACTION_KEY, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(manager.GUI_ACTION_KEY, PersistentDataType.STRING);
            if ("PREV_PAGE".equals(action)) {
                gui.setCompleted(true);
                ExtractorGUI newGui = new ExtractorGUI(plugin, gui.getWeapon(), gui.getPage() - 1);
                p.openInventory(newGui.getInventory());
            } else if ("NEXT_PAGE".equals(action)) {
                gui.setCompleted(true);
                ExtractorGUI newGui = new ExtractorGUI(plugin, gui.getWeapon(), gui.getPage() + 1);
                p.openInventory(newGui.getInventory());
            }
            return;
        }

        if (!meta.getPersistentDataContainer().has(manager.BOOK_ID_KEY, PersistentDataType.STRING)) return;

        String enchantId = meta.getPersistentDataContainer().get(manager.BOOK_ID_KEY, PersistentDataType.STRING);
        int level = meta.getPersistentDataContainer().getOrDefault(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, 1);

        ItemStack weapon = gui.getWeapon();
        Map<String, Integer> verifyEnchants = manager.getAllEnchantsOnItem(weapon);

        if (!verifyEnchants.containsKey(enchantId)) {
            sendMsg(p, "extract-error-gone");
            p.closeInventory();
            return;
        }

        // Lock processing sequence immediately
        gui.setCompleted(true);

        manager.removeEnchant(weapon, enchantId);
        ItemStack book = itemFactory.createEnchantBook(enchantId, level, 100, 0);

        if (!p.getInventory().addItem(book).isEmpty()) {
            p.getWorld().dropItem(p.getLocation(), book);
        }

        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
        sendMsg(p, "extract-success", "%enchant%", manager.getEnchantName(enchantId), "%level%", String.valueOf(level));
        p.closeInventory();
    }

    @EventHandler
    public void onCloseGUI(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof ExtractorGUI gui) {
            if (!gui.isCompleted()) {
                Player p = (Player) event.getPlayer();
                ItemStack refund = itemFactory.createExtractor("specific", 1);
                if (!p.getInventory().addItem(refund).isEmpty()) {
                    p.getWorld().dropItem(p.getLocation(), refund);
                }
                sendMsg(p, "extract-cancelled");
            }
        }
    }
}
