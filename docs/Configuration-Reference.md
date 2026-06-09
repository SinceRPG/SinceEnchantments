# Configuration Reference

This page is the index for every generated config file.

## File Overview

| File | Purpose |
| --- | --- |
| `settings.yml` | Global mechanics and visual rules |
| `enchants.yml` | Custom enchant definitions and vanilla display metadata |
| `limits.yml` | Slot caps, slot gem caps, and item whitelists |
| `items.yml` | Utility item templates |
| `gui.yml` | Extractor Dialog and GUI settings |
| `messages.yml` | Player-facing messages and console logs |
| `custom-items.yml` | Third-party custom item PDC detection |

## settings.yml

### Lore Placeholder

```yaml
settings:
  placeholder: "{enchants}"
```

If an item lore line contains this placeholder, SinceEnchantments replaces that line with visual enchant lore.

### Packet Cache

```yaml
packet-cache-expire-ms: 1000
packet-cache-max-size: 5000
```

Lower values refresh visuals faster. Higher values reduce repeated formatting work.

### Vanilla Override

```yaml
override-vanilla-enchants: true
```

If true:

- Vanilla enchants are hidden from the default tooltip.
- SinceEnchantments renders them inside the custom lore layout.

If false:

- Vanilla enchants are left alone.
- Only custom enchants are rendered by SinceEnchantments.

### Display Layout

```yaml
enchants-per-line: 2
separator: "&8 | "
default-color: "&9"
add-empty-line-above: true
add-empty-line-below: true
level-format: "ROMAN"
divider: "&8&m                         &8&m"
detailed-display-threshold: 5
```

`level-format` accepts:

- `ROMAN`
- `NUMBER`

### Slot Defaults

```yaml
default-max-custom-enchants-per-item: 5
max-slot-modifiers-allowed: 5
```

Material-specific and custom item-specific values are configured in `limits.yml`.

### Anvil Costs

```yaml
anvil-xp-combine-books: 5
anvil-xp-apply-book: 3
```

Used by the anvil listener when combining books or applying books in anvils.

### Extractor Mode

```yaml
extractor-mode: "DIALOG"
```

Values:

| Value | Behavior |
| --- | --- |
| `DIALOG` | Uses Paper's modern Dialog API |
| `GUI` | Uses legacy chest GUI |

### Protection and Tracker

```yaml
consume-protection-on-death: true
protected-format: "&a&lProtected &7(Keeps on death)"
tracker-header: "&8&m      &r &6&lStat Tracker &8&m      "
tracker-blocks: "&7Blocks Mined: &e%value%"
tracker-mobs: "&7Mobs Killed: &e%value%"
tracker-players: "&7Players Killed: &c%value%"
tracker-fish: "&7Fish Caught: &b%value%"
```

### External Hook Descriptions

```yaml
ae-default-description:
  - "&7Special effect from"
  - "&7AdvancedEnchantments."

ce-default-description:
  - "&7Special effect from"
  - "&7CrazyEnchantments."

ee-default-description:
  - "&7Special effect from"
  - "&7ExcellentEnchants."
```

Used when external enchantments are auto-registered without an override in `enchants.yml`.

### Target Categories

```yaml
enchant-targets:
  WEAPON:
    - "*_SWORD"
    - "*_AXE"
```

Used by `target` in custom enchant definitions.

### Enchantable Gear

```yaml
enchantable-gear-suffixes:
  - "_SWORD"
  - "_AXE"

enchantable-gear-exact:
  - "BOW"
  - "SHIELD"
```

These controls decide which vanilla materials automatically receive visual lore when they do not have `{enchants}`.

## limits.yml

See [Limits and Whitelists](Limits-and-Whitelists).

## items.yml

See [Items and Utilities](Items-and-Utilities).

## gui.yml

See [Anvils and Extractors](Anvils-and-Extractors).

## messages.yml

Messages use legacy color codes and placeholders.

Example:

```yaml
enchant-success: "&aEnchantment successful! Your item grows stronger."
```

## custom-items.yml

See [MMOItems and Custom Items](MMOItems-and-Custom-Items).
