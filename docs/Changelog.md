# Changelog

This page is maintained as a high-level documentation changelog. Release artifacts and build jars are published by the release workflow.

## Unreleased

### Added

- Added a dedicated random enchantment book command:

```text
/se giverandombook <player> [filters]
```

- Added random book filters that can be combined in one command:
  - `-level <number>` selects a random enchantment book at the exact requested level and rejects matches whose max level is lower.
  - `-rarity <rarity>` selects only enchantments with matching rarity metadata.
  - `-target <target>` selects only enchantments with matching target metadata.
  - `-type <type>` selects only enchantments from a specific source.
  - `-success <number|numbertoNumber>` sets fixed or randomized success rates.
  - `-failure <number|numbertoNumber>` sets fixed or randomized failure/destroy rates.
  - `-amount <number|numbertoNumber>` sets fixed or randomized book amounts.
- Added `-s` silent mode for `/se givebook` and `/se giverandombook` to suppress command-executor feedback, useful for console rewards or automation.
- Added natural tab completion for random filters, filter values, ranges, and silent mode after each completed filter.
- Added source type aliases for random filtering:
  - `vanilla`, `minecraft`, `mc`
  - `since`, `se`, `custom`
  - `ae`, `advancedenchantments`
  - `ce`, `crazyenchantments`
  - `ee`, `excellentenchants`
- Added ExcellentEnchants integration:
  - Detects `ExcellentEnchants` as an optional dependency.
  - Hooks through the real ExcellentEnchants API dependency, not reflection.
  - Adds Gradle dependencies for `su.nightexpress.excellentenchants:Core:5.4.3` and `su.nightexpress.nightcore:main:2.16.2`.
  - Reads registered ExcellentEnchants entries through `EnchantRegistry.getRegistered()`.
  - Auto-registers `excellentenchants:<id>` metadata for book creation, lore, tab completion, and random filtering.
  - Reads best-effort metadata from ExcellentEnchants enchant config files under `plugins/ExcellentEnchants/enchants`.
  - Falls back to Bukkit enchantment registry data for max level and item applicability.
- Added configurable default description text for ExcellentEnchants dynamic entries:

```yaml
settings:
  ee-default-description:
    - "&7Special effect from"
    - "&7ExcellentEnchants."
```

### Changed

- `/se givebook` is kept focused on specific books and remains backward-compatible with the old format:

```text
/se givebook <player> <enchant> <level> [success] [destroy]
```

- `/se givebook` no longer accepts `random`; use `/se giverandombook` instead.
- Random book selection now uses the same metadata accessors as normal book creation, so SinceEnchantments, vanilla Bukkit enchantments, AdvancedEnchantments, CrazyEnchantments, and ExcellentEnchants can all participate in filtered random results.
- Book creation now validates the requested level against the selected enchantment max level before giving the item.
- Help text and command docs now show the dedicated random book syntax.
- Random book filters now use space-separated option/value pairs, such as `-level 1`, so tab completion can advance naturally from filter names to values and back to remaining filters.

### Examples

```text
/se giverandombook Steve
/se giverandombook Steve -level 1 -target WEAPON -type ae
/se giverandombook Steve -rarity LEGENDARY -type ee -success 40to90 -failure 10to30 -amount 1to2 -s
/se givebook Steve "minecraft:sharpness" 5 60 40 -s
```

## Current Documentation Set

Added complete GitHub Wiki coverage for:

- Installation
- Quick start
- Commands and permissions
- Core concepts
- Enchantment configuration
- Full config reference
- Item utilities
- Lore and visual engine
- Limits and whitelists
- MMOItems and custom item hooks
- AdvancedEnchantments, CrazyEnchantments, and ExcellentEnchants hooks
- Anvils and extractors
- Developer API and external addons
- Troubleshooting
- FAQ
- Wiki build workflow
