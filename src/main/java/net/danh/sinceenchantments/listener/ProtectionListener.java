package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.ServerVersion;
import org.bukkit.GameRules;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;
import java.util.List;

public class ProtectionListener implements Listener {

    private final SinceEnchantments plugin;
    private final EnchantManager manager;

    public ProtectionListener(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player p = event.getEntity();
        boolean savedItem = false;
        boolean consumeOnDeath = plugin.getSettingsFile().getBoolean("settings.consume-protection-on-death", true);
        boolean keepInventory = ((ServerVersion.isAtMost(1, 21, 10)) ? p.getWorld().getGameRuleValue(org.bukkit.GameRule.KEEP_INVENTORY) : p.getWorld().getGameRuleValue(GameRules.KEEP_INVENTORY)) == Boolean.TRUE;

        if (keepInventory) {
            if (consumeOnDeath) {
                for (ItemStack item : event.getItemsToKeep()) {
                    if (item != null && item.hasItemMeta() && item.getItemMeta().getPersistentDataContainer().has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) {
                        ItemMeta meta = item.getItemMeta();
                        meta.getPersistentDataContainer().remove(manager.PROTECTED_ITEM_KEY);
                        item.setItemMeta(meta);
                        savedItem = true;
                    }
                }
            }
        } else {
            Iterator<ItemStack> iterator = event.getDrops().iterator();
            List<ItemStack> toKeep = event.getItemsToKeep();

            while (iterator.hasNext()) {
                ItemStack item = iterator.next();
                if (item == null || !item.hasItemMeta()) continue;

                if (item.getItemMeta().getPersistentDataContainer().has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) {
                    if (consumeOnDeath) {
                        ItemMeta meta = item.getItemMeta();
                        meta.getPersistentDataContainer().remove(manager.PROTECTED_ITEM_KEY);
                        item.setItemMeta(meta);
                    }
                    toKeep.add(item);
                    iterator.remove();
                    savedItem = true;
                }
            }
        }

        if (savedItem) {
            String prefix = plugin.getMessagesFile().getString("prefix", "");
            String msgKey = consumeOnDeath ? "item-protected-consumed" : "item-protected-saved";
            String msg = plugin.getMessagesFile().getString(msgKey, "");
            p.sendMessage(ColorUtils.parse(prefix + msg));
        }
    }
}