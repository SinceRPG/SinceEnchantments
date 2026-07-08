# Quick Start

This page gets a fresh server from install to a working enchantment flow.

## 1. Give Yourself a Book

```text
/se givebook YourName "since:lifesteal" 1 100 0
```

Arguments:

| Argument            | Meaning                         |
|---------------------|---------------------------------|
| `YourName`          | Target player                   |
| `"since:lifesteal"` | Enchantment ID                  |
| `1`                 | Book level                      |
| `100`               | Success chance                  |
| `0`                 | Destroy/failure chance metadata |

Quotes are recommended because Brigadier suggestions include namespaced IDs.

## 2. Apply the Book

1. Put a valid item in your inventory.
2. Pick up the enchantment book with your cursor.
3. Drag and drop it onto the item.
4. If checks pass, the book is consumed and the item receives the enchantment.

Checks include:

- Item is not stacked.
- Item is not locked.
- Enchantment target accepts the material.
- Item whitelist allows the enchantment.
- Requirements are met.
- Conflicts are clear.
- Slot limit is not exceeded.
- Success roll passes.

## 3. Make Lore Visible

SinceEnchantments renders enchant lore visually through packets. The item can show enchantments in two ways:

- The item is recognized as enchantable gear by material.
- The item lore contains the configured placeholder, default `{enchants}`.

For MMOItems, put this in the item lore where enchantments should appear:

```text
{enchants}
```

If no placeholder exists, SinceEnchantments appends visual enchant lore to recognized gear.

## 4. Test Utility Items

Give a random extractor:

```text
/se giveextractor YourName random 1
```

Give a specific extractor:

```text
/se giveextractor YourName specific 1
```

Give a slot gem:

```text
/se giveslotgem YourName 2 1
```

Give a lock scroll:

```text
/se givelock YourName 1
```

Give a purge scroll:

```text
/se givepurge YourName true 1
```

## 5. Reload After Config Edits

```text
/se reload
```

Reload updates:

- Settings
- Enchants
- Limits
- Messages
- Items
- GUI
- Dynamic AE/CE auto-load registrations

For deep plugin dependency changes, restart the server instead of only reloading.

## 6. First MMOItems Setup

In your MMOItems lore template:

```yaml
lore:
  - "&7Damage: &c10"
  - "{enchants}"
  - "&8A custom weapon."
```

In `limits.yml`, allow the item type:

```yaml
mmoitems-whitelist:
  SWORD:*:
    - since:lifesteal
    - minecraft:unbreaking

mmoitems-max-slots:
  SWORD:*: 3
```

The key format is:

```text
TYPE:ID
```

Examples:

- `SWORD:DRAGON_BLADE`
- `ARMOR:MYTHIC_CHESTPLATE`
- `TOOL:*`
- `*:LEGENDARY_*`

