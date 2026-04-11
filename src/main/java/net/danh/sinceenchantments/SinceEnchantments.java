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
import net.danh.sinceenchantments.utils.ServerVersion;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class SinceEnchantments extends JavaPlugin {

    private static SinceEnchantments instance;

    private ConfigUtils settingsFile;
    private ConfigUtils enchantsFile;
    private ConfigUtils limitsFile;
    private ConfigUtils messagesFile;
    private ConfigUtils itemsFile;
    private ConfigUtils guiFile;

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
        if (ServerVersion.isAtMost(1, 21, 11))
            getLogger().info("Running natively for Paper 1.21+ | NMS Version: " + ServerVersion.getNmsVersion());
        else {
            getLogger().info("Running natively for Paper 26.1+ | Version: v" + ServerVersion.getMajor() + "_" + ServerVersion.getMinor() + "_" + ServerVersion.getPatch());
            getLogger().info("Running natively for Paper 26.1+ | NMS Version: v" + ServerVersion.getMajor() + "_" + ServerVersion.getMinor() + "_R" + ServerVersion.getRevisionNumber());
        }
        PacketEvents.setAPI(SpigotPacketEventsBuilder.build(this));
        PacketEvents.getAPI().getSettings().reEncodeByDefault(true).checkForUpdates(true);
        PacketEvents.getAPI().load();
    }

    @Override
    public void onEnable() {
        this.settingsFile = new ConfigUtils(this, "settings.yml");
        this.enchantsFile = new ConfigUtils(this, "enchants.yml");
        this.limitsFile = new ConfigUtils(this, "limits.yml");
        this.messagesFile = new ConfigUtils(this, "messages.yml");
        this.itemsFile = new ConfigUtils(this, "items.yml");
        this.guiFile = new ConfigUtils(this, "gui.yml");

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
        getServer().getPluginManager().registerEvents(new StatTrackerListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        if (ServerVersion.isOlderThan(1, 21, 11))
            getLogger().warning("Warning: Your server version is below 1.21.11! If it have any error, join discord and report to author: https://discord.gg/zbMPtcM3wq");
        else if (ServerVersion.isAtLeast(26, 1))
            getLogger().warning("Warning: Your server version is below 26.1+! If it have any error, join discord and report to author: https://discord.gg/zbMPtcM3wq");
    }

    @Override
    public void onDisable() {
        PacketEvents.getAPI().terminate();
    }

    public ConfigUtils getSettingsFile() {
        return settingsFile;
    }

    public ConfigUtils getEnchantsFile() {
        return enchantsFile;
    }

    public ConfigUtils getLimitsFile() {
        return limitsFile;
    }

    public ConfigUtils getMessagesFile() {
        return messagesFile;
    }

    public ConfigUtils getItemsFile() {
        return itemsFile;
    }

    public ConfigUtils getGuiFile() {
        return guiFile;
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