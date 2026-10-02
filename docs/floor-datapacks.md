# Floor datapacks

Create a normal Minecraft 1.20.1 datapack with `pack_format: 15`, install it under `<world>/datapacks`, and run `/reload`. Use the `spiral_abyss` namespace for floor files, even if the referenced mobs/loot tables belong to another mod.

```text
my_abyss_pack/
  pack.mcmeta
  data/spiral_abyss/
    floor_1/
      floor.json
      wave_1.json
      wave_2.json
      wave_3.json
    floor_2/
      floor.json
      wave_1.json
    loot_tables/chests/my_reward.json
```

`pack.mcmeta`:

```json
{"pack":{"pack_format":15,"description":"My Spiral Abyss floors"}}
```

`floor_1/floor.json` combines floor attributes and loot configuration:

```json
{
  "health_scale": 1.25,
  "damage_scale": 1.1,
  "wave_delay": 5,
  "rewards": {
    "entries": [
      {"loot_table": "minecraft:chests/simple_dungeon", "weight": 2, "chance": 1.0},
      {"item": "minecraft:diamond", "min_count": 1, "max_count": 3, "chance": 0.5},
      {"item": "minecraft:emerald", "count": 6, "chance": 1.0}
    ],
    "rolls": 2,
    "experience": 100
  }
}
```


Each reward roll chooses one **loot-table** entry by `weight / total_table_weight`, then checks that table's `chance`. Independently, it checks **every item entry** against that item's `chance` and adds each successful item stack alongside the table loot. A failed table or item chance does not cancel the other rewards. Only tables use `weight`; an existing item `weight` remains accepted but is ignored.

With the example above and `rolls: 2`, the dungeon chest table runs twice. Each roll also has a 50% chance to add 1-3 diamonds and always adds 6 emeralds: 12 guaranteed emeralds total, with two independent diamond attempts. Entries use the original chest loot context. You can also use only items or only tables.

Party size multiplies roll count and XP by 1, 1.5, 2 or 2.5 for one through four players; fractional rolls use a probabilistic extra roll. Mob health and damage use the same party multiplier on top of the selected floor/wave attributes. All multipliers use the roster captured when the floor starts.

Victory saves the rolled bundle before creating the chest. The bundle pops out when shaking begins, 11 ticks (0.55 seconds) after opening starts. Claims are recorded immediately, while the chest finishes its 20-tick shake and settles into the looted pose. Rejoining during shaking cannot pay twice. Reloading never rerolls a pending reward.

`floor_1/wave_1.json`:

```json
{
  "total_time": 90,
  "bosses": [
    {"id": "minecraft:zombie", "count": 3},
    "minecraft:skeleton"
  ]
}
```

An ID string means one mob; an object allows a count. All entries spawn together at the start of the wave. The timer starts after spawning. The next wave starts only when **all** tracked enemies have died, followed by `wave_delay` seconds. Discarding/unloading a living mob or moving it to another dimension does not count as a kill; the attempt fails. Grounded/falling mobs outside the platform return to the center; airborne mobs can use the larger bounded flight area.

A harder wave can override either multiplier:

```json
{
  "total_time": 150,
  "health_scale": 2.0,
  "damage_scale": 1.5,
  "bosses": [
    {"id": "minecraft:zombie", "count": 4},
    {"id": "minecraft:skeleton", "count": 4}
  ]
}
```

Wave multipliers **replace** floor defaults; they do not multiply the floor multiplier again. Health scales the mob's initialized maximum health and starts it at full health, subject to Minecraft's attribute limits. Damage scales attributed incoming damage from that mob, including melee and projectiles. Other damage/armor systems still apply normally.

| Field | Default | Rules |
| --- | --- | --- |
| `floor.json`: `health_scale`, `damage_scale` | `1` | Positive, finite numbers, at most `1000` |
| `floor.json`: `wave_delay` | `5` | Whole seconds, `0` to `300`; delay between cleared waves |
| `rewards.entries` | `[]` | Up to 64 combined item/table entries |
| Entry `item` or `loot_table` | Required | Exactly one registry item ID or chest loot-table ID |
| Entry `weight` | `1` | Integer `1` to `10000`; relative selection weight for tables only (ignored on items) |
| Entry `chance` | `1` | Number `0` to `1`; table success after selection, or independent item chance per roll |
| Entry `count` | `1` | Item count, integer `1` to `4096` |
| Entry `min_count`, `max_count` | `count`, `min_count` | Inclusive random item-count range, `1` to `4096` |
| `rewards.loot_tables` | `[]` | Legacy shorthand: each table adds weight `1`, chance `1` |
| `rewards.rolls` | `1` with entries, otherwise `0` | Integer `0` to `100`; each roll chooses one table and checks every item bonus |
| `rewards.experience` | `0` | Integer `0` to `100000`, released once when the chest opens |
| `wave_N.json`: `total_time` | Required | Whole seconds, `1` to `86400` |
| `wave_N.json`: `bosses` | Required | Nonempty mob list, at most 128 mobs total |
| Boss object `count` | `1` | Integer `1` to `128` |
| Wave `health_scale`, `damage_scale` | Inherit floor | Same limits as floor fields |

Each floor requires `floor.json` and 1 to 100 consecutively numbered wave files, starting at 1. Floors must be numbered 1 to 6; files above floor 6 are not loaded. Ordinary mobs and modded bosses that extend Minecraft's `Mob` type are supported. Players, keys, reward chests, projectiles and other non-mob entities are not wave enemies. Boss-specific phase transformations that replace an entity need a future explicit phase definition; removal is not treated as victory.

Floor settings with no rewards are valid (`{}` supplies the defaults). A malformed file, unknown entity ID, or missing wave number disables that whole floor and logs the reason. Non-mob types and failed/blocked spawns fail the attempt and clean up partial spawns. Use existing **chest** loot tables or author chest tables under `data/<namespace>/loot_tables/`; the table ID omits `loot_tables/` and `.json`.

The loader uses normal per-file datapack priority. A higher-priority `wave_1.json` replaces the bundled file of the same name. Lower-priority wave files still exist unless overridden: overriding just wave 1 does not remove bundled waves 2 and 3. To reduce the bundled wave count, use Minecraft datapack `pack.mcmeta` filters to block lower-pack floor resources, then provide the replacement floor and waves. Example filter for floor 1:

```json
{
  "pack": {"pack_format": 15, "description": "Replace floor 1"},
  "filter": {"block": [{"namespace": "spiral_abyss", "path": "floor_1/.*"}]}
}
```

Reloading cancels the current attempt and removes its surviving mobs; right-click the key to retry with the new definition. It does not clear completed floor progress or grant another set of rewards. `next_floor` requires the current floor to be claimed and a valid definition for the following floor; it never advances past 6.

Mob loot is disabled throughout the Spiral Abyss dimension, including non-wave mobs. The `rewards` section is the intended source of floor loot and XP. This suppression does not remove player inventory/death drops or floor rewards. See the README for custom-mod drop-hook coverage.

Use `/spiral_abyss floor <1-6>` to enter your own arena on any loaded floor. This allows direct selection without clearing earlier floors. Claimed floors remain claimed when revisited. Switching floors is blocked during a fight or while the current floor has an unclaimed chest; it does not reset progress or generate more terrain. `/spiral_abyss join <host>` still accepts a friend invitation.
