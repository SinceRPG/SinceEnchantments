package net.danh.sinceenchantments.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SinceCommand {

    private final SinceEnchantments plugin;

    public SinceCommand(SinceEnchantments plugin) {
        this.plugin = plugin;
    }

    private void sendMessage(CommandSourceStack source, String path, String... replacements) {
        String prefix = plugin.getMessagesFile().getString("prefix", "");
        String msg = plugin.getMessagesFile().getString(path, "");
        for (int i = 0; i < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        source.getSender().sendMessage(ColorUtils.parse(prefix + msg));
    }

    public LiteralCommandNode<CommandSourceStack> buildCommand() {
        return Commands.literal("sinceenchantments")
                .requires(source -> source.getSender().hasPermission("sinceenchantments.admin"))

                // LỆNH: /se reload
                .then(Commands.literal("reload")
                        .executes(context -> {
                            plugin.getConfigFile().reload();
                            plugin.getMessagesFile().reload();
                            plugin.getItemsFile().reload();
                            plugin.getEnchantManager().loadEnchantsFromConfig();
                            sendMessage(context.getSource(), "reload");
                            return Command.SINGLE_SUCCESS;
                        })
                )

                // LỆNH: /se givebook <player> <enchant> <level> [success] [destroy]
                .then(Commands.literal("givebook")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .then(Commands.argument("enchant", StringArgumentType.word())
                                        // Gợi ý Tab Complete từ Registry
                                        .suggests((context, builder) -> {
                                            for (String id : plugin.getEnchantRegistry().getRegisteredIds()) {
                                                if (id.startsWith(builder.getRemainingLowerCase())) {
                                                    builder.suggest(id);
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1))
                                                .executes(context -> executeGiveBook(context, 100, 0))
                                                .then(Commands.argument("success", IntegerArgumentType.integer(0, 100))
                                                        .executes(context -> executeGiveBook(context, IntegerArgumentType.getInteger(context, "success"), 0))
                                                        .then(Commands.argument("destroy", IntegerArgumentType.integer(0, 100))
                                                                .executes(context -> executeGiveBook(context, IntegerArgumentType.getInteger(context, "success"), IntegerArgumentType.getInteger(context, "destroy")))
                                                        )
                                                )
                                        )
                                )
                        )
                )
                .build();
    }

    private int executeGiveBook(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int success, int destroy) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();

        String enchantId = StringArgumentType.getString(context, "enchant");
        int level = IntegerArgumentType.getInteger(context, "level");

        ItemStack book = plugin.getEnchantManager().createEnchantBook(enchantId, level, success, destroy);
        target.getInventory().addItem(book);

        sendMessage(context.getSource(), "give-book-success",
                "%enchant%", enchantId,
                "%level%", String.valueOf(level),
                "%player%", target.getName());

        return Command.SINGLE_SUCCESS;
    }
}