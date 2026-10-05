package com.pla.spiral_abyss.encounter.challenge;

import com.google.gson.*;
import com.pla.spiral_abyss.encounter.challenge.Challenge.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.*;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;
import java.io.Reader;
import java.util.*;

/** Optional per-type datapack files; empty files deliberately disable a type. */
public final class ChallengeCatalog extends SimplePreparableReloadListener<ChallengeCatalog.Snapshot> {
    public record Tier(int target, double percentage, List<ResourceLocation> pool, int choose, List<List<EquipmentSlot>> slotSets) {
        public Tier { pool = List.copyOf(pool); slotSets = slotSets.stream().map(List::copyOf).toList(); }
    }
    public record Snapshot(Map<Type, Map<Difficulty, Tier>> types) {
        public Snapshot {
            Map<Type, Map<Difficulty, Tier>> copy = new EnumMap<>(Type.class);
            types.forEach((type, tiers) -> copy.put(type, Map.copyOf(tiers))); types = Map.copyOf(copy);
        }
        public Tier tier(Type type, Difficulty difficulty) { return types.getOrDefault(type, Map.of()).get(difficulty); }
        /** Existing concrete choices are preferred, then missing IDs repaired before new rolls. */
        public List<Challenge> candidates(Type type, Difficulty difficulty, long seed, Challenge existing) {
            LinkedHashSet<Challenge> choices = new LinkedHashSet<>();
            if (existing != null && existing.type() == type && existing.difficulty() == difficulty && available(existing)) choices.add(existing);
            Tier tier = tier(type, difficulty);
            if (tier == null) return List.copyOf(choices);
            Random random = new Random(seed);
            if (existing != null && existing.type() == type) {
                var retained = existing.ids().stream().filter(id -> installed(type, id)).toList();
                Tier repair = new Tier(existing.target(), existing.percentage(), tier.pool(), tier.choose(), List.of(existing.forbiddenSlots()));
                for (int i = 0; i < 32; i++) {
                    Challenge candidate = create(type, difficulty, repair, random, retained);
                    if (candidate != null) choices.add(candidate);
                }
            }
            for (int i = 0; i < 64; i++) {
                Challenge candidate = create(type, difficulty, tier, random, List.of());
                if (candidate != null) choices.add(candidate);
            }
            return List.copyOf(choices);
        }
        public List<Challenge> roll(long seed) {
            return roll(seed, Integer.MAX_VALUE);
        }
        public List<Challenge> roll(long seed, int minimumEnemies) {
            return roll(seed, List.of(minimumEnemies));
        }
        public List<Challenge> roll(long seed, List<Integer> populations) {
            return roll(seed, populations, challenge -> true);
        }
        public List<Challenge> roll(long seed, List<Integer> populations, java.util.function.Predicate<Challenge> eligible) {
            List<Challenge> result = new ArrayList<>();
            if (populations.isEmpty() || populations.stream().anyMatch(count -> count < 1)) throw new IllegalArgumentException("Challenges need nonempty waves");
            if (!select(result, new Random(seed), true, populations, eligible, new int[]{8192})
                    && !select(result, new Random(seed), false, populations, eligible, new int[]{8192}))
                throw new IllegalArgumentException("No feasible easy, medium and hard challenge set is available");
            return List.copyOf(result);
        }
        private boolean select(List<Challenge> result, Random random, boolean distinct, List<Integer> populations,
                               java.util.function.Predicate<Challenge> eligible, int[] budget) {
            if (result.size() == 3) return true;
            Difficulty difficulty = Difficulty.values()[result.size()];
            List<Type> choices = new ArrayList<>(types.keySet()); choices.sort(Comparator.naturalOrder()); Collections.shuffle(choices, random);
            for (Type type : choices) {
                if (distinct && result.stream().anyMatch(challenge -> challenge.type() == type)) continue;
                Tier tier = tier(type, difficulty); if (tier == null) continue;
                // A bounded search tries alternative IDs when a weapon/enchantment pairing conflicts.
                for (int attempt = 0; attempt < 24; attempt++) {
                    if (--budget[0] < 0) return false;
                    Challenge candidate = create(type, difficulty, tier, random, List.of());
                    if (candidate == null) break;
                    if (!eligible.test(candidate)) continue;
                    result.add(candidate);
                    if (populations.stream().allMatch(count -> feasible(result, count)) && select(result, random, distinct, populations, eligible, budget)) return true;
                    result.remove(result.size() - 1);
                }
            }
            return false;
        }
        /** Preserve available rolls; repair missing IDs in place, or replace only their affected tier. */
        public List<Challenge> repair(List<Challenge> saved, long seed) {
            return repair(saved, seed, List.of(128));
        }
        public List<Challenge> repair(List<Challenge> saved, long seed, List<Integer> populations) {
            return repair(saved, seed, populations, challenge -> true);
        }
        public List<Challenge> repair(List<Challenge> saved, long seed, List<Integer> populations,
                                      java.util.function.Predicate<Challenge> eligible) {
            List<Challenge> result = new ArrayList<>(saved);
            for (int index = 0; index < result.size(); index++) {
                Challenge existing = result.get(index);
                boolean satisfiesRoster = eligible.test(existing);
                if (available(existing) && satisfiesRoster) continue;
                Tier tier = tier(existing.type(), existing.difficulty());
                List<ResourceLocation> retained = satisfiesRoster ? existing.ids().stream().filter(id -> installed(existing.type(), id)).toList() : List.of();
                Random random = new Random(seed ^ index ^ existing.type().ordinal());
                boolean repaired = false;
                // Preserve the saved requirement size, including custom enchantment cardinality.
                Tier repairTier = tier == null ? null : new Tier(existing.target(), existing.percentage(), tier.pool(), existing.ids().size(), List.of(existing.forbiddenSlots()));
                for (int attempt = 0; repairTier != null && attempt < 96; attempt++) {
                    Challenge replacement = create(existing.type(), existing.difficulty(), repairTier, random, retained);
                    if (replacement == null) break;
                    if (!eligible.test(replacement)) continue;
                    result.set(index, replacement);
                    if (populations.stream().allMatch(count -> feasible(result, count))) { repaired = true; break; }
                }
                if (!repaired) {
                    // If an optional mod supplied the entire family, replace only this tier.
                    result.set(index, existing);
                    List<Type> alternatives = new ArrayList<>(types.keySet()); alternatives.sort(Comparator.naturalOrder()); Collections.shuffle(alternatives, random);
                    for (Type replacementType : alternatives) {
                        Tier fallback = tier(replacementType, existing.difficulty()); if (fallback == null) continue;
                        for (int attempt = 0; attempt < 32; attempt++) {
                            Challenge replacement = create(replacementType, existing.difficulty(), fallback, random, List.of());
                            if (replacement == null) break;
                            if (!eligible.test(replacement)) continue;
                            result.set(index, replacement);
                            if (populations.stream().allMatch(count -> feasible(result, count))) { repaired = true; break; }
                        }
                        if (repaired) break;
                    }
                }
                if (!repaired) throw new IllegalArgumentException("No available repair for challenge " + existing.type().key());
            }
            return List.copyOf(result);
        }
    }
    private static volatile Snapshot current = new Snapshot(Map.of());
    public static Snapshot snapshot() { return current; }
    public static boolean countsKills(Type type) {
        return type != Type.TIME && type != Type.NO_ARMOR && type != Type.ARMOR_ENCHANTMENT
                && type != Type.TAKE_DAMAGE && type != Type.FISHING && type != Type.TOTEM
                && type != Type.TRICKSHOT && type != Type.ENDER_PEARL_HIT && type != Type.SHIELD_BLOCK;
    }
    public static boolean installed(Type type, ResourceLocation id) {
        return switch (type) {
            case ARMOR_ENCHANTMENT, WEAPON_ENCHANTMENT -> ForgeRegistries.ENCHANTMENTS.containsKey(id);
            case WEAPON -> ForgeRegistries.ITEMS.containsKey(id) && ForgeRegistries.ITEMS.getValue(id) != Items.AIR;
            case PROJECTILE -> ForgeRegistries.ENTITY_TYPES.containsKey(id);
            case PLAYER_EFFECT, MOB_EFFECT -> ForgeRegistries.MOB_EFFECTS.containsKey(id);
            default -> true;
        };
    }
    public static boolean available(Challenge challenge) { return challenge.ids().stream().allMatch(id -> installed(challenge.type(), id)); }
    private static Challenge create(Type type, Difficulty difficulty, Tier tier, Random random, List<ResourceLocation> retained) {
        List<ResourceLocation> pool = tier.pool().stream().filter(id -> installed(type, id)).distinct().collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool, random);
        if (type == Type.PROJECTILE) return new Challenge(type, difficulty, tier.target(), tier.percentage(), pool, List.of());
        List<ResourceLocation> ids = new ArrayList<>(retained);
        for (ResourceLocation id : pool) {
            if (ids.size() >= tier.choose()) break;
            if (ids.contains(id)) continue;
            ids.add(id);
            if (type == Type.WEAPON_ENCHANTMENT && !weaponEnchantmentsFit(ids, null)) ids.remove(ids.size() - 1);
        }
        if (ids.size() != tier.choose()) return null;
        List<EquipmentSlot> slots = tier.slotSets().isEmpty() ? List.of() : tier.slotSets().get(random.nextInt(tier.slotSets().size()));
        return new Challenge(type, difficulty, tier.target(), tier.percentage(), ids, slots);
    }
    public static boolean compatible(List<Challenge> challenges) {
        Set<EquipmentSlot> forbidden = EnumSet.noneOf(EquipmentSlot.class);
        challenges.stream().filter(c -> c.type() == Type.NO_ARMOR).forEach(c -> forbidden.addAll(c.forbiddenSlots()));
        if (forbidden.size() >= 4) return false;
        List<ResourceLocation> armor = challenges.stream().filter(c -> c.type() == Type.ARMOR_ENCHANTMENT).flatMap(c -> c.ids().stream()).distinct().toList();
        if (!armor.isEmpty() && !armorEnchantmentsFit(armor, forbidden)) return false;
        if (challenges.stream().anyMatch(c -> c.type() == Type.PLAYER_BURNING)
                && challenges.stream().anyMatch(c -> c.type() == Type.PLAYER_EFFECT && c.ids().stream().anyMatch(id -> id.toString().equals("minecraft:fire_resistance")))) return false;
        List<ResourceLocation> enchantments = challenges.stream().filter(c -> c.type() == Type.WEAPON_ENCHANTMENT).flatMap(c -> c.ids().stream()).distinct().toList();
        if (enchantments.isEmpty()) return true;
        // A partially repaired list can still contain an absent weapon in a later slot.
        var weapons = challenges.stream().filter(c -> c.type() == Type.WEAPON).flatMap(c -> c.ids().stream())
                .filter(id -> installed(Type.WEAPON, id)).toList();
        if (weapons.isEmpty()) return weaponEnchantmentsFit(enchantments, null);
        return weapons.stream().allMatch(weapon -> weaponEnchantmentsFit(enchantments, weapon));
    }
    public static boolean feasible(List<Challenge> challenges, int minimumEnemies) {
        if (!compatible(challenges) || challenges.stream().anyMatch(c -> countsKills(c.type()) && c.requiredCount(minimumEnemies) > minimumEnemies)) return false;
        // These final-blow conditions cannot all be credited by the same kill.
        Map<List<ResourceLocation>, Integer> weaponTargets = new HashMap<>();
        challenges.stream().filter(c -> c.type() == Type.WEAPON).forEach(c -> weaponTargets.merge(c.ids(), c.requiredCount(minimumEnemies), Math::max));
        long weaponKills = weaponTargets.values().stream().mapToLong(Integer::longValue).sum();
        int enchantedKills = challenges.stream().filter(c -> c.type() == Type.WEAPON_ENCHANTMENT).mapToInt(c -> c.requiredCount(minimumEnemies)).max().orElse(0);
        long separateKills = Math.max(weaponKills, enchantedKills);
        for (Type type : List.of(Type.PROJECTILE, Type.EXPLOSION))
            separateKills += challenges.stream().filter(c -> c.type() == type).mapToInt(c -> c.requiredCount(minimumEnemies)).max().orElse(0);
        return separateKills <= minimumEnemies;
    }
    private static boolean armorEnchantmentsFit(List<ResourceLocation> ids, Set<EquipmentSlot> forbidden) {
        List<EquipmentSlot> slots = Arrays.stream(EquipmentSlot.values()).filter(slot -> slot.getType() == EquipmentSlot.Type.ARMOR && !forbidden.contains(slot)).toList();
        Map<EquipmentSlot, List<ResourceLocation>> groups = new EnumMap<>(EquipmentSlot.class);
        slots.forEach(slot -> groups.put(slot, new ArrayList<>()));
        return distributeArmor(ids, 0, groups);
    }
    private static boolean distributeArmor(List<ResourceLocation> ids, int index, Map<EquipmentSlot, List<ResourceLocation>> groups) {
        if (index == ids.size()) return true;
        for (var entry : groups.entrySet()) {
            entry.getValue().add(ids.get(index));
            var enchantments = entry.getValue().stream().map(ForgeRegistries.ENCHANTMENTS::getValue).filter(Objects::nonNull).toList();
            boolean compatible = true;
            for (int i = 0; i < enchantments.size(); i++) for (int j = i + 1; j < enchantments.size(); j++)
                if (!enchantments.get(i).isCompatibleWith(enchantments.get(j))) compatible = false;
            boolean fits = compatible && ForgeRegistries.ITEMS.getValues().stream().filter(item -> item instanceof ArmorItem armor && armor.getEquipmentSlot() == entry.getKey())
                    .anyMatch(item -> enchantments.stream().allMatch(enchantment -> enchantment.canEnchant(new ItemStack(item))));
            if (fits && distributeArmor(ids, index + 1, groups)) return true;
            entry.getValue().remove(entry.getValue().size() - 1);
        }
        return false;
    }
    private static boolean weaponEnchantmentsFit(List<ResourceLocation> ids, ResourceLocation weapon) {
        var enchantments = ids.stream().map(ForgeRegistries.ENCHANTMENTS::getValue).filter(Objects::nonNull).toList();
        for (int i = 0; i < enchantments.size(); i++) for (int j = i + 1; j < enchantments.size(); j++)
            if (!enchantments.get(i).isCompatibleWith(enchantments.get(j))) return false;
        if (weapon != null) {
            var item = ForgeRegistries.ITEMS.getValue(weapon);
            return item != null && enchantments.stream().allMatch(enchantment -> enchantment.canEnchant(new ItemStack(item)));
        }
        return ForgeRegistries.ITEMS.getValues().stream().filter(item -> item instanceof TieredItem || item instanceof ProjectileWeaponItem || item instanceof TridentItem)
                .anyMatch(item -> enchantments.stream().allMatch(enchantment -> enchantment.canEnchant(new ItemStack(item))));
    }
    public static Map<Difficulty, Tier> parse(Type type, JsonElement json) {
        if (json == null || json.isJsonNull() || json.isJsonArray() && json.getAsJsonArray().size() == 0) return Map.of();
        if (!json.isJsonObject()) throw new JsonParseException("Challenge file must be an object or empty array");
        JsonObject root = json.getAsJsonObject();
        Map<Difficulty, Tier> tiers = new EnumMap<>(Difficulty.class);
        for (Difficulty difficulty : Difficulty.values()) {
            if (!root.has(difficulty.key())) continue;
            JsonObject object = root.getAsJsonObject(difficulty.key()); if (object.size() == 0) continue;
            int target = Challenge.counted(type) ? 1 : object.has("target") ? object.get("target").getAsInt() : 1;
            if (target < 1 || target > 10000 || type == Type.TIME && target > 300) throw new JsonParseException("Invalid challenge target");
            double percentage = Challenge.counted(type) ? object.get("percentage").getAsDouble() : 0;
            if (!Double.isFinite(percentage) || percentage < 0 || percentage > 100 || Challenge.counted(type) && percentage == 0)
                throw new JsonParseException("Invalid challenge percentage");
            List<ResourceLocation> pool = new ArrayList<>();
            if (object.has("pool")) for (JsonElement value : object.getAsJsonArray("pool")) {
                ResourceLocation id = ResourceLocation.tryParse(value.getAsString());
                if (id == null) throw new JsonParseException("Invalid challenge registry ID: " + value); pool.add(id);
            }
            boolean requiresPool = type == Type.ARMOR_ENCHANTMENT || type == Type.WEAPON_ENCHANTMENT || type == Type.WEAPON
                    || type == Type.PLAYER_EFFECT || type == Type.MOB_EFFECT;
            if (requiresPool && object.has("pool") && pool.isEmpty()) continue;
            if (object.has("inherits")) {
                Difficulty inherited;
                try { inherited = Difficulty.valueOf(object.get("inherits").getAsString().toUpperCase(Locale.ROOT)); }
                catch (IllegalArgumentException exception) { throw new JsonParseException("Unknown inherited challenge tier", exception); }
                if (inherited.ordinal() >= difficulty.ordinal()) throw new JsonParseException("A challenge tier may inherit only an earlier difficulty");
                List<ResourceLocation> combined = new ArrayList<>(tiers.containsKey(inherited) ? tiers.get(inherited).pool() : List.of());
                combined.addAll(pool); pool = new ArrayList<>(new LinkedHashSet<>(combined));
            }
            if (pool.size() > 256) throw new JsonParseException("Challenge pools allow at most 256 IDs");
            int defaultChoose = switch (type) {
                case ARMOR_ENCHANTMENT, WEAPON_ENCHANTMENT -> 1;
                case WEAPON, PLAYER_EFFECT, MOB_EFFECT -> 1;
                case PROJECTILE -> pool.size();
                default -> 0;
            };
            int choose = object.has("choose") ? object.get("choose").getAsInt() : defaultChoose;
            if ((type==Type.ARMOR_ENCHANTMENT||type==Type.WEAPON_ENCHANTMENT||type==Type.PLAYER_EFFECT||type==Type.MOB_EFFECT) && choose!=1)
                throw new JsonParseException("Enchantment and effect challenges choose exactly one ID");
            if (choose < 0 || choose > (type == Type.PROJECTILE ? 256 : 32) || defaultChoose > 0 && choose == 0) throw new JsonParseException("Invalid challenge pool selection size");
            List<List<EquipmentSlot>> slotSets = new ArrayList<>();
            if (object.has("slot_sets")) for (JsonElement entry : object.getAsJsonArray("slot_sets")) {
                List<EquipmentSlot> slots = new ArrayList<>();
                for (JsonElement value : entry.getAsJsonArray()) {
                    EquipmentSlot slot = EquipmentSlot.byName(value.getAsString());
                    if (slot.getType() != EquipmentSlot.Type.ARMOR) throw new JsonParseException("Only armor slots are allowed");
                    if (!slots.contains(slot)) slots.add(slot);
                }
                if (slots.isEmpty() || slots.size() == 4) throw new JsonParseException("Forbidden armor slots must leave at least one armor slot usable"); slotSets.add(slots);
            }
            if (type == Type.NO_ARMOR && slotSets.isEmpty()) throw new JsonParseException("no_armor requires slot_sets");
            tiers.put(difficulty, new Tier(target, percentage, pool, choose, slotSets));
        }
        return Map.copyOf(tiers);
    }
    @Override protected Snapshot prepare(ResourceManager manager, ProfilerFiller profiler) {
        Map<Type, Map<Difficulty, Tier>> definitions = new EnumMap<>(Type.class);
        for (Type type : Type.values()) {
            var id = ResourceLocation.fromNamespaceAndPath("spiral_abyss", "challenge/" + type.key() + ".json");
            var resource = manager.getResource(id); if (resource.isEmpty()) continue;
            try (Reader reader = resource.get().openAsReader()) {
                JsonElement json = JsonParser.parseReader(reader); definitions.put(type, parse(type, json));
            } catch (Exception exception) { throw new JsonParseException("Invalid Abyss challenge " + id, exception); }
        }
        return new Snapshot(definitions);
    }
    @Override protected void apply(Snapshot snapshot, ResourceManager manager, ProfilerFiller profiler) { current = snapshot; }
}
