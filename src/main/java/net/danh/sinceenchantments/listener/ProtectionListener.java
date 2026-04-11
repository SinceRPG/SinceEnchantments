package net.danh.sinceenchantments.listener;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.Iterator;

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

        Iterator<ItemStack> iterator = event.getDrops().iterator();
        while (iterator.hasNext()) {
            ItemStack item = iterator.next();
            if (item == null || !item.hasItemMeta()) continue;

            if (item.getItemMeta().getPersistentDataContainer().has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE)) {

                if (consumeOnDeath) {
                    ItemMeta meta = item.getItemMeta();
                    meta.getPersistentDataContainer().remove(manager.PROTECTED_ITEM_KEY);
                    item.setItemMeta(meta);
                }

                event.getItemsToKeep().add(item);
                iterator.remove();
                savedItem = true;
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