# Commands and Permissions

## Root Command

```text
/sinceenchantments
```

Default aliases:

```text
/se
/sinceenchant
```

Aliases are configured in `settings.yml`:

```yaml
settings:
  command-aliases:
    - "se"
    - "sinceenchant"
```

## Permission

All built-in admin commands require:

```text
sinceenchantments.admin
```

## Command Reference

| Command | Description |
| --- | --- |
| `/se help` | Shows the help menu |
| `/se reload` | Reloads configuration files and dynamic hook registrations |
| `/se givebook <player> <enchant> <level> [success] [destroy]` | Gives an enchantment book |
| `/se giveextractor <player> <random\|specific> <amount>` | Gives an extractor |
| `/se givecharm <player> <bonus> [amount]` | Gives a success charm |
| `/se giveslotgem <player> <modifier> [amount]` | Gives a slot gem |
| `/se givelock <player> [amount]` | Gives a lock scroll |
| `/se givepurge <player> <return_books> [amount]` | Gives a purge scroll |
| `/se giverandomizer <player> [amount]` | Gives a randomizer stone |
| `/se giveprotector <player> [amount]` | Gives a protection gem |
| `/se givetracker <player> [amount]` | Gives a stat tracker |

## Examples

Give a guaranteed custom enchantment book:

```text
/se givebook Steve "since:lifesteal" 1 100 0
```

Give a risky enchantment book:

```text
/se givebook Steve "minecraft:sharpness" 5 60 40
```

Give a random extractor:

```text
/se giveextractor Steve random 3
```

Give a specific extractor:

```text
/se giveextractor Steve specific 1
```

Give a success charm that adds 15 percent:

```text
/se givecharm Steve 15 1
```

Give a slot gem that adds two max slots:

```text
/se giveslotgem Steve 2 1
```

Give a slot gem that removes one max slot:

```text
/se giveslotgem Steve -1 1
```

Give a lock scroll:

```text
/se givelock Steve 1
```

Give a purge scroll that returns books:

```text
/se givepurge Steve true 1
```

Give a purge scroll that does not return books:

```text
/se givepurge Steve false 1
```

Give a protection gem:

```text
/se giveprotector Steve 1
```

Give a stat tracker:

```text
/se givetracker Steve 1
```

## Tab Completion

The enchantment argument suggests:

- Built-in custom enchant IDs
- Configured custom enchant IDs
- Auto-loaded `ae:` IDs
- Auto-loaded `ce:` IDs
- Bukkit vanilla enchant IDs

If an external enchant does not appear:

1. Check startup logs.
2. Confirm the external plugin loaded before SinceEnchantments.
3. Run `/se reload`.
4. For AdvancedEnchantments, confirm its `enchantments.yml` exists and contains enchant sections with `levels`.

