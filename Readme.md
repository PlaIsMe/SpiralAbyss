# SpiralAbyss

Minecraft 1.20.1 / Forge 47. Mod ID: `spiral_abyss`.

## Playing

Build with Java 17 using `gradlew.bat build`. Install `build/libs/SpiralAbyss-1.20.1-1.8.jar` on client and server.

- Get the reusable draft from Tools & Utilities or `/give @s spiral_abyss:abyss_draft`. Right-click to enter your arena; use it again or `/spiral_abyss leave` to return to your saved entry location.
- Right-click the key to start. The key disappears during combat. Defeat every enemy before each wave expires; the boss bar shows floor, wave, remaining enemies and time. Failure cleans up the enemies and restores the key immediately.
- Victory replaces the key with an invulnerable reward chest. Any party member can open it: opening animation (0.55 seconds), then shared items and XP pop out as shaking starts; after one second of shaking it holds the looted pose. Idle currently uses the closed model, ready for a future idle animation.
- Use `/spiral_abyss floor <1-6>` to enter your own arena at any selected floor, including from outside the Abyss. Claimed floors remain looted. Switching is blocked during combat or while a chest is unclaimed; it never resets claims.
- After claiming, the host uses `/spiral_abyss next_floor`. Six example floors are included, each with three zombie/skeleton waves. Every floor reuses the same physical platform.
- `/spiral_abyss reset` clears all six of the caller's floors, cancels their active fight and restores floor 1. It also clears their pending chest. Other players' saved claims remain intact. This deliberately permits replay and new rewards after an explicit reset.

## Parties and progress

The host enters their own arena, then uses `/spiral_abyss invite <player>`. The friend accepts with `/spiral_abyss join <host>` within 60 seconds. Parties contain at most four players. New players cannot join during combat; existing participants can reconnect while their party remains active. The host must be alive and present when starting; surviving friends can finish after the host leaves or dies.

Each host has independent saved progression. Claiming a shared chest marks that floor completed and claimed for every participant captured at the start, including disconnected players. When a friend later enters solo, their corresponding completed floor shows a looted chest. A player who already cleared the floor can still help a different, unfinished host and share that host's rewards. Only the actual floor beaten is credited; a guest joining a higher floor does not receive claims for skipped floors.

| Players at start | Mob HP and damage multiplier | Shared loot rolls and XP multiplier |
| --- | --- | --- |
| 1 | 1.0 | 1.0 |
| 2 | 1.5 | 1.5 |
| 3 | 2.0 | 2.0 |
| 4 | 2.5 | 2.5 |

Party multipliers multiply the datapack's floor/wave attributes. They are fixed for the attempt, so leaving does not reduce enemy strength or change rewards. Rewards are a single shared drop, not four personal copies. Fractional loot rolls receive a probabilistic extra roll, e.g. 1.5 rolls means one guaranteed roll plus a 50% second roll.

Progress, exact unclaimed rewards, opening progress and beneficiaries survive restarts. Active fights restart from wave 1 after a restart or `/reload`; stale wave mobs are removed. Reward contents are rolled once at victory, so reopening or rejoining cannot reroll them. Empty parties release their arena; pending chests are restored at the host's next visit.

## Bounded void dimension

There is one void dimension with a **2304 x 2304 block world border**, centered at X/Z 0.5. A fixed pool of **16 arena slots**, 512 blocks apart, is reused between hosts. At most 16 parties can occupy arenas simultaneously; additional parties wait for a free slot. Player records do not reserve chunks, and advancing floors never generates another arena.

Only the arena structures are placed: no ground, bedrock layer, features or generated structures. The supplied model's physical footprint is 66 x 65 blocks; the playable boundary is 60 x 60. Players stay above the base at Y=65, with the top of their hitbox below **world Y=120**. Leaving these limits returns them to their last safe grounded position or entrance.

Mobs at/below base level outside the platform return to the center. Airborne mobs can leave the platform within a **256 x 256** flight area, up to Y=240, before returning. Temporary tickets keep flying enemies ticking; each occupied arena forces only its central 3 x 3 chunks, released when empty. The dimension uses view distance 6 and simulation distance 4.

This bounds terrain growth from normal Abyss play and avoids dimensions per player or per floor. A Minecraft border is not a hard disk quota: administrative chunk generation or other mods can still request chunks beyond it. Existing region files are not deleted. Old bottom bedrock is removed as its chunks load; legacy shared progress is not assigned to every new private profile. A backup of the previous source is in `build/before-private-abyss.zip`.

## Custom floors

See [the datapack guide](docs/floor-datapacks.md). Each floor contains `floor.json` (attributes, delay and rewards) and consecutive `wave_1.json`, `wave_2.json`, etc. Each reward roll selects a weighted chest loot table and independently checks each custom item bonus chance. Item weights are ignored; table weights choose between tables. All durations are in seconds. Mobs and bosses use their installed registry IDs.

The old `dailyboss` loader, unlock menu, daily cooldowns and spawn/kill commands have been retired. The GatewaysToEternity reference informed the encounter lifecycle; this mod uses its own datapack format and has no Gateways or Cataclysm dependency.

## Code layout

- `world/AbyssParties`, `AbyssPlayers`, `ArenaSession`: invitations, bounded arena leases, persistent profiles, participant synchronization and chest lifecycle.
- `world/ArenaBounds`, `AbyssDimension`: player/mob limits, void arena placement and active chunk loading.
- `entity/KeyEntity`, `RewardChestEntity`: invulnerable interaction points; encounter logic lives outside entities.
- `encounter/data`: floor JSON parsing and reloads.
- `encounter/FloorController`, `WaveSpawner`: timers, waves, enemies, scaling and boss bar.
- `encounter/FloorProgress`, `FloorRewards`: completion/claim state and persisted chest-table loot and independent item bonuses.
- `event/AbyssMobDrops`, `mixin/*DropMixin`: mob drop and XP suppression.

## Mob-drop compatibility

Every mob in the Abyss is prevented from dropping items or XP, including ordinary spawned mobs. Tagged wave mobs retain protection outside the dimension. Players and floor rewards are excluded.

Required server-safe mixins intercept `LivingEntity.dropAllDeathLoot` before equipment, custom death-loot hooks and XP, and `Entity.spawnAtLocation(ItemStack, float)` before live or captured drops. Forge drop events are cleared and cancelled, XP events are zeroed, and direct item spawns with an identified mob owner are rejected.

A mod inserting ownerless items from unrelated tick/death-event code, bypassing these hooks, needs a targeted compatibility hook: those items cannot reliably be distinguished from legitimate player and reward items.

## Verification

Run `gradlew.bat build` with Java 17 to compile and package the mod.

Client rendering and modded bosses' custom flight/phase behavior still need in-game checks. The reward model, texture, and open/shake/looted keyframes come from the supplied reference exports.

## License & Use
We reserve all rights. The points below outline what you can, cannot and must do when dealing with the
contents of this repository.

### You CAN
* Use this mod in a modpack.
* Make videos and showcases about this mod.
* Write your own code that uses this code as a dependency (such as addons or datapacks).

### You CANNOT
* Use or redistribute ANY assets of this mod, even for personal use.

### Disclaimer
<sub> 
THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
</sub>
