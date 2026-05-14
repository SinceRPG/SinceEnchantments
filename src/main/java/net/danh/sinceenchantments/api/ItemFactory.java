package net.danh.sinceenchantments.api;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.ServerVersion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemRarity;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ItemFactory {

    private final SinceEnchantments plugin;
    private final EnchantManager manager;

    public ItemFactory(SinceEnchantments plugin) {
        this.plugin = plugin;
        this.manager = plugin.getEnchantManager();
    }

    private ItemStack buildItem(String configPath, String defMat, int amount) {
        String matStr = plugin.getItemsFile().getString(configPath + ".material", defMat);
        Material mat = Material.matchMaterial(matStr.toUpperCase());
        if (mat == null) {
            mat = Material.matchMaterial(defMat.toUpperCase());
        }
        if (mat == null) {
            mat = Material.PAPER;
        }
        return new ItemStack(mat, amount);
    }


    private void applyItemMeta(ItemStack item, String configPath, String defName, String... replacements) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        ConfigurationSection cfg = plugin.getItemsFile().getConfig().getConfigurationSection(configPath);
        if (cfg == null) {
            meta.displayName(ColorUtils.parse(defName).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
            return;
        }

        // Display name shown in hover text and inventory slots.
        String name = cfg.getString("name", defName);
        for (int i = 0; i < replacements.length; i += 2) name = name.replace(replacements[i], replacements[i + 1]);
        meta.displayName(ColorUtils.parse(name).decoration(TextDecoration.ITALIC, false));

        // Item name is stable 1.21+ metadata that cannot be renamed in an anvil.
        if (cfg.contains("item-name")) {
            try {
                String itemName = cfg.getString("item-name");
                for (int i = 0; i < replacements.length; i += 2)
                    itemName = itemName.replace(replacements[i], replacements[i + 1]);
                meta.itemName(ColorUtils.parse(itemName).decoration(TextDecoration.ITALIC, false));
            } catch (Throwable ignored) {
            }
        }

        // Lore supports placeholders and MiniMessage/color-code parsing.
        if (cfg.contains("lore")) {
            List<String> rawLore = cfg.getStringList("lore");
            List<Component> compLore = new ArrayList<>();
            for (String line : rawLore) {
                for (int i = 0; i < replacements.length; i += 2)
                    line = line.replace(replacements[i], replacements[i + 1]);
                compLore.add(ColorUtils.parse(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(compLore);
        }

        // Custom model data supports both legacy integer values and modern components.
        if (cfg.contains("custom-model-data")) {
            if (cfg.isConfigurationSection("custom-model-data")) {
                ConfigurationSection cmdSec = cfg.getConfigurationSection("custom-model-data");
                if (ServerVersion.isAtLeast(1, 21, 5)) {
                    try {
                        CustomModelDataComponent cmdc = meta.getCustomModelDataComponent();
                        if (cmdSec.contains("floats")) {
                            List<Float> floats = new ArrayList<>();
                            for (Double d : cmdSec.getDoubleList("floats")) floats.add(d.floatValue());
                            cmdc.setFloats(floats);
                        }
                        if (cmdSec.contains("strings")) cmdc.setStrings(cmdSec.getStringList("strings"));
                        if (cmdSec.contains("flags")) cmdc.setFlags(cmdSec.getBooleanList("flags"));
                        if (cmdSec.contains("colors")) {
                            List<Color> colors = new ArrayList<>();
                            for (String hex : cmdSec.getStringList("colors")) {
                                try {
                                    colors.add(Color.fromRGB(Integer.parseInt(hex.replace("#", ""), 16)));
                                } catch (Exception ignored) {
                                }
                            }
                            cmdc.setColors(colors);
                        }
                        meta.setCustomModelDataComponent(cmdc);
                    } catch (Throwable t) {
                        if (cmdSec.contains("value")) meta.setCustomModelData(cmdSec.getInt("value"));
                    }
                } else {
                    if (cmdSec.contains("value")) meta.setCustomModelData(cmdSec.getInt("value"));
                }
            } else {
                if (ServerVersion.isAtLeast(1, 21, 5)) {
                    try {
                        CustomModelDataComponent cmdc = meta.getCustomModelDataComponent();
                        cmdc.setFloats(List.of((float) cfg.getInt("custom-model-data")));
                        meta.setCustomModelDataComponent(cmdc);
                    } catch (Throwable t) {
                        meta.setCustomModelData(cfg.getInt("custom-model-data"));
                    }
                } else {
                    meta.setCustomModelData(cfg.getInt("custom-model-data"));
                }
            }
        }

        // 1.21+ item model override.
        if (cfg.contains("item-model")) {
            try {
                NamespacedKey key = NamespacedKey.fromString(cfg.getString("item-model"));
                if (key != null) meta.setItemModel(key);
            } catch (Throwable ignored) {
            }
        }

        // 1.21+ tooltip style override.
        if (cfg.contains("tooltip-style")) {
            try {
                NamespacedKey key = NamespacedKey.fromString(cfg.getString("tooltip-style"));
                if (key != null) meta.setTooltipStyle(key);
            } catch (Throwable ignored) {
            }
        }

        // 1.21+ stack-size override, clamped to vanilla-safe values.
        if (cfg.contains("max-stack-size")) {
            try {
                meta.setMaxStackSize(Math.max(1, Math.min(99, cfg.getInt("max-stack-size"))));
            } catch (Throwable ignored) {
            }
        }

        // Vanilla item rarity color.
        if (cfg.contains("rarity")) {
            try {
                meta.setRarity(ItemRarity.valueOf(cfg.getString("rarity").toUpperCase()));
            } catch (Throwable ignored) {
            }
        }

        // Hide the entire tooltip when configured.
        if (cfg.contains("hide-tooltip")) {
            try {
                meta.setHideTooltip(cfg.getBoolean("hide-tooltip"));
            } catch (Throwable ignored) {
            }
        }

        // Force or remove enchantment glint without requiring a real enchantment.
        if (cfg.contains("glint-override")) {
            try {
                meta.setEnchantmentGlintOverride(cfg.getBoolean("glint-override"));
            } catch (Throwable ignored) {
            }
        }

        // Enables glider behavior on supported server versions.
        if (cfg.contains("glider")) {
            try {
                meta.setGlider(cfg.getBoolean("glider"));
            } catch (Throwable ignored) {
            }
        }

        // Controls vanilla enchanting table weight on supported server versions.
        if (cfg.contains("enchantable")) {
            try {
                meta.setEnchantable(cfg.getInt("enchantable"));
            } catch (Throwable ignored) {
            }
        }

        // Makes the item ignore durability loss.
        if (cfg.contains("unbreakable")) meta.setUnbreakable(cfg.getBoolean("unbreakable"));

        // Item flags hide selected vanilla tooltip sections.
        if (cfg.contains("flags")) {
            for (String flag : cfg.getStringList("flags")) {
                try {
                    meta.addItemFlags(ItemFlag.valueOf(flag.toUpperCase()));
                } catch (Exception ignored) {
                }
            }
        }

        // 15. Vanilla Enchantments
        if (cfg.contains("enchants")) {
            ConfigurationSection enchSec = cfg.getConfigurationSection("enchants");
            if (enchSec != null) {
                for (String key : enchSec.getKeys(false)) {
                    NamespacedKey nsKey = NamespacedKey.fromString(key.toLowerCase());
                    if (nsKey != null) {
                        Enchantment enchantment = RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT).get(nsKey);
                        if (enchantment != null) meta.addEnchant(enchantment, enchSec.getInt(key), true);
                    }
                }
            }
        }

        // Attribute modifiers such as attack damage, max health, or movement speed.
        if (cfg.contains("attributes")) {
            ConfigurationSection attrSec = cfg.getConfigurationSection("attributes");
            if (attrSec != null) {
                for (String key : attrSec.getKeys(false)) {
                    NamespacedKey nsKey = NamespacedKey.fromString(key.toLowerCase());
                    if (nsKey != null) {
                        Attribute attribute = RegistryAccess.registryAccess().getRegistry(RegistryKey.ATTRIBUTE).get(nsKey);
                        if (attribute != null) {
                            String attrPath = "attributes." + key;
                            double amount = cfg.getDouble(attrPath + ".amount", 0.0);
                            String opStr = cfg.getString(attrPath + ".operation", "ADD_NUMBER");
                            String slotStr = cfg.getString(attrPath + ".slot", "ANY");

                            try {
                                AttributeModifier.Operation op = AttributeModifier.Operation.valueOf(opStr.toUpperCase());
                                EquipmentSlotGroup slotGroup = EquipmentSlotGroup.getByName(slotStr.toLowerCase());
                                if (slotGroup == null) slotGroup = EquipmentSlotGroup.ANY;
                                NamespacedKey modKey = new NamespacedKey(plugin, UUID.randomUUID().toString());
                                AttributeModifier modifier = new AttributeModifier(modKey, amount, op, slotGroup);
                                meta.addAttributeModifier(attribute, modifier);
                            } catch (Exception e) {
                                plugin.getLogger().warning("Failed to parse attribute modifier for " + key + " in " + configPath);
                            }
                        }
                    }
                }
            }
        }

        // Pre-applied durability damage.
        if (cfg.contains("damage") && meta instanceof Damageable dmgMeta) {
            dmgMeta.setDamage(cfg.getInt("damage"));
        }

        item.setItemMeta(meta);
    }

    public ItemStack createEnchantBook(String enchantId, int level, int successRate, int destroyRate) {
        ItemStack book = buildItem("enchant-book", "ENCHANTED_BOOK", 1);
        String eName = manager.getEnchantName(enchantId);
        String rName = manager.getRarity(enchantId);
        String rColor = plugin.getSettingsFile().getString("rarities." + rName, "&f");
        List<String> description = manager.getDescription(enchantId);

        // Apply shared item metadata before writing enchant-book-specific data.
        applyItemMeta(book, "enchant-book", "Book: %enchant_name%", "%enchant_name%", eName, "%level%", String.valueOf(level), "%rarity_name%", rName, "%rarity_color%", rColor);
        ItemMeta meta = book.getItemMeta();

        // Rebuild lore here so %description% can expand into multiple configured lines.
        List<String> rawLore = plugin.getItemsFile().getStringList("enchant-book.lore");
        if (!rawLore.isEmpty()) {
            List<Component> finalLore = new ArrayList<>();
            for (String line : rawLore) {
                if (line.contains("%description%")) {
                    for (String descLine : description)
                        finalLore.add(ColorUtils.parse(descLine).decoration(TextDecoration.ITALIC, false));
                    continue;
                }
                String parsedLine = line.replace("%enchant_name%", eName).replace("%level%", String.valueOf(level))
                        .replace("%success%", String.valueOf(successRate)).replace("%destroy%", String.valueOf(destroyRate))
                        .replace("%rarity_name%", rName).replace("%rarity_color%", rColor);
                finalLore.add(ColorUtils.parse(parsedLine).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(finalLore);
        }

        meta.getPersistentDataContainer().set(manager.BOOK_ID_KEY, PersistentDataType.STRING, enchantId);
        meta.getPersistentDataContainer().set(manager.BOOK_LEVEL_KEY, PersistentDataType.INTEGER, level);
        meta.getPersistentDataContainer().set(manager.BOOK_SUCCESS_KEY, PersistentDataType.INTEGER, successRate);
        meta.getPersistentDataContainer().set(manager.BOOK_DESTROY_KEY, PersistentDataType.INTEGER, destroyRate);
        book.setItemMeta(meta);
        return book;
    }

    public ItemStack createExtractor(String type, int amount) {
        String path = type.toLowerCase() + "-extractor";
        ItemStack item = buildItem(path, "PAPER", amount);
        applyItemMeta(item, path, "Extractor");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.EXTRACTOR_TYPE_KEY, PersistentDataType.STRING, type.toUpperCase());
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSuccessCharm(int bonus, int amount) {
        ItemStack item = buildItem("success-charm", "GLOWSTONE_DUST", amount);
        applyItemMeta(item, "success-charm", "Success Charm", "%bonus%", String.valueOf(bonus));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.CHARM_BONUS_KEY, PersistentDataType.INTEGER, bonus);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createSlotGem(int modifier, int amount) {
        ItemStack item = buildItem("slot-gem", "EMERALD", amount);
        String modStr = (modifier >= 0 ? "+" : "") + modifier;
        applyItemMeta(item, "slot-gem", "Slot Gem", "%modifier%", modStr);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.SLOT_GEM_KEY, PersistentDataType.INTEGER, modifier);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createLockScroll(int amount) {
        ItemStack item = buildItem("lock-scroll", "PAPER", amount);
        applyItemMeta(item, "lock-scroll", "Lock Scroll");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.LOCK_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createPurgeScroll(boolean returnBooks, int amount) {
        ItemStack item = buildItem("purge-scroll", "PAPER", amount);
        applyItemMeta(item, "purge-scroll", "Purge Scroll", "%returns%", returnBooks ? "True" : "False");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.PURGE_SCROLL_KEY, PersistentDataType.BYTE, (byte) 1);
            meta.getPersistentDataContainer().set(manager.PURGE_RETURN_KEY, PersistentDataType.BYTE, (byte) (returnBooks ? 1 : 0));
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createRandomizer(int amount) {
        ItemStack item = buildItem("randomizer-stone", "MAGMA_CREAM", amount);
        applyItemMeta(item, "randomizer-stone", "Randomizer Stone");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.RANDOMIZER_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createProtector(int amount) {
        ItemStack item = buildItem("protection-gem", "NETHER_STAR", amount);
        applyItemMeta(item, "protection-gem", "Protection Gem");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.PROTECTOR_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }

    public ItemStack createTracker(int amount) {
        ItemStack item = buildItem("stat-tracker", "CLOCK", amount);
        applyItemMeta(item, "stat-tracker", "Stat Tracker");
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.getPersistentDataContainer().set(manager.TRACKER_ITEM_KEY, PersistentDataType.BYTE, (byte) 1);
            item.setItemMeta(meta);
        }
        return item;
    }
}
