---
layout: home
title: SinceEnchantments Wiki
---

# SinceEnchantments Wiki

> A modern Paper custom-enchantments system built for clean item lore, drag-and-drop progression, MMOItems compatibility, Folia-ready scheduling, and optional hooks into AdvancedEnchantments and CrazyEnchantments.

[![Paper 1.21+](https://img.shields.io/badge/Paper-1.21%2B-2f81f7)](Installation)
[![Folia Supported](https://img.shields.io/badge/Folia-supported-238636)](Installation)
[![PacketEvents Required](https://img.shields.io/badge/PacketEvents-required-f0b429)](Installation)
[![MMOItems Ready](https://img.shields.io/badge/MMOItems-ready-8957e5)](MMOItems-and-Custom-Items)

SinceEnchantments gives server owners a full enchantment economy without forcing item lore to become messy permanent NBT. It stores real enchant data in stable item metadata and renders polished lore visually through packets. Players get a clean drag-and-drop experience, while admins keep deep control over limits, whitelists, item utilities, external enchant hooks, vanilla enchant display, and custom item compatibility.

## What You Get

| Area | Included |
| --- | --- |
| Custom enchantments | Built-in modules, external addon loading, config-driven metadata, requirements, conflicts, targets, descriptions, and rarity colors |
| Vanilla enchantments | Optional visual override for all vanilla enchants, configurable names, colors, and descriptions |
| Item utilities | Enchantment books, random and specific extractors, success charms, slot gems, lock scrolls, purge scrolls, randomizer stones, protection gems, and stat trackers |
| MMOItems support | MMOItems and MythicLib item detection, custom item whitelists, custom slot rules, and placeholder-driven lore injection |
| External hooks | Auto-registration for AdvancedEnchantments and CrazyEnchantments with `ae:` and `ce:` IDs |
| Interfaces | Paper Dialog extractor UI on modern Paper, legacy chest GUI fallback, Brigadier commands |
| Safety | Locking, protection-on-death, slot caps, max slot modifiers, whitelist previews, conflict checks, and requirement checks |
| Performance | Packet lore caching, packet-side visual formatting, Folia-safe delayed scheduling |

## Recommended Reading Order

1. [Installation](Installation)
2. [Quick Start](Quick-Start)
3. [Core Concepts](Core-Concepts)
4. [Commands and Permissions](Commands-and-Permissions)
5. [Configuration Reference](Configuration-Reference)
6. [Troubleshooting](Troubleshooting)

## Design Philosophy

SinceEnchantments separates item truth from item presentation.

- Real data is stored in PersistentDataContainer keys.
- Visual lore is injected only when items are sent to players.
- Fake injected lore is cleaned before inventory actions are processed.
- External enchant plugins can remain the source of truth for their own enchants.
- Config files stay readable and server-owner friendly.

This matters most on MMOItems servers, where the item itself may already contain custom stats, models, item IDs, and lore structure.

## Feature Map

| Feature | Page |
| --- | --- |
| Install dependencies and server requirements | [Installation](Installation) |
| First book, first enchant, first extractor | [Quick Start](Quick-Start) |
| Every command and permission | [Commands and Permissions](Commands-and-Permissions) |
| Custom enchant format | [Enchantments](Enchantments) |
| All config files | [Configuration Reference](Configuration-Reference) |
| Utility item behavior | [Items and Utilities](Items-and-Utilities) |
| Packet lore renderer | [Lore and Visual Engine](Lore-and-Visual-Engine) |
| Slot rules and whitelists | [Limits and Whitelists](Limits-and-Whitelists) |
| MMOItems, MythicMobs, ItemsAdder, Oraxen | [MMOItems and Custom Items](MMOItems-and-Custom-Items) |
| AdvancedEnchantments and CrazyEnchantments | [AdvancedEnchantments and CrazyEnchantments](AdvancedEnchantments-and-CrazyEnchantments) |
| Anvils and extraction | [Anvils and Extractors](Anvils-and-Extractors) |
| Developer modules and addon jars | [Developer API and Addons](Developer-API-and-Addons) |
| Known problems and fixes | [Troubleshooting](Troubleshooting) |

## Quick Example

Give yourself a Lifesteal book:

```text
/se givebook YourName "since:lifesteal" 1 100 0
```

Drag the book onto a valid weapon. If the item passes target, whitelist, requirement, conflict, slot, lock, and success-rate checks, the enchantment is written to the item and its lore updates visually.

## Support Notes

Always include this information when reporting issues:

- Server software and exact version
- SinceEnchantments version
- PacketEvents version
- Whether Folia is used
- Whether MMOItems, MythicLib, AdvancedEnchantments, or CrazyEnchantments are installed
- Full console logs from startup and the failed action
- Relevant config snippets

