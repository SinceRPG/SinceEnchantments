# Developer API and Addons

SinceEnchantments supports internal modules and external addon jars.

## Base Class

Custom enchant modules extend:

```java
net.danh.sinceenchantments.modules.SinceEnchant
```

Minimal module:

```java
package example;

import net.danh.sinceenchantments.modules.SinceEnchant;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class ExampleEnchant extends SinceEnchant {
    public ExampleEnchant() {
        super("example:example");
    }

    @EventHandler
    public void onDamage(EntityDamageByEntityEvent event) {
        // Read level with getLevel(item)
    }
}
```

## Useful SinceEnchant Methods

| Method                            | Purpose                        |
|-----------------------------------|--------------------------------|
| `getId()`                         | Returns enchant ID             |
| `getLevel(ItemStack item)`        | Reads enchant level on an item |
| `getName()`                       | Config display name            |
| `getMaxLevel()`                   | Config max level               |
| `getRarity()`                     | Config rarity                  |
| `isApplicable(Material material)` | Target check                   |
| `hasConflict(ItemStack item)`     | Conflict check                 |

Protected config helpers:

| Method                                      | Reads                                 |
|---------------------------------------------|---------------------------------------|
| `getInt(key, def)`                          | `custom-enchants.<id>.settings.<key>` |
| `getDouble(key, def)`                       | Same                                  |
| `getString(key, def)`                       | Same                                  |
| `getBoolean(key, def)`                      | Same                                  |
| `sendMessage(player, key, replacements...)` | `custom-enchants.<id>.messages.<key>` |

## Internal Modules

Internal modules live in:

```text
net.danh.sinceenchantments.modules
```

The internal loader scans that package and registers non-abstract `SinceEnchant` classes.

## External Addon Jars

External addons are loaded from:

```text
plugins/SinceEnchantments/Enchantments/
```

On first startup, the folder is created automatically.

Addon loading:

1. Each jar is opened.
2. Classes are scanned.
3. Non-abstract subclasses of `SinceEnchant` are instantiated using a no-args constructor.
4. Each enchant is registered as a Bukkit listener.

## Addon Requirements

Your addon enchant class must:

- Extend `SinceEnchant`.
- Be non-abstract.
- Have a public no-args constructor.
- Use a stable namespaced ID.
- Avoid shading SinceEnchantments itself.

## Config Entry for Addon Enchants

```yaml
custom-enchants:
  "example:example":
    name: "Example"
    rarity: "COMMON"
    max-level: 3
    target: "WEAPON"
    description:
      - "&7An external addon enchantment."
    settings:
      chance-per-level: 5
    messages:
      activate: "&aExample activated!"
```

## Registry Behavior

The registry stores active `SinceEnchant` modules and registers them as event listeners.

Dynamic external plugin hooks such as `ae:` and `ce:` are registered in `EnchantManager`, not as `SinceEnchant` event
modules.

## Persistent Data

Important key concepts:

| Data                   | Meaning                              |
|------------------------|--------------------------------------|
| `custom_enchants`      | SinceEnchantments custom enchant map |
| `book_enchant_id`      | Book enchant ID                      |
| `book_enchant_level`   | Book level                           |
| `book_success_rate`    | Book success rate                    |
| `book_destroy_rate`    | Book failure/destroy metadata        |
| `slot_modifier`        | Slot gem modifier value              |
| `item_locked`          | Lock state                           |
| `item_is_protected`    | Protection gem state                 |
| `stat_tracker_applied` | Stat tracker state                   |

Do not manually edit these keys unless you understand the storage format.

