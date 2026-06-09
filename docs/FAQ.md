# FAQ

## Is lore stored permanently on items?

No. SinceEnchantments stores enchant data in item metadata and renders lore visually through packets.

## Why use `{enchants}`?

It gives you exact control over where visual enchant lore appears. This is especially important for MMOItems.

## Can I use vanilla enchantments?

Yes. Use IDs like:

```text
minecraft:sharpness
minecraft:unbreaking
```

## Can I use AdvancedEnchantments?

Yes. Use:

```text
ae:<enchant_name>
```

SinceEnchantments does not need to bundle the AdvancedEnchantments jar.

## Can I use CrazyEnchantments?

Yes. Use:

```text
ce:<enchant_name>
```

## Can I use ExcellentEnchants?

Yes. Use Bukkit registry IDs like:

```text
excellentenchants:<enchant_name>
```

ExcellentEnchants is hooked through its real API dependency and requires NightCore on the server, the same as ExcellentEnchants itself.

## Why are external enchant entries not written into enchants.yml automatically?

They are registered in memory to avoid modifying server configs unexpectedly. Add overrides only for display customization.

## Can locked items still be extracted or purged?

No. Lock Scroll prevents enchanting, extraction, and purging.

## Can Slot Gems reduce slots?

Yes. Use a negative modifier:

```text
/se giveslotgem Steve -1 1
```

## Does the plugin support Folia?

Yes. It declares Folia support and uses Folia-aware scheduling helpers.

## Does `/se reload` reload everything?

It reloads SinceEnchantments configs and re-runs external auto-load registration. For dependency changes, restart the server.
