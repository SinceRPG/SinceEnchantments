package net.danh.sinceenchantments.api;

import com.google.common.reflect.ClassPath;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.modules.SinceEnchant;

import java.lang.reflect.Modifier;

/**
 * Auto-scans and registers custom enchantments built directly inside the plugin.
 */
public class InternalModuleLoader {

    private final SinceEnchantments plugin;
    private final String MODULES_PACKAGE = "net.danh.sinceenchantments.modules";

    public InternalModuleLoader(SinceEnchantments plugin) {
        this.plugin = plugin;
    }

    public void loadInternalModules() {
        plugin.getLogger().info(plugin.getMessagesFile().getString("log-internal-scanning", "Scanning for internal Custom Enchant modules..."));
        int loadedCount = 0;

        try {
            ClassPath classPath = ClassPath.from(plugin.getClass().getClassLoader());

            for (ClassPath.ClassInfo classInfo : classPath.getTopLevelClassesRecursive(MODULES_PACKAGE)) {
                Class<?> clazz = classInfo.load();

                if (SinceEnchant.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                    try {
                        SinceEnchant enchant = (SinceEnchant) clazz.getDeclaredConstructor().newInstance();
                        plugin.getEnchantRegistry().register(enchant);
                        loadedCount++;
                    } catch (Exception e) {
                        String errMsg = plugin.getMessagesFile().getString("log-internal-error", "Error initializing internal module: %class%").replace("%class%", clazz.getSimpleName());
                        plugin.getLogger().warning(errMsg);
                        e.printStackTrace();
                    }
                }
            }

            String successMsg = plugin.getMessagesFile().getString("log-internal-success", "Successfully auto-registered %count% enchants from internal modules!").replace("%count%", String.valueOf(loadedCount));
            plugin.getLogger().info(successMsg);

        } catch (Exception e) {
            plugin.getLogger().severe(plugin.getMessagesFile().getString("log-internal-severe", "System error while scanning internal modules!"));
            e.printStackTrace();
        }
    }
}