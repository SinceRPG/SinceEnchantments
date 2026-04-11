package net.danh.sinceenchantments.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.ConfigUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemPacketListener extends PacketListenerAbstract implements PacketListener {

    private static boolean isEnchantableGear(org.bukkit.inventory.ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();
        return name.endsWith("_SWORD") || name.endsWith("_AXE") || name.endsWith("_PICKAXE") ||
                name.endsWith("_SHOVEL") || name.endsWith("_HOE") || name.endsWith("_HELMET") ||
                name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS") ||
                name.equals("BOW") || name.equals("CROSSBOW") || name.equals("TRIDENT") ||
                name.equals("MACE") || name.equals("FISHING_ROD") || name.equals("SHIELD") ||
                name.equals("ELYTRA") || name.equals("SHEARS") || name.equals("FLINT_AND_STEEL") ||
                name.equals("BRUSH") || name.equals("CARROT_ON_A_STICK") || name.equals("WARPED_FUNGUS_ON_A_STICK");
    }

    @Override
    public void onPacketSend(@NonNull PacketSendEvent event) {
        try {
            if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
                ItemStack peItem = wrapper.getItem();
                if (peItem != null && !peItem.isEmpty()) {
                    org.bukkit.inventory.ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
                    bukkitItem = formatSkyblockItem(bukkitItem);
                    wrapper.setItem(SpigotConversionUtil.fromBukkitItemStack(bukkitItem));
                }
            } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
                List<ItemStack> items = wrapper.getItems();
                boolean modified = false;
                for (int i = 0; i < items.size(); i++) {
                    ItemStack peItem = items.get(i);
                    if (peItem != null && !peItem.isEmpty()) {
                        org.bukkit.inventory.ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
                        bukkitItem = formatSkyblockItem(bukkitItem);
                        items.set(i, SpigotConversionUtil.fromBukkitItemStack(bukkitItem));
                        modified = true;
                    }
                }
                if (modified) {
                    wrapper.setItems(items);
                }
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent event) {
        try {
            if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
                WrapperPlayClientCreativeInventoryAction wrapper = new WrapperPlayClientCreativeInventoryAction(event);
                ItemStack peItem = wrapper.getItemStack();
                if (peItem != null && !peItem.isEmpty()) {
                    org.bukkit.inventory.ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
                    bukkitItem = cleanCreativeItem(bukkitItem);
                    wrapper.setItemStack(SpigotConversionUtil.fromBukkitItemStack(bukkitItem));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private org.bukkit.inventory.ItemStack cleanCreativeItem(org.bukkit.inventory.ItemStack item) {
        SinceEnchantments.getInstance().getEnchantManager().cleanItemLore(item);
        if (item.hasItemMeta()) {
            ItemMeta meta = item.getItemMeta();
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private org.bukkit.inventory.ItemStack formatSkyblockItem(org.bukkit.inventory.ItemStack item) {
        EnchantManager manager = SinceEnchantments.getInstance().getEnchantManager();
        manager.cleanItemLore(item);

        if (item == null || item.getType().isAir()) return item;
        boolean hadMetaInitially = item.hasItemMeta();
        ItemMeta meta = hadMetaInitially ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
        if (meta == null) return item;

        ConfigUtils settings = SinceEnchantments.getInstance().getSettingsFile();
        ConfigUtils enchantsConfig = SinceEnchantments.getInstance().getEnchantsFile();

        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        String placeholderStr = settings.getString("settings.placeholder", "#enchants#").toLowerCase();
        int targetIndex = -1;

        for (int i = lore.size() - 1; i >= 0; i--) {
            String plainLore = ColorUtils.toPlainText(lore.get(i)).toLowerCase();
            if (plainLore.contains(placeholderStr)) {
                targetIndex = i;
                lore.remove(i);
                break;
            }
        }

        Map<Enchantment, Integer> vanillaEnchants = meta.getEnchants();
        Map<String, Integer> customEnchants = manager.getCustomEnchants(item);
        boolean overrideVanilla = settings.getBoolean("settings.override-vanilla-enchants", true);

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        boolean hasProtect = pdc.has(manager.PROTECTED_ITEM_KEY, PersistentDataType.BYTE);
        boolean hasTracker = pdc.has(manager.TRACKER_KEY, PersistentDataType.BYTE);
        boolean isLocked = manager.isLocked(item);

        boolean isGear = isEnchantableGear(item);
        boolean hasPlaceholder = (targetIndex != -1);
        boolean hasCustom = !customEnchants.isEmpty();
        boolean hasVanilla = !vanillaEnchants.isEmpty();
        boolean hasWhitelist = !manager.getWhitelistedEnchants(item).isEmpty();
        if (!isGear && !hasPlaceholder && !hasCustom && (!hasVanilla || !overrideVanilla) && !hasWhitelist && !hasProtect && !hasTracker && !isLocked) {
            if (hadMetaInitially) {
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        if (overrideVanilla) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        List<Component> injectComponents = new ArrayList<>();

        if (hasProtect) {
            injectComponents.add(ColorUtils.parse(settings.getString("settings.protected-format", "&a&lProtected &7(Keeps on death)")).decoration(TextDecoration.ITALIC, false));
            injectComponents.add(Component.empty());
        }

        if (hasTracker) {
            injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-header", "&8&m      &r &6&lStat Tracker &8&m      ")).decoration(TextDecoration.ITALIC, false));
            if (pdc.has(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER)) {
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-blocks").replace("%value%", String.valueOf(pdc.get(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
            }
            if (pdc.has(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER)) {
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-mobs").replace("%value%", String.valueOf(pdc.get(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
            }
            if (pdc.has(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER)) {
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-players").replace("%value%", String.valueOf(pdc.get(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
            }
            if (pdc.has(manager.STAT_FISH_KEY, PersistentDataType.INTEGER)) {
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-fish").replace("%value%", String.valueOf(pdc.get(manager.STAT_FISH_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
            }
            injectComponents.add(Component.empty());
        }

        int detailedThreshold = settings.getInt("settings.detailed-display-threshold", 5);
        int totalEnchantsApplied = (overrideVanilla ? vanillaEnchants.size() : 0) + customEnchants.size();
        boolean useDetailedDisplay = totalEnchantsApplied > 0 && totalEnchantsApplied <= detailedThreshold;

        int count = 0;
        int maxPerLine = settings.getInt("settings.enchants-per-line", 2);
        String separator = settings.getString("settings.separator", "&8 | ");
        StringBuilder currentLine = new StringBuilder();
        boolean useRoman = settings.getString("settings.level-format", "ROMAN").equalsIgnoreCase("ROMAN");

        if (overrideVanilla) {
            for (Map.Entry<Enchantment, Integer> entry : vanillaEnchants.entrySet()) {
                String fullKey = entry.getKey().getKey().getNamespace() + ":" + entry.getKey().getKey().getKey();
                String cName = enchantsConfig.getString("vanilla-enchants." + fullKey + ".name", formatDefaultName(entry.getKey().getKey().getKey()));
                String cColor = enchantsConfig.getString("vanilla-enchants." + fullKey + ".color", settings.getString("settings.default-color", "&9"));
                String formatted = cColor + cName + " " + (useRoman ? toRoman(entry.getValue()) : entry.getValue());

                if (useDetailedDisplay) {
                    injectComponents.add(ColorUtils.parse(formatted).decoration(TextDecoration.ITALIC, false));
                    for (String dLine : manager.getDescription(fullKey)) {
                        injectComponents.add(ColorUtils.parse(dLine).decoration(TextDecoration.ITALIC, false));
                    }
                } else {
                    if (count > 0) currentLine.append(separator);
                    currentLine.append(formatted);
                    count++;
                    if (count == maxPerLine) {
                        injectComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
                        currentLine = new StringBuilder();
                        count = 0;
                    }
                }
            }
            if (!vanillaEnchants.isEmpty() && !customEnchants.isEmpty() && !useDetailedDisplay) {
                if (count > 0) {
                    injectComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
                    currentLine = new StringBuilder();
                    count = 0;
                }
                injectComponents.add(ColorUtils.parse(settings.getString("settings.divider", "&7&m----------------------")).decoration(TextDecoration.ITALIC, false));
            }
        }

        for (Map.Entry<String, Integer> entry : customEnchants.entrySet()) {
            String eId = entry.getKey();
            int eLvl = entry.getValue();

            String eName = enchantsConfig.getString("custom-enchants." + eId + ".name", eId);
            String rarityKey = enchantsConfig.getString("custom-enchants." + eId + ".rarity", "COMMON");
            String rarityColor = settings.getString("rarities." + rarityKey, "&f");

            String formatted = rarityColor + eName + " " + (useRoman ? toRoman(eLvl) : eLvl);

            if (useDetailedDisplay) {
                injectComponents.add(ColorUtils.parse(formatted).decoration(TextDecoration.ITALIC, false));
                for (String dLine : manager.getDescription(eId)) {
                    injectComponents.add(ColorUtils.parse(dLine).decoration(TextDecoration.ITALIC, false));
                }
            } else {
                if (count > 0) currentLine.append(separator);
                currentLine.append(formatted);
                count++;
                if (count == maxPerLine) {
                    injectComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
                    currentLine = new StringBuilder();
                    count = 0;
                }
            }
        }

        if (!useDetailedDisplay && count > 0) {
            injectComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
        }

        if (settings.getBoolean("settings.show-slots", true)) {
            int maxSlots = manager.getMaxSlots(item);
            String slotLine = settings.getString("settings.slots-format", "&7Enchantment Slots: &e%current% / %max%");
            slotLine = slotLine.replace("%current%", String.valueOf(totalEnchantsApplied)).replace("%max%", String.valueOf(maxSlots));
            injectComponents.add(ColorUtils.parse(slotLine).decoration(TextDecoration.ITALIC, false));
        }

        if (isLocked) {
            injectComponents.add(ColorUtils.parse(settings.getString("settings.locked-format", "&c&lLocked")).decoration(TextDecoration.ITALIC, false));
        }

        if (settings.getBoolean("settings.show-whitelist-preview", true)) {
            List<String> allowedEnchants = manager.getWhitelistedEnchants(item);
            List<String> unappliedAllowed = new ArrayList<>();
            for (String allowedId : allowedEnchants) {
                boolean applied = customEnchants.containsKey(allowedId);
                if (!applied) {
                    for (Enchantment vEnch : vanillaEnchants.keySet()) {
                        if ((vEnch.getKey().getNamespace() + ":" + vEnch.getKey().getKey()).equals(allowedId)) {
                            applied = true;
                            break;
                        }
                    }
                }
                if (!applied) unappliedAllowed.add(allowedId);
            }

            if (!unappliedAllowed.isEmpty()) {
                injectComponents.add(Component.empty());
                injectComponents.add(ColorUtils.parse(settings.getString("settings.whitelist-header", "&8Allowed:")).decoration(TextDecoration.ITALIC, false));
                String format = settings.getString("settings.whitelist-preview-format", "&8 - %enchant_name%");
                for (String allowedId : unappliedAllowed) {
                    injectComponents.add(ColorUtils.parse(format.replace("%enchant_name%", manager.getEnchantName(allowedId))).decoration(TextDecoration.ITALIC, false));
                }
            }
        }
        if (injectComponents.isEmpty()) {
            if (hadMetaInitially) {
                meta.lore(lore);
                item.setItemMeta(meta);
            }
            return item;
        }

        boolean addEmptyLineAbove = settings.getBoolean("settings.add-empty-line-above", true);
        boolean addEmptyLineBelow = settings.getBoolean("settings.add-empty-line-below", true);

        if (hasPlaceholder) {
            if (addEmptyLineAbove && targetIndex > 0) {
                if (!ColorUtils.toPlainText(lore.get(targetIndex - 1)).trim().isEmpty())
                    injectComponents.add(0, Component.empty());
            }
            if (addEmptyLineBelow && targetIndex < lore.size()) {
                if (!ColorUtils.toPlainText(lore.get(targetIndex)).trim().isEmpty())
                    injectComponents.add(Component.empty());
            }
        } else if (addEmptyLineAbove && !lore.isEmpty()) {
            if (!ColorUtils.toPlainText(lore.get(lore.size() - 1)).trim().isEmpty())
                injectComponents.add(0, Component.empty());
        }

        int startIdx = hasPlaceholder ? targetIndex : lore.size();
        if (hasPlaceholder) lore.addAll(targetIndex, injectComponents);
        else lore.addAll(injectComponents);

        pdc.set(new NamespacedKey(SinceEnchantments.getInstance(), "lore_start"), PersistentDataType.INTEGER, startIdx);
        pdc.set(new NamespacedKey(SinceEnchantments.getInstance(), "lore_count"), PersistentDataType.INTEGER, injectComponents.size());
        pdc.set(new NamespacedKey(SinceEnchantments.getInstance(), "lore_placeholder"), PersistentDataType.BYTE, (byte) (hasPlaceholder ? 1 : 0));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatDefaultName(String rawName) {
        String name = rawName.replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty())
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
        }
        return sb.toString().trim();
    }

    private String toRoman(int number) {
        String[] roman = {"O", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        return (number > 0 && number < roman.length) ? roman[number] : String.valueOf(number);
    }
}