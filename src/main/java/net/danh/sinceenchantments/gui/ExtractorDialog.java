package net.danh.sinceenchantments.gui;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings({"UnstableApiUsage", "SpellCheckingInspection"})
public class ExtractorDialog {

    public static void open(SinceEnchantments plugin, Player player, ItemStack weapon) {
        String titleRaw = plugin.getConfigFile().getString("dialog.extractor.title", "<dark_gray><bold>Extract Enchantment");
        String bodyRaw = plugin.getConfigFile().getString("dialog.extractor.body", "<gray>Choose an enchantment below:");

        Component title = ColorUtils.parse(titleRaw);
        Component body = ColorUtils.parse(bodyRaw);

        // Lấy toàn bộ enchant đang có trên vũ khí
        Map<String, Integer> allEnchants = plugin.getEnchantManager().getAllEnchantsOnItem(weapon);
        List<ActionButton> buttons = new ArrayList<>();

        String btnFormat = plugin.getConfigFile().getString("dialog.extractor.enchant-button.name", "%rarity_color%%enchant_name% %level%");
        String tooltipFormat = plugin.getConfigFile().getString("dialog.extractor.enchant-button.tooltip", "&7Extract %enchant_name%");

        for (Map.Entry<String, Integer> entry : allEnchants.entrySet()) {
            String id = entry.getKey();
            int level = entry.getValue();

            String name = plugin.getEnchantManager().getEnchantName(id);
            String rarity = plugin.getEnchantManager().getRarity(id);
            String color = plugin.getConfigFile().getString("rarities." + rarity, "&f");

            String parsedName = btnFormat.replace("%enchant_name%", name)
                    .replace("%level%", String.valueOf(level))
                    .replace("%rarity_name%", rarity)
                    .replace("%rarity_color%", color);

            String parsedTooltip = tooltipFormat.replace("%enchant_name%", name)
                    .replace("%level%", String.valueOf(level))
                    .replace("%rarity_name%", rarity)
                    .replace("%rarity_color%", color);

            // Nút xử lý khi người chơi bấm chọn rút Enchant
            DialogAction action = DialogAction.customClick((view, audience) -> {
                audience.closeDialog(); // Đóng dialog ngay lập tức

                if (audience instanceof Player p) {
                    // Kiểm tra lại lần cuối để tránh lỗi nếu item bị thay đổi ngoài ý muốn
                    Map<String, Integer> verify = plugin.getEnchantManager().getAllEnchantsOnItem(weapon);
                    if (!verify.containsKey(id)) {
                        String prefix = plugin.getMessagesFile().getString("prefix", "");
                        String msg = plugin.getMessagesFile().getString("extract-error-gone", "");
                        p.sendMessage(ColorUtils.parse(prefix + msg));
                        return;
                    }

                    // Thực thi logic xoá enchant và đưa sách cho người chơi
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

        // Nút Cancel & Refund
        String cancelName = plugin.getConfigFile().getString("dialog.extractor.cancel-button.name", "&cCancel");
        String cancelTooltip = plugin.getConfigFile().getString("dialog.extractor.cancel-button.tooltip", "&7Refund extractor");

        DialogAction cancelAction = DialogAction.customClick((view, audience) -> {
            audience.closeDialog(); // Đóng dialog

            if (audience instanceof Player p) {
                // Trả lại Extractor vì người chơi đã huỷ
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

        int columns = plugin.getConfigFile().getInt("dialog.extractor.columns", 3);

        // Khởi tạo và xây dựng Dialog
        Dialog dialog = Dialog.create(builder -> builder.empty()
                .base(DialogBase.builder(title)
                        .canCloseWithEscape(false) // Bắt buộc người chơi phải nhấn chọn hoặc nhấn Cancel
                        .body(List.<DialogBody>of(
                                DialogBody.plainMessage(body),
                                DialogBody.item(weapon).build() // Đã thêm .build() để lấy DialogBody chuẩn
                        ))
                        .build()
                )
                .type(DialogType.multiAction(buttons, exitAction, columns))
        );

        // Hiển thị cho người chơi
        player.showDialog(dialog);
    }
}