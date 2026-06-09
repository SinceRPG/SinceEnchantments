# Lore and Visual Engine

SinceEnchantments uses packet-side lore rendering. This is one of the most important parts of the plugin.

## How It Works

1. Real enchant data is stored on the item.
2. Before the item is sent to a player, PacketEvents intercepts the item packet.
3. SinceEnchantments clones the item.
4. Existing injected lore is cleaned from the clone.
5. New visual lore is generated.
6. The modified clone is sent to the client.
7. The real server item remains clean.

## Why Packet Lore?

Packet lore prevents:

- Permanent duplicate enchant lines.
- MMOItems lore corruption.
- Admin item flags being overwritten unintentionally.
- Visual-only data from becoming real item data.

## Placeholder Mode

Default placeholder:

```yaml
settings:
  placeholder: "{enchants}"
```

If an item contains:

```text
{enchants}
```

the visual enchant block is inserted there.

This is the recommended mode for MMOItems.

## Automatic Gear Mode

If there is no placeholder, items can still show visual enchant lore if their material matches:

```yaml
enchantable-gear-suffixes:
  - "_SWORD"
  - "_AXE"
  - "_PICKAXE"

enchantable-gear-exact:
  - "BOW"
  - "SHIELD"
```

## Detailed vs Compact Display

```yaml
detailed-display-threshold: 5
```

If total enchant count is less than or equal to the threshold:

```text
Lifesteal I
Heals you for a portion
of your max health on hit.
```

If above threshold:

```text
Lifesteal I | Thunder III
Excavator II | Unbreaking III
```

## Vanilla Override

```yaml
override-vanilla-enchants: true
```

When true, SinceEnchantments hides vanilla enchant lines and renders them in its own layout.

When false, vanilla enchants stay in the vanilla tooltip and custom enchants render separately.

## Empty Line Controls

```yaml
add-empty-line-above: true
add-empty-line-below: true
```

These keep the enchant block visually separated from item stats and descriptions.

## Cache Controls

```yaml
packet-cache-expire-ms: 1000
packet-cache-max-size: 5000
```

If item lore appears stale after rapid inventory interaction, lower `packet-cache-expire-ms`.

Recommended troubleshooting value:

```yaml
packet-cache-expire-ms: 250
```

## Creative Mode Safety

Creative inventory packets are cleaned before reaching the server. This prevents packet-injected lore from being saved into real item data by creative actions.

## Common Lore Problems

| Symptom | Likely Cause | Fix |
| --- | --- | --- |
| Lore does not show on MMOItems item | Missing `{enchants}` placeholder | Add placeholder to MMOItems lore |
| Lore only appears after reopening inventory | Client slot sync race | Update to latest build; cache is cleared on click/drag |
| Vanilla enchant lines duplicate | `override-vanilla-enchants` conflict or another plugin rewriting lore | Test with override false |
| Item stats disappear | Another plugin stores stats in lore and no placeholder is used | Use placeholder mode |

