# External Enchantment Plugins

SinceEnchantments can display, apply, extract, and slot-count enchantments from supported external enchantment plugins.

## ID Format

| Plugin               | ID Format                          |
|----------------------|------------------------------------|
| AdvancedEnchantments | `ae:<enchant_name>`                |
| CrazyEnchantments    | `ce:<enchant_name>`                |
| ExcellentEnchants    | `excellentenchants:<enchant_name>` |

Examples:

```text
ae:harvest
ae:vampire
ce:lifesteal
excellentenchants:tunnel
```

## AdvancedEnchantments Hook

Startup behavior:

1. Detect plugin named `AdvancedEnchantments`.
2. Reflect `net.advancedplugins.ae.api.AEAPI`.
3. Read `getAllEnchantments()`.
4. If the API returns `0`, retry several times because AE can finish enchant loading after its plugin enable phase.
5. If still `0`, scan `plugins/AdvancedEnchantments/enchantments.yml` as a fallback.

Useful logs:

```text
AdvancedEnchantments detected (version ...). Preparing AE API hook.
AdvancedEnchantments API hook is ready, but the enchantment list is currently empty.
AdvancedEnchantments API returned 0 enchantments on attempt 1/5.
AdvancedEnchantments API returned 0 enchantments. Fallback config scan found ... entries.
AdvancedEnchantments auto-load finished from config: registered ..., skipped ..., failed ....
```

## CrazyEnchantments Hook

CrazyEnchantments uses its public API dependency.

Startup behavior:

1. Detect plugin named `CrazyEnchantments`.
2. Read `CrazyManager`.
3. Read registered enchantments.
4. Auto-register missing `ce:` entries in memory.

## ExcellentEnchants Hook

Startup behavior:

1. Detect plugin named `ExcellentEnchants`.
2. Read `su.nightexpress.excellentenchants.enchantment.EnchantRegistry`.
3. Read registered `CustomEnchantment` entries.
4. Read display name, max level, description, and item sets from the API.
5. Read optional rarity/target overrides from `plugins/ExcellentEnchants/enchants`.
6. Auto-register missing `excellentenchants:` entries in memory.

## Auto-Registration

External enchantments do not need to be manually added to `enchants.yml`.

When detected, SinceEnchantments registers them in memory with:

- ID
- Display name
- Max level
- Rarity, when available from plugin metadata or fallback config
- Target, when available from plugin metadata or fallback item applicability
- Description

Default descriptions:

```yaml
settings:
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

## Overrides

Add an override only when you want custom display metadata.

```yaml
custom-enchants:
  "ae:harvest":
    name: "Harvest"
    rarity: "LEGENDARY"
    max-level: 5
    target: "TOOL"
    description:
      - "&7Chance to harvest crops in an area."
```

```yaml
custom-enchants:
  "excellentenchants:tunnel":
    name: "Tunnel"
    rarity: "RARE"
    max-level: 3
    target: "TOOL"
```

```yaml
custom-enchants:
  "ce:lifesteal":
    name: "Lifesteal"
    rarity: "EPIC"
    max-level: 3
    target: "WEAPON"
```

## Applying External Enchants

Use books as usual:

```text
/se givebook Steve "ae:harvest" 1 100 0
```

```text
/se givebook Steve "ce:lifesteal" 1 100 0
```

```text
/se givebook Steve "excellentenchants:tunnel" 1 100 0
```

When hooks are active, SinceEnchantments delegates AdvancedEnchantments and CrazyEnchantments apply/remove behavior to
those plugins. ExcellentEnchants registers its enchantments in Bukkit's enchantment registry, so SinceEnchantments
applies those books through the normal Bukkit enchantment path while using the ExcellentEnchants API for metadata and
random filtering.

## Troubleshooting External Hooks

| Symptom                                      | Cause                                                               | Fix                                                                                                                      |
|----------------------------------------------|---------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `Found 0 enchantments` for AE                | AE has not finished loading enchant config yet                      | Update to latest build; retry and config fallback handle this                                                            |
| `ae:` commands do not suggest                | AE not installed, disabled, or fallback config missing              | Check startup logs and `plugins/AdvancedEnchantments/enchantments.yml`                                                   |
| Book applies but effect does not work        | External plugin rejected apply or item is not valid for that plugin | Check console warning and external plugin rules                                                                          |
| CE hook fails                                | CrazyManager not ready or API changed                               | Check CE version and startup logs                                                                                        |
| EE hook fails at startup                     | ExcellentEnchants or NightCore version mismatch                     | Use matching ExcellentEnchants and NightCore versions, then rebuild with the Gradle dependency versions documented above |
| `excellentenchants:` commands do not suggest | EE is not installed, disabled, or registry is empty                 | Check `ExcellentEnchants detected ...` and `Successfully hooked into ExcellentEnchants API!` startup logs                |
