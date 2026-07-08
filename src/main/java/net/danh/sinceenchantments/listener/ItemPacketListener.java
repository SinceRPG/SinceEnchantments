package net.danh.sinceenchantments.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientCreativeInventoryAction;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.danh.sinceenchantments.SinceEnchantments;
import net.danh.sinceenchantments.api.EnchantManager;
import net.danh.sinceenchantments.api.PersistentKeyNames;
import net.danh.sinceenchantments.utils.ColorUtils;
import net.danh.sinceenchantments.utils.ConfigUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * ITEM PACKET LISTENER
 * <p>
 * Functionality:
 * Intercepts packets sent to the client to inject dynamically evaluated lore and enchantment
 * visuals. Items on the server remain clean, but appear fully customized to the player.
 * <p>
 * Lag Optimization Applied:
 * Highly optimized using Guava Caching and string-based Component hash Fast-Failing to maintain
 * perfect server TPS even during heavy inventory spam. Fixed 1.21 custom model data collision bugs.
 */
public class ItemPacketListener extends PacketListenerAbstract implements PacketListener {

    /**
     * Memoization cache: Stores processed items for 1 second to prevent lag spikes during UI spam.
     * The cache uses the Bukkit ItemStack hash to ensure 1.21+ Data Components are distinctly recognized.
     */
    private final Cache<Integer, Optional<com.github.retrooper.packetevents.protocol.item.ItemStack>> itemCache;

    public ItemPacketListener() {
        ConfigUtils settings = SinceEnchantments.getInstance().getSettingsFile();
        long cacheExpireMillis = Math.max(100L, settings.getInt("settings.packet-cache-expire-ms", 1000));
        long cacheMaxSize = Math.max(128L, settings.getInt("settings.packet-cache-max-size", 5000));
        this.itemCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheExpireMillis, TimeUnit.MILLISECONDS)
                .maximumSize(cacheMaxSize)
                .build();
    }

    public void clearCache() {
        itemCache.invalidateAll();
    }

    /**
     * Dynamically verifies if an item is considered enchantable gear based on the configuration file.
     * This replaces previous hardcoded string checks.
     *
     * @param item The ItemStack to verify.
     * @return TRUE if the item is enchantable gear, FALSE otherwise.
     */
    private boolean isEnchantableGear(ItemStack item) {
        if (item == null) return false;
        String name = item.getType().name();

        ConfigUtils settings = SinceEnchantments.getInstance().getSettingsFile();
        List<String> suffixes = settings.getStringList("settings.enchantable-gear-suffixes");
        List<String> exacts = settings.getStringList("settings.enchantable-gear-exact");

        if (suffixes != null) {
            for (String suffix : suffixes) {
                if (name.endsWith(suffix)) return true;
            }
        }
        if (exacts != null) {
            for (String exact : exacts) {
                if (name.equals(exact)) return true;
            }
        }
        return false;
    }

    @Override
    public void onPacketSend(@NonNull PacketSendEvent event) {
        try {
            Player player = (Player) event.getPlayer();
            if (player == null) return;

            if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
                WrapperPlayServerSetSlot wrapper = new WrapperPlayServerSetSlot(event);
                var peItem = wrapper.getItem();
                var modified = processItem(player, peItem);
                if (modified != null) wrapper.setItem(modified);

            } else if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
                WrapperPlayServerWindowItems wrapper = new WrapperPlayServerWindowItems(event);
                var items = wrapper.getItems();
                boolean modified = false;

                for (int i = 0; i < items.size(); i++) {
                    var peItem = items.get(i);
                    var modItem = processItem(player, peItem);
                    if (modItem != null) {
                        items.set(i, modItem);
                        modified = true;
                    }
                }
                if (modified) wrapper.setItems(items);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onPacketReceive(@NonNull PacketReceiveEvent event) {
        try {
            if (event.getPacketType() == PacketType.Play.Client.CREATIVE_INVENTORY_ACTION) {
                WrapperPlayClientCreativeInventoryAction wrapper = new WrapperPlayClientCreativeInventoryAction(event);
                var peItem = wrapper.getItemStack();
                if (peItem != null && !peItem.isEmpty()) {
                    ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem).clone();
                    bukkitItem = cleanCreativeItem(bukkitItem);
                    wrapper.setItemStack(SpigotConversionUtil.fromBukkitItemStack(bukkitItem));
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Processes an item with maximum efficiency utilizing memory caching.
     * Prevents DataComponentMap corruption and solves 1.21+ texture overriding bugs
     * by hashing the converted Bukkit ItemStack instead of raw NBT.
     *
     * @param player The player receiving the packet.
     * @param peItem The PacketEvents ItemStack.
     * @return The modified PacketEvents ItemStack, or null if no changes were made.
     */
    private com.github.retrooper.packetevents.protocol.item.ItemStack processItem(Player player, com.github.retrooper.packetevents.protocol.item.ItemStack peItem) {
        if (peItem == null || peItem.getAmount() <= 0 || peItem.getType() == ItemTypes.AIR) return null;

        // CRITICAL FIX: Convert to Bukkit ItemStack BEFORE hashing.
        // Bukkit's ItemStack.hashCode() natively checks all 1.21 Data Components (item_model, custom_model_data, etc).
        // peItem.getNBT() is often null or missing component data in 1.21+, which caused the texture overwrite bug!
        ItemStack bukkitItem = SpigotConversionUtil.toBukkitItemStack(peItem).clone();

        int cacheKey = Objects.hash(player.getUniqueId(), bukkitItem);

        Optional<com.github.retrooper.packetevents.protocol.item.ItemStack> cachedResult = itemCache.getIfPresent(cacheKey);
        if (cachedResult != null) {
            return cachedResult.orElse(null);
        }

        ItemStack originalItem = bukkitItem.clone();
        boolean modified = formatSkyblockItem(bukkitItem);

        if (!modified && bukkitItem.equals(originalItem)) {
            itemCache.put(cacheKey, Optional.empty());
            return null;
        }

        var result = SpigotConversionUtil.fromBukkitItemStack(bukkitItem);
        itemCache.put(cacheKey, Optional.of(result));
        return result;
    }

    /**
     * Cleans an item extracted from the Creative Menu to ensure packet-injected lore
     * is completely wiped before reaching the server's actual inventory.
     *
     * @param item The Bukkit ItemStack.
     * @return The cleaned Bukkit ItemStack.
     */
    private ItemStack cleanCreativeItem(ItemStack item) {
        SinceEnchantments.getInstance().getEnchantManager().cleanItemLore(item);
        return item;
    }

    /**
     * Injects the visual enchantments into the ItemStack seamlessly.
     *
     * @param item The Bukkit ItemStack to format.
     * @return TRUE if the item was modified, FALSE otherwise.
     */
    private boolean formatSkyblockItem(ItemStack item) {
        EnchantManager manager = SinceEnchantments.getInstance().getEnchantManager();
        boolean cleaned = manager.cleanItemLore(item);

        if (item == null || item.getType().isAir()) return cleaned;
        boolean hadMetaInitially = item.hasItemMeta();
        ItemMeta meta = hadMetaInitially ? item.getItemMeta() : Bukkit.getItemFactory().getItemMeta(item.getType());
        if (meta == null) return cleaned;

        ConfigUtils settings = SinceEnchantments.getInstance().getSettingsFile();
        ConfigUtils enchantsConfig = SinceEnchantments.getInstance().getEnchantsFile();
        List<Component> lore = meta.hasLore() ? new ArrayList<>(meta.lore()) : new ArrayList<>();

        String placeholderStr = settings.getString("settings.placeholder", "{enchants}").toLowerCase();
        int targetIndex = -1;

        for (int i = lore.size() - 1; i >= 0; i--) {
            String plainLore = ColorUtils.toPlainText(lore.get(i)).toLowerCase();
            if (plainLore.contains(placeholderStr)) {
                targetIndex = i;
                lore.remove(i);
                break;
            }
        }

        Map<String, Integer> customEnchants = manager.getCustomEnchants(item);
        Map<Enchantment, Integer> vanillaEnchants = new HashMap<>();
        for (Map.Entry<Enchantment, Integer> entry : meta.getEnchants().entrySet()) {
            String id = entry.getKey().getKey().toString().toLowerCase();
            if (!customEnchants.containsKey(id)) {
                vanillaEnchants.put(entry.getKey(), entry.getValue());
            }
        }
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
            if (hadMetaInitially && hasPlaceholder) {
                meta.lore(lore);
                item.setItemMeta(meta);
                return true;
            }
            return cleaned;
        }

        List<Component> injectComponents = new ArrayList<>();

        if (overrideVanilla && !meta.hasItemFlag(ItemFlag.HIDE_ENCHANTS)) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            pdc.set(new NamespacedKey(SinceEnchantments.getInstance(), PersistentKeyNames.LORE_HID_ENCHANTS), PersistentDataType.BYTE, (byte) 1);
        }

        if (hasProtect) {
            injectComponents.add(ColorUtils.parse(settings.getString("settings.protected-format", "&a&lProtected &7(Keeps on death)")).decoration(TextDecoration.ITALIC, false));
            injectComponents.add(Component.empty());
        }

        if (hasTracker) {
            injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-header", "&8&m      &r &6&lStat Tracker &8&m      ")).decoration(TextDecoration.ITALIC, false));
            if (pdc.has(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER))
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-blocks", "&7Blocks Mined: &e%value%").replace("%value%", String.valueOf(pdc.get(manager.STAT_BLOCKS_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
            if (pdc.has(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER))
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-mobs", "&7Mobs Killed: &e%value%").replace("%value%", String.valueOf(pdc.get(manager.STAT_MOBS_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
            if (pdc.has(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER))
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-players", "&7Players Killed: &c%value%").replace("%value%", String.valueOf(pdc.get(manager.STAT_PLAYERS_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
            if (pdc.has(manager.STAT_FISH_KEY, PersistentDataType.INTEGER))
                injectComponents.add(ColorUtils.parse(settings.getString("settings.tracker-fish", "&7Fish Caught: &b%value%").replace("%value%", String.valueOf(pdc.get(manager.STAT_FISH_KEY, PersistentDataType.INTEGER)))).decoration(TextDecoration.ITALIC, false));
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
            String eName = manager.getEnchantName(eId);
            String rarityKey = manager.getRarity(eId);
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
            if (totalEnchantsApplied > 0 || maxSlots > 0) {
                String slotLine = settings.getString("settings.slots-format", "&7Enchantment Slots: &e%current% / %max%");
                slotLine = slotLine.replace("%current%", String.valueOf(totalEnchantsApplied)).replace("%max%", String.valueOf(maxSlots));
                injectComponents.add(ColorUtils.parse(slotLine).decoration(TextDecoration.ITALIC, false));
            }
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
            return hasPlaceholder || cleaned;
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

        pdc.set(new NamespacedKey(SinceEnchantments.getInstance(), PersistentKeyNames.LORE_START), PersistentDataType.INTEGER, startIdx);
        pdc.set(new NamespacedKey(SinceEnchantments.getInstance(), PersistentKeyNames.LORE_COUNT), PersistentDataType.INTEGER, injectComponents.size());
        pdc.set(new NamespacedKey(SinceEnchantments.getInstance(), PersistentKeyNames.LORE_PLACEHOLDER), PersistentDataType.BYTE, (byte) (hasPlaceholder ? 1 : 0));

        meta.lore(lore);
        item.setItemMeta(meta);
        return true;
    }

    /**
     * Formats default vanilla names nicely.
     *
     * @param rawName The raw key format.
     * @return Formatted Title Case name.
     */
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

    /**
     * Converts an integer to a Roman Numeral string.
     *
     * @param number The level.
     * @return Roman Numeral representation.
     */
    private String toRoman(int number) {
        String[] roman = {"O", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII", "XIII", "XIV", "XV", "XVI", "XVII", "XVIII", "XIX", "XX"};
        return (number > 0 && number < roman.length) ? roman[number] : String.valueOf(number);
    }
}
