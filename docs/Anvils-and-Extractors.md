# Anvils and Extractors

## Anvil Book Combining

Two SinceEnchantments books can be combined if:

- Both books have the same enchantment ID.
- Both books have the same level.
- Next level does not exceed max level.

Result:

- Level increases by 1.
- Success rate is averaged.
- Destroy/failure rate is averaged.
- XP cost uses `settings.anvil-xp-combine-books`.

Config:

```yaml
settings:
  anvil-xp-combine-books: 5
```

## Anvil Book Application

Books can be applied through anvils when valid.

Validation includes:

- Target material
- Whitelist
- Requirements
- Conflicts
- Slot capacity
- Max level

Config:

```yaml
settings:
  anvil-xp-apply-book: 3
```

## MMOCore Experience Sync

If MMOCore is hooked, SinceEnchantments syncs vanilla level state after anvil result take.

Logs:

```text
Successfully hooked into MMOCore API (Latest Version) for Anvil EXP sync!
Successfully hooked into MMOCore API (Legacy Version)!
```

## Random Extractor

Command:

```text
/se giveextractor Steve random 1
```

Behavior:

1. Drag extractor onto enchanted item.
2. Plugin chooses one enchantment randomly.
3. Removes it from the item.
4. Gives an enchantment book with level preserved.
5. Extractor is consumed.

## Specific Extractor

Command:

```text
/se giveextractor Steve specific 1
```

Behavior:

1. Drag extractor onto enchanted item.
2. Extractor is consumed temporarily.
3. Player chooses an enchantment.
4. Selected enchant is removed.
5. Book is given.
6. If cancelled, extractor is refunded.

## Extractor Modes

```yaml
settings:
  extractor-mode: "DIALOG"
```

| Mode     | Description            |
|----------|------------------------|
| `DIALOG` | Modern Paper Dialog UI |
| `GUI`    | Legacy chest GUI       |

## Dialog Config

File:

```text
gui.yml
```

```yaml
dialog:
  extractor:
    title: "<dark_gray><bold>Extract Enchantment"
    body: "<gray>Choose an enchantment from your item below\n<gray>to extract it safely into a book."
    columns: 3
```

## Legacy GUI Config

```yaml
gui:
  extractor:
    title: "&8Select Enchant (Page %page%)"
    size: 54
```

GUI size must be a multiple of 9.

## Extraction Safety

Extraction is blocked if:

- Item is stacked.
- Item is locked.
- Item has no enchantments.
- Selected enchant no longer exists by the time the player clicks.

