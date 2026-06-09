# Core Concepts

## Namespaced Enchantment IDs

SinceEnchantments uses namespaced IDs everywhere.

| Type | Example |
| --- | --- |
| Built-in custom enchant | `since:lifesteal` |
| Vanilla enchant | `minecraft:sharpness` |
| AdvancedEnchantments enchant | `ae:harvest` |
| CrazyEnchantments enchant | `ce:lifesteal` |
| ExcellentEnchants enchant | `excellentenchants:tunnel` |

Always use lowercase IDs in commands and configs unless a page explicitly says otherwise.

## Real Data vs Visual Lore

The plugin stores real enchant data in item metadata. Lore is rendered visually through packets.

Benefits:

- Keeps server-side item data clean.
- Prevents duplicate lore lines from stacking permanently.
- Allows MMOItems and custom item lore to stay structured.
- Lets vanilla and custom enchants be displayed in one consistent style.

Important behavior:

- Visual lore may not exist in raw NBT.
- Creative-mode item edits are cleaned before reaching the server.
- Inventory clicks clean injected lore before processing.
- The client receives fresh visual lore after item updates.

## Enchantment Application Flow

When a player drags a book onto an item:

1. The listener detects a SinceEnchantments book using PDC keys.
2. The target item is validated.
3. Slot limits are checked.
4. Requirements and conflicts are checked.
5. Success chance is rolled.
6. The enchantment is written to the item.
7. The clicked slot is synced.
8. Packet lore cache is cleared.
9. The player sees the updated visual lore.

## Slot System

Each item has a maximum number of custom enchantment slots.

Slot sources:

- `settings.default-max-custom-enchants-per-item`
- `limits.yml` material rules
- `limits.yml` custom item rules
- Slot Gem modifiers

If a custom item matches a custom item rule, custom item rules take priority over vanilla material rules.

## Requirements

An enchantment can require other enchantments.

Example:

```yaml
requires:
  - "minecraft:sharpness"
  - "since:thunder"
```

The target item must already have all required enchantments before the book can apply.

## Conflicts

Custom enchantments can conflict with other enchantment IDs.

Example:

```yaml
conflicts:
  - "since:lifesteal"
  - "minecraft:smite"
```

Vanilla enchantment conflicts are also respected when applying vanilla enchantments unless overridden by config behavior.

## Targets

Each custom enchant has a target:

```yaml
target: "WEAPON"
```

Target names resolve through `settings.enchant-targets`.

Example:

```yaml
enchant-targets:
  WEAPON:
    - "*_SWORD"
    - "*_AXE"
    - "TRIDENT"
```

Patterns support:

| Pattern | Meaning |
| --- | --- |
| `*` | Everything |
| `*_SWORD` | Ends with `_SWORD` |
| `DIAMOND_*` | Starts with `DIAMOND_` |
| `*DIAMOND*` | Contains `DIAMOND` |
| `BOW` | Exact match |

## External Enchants

External enchant plugins are not copied into SinceEnchantments.

Instead:

- AdvancedEnchantments uses `ae:<name>`.
- CrazyEnchantments uses `ce:<name>`.
- ExcellentEnchants uses Bukkit registry IDs such as `excellentenchants:<name>`.
- Missing entries are auto-registered in memory.
- Optional overrides can be added to `enchants.yml`.
