# Items and Utilities

SinceEnchantments ships a full item utility system. All utility item visuals are configured in `items.yml`.

## Item Template Features

Most item templates support:

| Field | Meaning |
| --- | --- |
| `material` | Bukkit material |
| `name` | Display name with legacy colors or MiniMessage |
| `item-name` | Paper 1.21 item name |
| `item-model` | Namespaced item model key |
| `tooltip-style` | Paper tooltip style key |
| `lore` | Lore lines |
| `custom-model-data` | Legacy integer or Paper component map |
| `unbreakable` | Makes item unbreakable |
| `hide-tooltip` | Hides tooltip |
| `glint-override` | Forces or removes enchantment glint |
| `glider` | Enables glider behavior where supported |
| `enchantable` | Overrides vanilla enchantability |
| `max-stack-size` | Sets max stack size where supported |
| `rarity` | Vanilla item rarity |
| `damage` | Initial durability damage |
| `flags` | Bukkit ItemFlags |
| `enchants` | Vanilla enchantments on the utility item |
| `attributes` | Bukkit attribute modifiers |

## Enchantment Book

Command:

```text
/se givebook <player> <enchant> <level> [success] [destroy]
```

Behavior:

- Drag onto an item to apply.
- Uses success chance.
- Stores book data in PDC.
- Supports vanilla, custom, AdvancedEnchantments, and CrazyEnchantments IDs.

Placeholders:

| Placeholder | Meaning |
| --- | --- |
| `%enchant_name%` | Display name |
| `%level%` | Book level |
| `%success%` | Success rate |
| `%destroy%` | Failure/destroy rate |
| `%rarity_name%` | Rarity key |
| `%rarity_color%` | Rarity color |
| `%description%` | Multi-line enchant description |

## Success Charm

Command:

```text
/se givecharm <player> <bonus> [amount]
```

Behavior:

- Drag onto an enchantment book.
- Raises success chance.
- Lowers destroy chance by the same amount.
- Cannot exceed 100 percent success.

## Slot Gem

Command:

```text
/se giveslotgem <player> <modifier> [amount]
```

Behavior:

- Drag onto an item.
- Adds or removes slot modifier value.
- Respects `settings.max-slot-modifiers-allowed`.
- Does not work on locked items.

## Lock Scroll

Command:

```text
/se givelock <player> [amount]
```

Behavior:

- Toggles item lock state.
- Locked items cannot be enchanted, purged, or extracted from.
- Visual lock line is configured in `settings.locked-format`.

## Purge Scroll

Command:

```text
/se givepurge <player> <return_books> [amount]
```

Behavior:

- Removes all enchantments from an item.
- Can return enchantment books if `return_books` is true.
- Does not work on locked items.

## Randomizer Stone

Command:

```text
/se giverandomizer <player> [amount]
```

Behavior:

- Drag onto an enchantment book.
- Randomizes success and destroy values from 0 to 100.

## Protection Gem

Command:

```text
/se giveprotector <player> [amount]
```

Behavior:

- Marks an item as protected.
- Protected item can be saved on death.
- If `consume-protection-on-death` is true, the protection is removed after saving the item.

## Stat Tracker

Command:

```text
/se givetracker <player> [amount]
```

Behavior:

- Drag onto weapons, tools, or fishing rods.
- Tracks blocks mined, mobs killed, players killed, or fish caught.
- Valid item categories are configured in `settings.stat-tracker-categories`.

