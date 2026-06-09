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
| `/se givebook <player> <enchant> <level> [success] [destroy] [-s]` | Gives a specific enchantment book |
| `/se giverandombook <player> [options]` | Gives filtered random enchantment books |
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

Give a random enchantment book:

```text
/se giverandombook Steve
```

Give a random level 1 weapon enchantment book from AdvancedEnchantments:

```text
/se giverandombook Steve -level:1 -target:WEAPON -type:ae
```

Give one or two random legendary ExcellentEnchants books with random success and failure rates, silently from console:

```text
/se giverandombook Steve -rarity:LEGENDARY -type:ee -success:40to90 -failure:10to30 -amount:1to2 -s
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
- Auto-loaded `excellentenchants:` IDs
- Bukkit vanilla enchant IDs
`giverandombook` options are suggested after the player argument:

- `-level:<number>` limits random selection to enchantments that support the exact level and gives books at that level
- `-rarity:<rarity>` limits random selection to matching rarity metadata
- `-target:<target>` limits random selection to matching target metadata
- `-type:<type>` limits random selection to one enchantment source
- `-success:<number|numbertoNumber>` sets a fixed or random success rate
- `-failure:<number|numbertoNumber>` sets a fixed or random failure/destroy rate
- `-amount:<number|numbertoNumber>` sets a fixed or random amount from 1 to 64
- `-s` suppresses the success/error message sent to the command executor

Supported built-in type aliases:

| Alias | Source |
| --- | --- |
| `vanilla`, `minecraft`, `mc` | Bukkit/Minecraft enchantments |
| `since`, `se`, `custom` | SinceEnchantments custom/addon enchantments |
| `ae`, `advancedenchantments` | AdvancedEnchantments |
| `ce`, `crazyenchantments` | CrazyEnchantments |
| `ee`, `excellentenchants` | ExcellentEnchants |

Multiple random filters can be combined. For example, `-level:1 -type:ae -target:WEAPON` only chooses AdvancedEnchantments entries that are known as weapon enchantments and support level 1.

If `-level` is omitted, the command chooses a random valid level between `1` and the selected enchantment's max level.

If an external enchant does not appear:

1. Check startup logs.
2. Confirm the external plugin loaded before SinceEnchantments.
3. Run `/se reload`.
4. For AdvancedEnchantments, confirm its `enchantments.yml` exists and contains enchant sections with `levels`.
5. For ExcellentEnchants, confirm the plugin is enabled and its enchant files exist under `plugins/ExcellentEnchants/enchants`.
