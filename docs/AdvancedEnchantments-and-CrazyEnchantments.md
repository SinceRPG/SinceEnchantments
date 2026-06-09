# AdvancedEnchantments and CrazyEnchantments

SinceEnchantments can display, apply, extract, and slot-count enchantments from supported external enchantment plugins.

## ID Format

| Plugin | ID Format |
| --- | --- |
| AdvancedEnchantments | `ae:<enchant_name>` |
| CrazyEnchantments | `ce:<enchant_name>` |

Examples:

```text
ae:harvest
ae:vampire
ce:lifesteal
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

## Auto-Registration

External enchantments do not need to be manually added to `enchants.yml`.

When detected, SinceEnchantments registers them in memory with:

- ID
- Display name
- Max level
- Default rarity
- Default target
- Default description

Default descriptions:

```yaml
settings:
  ae-default-description:
    - "&7Special effect from"
    - "&7AdvancedEnchantments."

  ce-default-description:
    - "&7Special effect from"
    - "&7CrazyEnchantments."
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

When hooks are active, SinceEnchantments delegates apply/remove behavior to the external plugin.

## Troubleshooting External Hooks

| Symptom | Cause | Fix |
| --- | --- | --- |
| `Found 0 enchantments` for AE | AE has not finished loading enchant config yet | Update to latest build; retry and config fallback handle this |
| `ae:` commands do not suggest | AE not installed, disabled, or fallback config missing | Check startup logs and `plugins/AdvancedEnchantments/enchantments.yml` |
| Book applies but effect does not work | External plugin rejected apply or item is not valid for that plugin | Check console warning and external plugin rules |
| CE hook fails | CrazyManager not ready or API changed | Check CE version and startup logs |

