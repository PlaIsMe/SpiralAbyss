# Spiral Abyss

<p align="center">
  <a href="https://www.youtube.com/@pla_is_me" title="YouTube" style="text-decoration:none;">
    <img src="https://res.cloudinary.com/dxehc2hn3/image/upload/v1761811663/youtube_icon_bcbanf.png" alt="YouTube" width="80" height="80">
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://discord.com/invite/EsvPsnqJpz" title="Discord" style="text-decoration:none;">
    <img src="https://res.cloudinary.com/dxehc2hn3/image/upload/v1761811736/discord_icon_bg_rp4omx.png" alt="Discord" width="80" height="80">
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://ko-fi.com/pla_is_me" title="Ko-fi" style="text-decoration:none;">
    <img src="https://res.cloudinary.com/dxehc2hn3/image/upload/v1761815764/kofi_icon_bg_sy3foi.png" alt="Ko-fi" width="80" height="80">
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://www.curseforge.com/minecraft/mc-mods/annoying-villagers-herobrine-invasion" title="CurseForge" style="text-decoration:none;">
    <img src="https://res.cloudinary.com/dxehc2hn3/image/upload/v1761811729/cruseforge_icon_bg_gucfxc.png" alt="CurseForge" width="80" height="80">
  </a>
  &nbsp;&nbsp;&nbsp;&nbsp;
  <a href="https://modrinth.com/mod/annoying-villagers-herobrine-invasion" title="Modrinth" style="text-decoration:none;">
    <img src="https://res.cloudinary.com/dxehc2hn3/image/upload/v1761815749/modrinth_icon_bg_q4jxrz.png" alt="Modrinth" width="80" height="80">
  </a>
</p>

## Boss environment examples

Add these fields to a boss's metadata object in `data/spiral_abyss/mob_pools/boss.json`, keeping reward objects separate:

```json
[
  ["minecraft:elder_guardian", {
    "environment": ["minecraft:prismarine"],
    "decoration_environment": ["half_water"],
    "entity_place": [
      {"entity": "minecraft:guardian", "position": "north far", "count": 2},
      {"entity": "minecraft:guardian", "position": "north east corner middle", "count": 1}
    ],
    "block_place": [
      {"block": "minecraft:sea_lantern", "position": "middle", "count": 1},
      {"block": "minecraft:prismarine", "position": "close_south", "count": 4}
    ]
  }]
]
```

- `full_water`: fills the 65 x 65 base footprint 120 blocks high, Y=65–184.
- `half_water`: fills it 15 blocks high, Y=65–79, then lifts players and the boss to the water surface.
- `entity_place`: a list of vanilla or installed mod entity IDs, positions and counts. Entities initially face the boss and are removed with its environment; they do not add to wave counts or rewards.
- `block_place`: the same format with block IDs; temporary blocks restore their original positions when the environment ends.

The player ceiling remains Y=120. Water cannot flow in the Abyss, including out of the base. Other dimensions retain normal water flow. Full-water placements use the underwater solid surface; half-water placements use the water/solid surface. Defeat, cancellation, failure and reload clean up the environment.

Use `middle` for the key position, or north/south/east/west and diagonal directions with `close`, `middle` or `far`: `close_north`, `north middle`, `north east corner far`. Far positions remain inside the playable boundary. Counts spread objects around the chosen anchor. Either nonempty placement list centers the boss. Placements appear after terrain/water growth and use the resulting surface. Ordinary surface decorations such as soul fire can also accompany placement lists.

Replace IDs with installed mod IDs as needed. Each placement list supports at most 64 total objects; unavailable IDs and obstructed placements are skipped with warnings. Blocks use default states and must be stable terrain blocks (no fluids, falling blocks, TNT, plants or block entities). Players, item entities and arena furniture cannot be placed. Each water preset must be the only `decoration_environment` entry. `environment` is optional for water-only or placement-only setups.

## License & Use

All Rights Reserved

Third-party components listed below remain subject to their respective licenses.

## Credits

- Earth button artwork adapted from [earth-cube](https://sketchfab.com/3d-models/earth-cube-ed594f0b3a5b4bcc947561af91a9742e) by madexc, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The model was rendered for the menu button.
- Broadcast UI textures and portions of the animation/rendering code were adapted from Fancy Toasts by Bivrik, licensed under the MIT License.

### Fancy Toasts — MIT License

Copyright (c) 2026 Bivrik

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
