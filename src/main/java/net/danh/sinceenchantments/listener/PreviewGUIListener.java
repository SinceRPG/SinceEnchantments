package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.gui.PreviewGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class PreviewGUIListener implements Listener {
    private final SinceEnchantments plugin;

    public PreviewGUIListener(SinceEnchantments plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onClickGUI(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof PreviewGUI gui)) return;

        if (!(event.getWhoClicked() instanceof Player p)) return;
        
        int itemSlot = plugin.getGuiFile().getInt("gui.preview.item-slot", 10);
        int nextSlot = plugin.getGuiFile().getInt("gui.preview.next-page.slot", 26);
        int prevSlot = plugin.getGuiFile().getInt("gui.preview.prev-page.slot", 17);

        // If clicking in player inventory, allow if it's not a shift click, or handle shift click
        if (event.getClickedInventory() != null && !event.getClickedInventory().equals(gui.getInventory())) {
            if (event.isShiftClick()) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR) {
                    if (gui.getWeapon() == null || gui.getWeapon().getType() == Material.AIR || gui.isDummyWeapon()) {
                        gui.setWeapon(clicked.clone(), false);
                        event.setCurrentItem(new ItemStack(Material.AIR));
                    }
                }
            }
            return;
        }
        
        // If they click inside the GUI
        if (event.getClickedInventory() != null && event.getClickedInventory().equals(gui.getInventory())) {
            List<Integer> innerSlots = plugin.getGuiFile().getConfig().getIntegerList("gui.preview.enchantment-slots");
            if (innerSlots.isEmpty()) {
                innerSlots = List.of(12, 13, 14, 15, 16, 21, 22, 23, 24, 25);
            }

            if (event.getSlot() == itemSlot) {
                if (gui.isDummyWeapon()) {
                    event.setCancelled(true);
                    ItemStack cursor = event.getCursor();
                    if (cursor != null && cursor.getType() != Material.AIR) {
                        ItemStack realItem = cursor.clone();
                        event.getView().setCursor(null);
                        gui.setWeapon(realItem, false);
                    } else {
                        gui.setWeapon(null, false);
                    }
                } else {
                    plugin.getServer().getScheduler().runTask(plugin, () -> {
                        ItemStack item = gui.getInventory().getItem(itemSlot);
                        gui.setWeapon(item != null ? item.clone() : null, false);
                    });
                }
            } else if (innerSlots.contains(event.getSlot())) {
                event.setCancelled(true);
                ItemStack clicked = event.getCurrentItem();
                if (clicked != null && clicked.getType() != Material.AIR) {
                    if (gui.getWeapon() == null || gui.getWeapon().getType() == Material.AIR) {
                        NamespacedKey dummyKey = new NamespacedKey(plugin, "gui_dummy_item");
                        if (clicked.hasItemMeta() && clicked.getItemMeta().getPersistentDataContainer().has(dummyKey, PersistentDataType.STRING)) {
                            ItemStack dummyWep = new ItemStack(clicked.getType());
                            if (clicked.getItemMeta().hasCustomModelData()) {
                                ItemMeta dummyMeta = dummyWep.getItemMeta();
                                dummyMeta.setCustomModelData(clicked.getItemMeta().getCustomModelData());
                                dummyWep.setItemMeta(dummyMeta);
                            }
                            gui.setWeapon(dummyWep, true);
                        }
                    }
                }
            } else if (event.getSlot() == nextSlot) {
                event.setCancelled(true);
                ItemStack item = gui.getWeapon();
                if (item != null && item.getType() != Material.AIR) {
                    List<String> whitelisted;
                    if (gui.isDummyWeapon()) {
                        whitelisted = plugin.getEnchantManager().getWhitelistedEnchants(item);
                    } else {
                        whitelisted = plugin.getEnchantManager().getWhitelistedEnchants(item);
                    }
                    int maxItems = innerSlots.size();
                    int totalPages = (int) Math.ceil((double) whitelisted.size() / maxItems);
                    if (totalPages == 0) totalPages = 1;
                    if (gui.getPage() < totalPages - 1) {
                        gui.getInventory().setItem(itemSlot, null);
                        PreviewGUI newGui = new PreviewGUI(plugin, item, gui.getPage() + 1, gui.isDummyWeapon());
                        p.openInventory(newGui.getInventory());
                    }
                } else {
                    // Pagination for default items
                    ConfigurationSection defaultItemsSec = plugin.getGuiFile().getConfig().getConfigurationSection("gui.preview.default-items");
                    if (defaultItemsSec != null) {
                        List<String> keys = new ArrayList<>(defaultItemsSec.getKeys(false));
                        int maxItems = innerSlots.size();
                        int totalPages = (int) Math.ceil((double) keys.size() / maxItems);
                        if (totalPages == 0) totalPages = 1;
                        if (gui.getPage() < totalPages - 1) {
                            PreviewGUI newGui = new PreviewGUI(plugin, null, gui.getPage() + 1, false);
                            p.openInventory(newGui.getInventory());
                        }
                    }
                }
            } else if (event.getSlot() == prevSlot) {
                event.setCancelled(true);
                ItemStack item = gui.getWeapon();
                if (item != null && item.getType() != Material.AIR) {
                    if (gui.getPage() > 0) {
                        gui.getInventory().setItem(itemSlot, null);
                        PreviewGUI newGui = new PreviewGUI(plugin, item, gui.getPage() - 1, gui.isDummyWeapon());
                        p.openInventory(newGui.getInventory());
                    }
                } else {
                    if (gui.getPage() > 0) {
                        PreviewGUI newGui = new PreviewGUI(plugin, null, gui.getPage() - 1, false);
                        p.openInventory(newGui.getInventory());
                    }
                }
            } else {
                event.setCancelled(true);
                int backSlot = plugin.getGuiFile().getInt("gui.preview.back-button.slot", -1);
                if (event.getSlot() == backSlot) {
                    p.closeInventory();
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDragInGUI(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof PreviewGUI gui) {
            int itemSlot = plugin.getGuiFile().getInt("gui.preview.item-slot", 10);
            if (event.getRawSlots().contains(itemSlot)) {
                if (gui.isDummyWeapon()) {
                    event.setCancelled(true);
                    return;
                }
                plugin.getServer().getScheduler().runTask(plugin, () -> {
                    ItemStack item = gui.getInventory().getItem(itemSlot);
                    gui.setWeapon(item != null ? item.clone() : null, false);
                });
            }
            for (int slot : event.getRawSlots()) {
                if (slot != itemSlot && slot < gui.getInventory().getSize()) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler
    public void onCloseGUI(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof PreviewGUI gui) {
            if (gui.isDummyWeapon()) return;
            Player p = (Player) event.getPlayer();
            int itemSlot = plugin.getGuiFile().getInt("gui.preview.item-slot", 10);
            ItemStack item = gui.getInventory().getItem(itemSlot);
            if (item != null && item.getType() != Material.AIR) {
                if (!p.getInventory().addItem(item).isEmpty()) {
                    p.getWorld().dropItem(p.getLocation(), item);
                }
            }
        }
    }
}
