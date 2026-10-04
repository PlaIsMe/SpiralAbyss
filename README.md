# Spiral Abyss

See [Mob pools and linked rewards](DATAPACKS.md) for the datapack format, configuration migration, and default wave sizes.

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

## Gate building blocks

Four gate pieces are available in the **Spiral Abyss** creative tab:

| Block ID (`spiral_abyss:`) | Placement |
| --- | --- |
| `abyss_gate_vertical` | Offset vertical post using gate texture 1; the offset faces the player in all four horizontal directions |
| `abyss_gate_horizontal` | Directional rail slab: bottom uses texture 1, top uses texture 2; two halves merge and waterlogging works like vanilla slabs |
| `abyss_gate_curve` | Gate texture 2, four horizontal directions and upside-down placement like stairs; retains its fixed curve rather than joining stair corners |
| `abyss_gate_activate_block` | 45-degree diamond, offset four pixels away from the placer with matching collision, zero light blocking and no neighboring-face occlusion |

The reference export named `abyss_gate_horizonal.json` uses the corrected `horizontal` registry name. Models, blockstates, item models, loot tables and textures are bundled. Gate pieces are mineable like cobblestone (hardness 2, blast resistance 6) and drop with a pickaxe. They have no crafting recipes yet. Build a 5x6 ring using four vertical posts, four curves, and six horizontal slabs; place the activation block in front of its top-center slab facing outward to activate it in the Overworld. Breaking the controller deactivates the portal. Use another activation-block item on the active controller to reset your personal progress (consumes one in survival). End City chests have a 25% bonus chance of an activation block. The old andesite portal, Abyss Key item and Abyss Draft are retired; existing keys migrate to activation blocks. The creative tab uses the supplied key artwork as its icon. Its collision uses thin box slices to approximate the rotated diamond and preserves the model's overhang. These models use Forge's [cutout render type](https://docs.minecraftforge.net/en/latest/rendering/modelextensions/rendertypes/) for texture transparency.

## Development runs

Use the Gradle `runClient` or `runServer` task. Each launch copies the mod into `run/.mod-snapshots/` so rebuilding while playing cannot remove classes from the running game. Restart the game to pick up code or resource changes. Direct IDE Java launches do not use this protection. Old snapshots can be removed once their game processes have exited.

## License & Use

Original Spiral Abyss content is all rights reserved. The third-party assets credited below retain their own licenses.

## Credits

- Reward chest model adapted from [Golden Chest](https://voxelwave.art/product/golden-chest) by GriddenFox.
- Earth button artwork adapted from [earth-cube](https://sketchfab.com/3d-models/earth-cube-ed594f0b3a5b4bcc947561af91a9742e) by madexc, licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The model was rendered for the menu button.
- Scroll entity model adapted from [Minecraft style scroll | Blockbench](https://sketchfab.com/3d-models/minecraft-style-scroll-blockbench-71ad2da73cd9409a81c8e17721fc797e) by 3DartSam (samirdevuz), licensed under [CC BY 4.0](https://creativecommons.org/licenses/by/4.0/). The model was adapted for the animated entity.
