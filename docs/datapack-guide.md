# Customize Spiral Abyss with datapacks

You can change wave compositions, enemy pools, and chest rewards with a server-side datapack. Clients do not need to install the datapack.

These examples target Minecraft 1.21.1 / NeoForge. Use `pack_format: 48` and the singular `loot_table` directory.

## Folder structure

Create a folder inside your world's `datapacks` directory:

```text
<world>/datapacks/
└── MyAbyssTweaks/
    ├── pack.mcmeta
    └── data/
        ├── spiral_abyss/
        │   ├── floor_1/
        │   │   ├── floor.json          # Challenges and chest rewards
        │   │   ├── wave_1.json         # Wave 1 encounter choices
        │   │   ├── wave_2.json         # Wave 2 encounter choices
        │   │   └── wave_3.json         # Wave 3 encounter choices
        │   ├── floor_2/               # Same filenames; floors 2 through 6
        │   ├── wave_types/
        │   │   └── my_zombie_wave.json # Your custom encounter composition
        │   ├── mob_pools/
        │   │   ├── fodder_mob.json
        │   │   ├── common_mob.json
        │   │   ├── champion_mob.json
        │   │   ├── mini_boss.json
        │   │   └── boss.json
        │   └── mob_types/
        │       ├── armor_mobs.json
        │       ├── melee_mobs.json
        │       ├── ranged_mobs.json
        │       ├── bow_users.json
        │       ├── crossbow_users.json
        │       ├── trident_users.json
        │       └── ghost_mobs.json
        └── my_abyss_tweaks/
            └── loot_table/
                └── chests/
                    └── floor_1.json
```

**Include only files you want to change.** The tree shows available locations, not a list of required files. Omitted files keep the mod's defaults. Each included JSON file replaces the entire file at that path; arrays do not merge. Copy a default file out of the mod JAR before editing it if you want to keep its other entries. If distributing a ZIP, put `pack.mcmeta` and `data` directly at the ZIP root.

For 1.21.1, `pack.mcmeta` contains:

```json
{
  "pack": {
    "pack_format": 48,
    "description": "My Spiral Abyss tweaks"
  }
}
```

## Change a wave

The filenames are `wave_1.json`, `wave_2.json`, and `wave_3.json`, rather than `wave.json`. Each floor always has three five-minute waves with five-second transitions.

To change floor 1, wave 1, create `data/spiral_abyss/floor_1/wave_1.json`:

```json
{
  "wave_types": ["my_zombie_wave"]
}
```

Then create `data/spiral_abyss/wave_types/my_zombie_wave.json`:

```json
{
  "tier": 1,
  "sequence": [
    {
      "groups": [
        {"category": "fodder_mob", "role": "melee", "count": 12}
      ]
    },
    {
      "groups": [
        {"category": "common_mob", "role": "any", "count": 4}
      ]
    }
  ]
}
```

This spawns 12 melee fodder enemies, then 4 common enemies after the first stage clears. Groups in the same stage spawn together. Each group chooses a species from its category pool; the name `my_zombie_wave` alone does not force zombies. Use the pool override below to restrict the fodder species.

You may list several type names in `wave_types`; the server selects one, preferring a type not already used on that floor. Create separate type files to control all three waves. Valid categories are `fodder_mob`, `common_mob`, `champion_mob`, `mini_boss`, and `boss`. Roles are `melee`, `ranged`, and `any`. Counts must be 1–128, with at most 128 enemies across an encounter's stages.

Boss encounters are restricted to floor 5 wave 3 and floor 6 waves 2–3. Floor 5 wave 3 and floor 6 wave 2 require one boss; floor 6 wave 3 requires one or two. Use category `boss` with role `any` for those slots. Other waves must not contain bosses. A custom floor 7 is not supported; menu destination 7 is Endless Mode.

## Choose which mobs can spawn

For example, `data/spiral_abyss/mob_pools/fodder_mob.json` can contain:

```json
[
  "minecraft:zombie",
  "minecraft:husk"
]
```

This replaces the fodder pool everywhere it is used, including Endless Mode. With the custom wave above, the first stage chooses either zombies or husks. A pool containing only `"minecraft:zombie"` makes that category select zombies only. You can use installed mods' entity IDs in the same way.

To add a mob while preserving the existing pool, copy the complete original file and add its ID. Preserve any existing scaling and behavior metadata, especially boss environments and death/reward suppression. Do not put the key or reward chest entities into enemy pools, and do not add custom linked rewards to miniboss entries.

`mob_types` files describe equipment eligibility and combat roles, separately from difficulty pools. For example, `ranged_mobs.json` marks native ranged attackers; it does not give them a bow or ranged AI. Gear items, chances, and enchantments are configured under `config/spiral_abyss/gear/`, rather than in wave JSON.

## Override chest rewards

Edit `data/spiral_abyss/floor_1/floor.json` to change floor 1's chest rewards. Keep all three challenge lists when editing rewards. This complete example uses a custom chest table plus an independent emerald bonus:

```json
{
  "challenge": {
    "easy": ["time"],
    "medium": ["time"],
    "hard": ["time"]
  },
  "rewards": {
    "experience": 300,
    "entries": [
      {"loot_table": "my_abyss_tweaks:chests/floor_1", "weight": 1, "chance": 1.0},
      {"item": "minecraft:emerald", "min_count": 1, "max_count": 3, "chance": 0.5}
    ]
  }
}
```

Create the referenced table at `data/my_abyss_tweaks/loot_table/chests/floor_1.json`:

```json
{
  "type": "minecraft:chest",
  "pools": [
    {
      "rolls": 1,
      "entries": [
        {"type": "minecraft:item", "name": "minecraft:iron_ingot", "weight": 4},
        {"type": "minecraft:item", "name": "minecraft:diamond", "weight": 1}
      ]
    }
  ]
}
```

Each table roll yields one iron ingot or one diamond in this example. To use an existing valid chest table instead, reference its ID directly, such as `minecraft:chests/simple_dungeon`; no custom table file is needed.

- Copper, iron, and golden chests give 1, 2, and 3 table rolls per eligible player respectively. Each roll also checks every floor-defined item bonus independently.
- When several table entries exist, their relative `weight` controls selection. `chance` is between 0 and 1. A failed chance or empty table result tries another unattempted available table, so a table's chance is not necessarily the overall chance of getting chest loot.
- Floor-defined item bonuses use their own `chance` and count; table weights do not select between those item bonuses.
- Boss-linked rare items remain a separate golden-only reward: at most one item of count 1 across all bosses and eligible players. Their chance comes from the server config's `rare_reward_chance`.
- Already-opened chest bundles retain their saved contents. Editing the datapack does not reroll them.

Define your custom loot under your own namespace. If another mod's native table is malformed or unavailable, omit its reference rather than overriding that mod's loot data.

## Apply your changes

1. Install the folder or ZIP in `<world>/datapacks/` and run `/reload` as an operator.
2. Use `/datapack list enabled` to check that it loaded. If multiple packs replace the same file, the higher-priority pack wins; `/datapack enable "file/MyAbyssTweaks" last` gives this folder pack highest priority.
3. Check the server log for invalid floor, wave, mob, or loot IDs. Missing optional mods are skipped silently; invalid IDs belonging to installed mods are reported. Keep usable enemies in every category your waves require.
4. Retry the floor. Reloading cancels an active attempt. Mob choices are saved per global cycle, so adding pool entries does not reroll currently valid saved selections. To deliberately start a new cycle, use `/spiral_abyss reset` only when you intend to clear **everyone's progress and pending rewards**. Personal resets do not reroll the shared cycle.

Test your wave and rewards in a separate world before publishing the pack.
