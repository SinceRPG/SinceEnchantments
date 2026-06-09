# Troubleshooting

## Enchantment Says Success but Item Does Not Change

Possible causes:

- Another plugin restored the old item after click.
- External plugin rejected the enchant.
- Item is an MMOItems item without stable sync.
- Book was consumed before item write in older builds.

Fix:

1. Update to the latest SinceEnchantments build.
2. Check console for:

```text
Failed to write enchantment ...
```

3. Confirm item is not stacked.
4. Confirm item is not locked.
5. Confirm whitelist allows the enchant.
6. Confirm the external hook is active for `ae:`, `ce:`, or `excellentenchants:` IDs.

## Lore Disappears Until Inventory Reopens

Cause:

Packet lore is visual. Rapid click/drop actions can make the client show a cleaned server item until the slot is resent.

Fixes:

- Update to a build that clears cache on click/drag.
- Lower cache expiry:

```yaml
packet-cache-expire-ms: 250
```

- Make sure the item has `{enchants}` if it is an MMOItems item.

## MMOItems Item Does Not Show Lore

Checklist:

- MythicLib or MMOItems is installed.
- Item has valid MMOItems type/id.
- Item lore contains `{enchants}`.
- Item has an enchant or a whitelist preview.
- `override-vanilla-enchants` is set as desired.

## Enchant Is Not Whitelisted

Add a matching rule:

```yaml
mmoitems-whitelist:
  SWORD:*:
    - since:lifesteal
```

or:

```yaml
item-whitelist:
  "*_SWORD":
    - since:lifesteal
```

## Slot Limit Reached Too Early

Check:

- `settings.default-max-custom-enchants-per-item`
- `item-max-slots`
- `mmoitems-max-slots`
- `custom-item-max-slots`
- Slot Gem modifier value
- `settings.max-slot-modifiers-allowed`

## AdvancedEnchantments Shows 0 Enchants

Since AE can finish enchant loading after plugin enable, SinceEnchantments retries and then falls back to config scanning.

Look for:

```text
AdvancedEnchantments API returned 0 enchantments on attempt ...
AdvancedEnchantments API returned 0 enchantments. Fallback config scan found ...
```

If fallback finds 0:

- Confirm `plugins/AdvancedEnchantments/enchantments.yml` exists.
- Confirm enchant sections have a `levels` section.
- Confirm AdvancedEnchantments is enabled.

## CrazyEnchantments Hook Fails

Check:

- CrazyEnchantments is installed and enabled.
- Version includes the expected Paper API.
- Startup log mentions CrazyManager readiness.

## ExcellentEnchants Hook Fails

Check:

- ExcellentEnchants is installed and enabled.
- NightCore is installed and enabled.
- Gradle dependencies use compatible NightCore and ExcellentEnchants API versions.
- Startup log mentions `Successfully hooked into ExcellentEnchants API`.
- ExcellentEnchants has registered enchantments before SinceEnchantments runs `/se reload`.

If `-rarity:<rarity>` filters do not match ExcellentEnchants books, add an override in `enchants.yml` or confirm the ExcellentEnchants enchant config has a supported rarity-style key such as `Rarity`, `Tier`, or `Group`.

## Vanilla Enchants Duplicate

Set:

```yaml
override-vanilla-enchants: true
```

If another plugin also rewrites vanilla enchant lore, test with:

```yaml
override-vanilla-enchants: false
```

## Extractor Does Nothing

Check:

- Extractor is on cursor.
- Target item has enchantments.
- Target item is not locked.
- Target item amount is 1.
- Specific extractor mode is valid: `DIALOG` or `GUI`.

## Anvil Result Missing

Check:

- Book IDs match when combining books.
- Book levels match when combining books.
- Result level does not exceed max level.
- Target item passes whitelist, requirement, conflict, slot, and target checks.

## What to Send in a Bug Report

Include:

- SinceEnchantments version.
- Server software and version.
- Java version.
- PacketEvents version.
- Optional hooks installed.
- Full startup log.
- Full error log.
- Relevant item config and enchant config.
- Steps to reproduce.
