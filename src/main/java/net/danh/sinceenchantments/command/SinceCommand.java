package net.danh.sinceenchantments.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@SuppressWarnings("SpellCheckingInspection")
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
                .executes(this::executeHelp)
                .then(Commands.literal("help").executes(this::executeHelp))
                .then(Commands.literal("reload")
                        .executes(context -> {
                            plugin.getSettingsFile().reload();
                            plugin.getEnchantsFile().reload();
                            plugin.getLimitsFile().reload();
                            plugin.getMessagesFile().reload();
                            plugin.getItemsFile().reload();
                            plugin.getGuiFile().reload();
                            plugin.getEnchantManager().loadEnchantsFromConfig();
                            sendMessage(context.getSource(), "reload");
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .then(Commands.literal("givebook")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .then(Commands.argument("enchant", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            String remaining = builder.getRemainingLowerCase();
                                            for (String id : plugin.getEnchantRegistry().getRegisteredIds()) {
                                                if (id.toLowerCase().contains(remaining))
                                                    builder.suggest('"' + id + '"');
                                            }
                                            for (Enchantment enc : RegistryAccess.registryAccess().getRegistry(RegistryKey.ENCHANTMENT)) {
                                                String id = enc.getKey().toString().toLowerCase();
                                                if (id.contains(remaining))
                                                    builder.suggest('"' + id + '"');
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
                .then(Commands.literal("giveextractor")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((context, builder) -> {
                                            builder.suggest("random");
                                            builder.suggest("specific");
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(this::executeGiveExtractor)
                                        )
                                )
                        )
                )
                .then(Commands.literal("givecharm")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .then(Commands.argument("bonus", IntegerArgumentType.integer(1, 100))
                                        .executes(context -> executeGiveCharm(context, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> executeGiveCharm(context, IntegerArgumentType.getInteger(context, "amount")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("giveslotgem")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .then(Commands.argument("modifier", IntegerArgumentType.integer(-100, 100))
                                        .executes(context -> executeGiveSlotGem(context, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> executeGiveSlotGem(context, IntegerArgumentType.getInteger(context, "amount")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("givelock")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .executes(context -> executeGiveLock(context, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> executeGiveLock(context, IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
                )
                .then(Commands.literal("givepurge")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .then(Commands.argument("return_books", BoolArgumentType.bool())
                                        .executes(context -> executeGivePurge(context, 1))
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                                .executes(context -> executeGivePurge(context, IntegerArgumentType.getInteger(context, "amount")))
                                        )
                                )
                        )
                )
                .then(Commands.literal("giverandomizer")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .executes(context -> executeGiveRandomizer(context, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> executeGiveRandomizer(context, IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
                )
                .then(Commands.literal("giveprotector")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .executes(context -> executeGiveProtector(context, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> executeGiveProtector(context, IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
                )
                .then(Commands.literal("givetracker")
                        .then(Commands.argument("target", ArgumentTypes.player())
                                .executes(context -> executeGiveTracker(context, 1))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(context -> executeGiveTracker(context, IntegerArgumentType.getInteger(context, "amount")))
                                )
                        )
                )
                .build();
    }

    private int executeHelp(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) {
        sendMessage(context.getSource(), "help-header");
        for (String line : plugin.getMessagesFile().getStringList("help-commands")) {
            context.getSource().getSender().sendMessage(ColorUtils.parse(line));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveBook(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int success, int destroy) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();
        String enchantId = StringArgumentType.getString(context, "enchant").replace("\"", "").toLowerCase();

        if (!plugin.getEnchantManager().enchantExists(enchantId)) {
            sendMessage(context.getSource(), "enchant-not-found", "%enchant%", enchantId);
            return Command.SINGLE_SUCCESS;
        }

        int level = IntegerArgumentType.getInteger(context, "level");
        ItemStack book = plugin.getEnchantManager().createEnchantBook(enchantId, level, success, destroy);
        target.getInventory().addItem(book);

        sendMessage(context.getSource(), "give-book-success", "%enchant%", enchantId, "%level%", String.valueOf(level), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveExtractor(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();

        String type = StringArgumentType.getString(context, "type");
        int amount = IntegerArgumentType.getInteger(context, "amount");

        if (!type.equalsIgnoreCase("random") && !type.equalsIgnoreCase("specific")) {
            sendMessage(context.getSource(), "invalid-extractor-type");
            return Command.SINGLE_SUCCESS;
        }

        ItemStack extractor = plugin.getEnchantManager().createExtractor(type, amount);
        target.getInventory().addItem(extractor);
        sendMessage(context.getSource(), "give-extractor-success", "%type%", type.toUpperCase(), "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveCharm(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();
        int bonus = IntegerArgumentType.getInteger(context, "bonus");

        ItemStack charm = plugin.getEnchantManager().createSuccessCharm(bonus, amount);
        target.getInventory().addItem(charm);
        sendMessage(context.getSource(), "give-charm-success", "%bonus%", String.valueOf(bonus), "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveSlotGem(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();
        int modifier = IntegerArgumentType.getInteger(context, "modifier");

        ItemStack gem = plugin.getEnchantManager().createSlotGem(modifier, amount);
        target.getInventory().addItem(gem);
        sendMessage(context.getSource(), "give-slotgem-success", "%modifier%", String.valueOf(modifier), "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveLock(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();

        ItemStack scroll = plugin.getEnchantManager().createLockScroll(amount);
        target.getInventory().addItem(scroll);
        sendMessage(context.getSource(), "give-lock-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGivePurge(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();
        boolean returnBooks = BoolArgumentType.getBool(context, "return_books");

        ItemStack scroll = plugin.getEnchantManager().createPurgeScroll(returnBooks, amount);
        target.getInventory().addItem(scroll);
        sendMessage(context.getSource(), "give-purge-success", "%return%", returnBooks ? "True" : "False", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveRandomizer(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();
        ItemStack item = plugin.getEnchantManager().createRandomizer(amount);
        target.getInventory().addItem(item);
        sendMessage(context.getSource(), "give-randomizer-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveProtector(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();
        ItemStack item = plugin.getEnchantManager().createProtector(amount);
        target.getInventory().addItem(item);
        sendMessage(context.getSource(), "give-protector-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveTracker(com.mojang.brigadier.context.CommandContext<CommandSourceStack> context, int amount) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        PlayerSelectorArgumentResolver targetResolver = context.getArgument("target", PlayerSelectorArgumentResolver.class);
        Player target = targetResolver.resolve(context.getSource()).getFirst();
        ItemStack item = plugin.getEnchantManager().createTracker(amount);
        target.getInventory().addItem(item);
        sendMessage(context.getSource(), "give-tracker-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }
}