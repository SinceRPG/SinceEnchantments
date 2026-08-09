package net.danh.sinceenchantments.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.command.brigadier.argument.resolvers.selector.PlayerSelectorArgumentResolver;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.gui.PreviewGUI;
import net.danh.sinceenchantments.utils.ColorUtils;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;

@SuppressWarnings("SpellCheckingInspection")
public class SinceCommand {
    private static final int RANDOM_BOOK_OPTION_LIMIT = 8;
    private static final List<String> RANDOM_BOOK_FILTERS = List.of("-level", "-rarity", "-target", "-type", "-success", "-failure", "-amount");
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
                .then(Commands.literal("reload").executes(context -> {
                    plugin.getSettingsFile().reload();
                    plugin.getEnchantsFile().reload();
                    plugin.getLimitsFile().reload();
                    plugin.getMessagesFile().reload();
                    plugin.getItemsFile().reload();
                    plugin.getGuiFile().reload();
                    plugin.getEnchantManager().loadEnchantsFromConfig();

                    plugin.getAdvancedEnchantmentsHook().loadAEEnchantments();
                    plugin.getCrazyEnchantmentsHook().loadCrazyEnchantments();
                    plugin.getExcellentEnchantsHook().loadExcellentEnchantments();
                    plugin.getEnchantsSquaredHook().loadEnchantsSquared();

                    sendMessage(context.getSource(), "reload");
                    return Command.SINGLE_SUCCESS;
                }))
                .then(Commands.literal("givebook").then(buildGiveBookCommand()))
                .then(Commands.literal("giverandombook").then(buildGiveRandomBookCommand()))
                .then(Commands.literal("giveextractor").then(Commands.argument("target", ArgumentTypes.player()).then(Commands.argument("type", StringArgumentType.word())
                        .suggests((context, builder) -> {
                            builder.suggest("random");
                            builder.suggest("specific");
                            return builder.buildFuture();
                        }).then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(this::executeGiveExtractor)))))
                .then(Commands.literal("givecharm").then(Commands.argument("target", ArgumentTypes.player()).then(Commands.argument("bonus", IntegerArgumentType.integer(1, 100)).executes(context -> executeGiveCharm(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(context -> executeGiveCharm(context, IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("giveslotgem").then(Commands.argument("target", ArgumentTypes.player()).then(Commands.argument("modifier", IntegerArgumentType.integer(-100, 100)).executes(context -> executeGiveSlotGem(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(context -> executeGiveSlotGem(context, IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("givelock").then(Commands.argument("target", ArgumentTypes.player()).executes(context -> executeGiveLock(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(context -> executeGiveLock(context, IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("givepurge").then(Commands.argument("target", ArgumentTypes.player()).then(Commands.argument("return_books", BoolArgumentType.bool()).executes(context -> executeGivePurge(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(context -> executeGivePurge(context, IntegerArgumentType.getInteger(context, "amount")))))))
                .then(Commands.literal("giverandomizer").then(Commands.argument("target", ArgumentTypes.player()).executes(context -> executeGiveRandomizer(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(context -> executeGiveRandomizer(context, IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("giveprotector").then(Commands.argument("target", ArgumentTypes.player()).executes(context -> executeGiveProtector(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(context -> executeGiveProtector(context, IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("givetracker").then(Commands.argument("target", ArgumentTypes.player()).executes(context -> executeGiveTracker(context, 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64)).executes(context -> executeGiveTracker(context, IntegerArgumentType.getInteger(context, "amount"))))))
                .then(Commands.literal("gui").executes(this::executeGui))
                .build();
    }

    private RequiredArgumentBuilder<CommandSourceStack, PlayerSelectorArgumentResolver> buildGiveBookCommand() {
        return Commands.argument("target", ArgumentTypes.player()).then(Commands.argument("enchant", StringArgumentType.string())
                .suggests(this::suggestEnchantIds)
                .then(Commands.argument("level", IntegerArgumentType.integer(1))
                        .executes(context -> executeGiveBook(context, 100, 0, false))
                        .then(Commands.literal("-s").executes(context -> executeGiveBook(context, 100, 0, true)))
                        .then(Commands.argument("success", IntegerArgumentType.integer(0, 100))
                                .executes(context -> executeGiveBook(context, IntegerArgumentType.getInteger(context, "success"), 0, false))
                                .then(Commands.literal("-s").executes(context -> executeGiveBook(context, IntegerArgumentType.getInteger(context, "success"), 0, true)))
                                .then(Commands.argument("destroy", IntegerArgumentType.integer(0, 100))
                                        .executes(context -> executeGiveBook(context, IntegerArgumentType.getInteger(context, "success"), IntegerArgumentType.getInteger(context, "destroy"), false))
                                        .then(Commands.literal("-s").executes(context -> executeGiveBook(context, IntegerArgumentType.getInteger(context, "success"), IntegerArgumentType.getInteger(context, "destroy"), true)))))));
    }

    private RequiredArgumentBuilder<CommandSourceStack, PlayerSelectorArgumentResolver> buildGiveRandomBookCommand() {
        return Commands.argument("target", ArgumentTypes.player())
                .executes(context -> executeGiveRandomBook(context, new RandomBookOptions()))
                .then(Commands.argument("options", StringArgumentType.greedyString())
                        .suggests(this::suggestRandomBookOptionsGreedy)
                        .executes(this::executeGiveRandomBookGreedy));
    }

    private int executeGiveRandomBookGreedy(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        String optionsStr = StringArgumentType.getString(context, "options");
        RandomBookOptions options = parseRandomBookOptions(optionsStr);
        return executeGiveRandomBook(context, options);
    }

    private RandomBookOptions parseRandomBookOptions(String optionsStr) {
        RandomBookOptions options = new RandomBookOptions();
        String[] args = optionsStr.split(" ");
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (token.equalsIgnoreCase("-s")) {
                options.silent = true;
            } else if (token.startsWith("-")) {
                if (i + 1 < args.length) {
                    String value = args[i + 1];
                    i++;
                    applyRandomBookOption(options, token, value);
                }
            }
        }
        return options;
    }

    private void applyRandomBookOption(RandomBookOptions options, String filter, String value) {
        switch (filter.toLowerCase(Locale.ROOT)) {
            case "-level" -> {
                try {
                    options.level = Math.max(1, Integer.parseInt(value));
                } catch (NumberFormatException ignored) {}
            }
            case "-rarity" -> options.rarity = value.toUpperCase(Locale.ROOT);
            case "-target" -> options.target = value.toUpperCase(Locale.ROOT);
            case "-type" -> options.type = normalizeType(value);
            case "-success" -> {
                options.success = IntRange.parse(value, 0, 100, options.success);
                options.hasSuccess = true;
            }
            case "-failure", "-destroy" -> {
                options.failure = IntRange.parse(value, 0, 100, options.failure);
                options.hasFailure = true;
            }
            case "-amount" -> options.amount = IntRange.parse(value, 1, 64, options.amount);
        }
    }

    private int executeHelp(CommandContext<CommandSourceStack> context) {
        sendMessage(context.getSource(), "help-header");
        for (String line : plugin.getMessagesFile().getStringList("help-commands")) {
            context.getSource().getSender().sendMessage(ColorUtils.parse(line));
        }
        return Command.SINGLE_SUCCESS;
    }

    private int executeGui(CommandContext<CommandSourceStack> context) {
        if (context.getSource().getSender() instanceof Player p) {
            PreviewGUI gui = new PreviewGUI(plugin, null, 0);
            p.openInventory(gui.getInventory());
        }
        return Command.SINGLE_SUCCESS;
    }

    private CompletableFuture<Suggestions> suggestEnchantIds(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        for (String id : plugin.getEnchantManager().getAllKnownEnchantIds()) {
            if (id.toLowerCase(Locale.ROOT).contains(remaining)) builder.suggest('"' + id + '"');
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestRandomBookLevels(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestValues(builder, List.of("1", "2", "3", "4", "5"));
    }

    private CompletableFuture<Suggestions> suggestRandomBookRarities(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestValues(builder, getKnownRarities());
    }

    private CompletableFuture<Suggestions> suggestRandomBookTargets(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestValues(builder, getKnownTargets());
    }

    private CompletableFuture<Suggestions> suggestRandomBookTypes(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestValues(builder, List.of("vanilla", "since", "ae", "ce", "ee", "advancedenchantments", "crazyenchantments", "excellentenchants"));
    }

    private CompletableFuture<Suggestions> suggestRandomBookSuccessRanges(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestValues(builder, List.of("100", "80to100", "40to90"));
    }

    private CompletableFuture<Suggestions> suggestRandomBookFailureRanges(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestValues(builder, List.of("0", "0to20", "10to30"));
    }

    private CompletableFuture<Suggestions> suggestRandomBookAmounts(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        return suggestValues(builder, List.of("1", "1to2", "1to3"));
    }

    private CompletableFuture<Suggestions> suggestValues(SuggestionsBuilder builder, List<String> values) {
        String remaining = builder.getRemainingLowerCase();
        for (String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(remaining)) {
                builder.suggest(value);
            }
        }
        return builder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestRandomBookOptionsGreedy(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder) {
        String currentStr = builder.getRemaining();
        String[] parts = currentStr.split(" ", -1);
        String lastPart = parts[parts.length - 1];

        if (parts.length > 1 && parts[parts.length - 2].startsWith("-") && !parts[parts.length - 2].equalsIgnoreCase("-s")) {
            String filter = parts[parts.length - 2];
            int offset = builder.getStart() + currentStr.length() - lastPart.length();
            SuggestionsBuilder wordBuilder = builder.createOffset(offset);
            return suggestRandomBookFilterValueGreedy(context, wordBuilder, filter);
        }

        int offset = builder.getStart() + currentStr.length() - lastPart.length();
        SuggestionsBuilder wordBuilder = builder.createOffset(offset);
        List<String> usedFilters = new ArrayList<>();
        for (String part : parts) {
            if (part.startsWith("-")) usedFilters.add(part.toLowerCase(Locale.ROOT));
        }

        for (String filter : RANDOM_BOOK_FILTERS) {
            if (!usedFilters.contains(filter) && filter.startsWith(lastPart.toLowerCase(Locale.ROOT))) {
                wordBuilder.suggest(filter);
            }
        }
        if (!usedFilters.contains("-s") && "-s".startsWith(lastPart.toLowerCase(Locale.ROOT))) {
            wordBuilder.suggest("-s");
        }

        return wordBuilder.buildFuture();
    }

    private CompletableFuture<Suggestions> suggestRandomBookFilterValueGreedy(CommandContext<CommandSourceStack> context, SuggestionsBuilder builder, String filter) {
        return switch (filter.toLowerCase(Locale.ROOT)) {
            case "-level" -> suggestRandomBookLevels(context, builder);
            case "-rarity" -> suggestRandomBookRarities(context, builder);
            case "-target" -> suggestRandomBookTargets(context, builder);
            case "-type" -> suggestRandomBookTypes(context, builder);
            case "-success" -> suggestRandomBookSuccessRanges(context, builder);
            case "-failure", "-destroy" -> suggestRandomBookFailureRanges(context, builder);
            case "-amount" -> suggestRandomBookAmounts(context, builder);
            default -> builder.buildFuture();
        };
    }

    private List<String> getKnownRarities() {
        List<String> rarities = new ArrayList<>();
        for (String id : plugin.getEnchantManager().getAllKnownEnchantIds()) {
            String rarity = plugin.getEnchantManager().getRarity(id);
            if (!rarities.contains(rarity)) rarities.add(rarity);
        }
        return rarities;
    }

    private List<String> getKnownTargets() {
        List<String> targets = new ArrayList<>();
        for (String id : plugin.getEnchantManager().getAllKnownEnchantIds()) {
            String target = plugin.getEnchantManager().getTarget(id);
            if (!targets.contains(target)) targets.add(target);
        }
        return targets;
    }

    private int executeGiveBook(CommandContext<CommandSourceStack> context, int success, int destroy, boolean silent) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        String enchantId = StringArgumentType.getString(context, "enchant").replace("\"", "").toLowerCase();
        int level = IntegerArgumentType.getInteger(context, "level");

        if (!plugin.getEnchantManager().enchantExists(enchantId)) {
            if (!silent) sendMessage(context.getSource(), "enchant-not-found", "%enchant%", enchantId);
            return Command.SINGLE_SUCCESS;
        }
        int maxLevel = plugin.getEnchantManager().getMaxLevel(enchantId);
        if (level > maxLevel) {
            if (!silent)
                sendMessage(context.getSource(), "give-book-level-too-high", "%enchant%", enchantId, "%level%", String.valueOf(level), "%max%", String.valueOf(maxLevel));
            return Command.SINGLE_SUCCESS;
        }

        ItemStack book = plugin.getItemFactory().createEnchantBook(enchantId, level, success, destroy);
        target.getInventory().addItem(book);
        if (!silent)
            sendMessage(context.getSource(), "give-book-success", "%enchant%", enchantId, "%level%", String.valueOf(level), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    // removed obsolete methods

    private int executeGiveRandomBook(CommandContext<CommandSourceStack> context, RandomBookOptions options) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        List<String> candidates = getRandomBookCandidates(options);

        if (candidates.isEmpty()) {
            if (!options.silent) sendMessage(context.getSource(), "give-random-book-empty");
            return Command.SINGLE_SUCCESS;
        }

        int amount = options.amount.roll();
        for (int i = 0; i < amount; i++) {
            String enchantId = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
            int level = options.level == null ? ThreadLocalRandom.current().nextInt(1, plugin.getEnchantManager().getMaxLevel(enchantId) + 1) : options.level;
            int success;
            int failure;
            if (options.hasSuccess && !options.hasFailure) {
                success = options.success.roll();
                failure = 100 - success;
            } else if (!options.hasSuccess && options.hasFailure) {
                failure = options.failure.roll();
                success = 100 - failure;
            } else if (!options.hasSuccess && !options.hasFailure) {
                success = ThreadLocalRandom.current().nextInt(0, 101);
                failure = 100 - success;
            } else {
                success = options.success.roll();
                failure = options.failure.roll();
            }
            ItemStack book = plugin.getItemFactory().createEnchantBook(enchantId, level, success, failure);
            target.getInventory().addItem(book);
        }

        if (!options.silent) {
            sendMessage(context.getSource(), "give-random-book-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        }
        return Command.SINGLE_SUCCESS;
    }

    private List<String> getRandomBookCandidates(RandomBookOptions options) {
        List<String> candidates = new ArrayList<>();
        for (String id : plugin.getEnchantManager().getAllKnownEnchantIds()) {
            if (!plugin.getEnchantManager().enchantExists(id)) continue;
            if (options.level != null && options.level > plugin.getEnchantManager().getMaxLevel(id)) continue;
            if (options.rarity != null && !plugin.getEnchantManager().getRarity(id).equalsIgnoreCase(options.rarity))
                continue;
            if (options.target != null && !plugin.getEnchantManager().getTarget(id).equalsIgnoreCase(options.target))
                continue;
            if (options.type != null && !plugin.getEnchantManager().getEnchantType(id).equalsIgnoreCase(options.type))
                continue;
            candidates.add(id);
        }
        return candidates;
    }

    private String normalizeType(String type) {
        return switch (type.toLowerCase(Locale.ROOT)) {
            case "minecraft", "vanilla", "mc" -> "vanilla";
            case "advancedenchantments", "advanced", "ae" -> "ae";
            case "crazyenchantments", "crazy", "ce" -> "ce";
            case "excellentenchants", "excellent", "ee" -> "ee";
            case "sinceenchantments", "since", "se", "custom" -> "since";
            default -> type.toLowerCase(Locale.ROOT);
        };
    }

    private int executeGiveExtractor(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        String type = StringArgumentType.getString(context, "type");
        int amount = IntegerArgumentType.getInteger(context, "amount");
        if (!type.equalsIgnoreCase("random") && !type.equalsIgnoreCase("specific")) {
            sendMessage(context.getSource(), "invalid-extractor-type");
            return Command.SINGLE_SUCCESS;
        }
        ItemStack extractor = plugin.getItemFactory().createExtractor(type, amount);
        target.getInventory().addItem(extractor);
        sendMessage(context.getSource(), "give-extractor-success", "%type%", type.toUpperCase(), "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveCharm(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        int bonus = IntegerArgumentType.getInteger(context, "bonus");
        ItemStack charm = plugin.getItemFactory().createSuccessCharm(bonus, amount);
        target.getInventory().addItem(charm);
        sendMessage(context.getSource(), "give-charm-success", "%bonus%", String.valueOf(bonus), "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveSlotGem(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        int modifier = IntegerArgumentType.getInteger(context, "modifier");
        ItemStack gem = plugin.getItemFactory().createSlotGem(modifier, amount);
        target.getInventory().addItem(gem);
        sendMessage(context.getSource(), "give-slotgem-success", "%modifier%", String.valueOf(modifier), "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveLock(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        ItemStack scroll = plugin.getItemFactory().createLockScroll(amount);
        target.getInventory().addItem(scroll);
        sendMessage(context.getSource(), "give-lock-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGivePurge(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        boolean returnBooks = BoolArgumentType.getBool(context, "return_books");
        ItemStack scroll = plugin.getItemFactory().createPurgeScroll(returnBooks, amount);
        target.getInventory().addItem(scroll);
        sendMessage(context.getSource(), "give-purge-success", "%return%", returnBooks ? "True" : "False", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveRandomizer(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        ItemStack item = plugin.getItemFactory().createRandomizer(amount);
        target.getInventory().addItem(item);
        sendMessage(context.getSource(), "give-randomizer-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveProtector(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        ItemStack item = plugin.getItemFactory().createProtector(amount);
        target.getInventory().addItem(item);
        sendMessage(context.getSource(), "give-protector-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private int executeGiveTracker(CommandContext<CommandSourceStack> context, int amount) throws CommandSyntaxException {
        Player target = context.getArgument("target", PlayerSelectorArgumentResolver.class).resolve(context.getSource()).getFirst();
        ItemStack item = plugin.getItemFactory().createTracker(amount);
        target.getInventory().addItem(item);
        sendMessage(context.getSource(), "give-tracker-success", "%amount%", String.valueOf(amount), "%player%", target.getName());
        return Command.SINGLE_SUCCESS;
    }

    private static class RandomBookOptions {
        private boolean silent = false;
        private Integer level;
        private String rarity;
        private String target;
        private String type;
        private IntRange success = new IntRange(100, 100);
        private IntRange failure = new IntRange(0, 0);
        private IntRange amount = new IntRange(1, 1);
        private boolean hasSuccess = false;
        private boolean hasFailure = false;
    }

    private record IntRange(int min, int max) {
        private static IntRange parse(String raw, int minAllowed, int maxAllowed, IntRange fallback) {
            String value = raw.toLowerCase(Locale.ROOT).replace(" ", "");
            String[] parts = value.split("to", 2);
            try {
                int min = Integer.parseInt(parts[0]);
                int max = parts.length == 2 ? Integer.parseInt(parts[1]) : min;
                min = clamp(min, minAllowed, maxAllowed);
                max = clamp(max, minAllowed, maxAllowed);
                if (min > max) {
                    int tmp = min;
                    min = max;
                    max = tmp;
                }
                return new IntRange(min, max);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }

        private static int clamp(int value, int min, int max) {
            return Math.max(min, Math.min(max, value));
        }

        private int roll() {
            if (min == max) return min;
            return ThreadLocalRandom.current().nextInt(min, max + 1);
        }
    }
}
