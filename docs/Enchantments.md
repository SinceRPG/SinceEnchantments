# Enchantments

This page explains custom enchant configuration, vanilla enchant display metadata, external overrides, built-in modules, and book behavior.

## File

```text
plugins/SinceEnchantments/enchants.yml
```

## Custom Enchant Format

```yaml
custom-enchants:
  "since:lifesteal":
    name: "Lifesteal"
    rarity: "EPIC"
    max-level: 5
    target: "WEAPON"
    requires:
      - "minecraft:sharpness"
    conflicts:
      - "since:dodge"
    description:
      - "&7Heals you for a portion"
      - "&7of your max health on hit."
    settings:
      heal-multiplier: 1.0
    messages:
      action: "&a+ %amount% HP (Lifesteal)!"
```

## Fields

| Field | Required | Meaning |
| --- | --- | --- |
| `name` | Recommended | Display name shown in books and lore |
| `rarity` | Recommended | Key from `settings.yml` `rarities` |
| `max-level` | Recommended | Highest level allowed |
| `target` | Recommended | Target category from `settings.enchant-targets` |
| `requires` | Optional | Enchants that must already exist on the item |
| `conflicts` | Optional | Enchants that cannot coexist |
| `description` | Optional | Lines shown in detailed lore and books |
| `settings` | Module-specific | Values read by built-in or addon code |
| `messages` | Module-specific | Messages read by built-in or addon code |

## Built-In Enchantments

| ID | Target | Behavior |
| --- | --- | --- |
| `since:lifesteal` | `WEAPON` | Heals the attacker for a configured amount on hit |
| `since:fireball` | `SWORD` | Lets players launch fireballs with cooldown |
| `since:thunder` | `WEAPON` | Chance to strike lightning and deal extra damage |
| `since:excavator` | `TOOL` | Mines an area around the broken block |
| `since:dodge` | `ARMOR` | Chance to evade incoming damage |

## Rarities

Rarity colors are defined in `settings.yml`:

```yaml
rarities:
  COMMON: "&f"
  UNCOMMON: "&a"
  RARE: "&9"
  EPIC: "&5"
  LEGENDARY: "&6&l"
```

Use any key you define there.

## Targets

Target categories are defined in `settings.yml`:

```yaml
settings:
  enchant-targets:
    WEAPON:
      - "*_SWORD"
      - "*_AXE"
      - "TRIDENT"
      - "MACE"
```

Then use:

```yaml
target: "WEAPON"
```

## Requirements

Requirements can use any supported enchantment ID.

```yaml
requires:
  - "minecraft:efficiency"
  - "since:thunder"
  - "ae:harvest"
```

The target item must already have all listed enchantments.

## Conflicts

```yaml
conflicts:
  - "minecraft:smite"
  - "since:lifesteal"
```

If the item already has a conflicting enchantment, the book cannot be applied.

## Vanilla Enchant Display

Vanilla enchant metadata lives under `vanilla-enchants`.

```yaml
vanilla-enchants:
  "minecraft:sharpness":
    name: "Sharpness"
    color: "&c"
    description:
      - "&7Increases melee damage against all targets."
```

This does not create vanilla enchantments. It controls how existing vanilla enchantments are displayed by the visual lore engine.

## External Enchant Overrides

External enchants auto-register in memory.

Use overrides only when you want nicer display data:

```yaml
custom-enchants:
  "ae:harvest":
    name: "Harvest"
    rarity: "ULTIMATE"
    max-level: 5
    target: "TOOL"
    description:
      - "&7Chance to harvest crops in an area."
```

```yaml
custom-enchants:
  "ce:lifesteal":
    name: "Lifesteal"
    rarity: "EPIC"
    max-level: 3
    target: "WEAPON"
```

## Book Creation

Books store:

- Enchantment ID
- Level
- Success rate
- Destroy/failure rate metadata

Command:

```text
/se givebook Steve "since:lifesteal" 1 100 0
```

Players apply books by dragging them onto items.

