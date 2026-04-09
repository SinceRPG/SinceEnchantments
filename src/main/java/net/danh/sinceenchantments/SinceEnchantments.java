package net.danh.sinceenchantments;

import com.github.retrooper.packetevents.PacketEvents;
import io.github.retrooper.packetevents.factory.spigot.SpigotPacketEventsBuilder;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.danh.sinceenchantments.api.AddonLoader;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.api.EnchantRegistry;
import net.danh.sinceenchantments.api.InternalModuleLoader;
import net.danh.sinceenchantments.command.SinceCommand;
import net.danh.sinceenchantments.listener.*;
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
    private AddonLoader addonLoader;
    private InternalModuleLoader internalModuleLoader;

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
        this.configFile = new ConfigUtils(this, "config.yml");
        this.messagesFile = new ConfigUtils(this, "messages.yml");
        this.itemsFile = new ConfigUtils(this, "items.yml");

        this.enchantManager = new EnchantManager(this);
        this.enchantRegistry = new EnchantRegistry(this);

        this.internalModuleLoader = new InternalModuleLoader(this);
        this.internalModuleLoader.loadInternalModules();

        this.addonLoader = new AddonLoader(this);
        this.addonLoader.loadAddons();

        SinceCommand commandClass = new SinceCommand(this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            event.registrar().register(
                    commandClass.buildCommand(),
                    "SinceEnchantments management command",
                    List.of("se", "sinceenchant")
            );
        });

        PacketEvents.getAPI().init();
        PacketEvents.getAPI().getEventManager().registerListener(
                new ItemPacketListener(),
                com.github.retrooper.packetevents.event.PacketListenerPriority.NORMAL
        );
        getServer().getPluginManager().registerEvents(new InventoryFixListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantApplyListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new ExtractorListener(this), this);
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

    public ConfigUtils getItemsFile() {
        return itemsFile;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public EnchantRegistry getEnchantRegistry() {
        return enchantRegistry;
    }

    public AddonLoader getAddonLoader() {
        return addonLoader;
    }

    public InternalModuleLoader getInternalModuleLoader() {
        return internalModuleLoader;
    }
}