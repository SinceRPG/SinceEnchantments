package net.danh.sinceenchantments.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.ConfigUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage"})
public class ExtractorDialog {

    public static void open(SinceEnchantments plugin, Player player, ItemStack weapon) {
        String titleRaw = plugin.getGUIFile().getString("dialog.extractor.title", "<dark_gray><bold>Extract Enchantment");
        String bodyRaw = plugin.getGUIFile().getString("dialog.extractor.body", "<gray>Choose an enchantment below:");

        Component title = ColorUtils.parse(titleRaw);
        Component body = ColorUtils.parse(bodyRaw);

        Map<String, Integer> allEnchants = plugin.getEnchantManager().getAllEnchantsOnItem(weapon);
        List<ActionButton> buttons = new ArrayList<>();

        String btnFormat = plugin.getGUIFile().getString("dialog.extractor.enchant-button.name", "%rarity_color%%enchant_name% %level%");
        String tooltipFormat = plugin.getGUIFile().getString("dialog.extractor.enchant-button.tooltip", "&7Extract %enchant_name%");

        for (Map.Entry<String, Integer> entry : allEnchants.entrySet()) {
            String id = entry.getKey();
            int level = entry.getValue();

            String name = plugin.getEnchantManager().getEnchantName(id);
            String rarity = plugin.getEnchantManager().getRarity(id);
            String color = plugin.getSettingsFile().getString("rarities." + rarity, "&f");

            String parsedName = btnFormat.replace("%enchant_name%", name)
                    .replace("%level%", String.valueOf(level))
                    .replace("%rarity_name%", rarity)
                    .replace("%rarity_color%", color);

            String parsedTooltip = tooltipFormat.replace("%enchant_name%", name)
                    .replace("%level%", String.valueOf(level))
                    .replace("%rarity_name%", rarity)
                    .replace("%rarity_color%", color);

            DialogAction action = DialogAction.customClick((view, audience) -> {
                audience.closeDialog();
                if (audience instanceof Player p) {
                    Map<String, Integer> verify = plugin.getEnchantManager().getAllEnchantsOnItem(weapon);
                    if (!verify.containsKey(id)) {
                        String prefix = plugin.getMessagesFile().getString("prefix", "");
                        String msg = plugin.getMessagesFile().getString("extract-error-gone", "");
                        p.sendMessage(ColorUtils.parse(prefix + msg));
                        return;
                    }

                    plugin.getEnchantManager().removeEnchant(weapon, id);
                    ItemStack book = plugin.getEnchantManager().createEnchantBook(id, level, 100, 0);

                    if (!p.getInventory().addItem(book).isEmpty()) {
                        p.getWorld().dropItem(p.getLocation(), book);
                    }

                    p.playSound(p.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1f, 1f);

                    String prefix = plugin.getMessagesFile().getString("prefix", "");
                    String msg = plugin.getMessagesFile().getString("extract-success", "")
                            .replace("%enchant%", name)
                            .replace("%level%", String.valueOf(level));
                    p.sendMessage(ColorUtils.parse(prefix + msg));
                }
            }, ClickCallback.Options.builder().uses(1).build());

            buttons.add(ActionButton.builder(ColorUtils.parse(parsedName))
                    .tooltip(ColorUtils.parse(parsedTooltip))
                    .action(action)
                    .build());
        }

        String cancelName = plugin.getGUIFile().getString("dialog.extractor.cancel-button.name", "&cCancel");
        String cancelTooltip = plugin.getGUIFile().getString("dialog.extractor.cancel-button.tooltip", "&7Refund extractor");

        DialogAction cancelAction = DialogAction.customClick((view, audience) -> {
            audience.closeDialog();
            if (audience instanceof Player p) {
                ItemStack refund = plugin.getEnchantManager().createExtractor("specific", 1);
                if (!p.getInventory().addItem(refund).isEmpty()) {
                    p.getWorld().dropItem(p.getLocation(), refund);
                }

                String prefix = plugin.getMessagesFile().getString("prefix", "");
                String msg = plugin.getMessagesFile().getString("extract-cancelled", "");
                p.sendMessage(ColorUtils.parse(prefix + msg));
            }
        }, ClickCallback.Options.builder().uses(1).build());

        ActionButton exitAction = ActionButton.builder(ColorUtils.parse(cancelName))
                .tooltip(ColorUtils.parse(cancelTooltip))
                .action(cancelAction)
                .build();

        int columns = plugin.getGUIFile().getInt("dialog.extractor.columns", 3);
        ItemStack displayWeapon = formatDisplayWeapon(plugin, weapon.clone());

        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .canCloseWithEscape(false)
                        .body(List.<DialogBody>of(
                                DialogBody.plainMessage(body),
                                DialogBody.item(displayWeapon).build()
                        ))
                        .build()
                )
                .type(DialogType.multiAction(buttons, exitAction, columns))
        );

        player.showDialog(dialog);
    }

    private static ItemStack formatDisplayWeapon(SinceEnchantments plugin, ItemStack item) {
        plugin.getEnchantManager().cleanItemLore(item);

        if (item == null || !item.hasItemMeta()) return item;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        ConfigUtils config = plugin.getSettingsFile();
        ConfigUtils enchantsConfig = plugin.getEnchantsFile();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        String placeholderStr = config.getString("settings.placeholder", "#enchants#").toLowerCase();
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
        Map<String, Integer> customEnchants = plugin.getEnchantManager().getCustomEnchants(item);
        boolean overrideVanilla = config.getBoolean("settings.override-vanilla-enchants", true);

        if (((!overrideVanilla && customEnchants.isEmpty()) || (overrideVanilla && vanillaEnchants.isEmpty() && customEnchants.isEmpty()))
                && !plugin.getEnchantManager().isLocked(item)
                && plugin.getEnchantManager().getWhitelistedEnchants(item).isEmpty()) {

            if (targetIndex != -1) {
                lore.add(targetIndex, ColorUtils.parse(config.getString("settings.placeholder", "#enchants#")).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        if (overrideVanilla) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }

        List<Component> enchantComponents = new ArrayList<>();
        int detailedThreshold = config.getInt("settings.detailed-display-threshold", 5);
        int totalEnchantsApplied = (overrideVanilla ? vanillaEnchants.size() : 0) + customEnchants.size();
        boolean useDetailedDisplay = totalEnchantsApplied > 0 && totalEnchantsApplied <= detailedThreshold;

        int count = 0;
        int maxPerLine = config.getInt("settings.enchants-per-line", 2);
        String separator = config.getString("settings.separator", "&8 | ");
        StringBuilder currentLine = new StringBuilder();
        boolean useRoman = config.getString("settings.level-format", "ROMAN").equalsIgnoreCase("ROMAN");

        if (overrideVanilla) {
            for (Map.Entry<Enchantment, Integer> entry : vanillaEnchants.entrySet()) {
                String namespace = entry.getKey().getKey().getNamespace();
                String keyName = entry.getKey().getKey().getKey();
                String fullKey = namespace + ":" + keyName;

                String cName = enchantsConfig.getString("vanilla-enchants." + fullKey + ".name", formatDefaultName(keyName));
                String cColor = enchantsConfig.getString("vanilla-enchants." + fullKey + ".color", config.getString("settings.default-color", "&9"));

                String formatted = cColor + cName + " " + (useRoman ? toRoman(entry.getValue()) : entry.getValue());

                if (useDetailedDisplay) {
                    enchantComponents.add(ColorUtils.parse(formatted).decoration(TextDecoration.ITALIC, false));
                    List<String> descriptions = plugin.getEnchantManager().getDescription(fullKey);
                    for (String dLine : descriptions) {
                        enchantComponents.add(ColorUtils.parse(dLine).decoration(TextDecoration.ITALIC, false));
                    }
                } else {
                    if (count > 0) currentLine.append(separator);
                    currentLine.append(formatted);
                    count++;

                    if (count == maxPerLine) {
                        enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
                        currentLine = new StringBuilder();
                        count = 0;
                    }
                }
            }

            if (!vanillaEnchants.isEmpty() && !customEnchants.isEmpty() && !useDetailedDisplay) {
                if (count > 0) {
                    enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
                    currentLine = new StringBuilder();
                    count = 0;
                }
                String divider = config.getString("settings.divider", "&7&m----------------------");
                enchantComponents.add(ColorUtils.parse(divider).decoration(TextDecoration.ITALIC, false));
            }
        }

        for (Map.Entry<String, Integer> entry : customEnchants.entrySet()) {
            String eId = entry.getKey();
            int eLvl = entry.getValue();

            String eName = enchantsConfig.getString("custom-enchants." + eId + ".name", eId);
            String rarityKey = enchantsConfig.getString("custom-enchants." + eId + ".rarity", "COMMON");
            String rarityColor = config.getString("rarities." + rarityKey, "&f");

            String formatted = rarityColor + eName + " " + (useRoman ? toRoman(eLvl) : eLvl);

            if (useDetailedDisplay) {
                enchantComponents.add(ColorUtils.parse(formatted).decoration(TextDecoration.ITALIC, false));
                List<String> descriptions = plugin.getEnchantManager().getDescription(eId);
                for (String dLine : descriptions) {
                    enchantComponents.add(ColorUtils.parse(dLine).decoration(TextDecoration.ITALIC, false));
                }
            } else {
                if (count > 0) currentLine.append(separator);
                currentLine.append(formatted);
                count++;

                if (count == maxPerLine) {
                    enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
                    currentLine = new StringBuilder();
                    count = 0;
                }
            }
        }

        if (!useDetailedDisplay && count > 0) {
            enchantComponents.add(ColorUtils.parse(currentLine.toString()).decoration(TextDecoration.ITALIC, false));
        }

        if (config.getBoolean("settings.show-slots", true)) {
            int maxSlots = plugin.getEnchantManager().getMaxSlots(item);
            String slotLine = config.getString("settings.slots-format", "&7Enchantment Slots: &e%current% / %max%");
            slotLine = slotLine.replace("%current%", String.valueOf(totalEnchantsApplied)).replace("%max%", String.valueOf(maxSlots));
            enchantComponents.add(ColorUtils.parse(slotLine).decoration(TextDecoration.ITALIC, false));
        }

        if (plugin.getEnchantManager().isLocked(item)) {
            String lockLine = config.getString("settings.locked-format", "&c&lLocked");
            enchantComponents.add(ColorUtils.parse(lockLine).decoration(TextDecoration.ITALIC, false));
        }

        if (config.getBoolean("settings.show-whitelist-preview", true)) {
            List<String> allowedEnchants = plugin.getEnchantManager().getWhitelistedEnchants(item);
            List<String> unappliedAllowed = new ArrayList<>();
            for (String allowedId : allowedEnchants) {
                boolean applied = false;
                if (customEnchants.containsKey(allowedId)) {
                    applied = true;
                } else {
                    for (Enchantment vEnch : vanillaEnchants.keySet()) {
                        String fullKey = vEnch.getKey().getNamespace() + ":" + vEnch.getKey().getKey();
                        if (fullKey.equals(allowedId)) {
                            applied = true;
                            break;
                        }
                    }
                }
                if (!applied) {
                    unappliedAllowed.add(allowedId);
                }
            }

            if (!unappliedAllowed.isEmpty()) {
                enchantComponents.add(Component.empty());
                String header = config.getString("settings.whitelist-header", "&8Allowed Enchantments:");
                enchantComponents.add(ColorUtils.parse(header).decoration(TextDecoration.ITALIC, false));
                String format = config.getString("settings.whitelist-preview-format", "&8 - %enchant_name%");
                for (String allowedId : unappliedAllowed) {
                    String eName = plugin.getEnchantManager().getEnchantName(allowedId);
                    String line = format.replace("%enchant_name%", eName);
                    enchantComponents.add(ColorUtils.parse(line).decoration(TextDecoration.ITALIC, false));
                }
            }
        }

        if (enchantComponents.isEmpty()) {
            if (targetIndex != -1) {
                lore.add(targetIndex, ColorUtils.parse(config.getString("settings.placeholder", "#enchants#")).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
            return item;
        }

        boolean addEmptyLineAbove = config.getBoolean("settings.add-empty-line-above", true);
        boolean addEmptyLineBelow = config.getBoolean("settings.add-empty-line-below", true);
        boolean hadPlaceholder = (targetIndex != -1);

        if (hadPlaceholder) {
            if (addEmptyLineAbove && targetIndex > 0) {
                String abovePlain = ColorUtils.toPlainText(lore.get(targetIndex - 1)).trim();
                if (!abovePlain.isEmpty()) enchantComponents.add(0, Component.empty());
            }
            if (addEmptyLineBelow && targetIndex < lore.size()) {
                String belowPlain = ColorUtils.toPlainText(lore.get(targetIndex)).trim();
                if (!belowPlain.isEmpty()) enchantComponents.add(Component.empty());
            }
        } else {
            if (!lore.isEmpty()) {
                if (addEmptyLineAbove) {
                    String abovePlain = ColorUtils.toPlainText(lore.get(lore.size() - 1)).trim();
                    if (!abovePlain.isEmpty()) enchantComponents.add(0, Component.empty());
                }
            }
        }

        int startIdx;
        if (hadPlaceholder) {
            lore.addAll(targetIndex, enchantComponents);
            startIdx = targetIndex;
        } else {
            startIdx = lore.size();
            lore.addAll(enchantComponents);
        }

        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(new NamespacedKey(plugin, "lore_start"), PersistentDataType.INTEGER, startIdx);
        pdc.set(new NamespacedKey(plugin, "lore_count"), PersistentDataType.INTEGER, enchantComponents.size());
        pdc.set(new NamespacedKey(plugin, "lore_placeholder"), PersistentDataType.BYTE, (byte) (hadPlaceholder ? 1 : 0));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private static String formatDefaultName(String rawName) {
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

    private static String toRoman(int number) {
        String[] roman = {"O", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        if (number > 0 && number < roman.length) return roman[number];
        return String.valueOf(number);
    }
}