package net.danh.sinceenchantments;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.api.EnchantRegistry;
import net.danh.sinceenchantments.command.SinceCommand;
import net.danh.sinceenchantments.enchants.LifestealEnchant;
import net.danh.sinceenchantments.listener.AnvilListener;
import net.danh.sinceenchantments.listener.EnchantApplyListener;
import net.danh.sinceenchantments.listener.InventoryFixListener;
import net.danh.sinceenchantments.listener.ItemPacketListener;
import net.danh.sinceenchantments.utils.ConfigUtils;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class SinceEnchantments extends JavaPlugin {

    private static SinceEnchantments instance;

    private ConfigUtils configFile;
    private ConfigUtils messagesFile;
    private ConfigUtils itemsFile;

    private EnchantManager enchantManager;
    private EnchantRegistry enchantRegistry;

    public static SinceEnchantments getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        // 1. Tải toàn bộ cấu hình
        this.configFile = new ConfigUtils(this, "config.yml");
        this.messagesFile = new ConfigUtils(this, "messages.yml");
        this.itemsFile = new ConfigUtils(this, "items.yml");

        // 2. Khởi tạo API Lõi
        this.enchantManager = new EnchantManager(this);
        this.enchantRegistry = new EnchantRegistry(this);

        // 3. Đăng ký các Custom Enchant vào Registry
        enchantRegistry.register(new LifestealEnchant());
        // Thêm các enchant khác của bạn ở đây:
        // enchantRegistry.register(new FireballEnchant());
        // enchantRegistry.register(new MinerEnchant());

        // 4. Đăng ký Lệnh Brigadier thông qua LifecycleEvents
        SinceCommand commandClass = new SinceCommand(this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    commandClass.buildCommand(),
                    "Lenh quan ly SinceEnchantments",
                    List.of("se", "sinceenchant")
            );
        });

        // 5. Khởi động PacketEvents để render Lore
        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(
                new ItemPacketListener(),
                com.github.retrooper.packetevents.event.PacketListenerPriority.NORMAL
        );

        // 6. Đăng ký toàn bộ Bukkit Listeners (Bao gồm Anvil)
        getServer().getPluginManager().registerEvents(new InventoryFixListener(), this);
        getServer().getPluginManager().registerEvents(new EnchantApplyListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
        getLogger().info("SinceEnchantments da tat.");
    }

    public ConfigUtils getConfigFile() { return configFile; }
    public ConfigUtils getMessagesFile() { return messagesFile; }
    public ConfigUtils getItemsFile() { return itemsFile; }
    public EnchantManager getEnchantManager() { return enchantManager; }
    public EnchantRegistry getEnchantRegistry() { return enchantRegistry; }
}