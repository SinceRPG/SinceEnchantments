package net.danh.sinceenchantments.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ItemPacketListener extends PacketListenerAbstract implements PacketListener {

    // Sử dụng 5 dấu cách làm marker tàng hình cực kỳ an toàn, không bao giờ lỗi font ô vuông
    private static final String MARKER = "     ";
    // Marker dành riêng cho dòng trống để lúc quét xóa không bị dư thừa
    private static final String EMPTY_MARKER = "      "; // 6 dấu cách

    @Override
    public void onPacketSend(PacketSendEvent event) {
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
    }

    private org.bukkit.inventory.ItemStack formatSkyblockItem(org.bukkit.inventory.ItemStack item) {
        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        ConfigUtils config = SinceEnchantments.getInstance().getConfigFile();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        String placeholderStr = config.getString("settings.placeholder", "#enchants#");
        int targetIndex = -1;

        // Quét ngược từ dưới lên để xóa Lore cũ HOẶC Placeholder và tìm đúng vị trí
        for (int i = lore.size() - 1; i >= 0; i--) {
            String plainLore = ColorUtils.toPlainText(lore.get(i));

            // Nếu dòng này là #enchants# HOẶC là dòng enchant cũ của plugin HOẶC là dòng trống cũ của plugin
            if (plainLore.contains(placeholderStr) || plainLore.endsWith(MARKER) || plainLore.equals(EMPTY_MARKER)) {
                targetIndex = i; // Do duyệt ngược, targetIndex cuối cùng sẽ là vị trí cao nhất (dòng đầu tiên) của block bị xóa
                lore.remove(i);
            }
        }

        Map<Enchantment, Integer> enchants = meta.getEnchants();

        // 1. Không có enchant: Trả về luôn
        if (enchants.isEmpty()) {
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        // 2. Có enchant: Ẩn enchant mặc định
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);

        List<Component> enchantComponents = new ArrayList<>();
        StringBuilder currentLine = new StringBuilder();
        int count = 0;
        int maxPerLine = config.getInt("settings.enchants-per-line", 3);
        String separator = config.getString("settings.separator", "&8, ");
        String defaultColor = config.getString("settings.default-color", "&9");
        boolean addEmptyLineAbove = config.getBoolean("settings.add-empty-line-above", true);
        boolean addEmptyLineBelow = config.getBoolean("settings.add-empty-line-below", true);

        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            Enchantment ench = entry.getKey();
            int level = entry.getValue();

            String enchantKey = ench.getKey().toString();
            String rawKeyName = ench.getKey().getKey();

            String pathName = "custom-enchants." + enchantKey + ".name";
            String pathColor = "custom-enchants." + enchantKey + ".color";

            String customName = config.getString(pathName, formatDefaultName(rawKeyName));
            String customColor = config.getString(pathColor, defaultColor);
            String levelStr = toRoman(level);

            String formattedEnchant = customColor + customName + " " + levelStr;

            if (count > 0) {
                currentLine.append(separator);
            }
            currentLine.append(formattedEnchant);
            count++;

            // Dùng ColorUtils.parse() và chèn thêm 5 DẤU CÁCH (MARKER) vào cuối
            if (count == maxPerLine) {
                enchantComponents.add(
                        ColorUtils.parse(currentLine.toString())
                                .decoration(TextDecoration.ITALIC, false)
                                .append(Component.text(MARKER))
                );
                currentLine = new StringBuilder();
                count = 0;
            }
        }

        if (count > 0) {
            enchantComponents.add(
                    ColorUtils.parse(currentLine.toString())
                            .decoration(TextDecoration.ITALIC, false)
                            .append(Component.text(MARKER))
            );
        }

        // Xử lý chèn vào lore (Kiểm tra xem dòng trên/dưới có trống hay không)
        if (targetIndex != -1) {
            // Kiểm tra dòng ở trên
            if (addEmptyLineAbove && targetIndex > 0) {
                String abovePlain = ColorUtils.toPlainText(lore.get(targetIndex - 1)).trim();
                if (!abovePlain.isEmpty()) { // Chỉ chèn nếu dòng trên có nội dung (không phải dòng trống)
                    enchantComponents.add(0, Component.text(EMPTY_MARKER));
                }
            }

            // Kiểm tra dòng ở dưới
            if (addEmptyLineBelow && targetIndex < lore.size()) {
                String belowPlain = ColorUtils.toPlainText(lore.get(targetIndex)).trim();
                if (!belowPlain.isEmpty()) { // Chỉ chèn nếu dòng dưới có nội dung (không phải dòng trống)
                    enchantComponents.add(Component.text(EMPTY_MARKER));
                }
            }

            lore.addAll(targetIndex, enchantComponents);
        } else {
            // Nếu không có placeholder (đồ vanilla), mặc định chèn vào sau dòng 1 (nếu có)
            if (lore.size() >= 1) {
                if (addEmptyLineAbove) {
                    String abovePlain = ColorUtils.toPlainText(lore.get(0)).trim();
                    if (!abovePlain.isEmpty()) {
                        enchantComponents.add(0, Component.text(EMPTY_MARKER));
                    }
                }

                if (addEmptyLineBelow && lore.size() > 1) {
                    String belowPlain = ColorUtils.toPlainText(lore.get(1)).trim();
                    if (!belowPlain.isEmpty()) {
                        enchantComponents.add(Component.text(EMPTY_MARKER));
                    }
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
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)))
                        .append(w.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String toRoman(int number) {
        String[] roman = {"O", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X",
                "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        if (number > 0 && number < roman.length) {
            return roman[number];
        }
        return String.valueOf(number);
    }
}