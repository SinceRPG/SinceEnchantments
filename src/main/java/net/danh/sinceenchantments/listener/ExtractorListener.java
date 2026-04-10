package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.gui.ExtractorDialog;
import net.danh.sinceenchantments.gui.ExtractorGUI;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.Bukkit;
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

public class ExtractorListener implements Listener {

    private final SinceEnchantments plugin;
    private final EnchantManager manager;
    private final Random random = new Random();

    public ExtractorListener(SinceEnchantments plugin) {
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

        Map<String, Integer> allEnchants = manager.getAllEnchantsOnItem(current);
        if (allEnchants.isEmpty()) {
            sendMsg(p, "extract-no-enchants");
            return;
        }

        // Tạm thời thu hồi Extractor (Nếu huỷ thao tác sẽ được hoàn trả)
        cursor.setAmount(cursor.getAmount() - 1);
        p.setItemOnCursor(cursor);

        if (extractorType.equals("RANDOM")) {
            List<String> keys = new ArrayList<>(allEnchants.keySet());
            String removedId = keys.get(random.nextInt(keys.size()));
            int removedLevel = allEnchants.get(removedId);

            manager.removeEnchant(current, removedId);

            ItemStack book = manager.createEnchantBook(removedId, removedLevel, 100, 0);
            if (!p.getInventory().addItem(book).isEmpty()) {
                p.getWorld().dropItem(p.getLocation(), book);
            }

            p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
            sendMsg(p, "extract-success", "%enchant%", manager.getEnchantName(removedId), "%level%", String.valueOf(removedLevel));

        } else if (extractorType.equals("SPECIFIC")) {
            // Đọc cấu hình để quyết định dùng GUI hay Dialog API
            String mode = plugin.getConfigFile().getString("settings.extractor-mode", "DIALOG").toUpperCase();

            if (mode.equals("GUI")) {
                ExtractorGUI gui = new ExtractorGUI(plugin, current, 0); // Start at Page 0
                Bukkit.getScheduler().runTask(plugin, () -> p.openInventory(gui.getInventory()));
            } else {
                // Gọi Paper Dialog API
                ExtractorDialog.open(plugin, p, current);
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

        if (!(event.getWhoClicked() instanceof Player p)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(gui.getInventory())) return;

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;
        if (!clicked.hasItemMeta()) return;

        ItemMeta meta = clicked.getItemMeta();

        // Xử lý nút phân trang và border của GUI cũ
        if (meta.getPersistentDataContainer().has(manager.GUI_ACTION_KEY, PersistentDataType.STRING)) {
            String action = meta.getPersistentDataContainer().get(manager.GUI_ACTION_KEY, PersistentDataType.STRING);
            if ("PREV_PAGE".equals(action)) {
                gui.setCompleted(true); // Ngăn không refund
                ExtractorGUI newGui = new ExtractorGUI(plugin, gui.getWeapon(), gui.getPage() - 1);
                p.openInventory(newGui.getInventory());
            } else if ("NEXT_PAGE".equals(action)) {
                gui.setCompleted(true); // Ngăn không refund
                ExtractorGUI newGui = new ExtractorGUI(plugin, gui.getWeapon(), gui.getPage() + 1);
                p.openInventory(newGui.getInventory());
            }
            return;
        }

        // Xử lý Extractor của GUI cũ
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

        manager.removeEnchant(weapon, enchantId);
        ItemStack book = manager.createEnchantBook(enchantId, level, 100, 0);

        if (!p.getInventory().addItem(book).isEmpty()) {
            p.getWorld().dropItem(p.getLocation(), book);
        }

        gui.setCompleted(true);
        p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);
        sendMsg(p, "extract-success", "%enchant%", manager.getEnchantName(enchantId), "%level%", String.valueOf(level));

        p.closeInventory();
    }

    @EventHandler
    public void onCloseGUI(InventoryCloseEvent event) {
        // Chỉ xử lý Refund cho GUI Chest Legacy. Dialog API xử lý Refund trực tiếp trong Callback.
        if (event.getInventory().getHolder() instanceof ExtractorGUI gui) {
            if (!gui.isCompleted()) {
                Player p = (Player) event.getPlayer();
                ItemStack refund = manager.createExtractor("specific", 1);

                if (!p.getInventory().addItem(refund).isEmpty()) {
                    p.getWorld().dropItem(p.getLocation(), refund);
                }
                sendMsg(p, "extract-cancelled");
            }
        }
    }
}