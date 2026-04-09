package net.danh.sinceenchantments.api;

import com.google.common.reflect.ClassPath;
import net.danh.sinceenchantments.SinceEnchantments;

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
        plugin.getLogger().info("Scanning for internal Custom Enchant modules...");
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
                        plugin.getLogger().warning("Error initializing internal module: " + clazz.getSimpleName());
                        e.printStackTrace();
                    }
                }
            }

            plugin.getLogger().info("Successfully auto-registered " + loadedCount + " enchants from internal modules!");

        } catch (Exception e) {
            plugin.getLogger().severe("System error while scanning internal modules!");
            e.printStackTrace();
        }
    }
}