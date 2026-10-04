# Mob pools and linked rewards

Use a Minecraft 1.20.1 datapack (`pack_format: 15`). Override these files under `data/spiral_abyss/`:

- `mob_pools/fodder_mob.json`, `common_mob.json`, `champion_mob.json`, `mini_boss.json`, `boss.json`.
- `mob_equipment/armor_mobs.json`, `melee_mobs.json`, `bow_users.json`, `crossbow_users.json`, `trident_users.json`.

Each equipment file is an array of entity IDs, for example `["minecraft:skeleton", "example:archer"]`. Gear is always enabled. Per-floor gear items, probabilities and enchantments remain in `config/spiral_abyss/gear/floor_N.json`.

A pool supports plain mobs and mobs with optional chest rewards:

```json
[
  "minecraft:zombie",
  [
    "minecraft:skeleton",
    "minecraft:arrow",
    {"item": "example:gem", "min_count": 1, "max_count": 3, "chance": 0.15},
    {"loot_table": "example:chests/dungeon", "weight": 2, "chance": 0.25}
  ]
]
```

String bonuses identify registered items first, otherwise loot tables. Prefer explicit objects for modded entries. Bonuses default to a **25% chance** when `chance` is omitted. A table's weight controls selection among floor tables; its chance is checked after selection. Items are checked independently on every reward roll.

Linked entries augment `floor_N/floor.json` rewards only when that mob actually spawned during the successful attempt. Each mob type contributes once across all waves, regardless of its count. They use the floor's `rewards.rolls` and claim-time eligible-player scaling; set rolls explicitly if using only linked rewards. Zero rolls disables item payouts. Only opened chest contents are saved; their payout cursors never reroll. Unclicked offers are forfeited on departure.

Run `/reload` for updated definitions. Equipment eligibility and linked loot apply to the next attempt. Mob choices remain fixed for the current global cycle; new pool contents take effect on the next cycle, or after operator `/spiral_abyss reset` (which clears everyone's progress). Current pools also repair missing saved mob choices.

The common TOML now contains only `reset_days` and `mix_mobs`. Move customized category and equipment lists from old TOML files into these datapack arrays; `can_equip_gear` is removed.

Bundled fodder counts per wave:

| Floor | Wave 1 | Wave 2 | Wave 3 |
| --- | --- | --- | --- |
| 1 | 8 | 9 | 10 |
| 2 | 12 | 13 | 14 |
| 3 | 16 | 17 | 18 |
| 4 | 20 | 21 | 22 |
| 5 | 24 | 25 | 26 |
| 6 | 28 | 29 | 30 |

Existing common mobs, champions and bosses are additional to these counts.

## Bundled chest variety

All floors include dungeon, abandoned-mineshaft, igloo and village-temple tables. Floors 2–6 add jungle-temple and village-weaponsmith loot; floors 3–6 also include buried-treasure, stronghold-library and optional `minecraft:chests/chest_level_2` loot. Floors 4–6 add stronghold-corridor loot. Only floors 5–6 include desert-pyramid, optional `minecraft:chests/chest_level_3` and optional `minecraft:chests/shater` loot.

Buried treasure no longer requires drowned and is restricted to floors 3–6. It has weight 1 and a 40% chance after selection; weaponsmith and stronghold-corridor tables also have weight 1, with 75% and 65% chances after selection respectively. These expand the available table pool without increasing reward rolls.

Themed tables are linked to mobs in `mob_pools/*.json`, rather than included unconditionally in `floor.json`:

| Encountered mob | Additional eligible chest tables |
| --- | --- |
| Drowned | Small/big underwater ruins, shipwreck map/supply/treasure |
| Pillager, vindicator, evoker, ravager | Pillager outpost, woodland mansion |
| Vex | Woodland mansion |
| Warden | Ancient city, ancient-city ice box |
| Wither skeleton, blaze, ghast, wither | Nether fortress, ruined portal, optional `minecraft:chests/firewell_d` |
| Piglin brute | The same nether tables plus bastion bridge, hoglin stable, other and treasure |
| Shulker | End-city treasure |

Linked table chances range from 35% to 70% **after weighted selection**, so these rewards are never guaranteed. Repeated enemies or multiple mobs sharing the same reward entry do not multiply its weight. The existing two-roll floor budget and XP remain unchanged.

The four IDs `minecraft:chests/chest_level_2`, `minecraft:chests/chest_level_3`, `minecraft:chests/firewell_d` and `minecraft:chests/shater` are not vanilla 1.20.1 tables. They retain the supplied IDs and participate only if an installed mod or datapack provides them. Missing tables are excluded before selection. `/reload` applies these reward changes to the next attempt without resetting the current mob lineup or player progress.

## Wave challenges and chest rewards

Every floor now requires exactly three waves. Each has a fixed five-minute limit, with a five-second countdown between waves. Legacy `total_time` and `wave_delay` JSON fields are accepted but ignored.

A wave awards one star for finishing in under three minutes, two for under two minutes, or three for under one minute. Finishing at exactly a threshold does not earn that threshold's star. Your best **claimed** result for each wave is retained. The looted floor artwork means nine committed stars.

Three to five total stars unlocks the center chest; six to eight adds the left chest; nine adds the right chest. Fewer than three earns no chest or saved clear. Retry the floor to improve your result. Each tier pays only once per player until reset. Clicking an already-claimed chest can save an improved replay result without awarding loot again.

Each chest checks the still-present combat participants when opened. Departed players and players who already claimed its tier contribute nothing. Stars and entitlement are committed at that interaction, before the chest animates, and all eligible present teammates are credited together. A chest with no eligible players renders looted. Players who leave before claiming keep only their previous committed results.

Each tier uses one third of the solo floor loot/XP budget per eligible player. Claiming all three preserves the old solo reward budget in expectation. Party combat scaling stays fixed at attempt start. Personal activation-block resets and global cycle resets both clear stars and tier claims.

Old saves retain claimed-floor unlocks as three stars and the first chest tier; they do not receive nine-star mastery automatically. Old unopened rewards are abandoned. Both clients and servers require the updated mod (network protocol 7).
