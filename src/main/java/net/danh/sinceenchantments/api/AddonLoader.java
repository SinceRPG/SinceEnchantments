package net.danh.sinceenchantments.api;

import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.modules.SinceEnchant;

import java.io.File;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Handles dynamic loading of external .jar addons.
 */
public class AddonLoader {
    private final SinceEnchantments plugin;

    public AddonLoader(SinceEnchantments plugin) {
        this.plugin = plugin;
    }

    public void loadAddons() {
        File addonFolder = new File(plugin.getDataFolder(), "Enchantments");
        if (!addonFolder.exists()) {
            addonFolder.mkdirs();
            plugin.getLogger().info(plugin.getMessagesFile().getString("log-addon-folder-created", "Created 'Enchantments' folder. Drop external .jar addons here!"));
            return;
        }

        File[] jarFiles = addonFolder.listFiles((dir, name) -> name.endsWith(".jar"));
        if (jarFiles == null || jarFiles.length == 0) {
            plugin.getLogger().info(plugin.getMessagesFile().getString("log-addon-none-found", "No Addons (.jar) found in Enchantments folder."));
            return;
        }

        plugin.getLogger().info(plugin.getMessagesFile().getString("log-addon-scanning", "Scanning for Custom Enchant Addons..."));
        for (File jarFile : jarFiles) {
            try {
                loadEnchantsFromJar(jarFile);
            } catch (Exception e) {
                String errorMsg = plugin.getMessagesFile().getString("log-addon-load-failed", "Failed to load addon from file: %file%").replace("%file%", jarFile.getName());
                plugin.getLogger().severe(errorMsg);
                e.printStackTrace();
            }
        }
    }

    private void loadEnchantsFromJar(File file) throws Exception {
        URL[] urls = {file.toURI().toURL()};
        URLClassLoader loader = new URLClassLoader(urls, plugin.getClass().getClassLoader());
        try (JarFile jar = new JarFile(file)) {
            Enumeration<JarEntry> entries = jar.entries();
            int loadedCount = 0;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.getName().endsWith(".class") && !entry.getName().contains("$")) {
                    String className = entry.getName().replace("/", ".").replace(".class", "");
                    try {
                        Class<?> clazz = Class.forName(className, true, loader);
                        if (SinceEnchant.class.isAssignableFrom(clazz) && !Modifier.isAbstract(clazz.getModifiers())) {
                            SinceEnchant enchant = (SinceEnchant) clazz.getDeclaredConstructor().newInstance();
                            plugin.getEnchantRegistry().register(enchant);
                            loadedCount++;
                        }
                    } catch (NoClassDefFoundError | ClassNotFoundException e) {
                        String warnMsg = plugin.getMessagesFile().getString("log-addon-missing-deps", "Skipping class %class% due to missing dependencies.").replace("%class%", className);
                        plugin.getLogger().warning(warnMsg);
                    } catch (Exception e) {
                        String warnMsg = plugin.getMessagesFile().getString("log-addon-init-error", "Error initializing class %class% in %file%").replace("%class%", className).replace("%file%", file.getName());
                        plugin.getLogger().warning(warnMsg);
                        e.printStackTrace();
                    }
                }
            }
            String successMsg = plugin.getMessagesFile().getString("log-addon-success", "Successfully loaded %count% enchant(s) from addon: %file%").replace("%count%", String.valueOf(loadedCount)).replace("%file%", file.getName());
            plugin.getLogger().info(successMsg);
        }
    }
}