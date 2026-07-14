package net.danh.sinceenchantments;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.danh.sinceenchantments.api.*;
import net.danh.sinceenchantments.command.SinceCommand;
import net.danh.sinceenchantments.listener.*;
import net.danh.sinceenchantments.utils.ConfigUtils;
import net.danh.sinceenchantments.utils.ServerVersion;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Main class for the SinceEnchantments plugin.
 * Handles the initialization sequence, configuration loading, and dependency hooking.
 */
@SuppressWarnings("UnstableApiUsage")
public class SinceEnchantments extends JavaPlugin {
    private static SinceEnchantments instance;

    private ConfigUtils settingsFile;
    private ConfigUtils enchantsFile;
    private ConfigUtils limitsFile;
    private ConfigUtils messagesFile;
    private ConfigUtils itemsFile;
    private ConfigUtils guiFile;
    private ConfigUtils customItemsFile;

    private EnchantManager enchantManager;
    private ItemFactory itemFactory;
    private EnchantRegistry enchantRegistry;
    private AddonLoader addonLoader;
    private InternalModuleLoader internalModuleLoader;
    private MMOCoreHook mmoCoreHook;
    private MythicLibHook mythicLibHook;
    private AdvancedEnchantmentsHook advancedEnchantmentsHook;
    private CrazyEnchantmentsHook crazyEnchantmentsHook;
    private ExcellentEnchantsHook excellentEnchantsHook;
    private ItemPacketListener itemPacketListener;

    public static SinceEnchantments getInstance() {
        return instance;
    }

    @Override
    public void onLoad() {
        instance = this;

    }

    @Override
    public void onEnable() {
        this.settingsFile = new ConfigUtils(this, "settings.yml", true);
        this.messagesFile = new ConfigUtils(this, "messages.yml", settingsFile.getBoolean("auto-update.messages", true));

        logStartupVersion();

        this.enchantsFile = new ConfigUtils(this, "enchants.yml", settingsFile.getBoolean("auto-update.enchants", false));
        this.limitsFile = new ConfigUtils(this, "limits.yml", settingsFile.getBoolean("auto-update.limits", false));
        this.itemsFile = new ConfigUtils(this, "items.yml", settingsFile.getBoolean("auto-update.items", false));
        this.guiFile = new ConfigUtils(this, "gui.yml", settingsFile.getBoolean("auto-update.gui", false));
        this.customItemsFile = new ConfigUtils(this, "custom-items.yml", settingsFile.getBoolean("auto-update.custom-items", false));

        this.mythicLibHook = new MythicLibHook(this);
        this.enchantManager = new EnchantManager(this);
        this.itemFactory = new ItemFactory(this);
        this.enchantRegistry = new EnchantRegistry(this);
        this.mmoCoreHook = new MMOCoreHook(this);

        this.advancedEnchantmentsHook = new AdvancedEnchantmentsHook(this);
        this.advancedEnchantmentsHook.loadAEEnchantments();
        this.crazyEnchantmentsHook = new CrazyEnchantmentsHook(this);
        this.crazyEnchantmentsHook.loadCrazyEnchantments();
        this.excellentEnchantsHook = new ExcellentEnchantsHook(this);
        this.excellentEnchantsHook.loadExcellentEnchantments();

        this.internalModuleLoader = new InternalModuleLoader(this);
        this.internalModuleLoader.loadInternalModules();

        SinceCommand commandClass = new SinceCommand(this);
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            String desc = messagesFile.getString("command-description", "SinceEnchantments management command");
            List<String> aliases = settingsFile.getStringList("settings.command-aliases");
            if (aliases == null || aliases.isEmpty()) {
                aliases = new ArrayList<>();
                aliases.add("se");
                aliases.add("sinceenchant");
            }
            event.registrar().register(commandClass.buildCommand(), desc, aliases);
        });


        this.itemPacketListener = new ItemPacketListener();
        PacketEvents.getAPI().getEventManager().registerListener(itemPacketListener, PacketListenerPriority.NORMAL);

        getServer().getPluginManager().registerEvents(new InventoryFixListener(this), this);
        getServer().getPluginManager().registerEvents(new EnchantApplyListener(this), this);
        getServer().getPluginManager().registerEvents(new AnvilListener(this), this);
        getServer().getPluginManager().registerEvents(new ExtractorListener(this), this);
        getServer().getPluginManager().registerEvents(new StatTrackerListener(this), this);
        getServer().getPluginManager().registerEvents(new ProtectionListener(this), this);
        getServer().getPluginManager().registerEvents(new PreviewGUIListener(this), this);
    }

    @Override
    public void onDisable() {
        if (itemPacketListener != null) {
            itemPacketListener.clearCache();
            itemPacketListener = null;
        }

        instance = null;
    }

    /**
     * Reads version information and prints the appropriate configured message.
     */
    private void logStartupVersion() {
        if (ServerVersion.isAtMost(1, 21, 11)) {
            String msg = messagesFile.getString("startup-native", "Running natively for Paper 1.21+ | NMS: %nms%");
            getLogger().info(msg.replace("%nms%", ServerVersion.getNmsVersion()));
        } else {
            String msg = messagesFile.getString("startup-future", "Running natively for Paper 26.1+ | Version: %version%");
            getLogger().info(msg.replace("%version%", "v" + ServerVersion.getMajor() + "_" + ServerVersion.getMinor()));
        }

        if (ServerVersion.isOlderThan(1, 21, 3)) {
            getLogger().warning(messagesFile.getString("startup-warning", "Warning: Unsupported server version detected!"));
        }
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

    public ConfigUtils getCustomItemsFile() {
        return customItemsFile;
    }

    public EnchantManager getEnchantManager() {
        return enchantManager;
    }

    public ItemFactory getItemFactory() {
        return itemFactory;
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

    public MMOCoreHook getMMOCoreHook() {
        return mmoCoreHook;
    }

    public MythicLibHook getMythicLibHook() {
        return mythicLibHook;
    }

    public AdvancedEnchantmentsHook getAdvancedEnchantmentsHook() {
        return advancedEnchantmentsHook;
    }

    public CrazyEnchantmentsHook getCrazyEnchantmentsHook() {
        return crazyEnchantmentsHook;
    }

    public ExcellentEnchantsHook getExcellentEnchantsHook() {
        return excellentEnchantsHook;
    }

    public void clearItemPacketCache() {
        if (itemPacketListener != null) {
            itemPacketListener.clearCache();
        }
    }
}
