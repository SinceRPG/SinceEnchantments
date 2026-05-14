package net.danh.sinceenchantments.api;

/**
 * Centralizes PersistentDataContainer key names so item data remains stable across
 * updates and the keys are not repeated throughout the codebase.
 */
public final class PersistentKeyNames {
    public static final String CUSTOM_ENCHANTS = "custom_enchants";
    public static final String BOOK_ENCHANT_ID = "book_enchant_id";
    public static final String BOOK_ENCHANT_LEVEL = "book_enchant_level";
    public static final String BOOK_SUCCESS_RATE = "book_success_rate";
    public static final String BOOK_DESTROY_RATE = "book_destroy_rate";
    public static final String EXTRACTOR_TYPE = "extractor_type";
    public static final String CHARM_BONUS = "charm_bonus";
    public static final String GUI_ACTION = "gui_action";
    public static final String SLOT_GEM_ITEM = "slot_gem_item";
    public static final String SLOT_MODIFIER = "slot_modifier";
    public static final String ITEM_LOCKED = "item_locked";
    public static final String LOCK_SCROLL = "lock_scroll";
    public static final String PURGE_SCROLL = "purge_scroll";
    public static final String PURGE_RETURN_BOOKS = "purge_return_books";
    public static final String RANDOMIZER_STONE = "randomizer_stone";
    public static final String PROTECTION_GEM = "protection_gem";
    public static final String ITEM_IS_PROTECTED = "item_is_protected";
    public static final String STAT_TRACKER_ITEM = "stat_tracker_item";
    public static final String STAT_TRACKER_APPLIED = "stat_tracker_applied";
    public static final String STAT_BLOCKS_MINED = "stat_blocks_mined";
    public static final String STAT_MOBS_KILLED = "stat_mobs_killed";
    public static final String STAT_PLAYERS_KILLED = "stat_players_killed";
    public static final String STAT_FISH_CAUGHT = "stat_fish_caught";
    public static final String LORE_START = "lore_start";
    public static final String LORE_COUNT = "lore_count";
    public static final String LORE_PLACEHOLDER = "lore_placeholder";
    public static final String LORE_HID_ENCHANTS = "lore_hid_enchants";
    public static final String MMOITEMS_NAMESPACE = "mmoitems";
    public static final String MMOITEMS_TYPE = "type";
    public static final String MMOITEMS_ID = "id";
    public static final String ADVANCED_ENCHANTMENTS_NAMESPACE = "advancedenchantments";
    public static final String AE_ENCHANTMENT_PREFIX = "ae_enchantment-";
    public static final String AE_SLOTS = "slots";

    private PersistentKeyNames() {
    }
}
