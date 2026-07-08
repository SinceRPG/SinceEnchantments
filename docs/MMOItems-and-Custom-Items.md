# MMOItems and Custom Items

SinceEnchantments is designed to work with MMOItems-style servers.

## MMOItems Detection

MMOItems is supported out of the box.

Detection methods:

1. PersistentDataContainer keys:
    - `mmoitems:type`
    - `mmoitems:id`
2. MythicLib NBT API fallback:
    - `NBTItem.get(item)`
    - `hasType()`
    - `getType()`
    - `getString("MMOITEMS_ITEM_ID")`

Detected format:

```text
TYPE:ID
```

Example:

```text
SWORD:DRAGON_BLADE
ARMOR:MYTHIC_CHESTPLATE
```

## Recommended MMOItems Lore Setup

Add the enchant placeholder where the enchant block should appear:

```yaml
lore-format:
  - '{bar}'
  - '#lore#'
  - '#gem-stone-lore#'
  - '{bar}'
  - '#set#'
  - '{bar}'
  - '#tier#'
  - '{enchants}'
  - '#durability#'
```

This avoids appending enchant lore below unrelated MMOItems stats.

## MMOItems Limits

```yaml
mmoitems-max-slots:
  SWORD:*: 3
  ARMOR:*: 4
```

## MMOItems Whitelist

```yaml
mmoitems-whitelist:
  SWORD:*:
    - since:lifesteal
    - since:thunder
    - minecraft:unbreaking
```

## Custom Item Hooks

File:

```text
custom-items.yml
```

Example:

```yaml
hooks:
  itemsadder:
    pdc-keys:
      - "itemsadder:id"
    format: "ITEMSADDER:{id}"
```

If an item has PDC key `itemsadder:id` with value `ruby_sword`, SinceEnchantments sees:

```text
ITEMSADDER:RUBY_SWORD
```

## Built-In Hook Examples

| Hook            | PDC Keys                         | Format                 |
|-----------------|----------------------------------|------------------------|
| MythicMobs      | `mythicmobs:type`, `mythic-type` | `{id}`                 |
| ItemsAdder      | `itemsadder:id`                  | `ITEMSADDER:{id}`      |
| Oraxen          | `oraxen:id`                      | `ORAXEN:{id}`          |
| EcoItems        | `eco:id`                         | `ECOITEMS:{id}`        |
| ExecutableItems | `executableitems:id`             | `EXECUTABLEITEMS:{id}` |

## Adding a New Custom Item Plugin

1. Find the plugin's PDC key.
2. Add it to `custom-items.yml`.
3. Choose a stable format.
4. Add matching rules in `limits.yml`.

Example:

```yaml
hooks:
  myitems:
    pdc-keys:
      - "myitems:item_id"
    format: "MYITEMS:{id}"
```

```yaml
custom-item-max-slots:
  "MYITEMS:*": 3

custom-item-whitelist:
  "MYITEMS:FROST_SWORD":
    - "since:thunder"
```

## Common MMOItems Issues

| Problem                                         | Fix                                                     |
|-------------------------------------------------|---------------------------------------------------------|
| Enchant applies but lore appears in wrong place | Add `{enchants}` to MMOItems lore                       |
| Enchant says not whitelisted                    | Add `TYPE:ID` or `TYPE:*` to `mmoitems-whitelist`       |
| Slot limit is wrong                             | Add `mmoitems-max-slots` rule                           |
| Stat tracker rejects item                       | Add MMOItems type to `settings.stat-tracker-categories` |
| Lore appears only after reopening inventory     | Update to latest build and lower packet cache if needed |

