package net.danh.sinceenchantments.api;

import com.badbones69.crazyenchantments.paper.CrazyEnchantments;
import com.badbones69.crazyenchantments.paper.api.CrazyManager;
import com.badbones69.crazyenchantments.paper.api.objects.CEnchantment;
import net.danh.sinceenchantments.SinceEnchantments;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class CrazyEnchantmentsHook {
    private final SinceEnchantments plugin;
    private boolean hooked = false;
    private CrazyEnchantments crazyEnchantments;
    private CrazyManager crazyManager;

    public CrazyEnchantmentsHook(SinceEnchantments plugin) {
        this.plugin = plugin;
        setup();
    }

    private void setup() {
        Plugin detected = Bukkit.getPluginManager().getPlugin("CrazyEnchantments");
        if (detected == null || !detected.isEnabled()) {
            log("log-ce-hook-missing", "CrazyEnchantments not detected. Skipping CE hook.");
            return;
        }
        if (!(detected instanceof CrazyEnchantments crazyPlugin)) {
            logWarning("log-ce-hook-fail", "CrazyEnchantments detected, but failed to hook into API: unexpected plugin class %class%.", "%class%", detected.getClass().getName());
            return;
        }

        log("log-ce-hook-detected", "CrazyEnchantments detected (version %version%). Preparing CE API hook.", "%version%", detected.getPluginMeta().getVersion());

        try {
            if (crazyPlugin.getStarter() == null || crazyPlugin.getStarter().getCrazyManager() == null) {
                logWarning("log-ce-hook-fail", "CrazyEnchantments detected, but failed to hook into API: CrazyManager is not ready.");
                return;
            }

            this.crazyEnchantments = crazyPlugin;
            this.crazyManager = crazyPlugin.getStarter().getCrazyManager();
            this.hooked = true;
            int registered = crazyManager.getRegisteredEnchantments().size();
            log("log-ce-hook-success", "Successfully hooked into CrazyEnchantments API! Found %count% enchantments.", "%count%", String.valueOf(registered));
        } catch (LinkageError | RuntimeException e) {
            logWarning("log-ce-hook-fail", "CrazyEnchantments detected, but failed to hook into API: %error%", "%error%", e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    public boolean isHooked() {
        return hooked;
    }

    public void loadCrazyEnchantments() {
        if (!hooked) {
            log("log-ce-autoload-skip", "Skipping CrazyEnchantments auto-load because CE hook is not active.");
            return;
        }

        try {
            List<CEnchantment> crazyEnchants = crazyManager.getRegisteredEnchantments();
            log("log-ce-autoload-start", "Scanning %total% CrazyEnchantments enchantments for missing SinceEnchantments entries.", "%total%", String.valueOf(crazyEnchants.size()));
            int count = 0;
            int skipped = 0;
            int failed = 0;

            List<String> defaultDescription = plugin.getSettingsFile().getStringList("settings.ce-default-description");
            if (defaultDescription == null || defaultDescription.isEmpty()) {
                defaultDescription = new ArrayList<>();
            }

            for (CEnchantment enchantment : crazyEnchants) {
                String id = toEnchantId(enchantment.getName());

                if (plugin.getEnchantsFile().getConfig().contains("custom-enchants." + id)) {
                    skipped++;
                    continue;
                }

                try {
                    plugin.getEnchantManager().registerDynamicEnchant(
                            id,
                            cleanDisplayName(enchantment),
                            Math.max(1, enchantment.getMaxLevel()),
                            "EPIC",
                            "ALL",
                            defaultDescription
                    );
                    count++;
                } catch (LinkageError | RuntimeException e) {
                    failed++;
                    plugin.getLogger().warning("Failed to auto-register CE enchantment '" + enchantment.getName() + "': " + e.getMessage());
                }
            }

            log("log-ce-autoload", "CrazyEnchantments auto-load finished: registered %count%, skipped %skipped%, failed %failed%.",
                    "%count%", String.valueOf(count),
                    "%skipped%", String.valueOf(skipped),
                    "%failed%", String.valueOf(failed));
        } catch (LinkageError | RuntimeException e) {
            plugin.getLogger().warning("Error while auto-loading enchantments from CrazyEnchantments: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public Map<String, Integer> getEnchants(ItemStack item) {
        Map<String, Integer> enchants = new HashMap<>();
        if (!hooked || item == null) return enchants;

        try {
            for (Map.Entry<CEnchantment, Integer> entry : crazyEnchantments.getStarter().getEnchantmentBookSettings().getEnchantments(item).entrySet()) {
                enchants.put(toEnchantId(entry.getKey().getName()), entry.getValue());
            }
        } catch (LinkageError | RuntimeException ignored) {
        }

        return enchants;
    }

    public boolean applyEnchant(ItemStack item, String enchantId, int level) {
        if (!hooked || item == null || !enchantId.startsWith("ce:")) return false;

        try {
            CEnchantment enchantment = crazyManager.getEnchantmentFromName(enchantId.substring(3));
            if (enchantment == null) return false;
            crazyManager.addEnchantment(item, enchantment, level);
            return true;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    public boolean removeEnchant(ItemStack item, String enchantId) {
        if (!hooked || item == null || !enchantId.startsWith("ce:")) return false;

        try {
            CEnchantment enchantment = crazyManager.getEnchantmentFromName(enchantId.substring(3));
            if (enchantment == null) return false;
            crazyEnchantments.getStarter().getEnchantmentBookSettings().removeEnchantment(item, enchantment);
            return true;
        } catch (LinkageError | RuntimeException ignored) {
            return false;
        }
    }

    private String toEnchantId(String name) {
        return "ce:" + name.toLowerCase(Locale.ROOT);
    }

    private String cleanDisplayName(CEnchantment enchantment) {
        String customName = enchantment.getCustomName();
        if (customName == null || customName.isBlank()) return enchantment.getName();
        return customName;
    }

    private void log(String path, String fallback, String... replacements) {
        plugin.getLogger().info(formatMessage(path, fallback, replacements));
    }

    private void logWarning(String path, String fallback, String... replacements) {
        plugin.getLogger().warning(formatMessage(path, fallback, replacements));
    }

    private String formatMessage(String path, String fallback, String... replacements) {
        String message = plugin.getMessagesFile().getString(path, fallback);
        for (int i = 0; i < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        return message;
    }
}
