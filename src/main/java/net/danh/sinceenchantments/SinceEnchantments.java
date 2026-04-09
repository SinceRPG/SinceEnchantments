package net.danh.sinceenchantments;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import net.danh.sinceenchantments.listener.InventoryFixListener; // Thêm import này
import net.danh.sinceenchantments.listener.ItemPacketListener;
import net.danh.sinceenchantments.utils.ConfigUtils;
import org.bukkit.plugin.java.JavaPlugin;

public class SinceEnchantments extends JavaPlugin {

    private static SinceEnchantments instance;
    private ConfigUtils configFile;
    private ConfigUtils messagesFile;

    public static SinceEnchantments getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings()
                .reEncodeByDefault(true)
                .checkForUpdates(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        this.configFile = new ConfigUtils(this, "config.yml");
        this.messagesFile = new ConfigUtils(this, "messages.yml");

        PacketEvents.getAPI().init();

        PacketEvents.getAPI().getEventManager().registerListener(
                new ItemPacketListener(),
                com.github.retrooper.packetevents.event.PacketListenerPriority.NORMAL
        );

        getServer().getPluginManager().registerEvents(new InventoryFixListener(), this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public ConfigUtils getConfigFile() {
        return configFile;
    }

    public ConfigUtils getMessagesFile() {
        return messagesFile;
    }
}