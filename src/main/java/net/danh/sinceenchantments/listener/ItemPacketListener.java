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
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.ConfigUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemPacketListener extends PacketListenerAbstract implements PacketListener {

    private static final String MARKER = "     ";
    private static final String EMPTY_MARKER = "      ";

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
                for (int i = 0; i < items.size(); i++) {
                    ItemStack peItem = items.get(i);
                    if (peItem != null && !peItem.isEmpty()) {
                        org.bukkit.inventory.ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem);
                        bukkitItem = formatSkyblockItem(bukkitItem);
                        items.set(i, SpigotConversionUtil.fromBukkitItemStack(bukkitItem));
                    }
                }
                wrapper.setItems(items);
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
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();
        boolean changed = false;

        for (int i = lore.size() - 1; i >= 0; i--) {
            String plain = ColorUtils.toPlainText(lore.get(i));
            if (plain.endsWith(MARKER) || plain.equals(EMPTY_MARKER)) {
                lore.remove(i);
                changed = true;
            }
        }

        if (changed) {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private org.bukkit.inventory.ItemStack formatSkyblockItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        ConfigUtils config = SinceEnchantments.getInstance().getConfigFile();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        String placeholderStr = config.getString("settings.placeholder", "#enchants#").toLowerCase();
        int targetIndex = -1;

        for (int i = lore.size() - 1; i >= 0; i--) {
            String plainLore = ColorUtils.toPlainText(lore.get(i));
            String plainLoreLower = plainLore.toLowerCase();
            if (plainLoreLower.contains(placeholderStr) || plainLore.endsWith(MARKER) || plainLore.equals(EMPTY_MARKER)) {
                targetIndex = i;
                lore.remove(i);
            }
        }

        Map<Enchantment, Integer> vanillaEnchants = meta.getEnchants();
        Map<String, Integer> customEnchants = SinceEnchantments.getInstance().getEnchantManager().getCustomEnchants(item);

        if (vanillaEnchants.isEmpty() && customEnchants.isEmpty()) {
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<Component> enchantComponents = new ArrayList<>();
        int count = 0;
        int maxPerLine = config.getInt("settings.enchants-per-line", 2);
        String separator = config.getString("settings.separator", "&8 | ");
        StringBuilder currentLine = new StringBuilder();
        boolean useRoman = config.getString("settings.level-format", "ROMAN").equalsIgnoreCase("ROMAN");

        for (Map.Entry<Enchantment, Integer> entry : vanillaEnchants.entrySet()) {
            String keyName = entry.getKey().getKey().getKey();
            String cName = config.getString("vanilla-enchants.minecraft:" + keyName + ".name", formatDefaultName(keyName));
            String cColor = config.getString("vanilla-enchants.minecraft:" + keyName + ".color", config.getString("settings.default-color", "&9"));

            String formatted = cColor + cName + " " + (useRoman ? toRoman(entry.getValue()) : entry.getValue());
            if (count > 0) currentLine.append(separator);
            currentLine.append(formatted);
            count++;

            if (count == maxPerLine) {
                enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false).append(Component.text(MARKER)));
                currentLine = new StringBuilder();
                count = 0;
            }
        }

        if (!vanillaEnchants.isEmpty() && !customEnchants.isEmpty()) {
            if (count > 0) {
                enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false).append(Component.text(MARKER)));
                currentLine = new StringBuilder();
                count = 0;
            }
            String divider = config.getString("settings.divider", "&7&m----------------------");
            enchantComponents.add(ColorUtils.parse(divider).decoration(TextDecoration.ITALIC, false).append(Component.text(MARKER)));
        }

        for (Map.Entry<String, Integer> entry : customEnchants.entrySet()) {
            String eId = entry.getKey();
            int eLvl = entry.getValue();

            String eName = config.getString("custom-enchants." + eId + ".name", eId);
            String rarityKey = config.getString("custom-enchants." + eId + ".rarity", "COMMON");
            String rarityColor = config.getString("rarities." + rarityKey, "&f");

            String formatted = rarityColor + eName + " " + (useRoman ? toRoman(eLvl) : eLvl);
            if (count > 0) currentLine.append(separator);
            currentLine.append(formatted);
            count++;

            if (count == maxPerLine) {
                enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false).append(Component.text(MARKER)));
                currentLine = new StringBuilder();
                count = 0;
            }
        }

        if (count > 0) {
            enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false).append(Component.text(MARKER)));
        }

        boolean addEmptyLineAbove = config.getBoolean("settings.add-empty-line-above", true);
        boolean addEmptyLineBelow = config.getBoolean("settings.add-empty-line-below", true);

        if (targetIndex != -1) {
            if (addEmptyLineAbove && targetIndex > 0) {
                String abovePlain = ColorUtils.toPlainText(lore.get(targetIndex - 1)).trim();
                if (!abovePlain.isEmpty()) enchantComponents.add(0, Component.text(EMPTY_MARKER));
            }
            if (addEmptyLineBelow && targetIndex < lore.size()) {
                String belowPlain = ColorUtils.toPlainText(lore.get(targetIndex)).trim();
                if (!belowPlain.isEmpty()) enchantComponents.add(Component.text(EMPTY_MARKER));
            }
            lore.addAll(targetIndex, enchantComponents);
        } else {
            if (!lore.isEmpty()) {
                if (addEmptyLineAbove) {
                    String abovePlain = ColorUtils.toPlainText(lore.get(0)).trim();
                    if (!abovePlain.isEmpty()) enchantComponents.add(0, Component.text(EMPTY_MARKER));
                }
                if (addEmptyLineBelow && lore.size() > 1) {
                    String belowPlain = ColorUtils.toPlainText(lore.get(1)).trim();
                    if (!belowPlain.isEmpty()) enchantComponents.add(Component.text(EMPTY_MARKER));
                }
                lore.addAll(1, enchantComponents);
            } else {
                lore.addAll(enchantComponents);
            }
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private String formatDefaultName(String rawName) {
        String name = rawName.replace("_", " ");
        String[] words = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (!w.isEmpty()) {
                sb.append(Character.toUpperCase(w.charAt(0))).append(w.substring(1).toLowerCase()).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String toRoman(int number) {
        String[] roman = {"O", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        if (number > 0 && number < roman.length) return roman[number];
        return String.valueOf(number);
    }
}