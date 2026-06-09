# Limits and Whitelists

Limits and whitelists are configured in:

```text
plugins/SinceEnchantments/limits.yml
```

## Pattern Syntax

| Pattern | Matches |
| --- | --- |
| `*` | Everything |
| `*_SWORD` | Any material ending in `_SWORD` |
| `DIAMOND_*` | Any material starting with `DIAMOND_` |
| `*DIAMOND*` | Any value containing `DIAMOND` |
| `BOW` | Exact value |
| `SWORD:*` | Any MMOItems item of type `SWORD` |
| `*:LEGENDARY_*` | Any custom item ID starting with `LEGENDARY_` |

## Vanilla Item Max Slots

```yaml
item-max-slots:
  "*_SWORD": 2
  "*_CHESTPLATE": 3
  "*DIAMOND*": 1
```

If multiple vanilla rules match, values are summed.

Example:

`DIAMOND_SWORD` matches:

- `*_SWORD`: 2
- `*DIAMOND*`: 1

Total: `3`.

## MMOItems Max Slots

```yaml
mmoitems-max-slots:
  SWORD:*: 2
  "*:LEGENDARY_*": 3
  ARMOR:MYTHIC_CHESTPLATE: 5
```

MMOItems keys use:

```text
TYPE:ID
```

## Custom Item Max Slots

```yaml
custom-item-max-slots:
  "SWORD:*": 2
  "*:LEGENDARY_*": 3
  "MYTHICMOBS:EXCALIBUR": 5
```

Custom item keys come from `custom-items.yml` hook formats.

## Slot Modifier Caps

Slot gems modify item max slots.

```yaml
item-max-slot-modifiers:
  "*_SWORD": 3

mmoitems-max-slot-modifiers:
  SWORD:*: 4

custom-item-max-slot-modifiers:
  "MYTHICMOBS:EXCALIBUR": 8
```

The global cap is:

```yaml
settings:
  max-slot-modifiers-allowed: 5
```

## Whitelist Rules

Whitelists restrict which enchantments can be applied.

```yaml
item-whitelist:
  "*_SWORD":
    - since:lifesteal
    - minecraft:sharpness
```

If an item matches a whitelist, only listed enchantments are allowed.

If no whitelist matches, all enchantments are allowed unless another validation blocks them.

## MMOItems Whitelist

```yaml
mmoitems-whitelist:
  SWORD:*:
    - since:fireball
    - since:thunder
    - minecraft:unbreaking
```

## Custom Item Whitelist

```yaml
custom-item-whitelist:
  "MYTHICMOBS:EXCALIBUR":
    - "since:fireball"
```

## Whitelist Preview

Enable preview:

```yaml
show-whitelist-preview: true
whitelist-header: "&8Allowed Enchantments:"
whitelist-preview-format: "&8 - %enchant_name%"
```

Items with a whitelist can show unapplied allowed enchantments in lore.

