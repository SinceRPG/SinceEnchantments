# Installation

This page covers server requirements, dependencies, install order, update safety, and first startup checks.

## Requirements

| Requirement          | Status    | Notes                                                                                   |
|----------------------|-----------|-----------------------------------------------------------------------------------------|
| Paper                | Required  | `api-version: 1.21`; designed for modern Paper                                          |
| Java                 | Required  | Build uses modern Java tooling; run with the Java version required by your Paper server |
| PacketEvents         | Required  | Must load before SinceEnchantments                                                      |
| Folia                | Supported | Plugin declares `folia-supported: true` and uses Folia-aware schedulers                 |
| MythicLib            | Optional  | Used for MMOItems item detection                                                        |
| MMOItems             | Optional  | Supported through PDC and MythicLib NBT detection                                       |
| MMOCore              | Optional  | Used for anvil experience sync                                                          |
| AdvancedEnchantments | Optional  | Auto-hooked through runtime reflection                                                  |
| CrazyEnchantments    | Optional  | Auto-hooked through its public API dependency                                           |
| NightCore            | Optional  | Required by ExcellentEnchants when that hook is used                                    |
| ExcellentEnchants    | Optional  | Auto-hooked through its public API dependency                                           |

## Plugin Load Order

SinceEnchantments declares these plugin dependencies in `paper-plugin.yml`:

```yaml
dependencies:
  server:
    packetevents:
      load: BEFORE
      required: true
    MythicLib:
      load: BEFORE
      required: false
    MMOCore:
      load: BEFORE
      required: false
    AdvancedEnchantments:
      load: BEFORE
      required: false
    CrazyEnchantments:
      load: BEFORE
      required: false
    ExcellentEnchants:
      load: BEFORE
      required: false
```

## Install Steps

1. Stop the server.
2. Install PacketEvents into `plugins/`.
3. Install optional plugins you use, such as MythicLib, MMOItems, MMOCore, AdvancedEnchantments, CrazyEnchantments,
   NightCore, or ExcellentEnchants.
4. Put `SinceEnchantments.jar` into `plugins/`.
5. Start the server.
6. Confirm startup logs mention PacketEvents initialization and SinceEnchantments version support.
7. Stop the server if you want to edit generated config files.
8. Edit configs under `plugins/SinceEnchantments/`.
9. Start again or use `/se reload` after safe config edits.

## First Startup Files

The plugin creates these files:

| File               | Purpose                                                                                |
|--------------------|----------------------------------------------------------------------------------------|
| `settings.yml`     | Global behavior, lore display, slots, tracker categories, target categories            |
| `enchants.yml`     | Built-in custom enchants, external enchant overrides, vanilla enchant display metadata |
| `limits.yml`       | Item slot limits, slot modifier limits, whitelists                                     |
| `items.yml`        | Enchantment books and utility item templates                                           |
| `gui.yml`          | Extractor Dialog and legacy GUI display settings                                       |
| `messages.yml`     | Player messages and startup/hook logs                                                  |
| `custom-items.yml` | Custom item detection hooks for third-party plugins                                    |

## Updating

Before updating:

1. Back up `plugins/SinceEnchantments/`.
2. Back up player data if your server stores inventories externally.
3. Check release notes.
4. Update dependencies first when needed.
5. Start in a test server if you use MMOItems or external enchant hooks.

The `auto-update` section in `settings.yml` controls whether missing keys are added from jar defaults.

```yaml
auto-update:
  enchants: false
  limits: false
  messages: true
  items: false
  gui: false
  custom-items: false
```

Recommended production behavior:

- Keep `messages: true` so new message keys are added.
- Keep gameplay configs manual unless you want default keys merged automatically.

## Startup Checklist

Look for logs like:

```text
Running natively for Paper 1.21+ | NMS: ...
Successfully hooked into MythicLib/MMOItems NBT API!
AdvancedEnchantments detected ...
CrazyEnchantments detected ...
ExcellentEnchants detected ...
Successfully auto-registered ... enchants from internal modules!
```

Not every optional hook must be present. Missing optional plugins should log a skip or fallback, not a crash.

## Important Security Note for Premium Jars

Do not commit premium plugin jars such as AdvancedEnchantments to GitHub. SinceEnchantments does not need to ship the
AdvancedEnchantments jar. The hook uses runtime reflection and reads the installed plugin on the server.

ExcellentEnchants is integrated through compile-time API dependencies:

```gradle
compileOnly("su.nightexpress.nightcore:main:2.16.2")
compileOnly("su.nightexpress.excellentenchants:Core:5.4.3")
```

Those jars are still provided by the server at runtime. They should not be bundled into SinceEnchantments.

Recommended repository rule:

```gitignore
/libs/*.jar
```
