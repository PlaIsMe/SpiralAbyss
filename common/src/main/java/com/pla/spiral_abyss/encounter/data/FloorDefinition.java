package com.pla.spiral_abyss.encounter.data;

import com.pla.spiral_abyss.encounter.challenge.Challenge;
import net.minecraft.resources.ResourceLocation;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;

/** Validated, immutable datapack definitions. Durations are converted to ticks when loaded. */
public record FloorDefinition(int number, int timeTicks, int waveDelayTicks, Rewards rewards, List<Wave> waves,
                              Map<Challenge.Difficulty, List<Challenge.Type>> challenge) {
    public FloorDefinition {
        waves = List.copyOf(waves);
        var choices = new EnumMap<Challenge.Difficulty, List<Challenge.Type>>(Challenge.Difficulty.class);
        for (var difficulty : Challenge.Difficulty.values()) {
            var types = challenge.get(difficulty);
            if (types == null || types.isEmpty()) throw new IllegalArgumentException("Missing challenge." + difficulty.key());
            choices.put(difficulty, List.copyOf(types));
        }
        challenge = Map.copyOf(choices);
    }

    public record Wave(Map<MobCategory, Integer> categories, List<Enemy> bosses, List<WaveType> waveTypes) {
        public Wave { categories = Map.copyOf(categories); bosses = List.copyOf(bosses); waveTypes = List.copyOf(waveTypes); }
        public Wave(Map<MobCategory, Integer> categories, List<Enemy> bosses) {
            this(categories, bosses, List.of());
        }
        public int minimumEnemies() {
            return waveTypes.isEmpty() ? categories.values().stream().mapToInt(Integer::intValue).sum()
                    + bosses.stream().mapToInt(Enemy::count).sum()
                    : waveTypes.stream().mapToInt(WaveType::count).min().orElse(0);
        }
        public boolean bossOnly() {
            return waveTypes.isEmpty() ? categories.keySet().stream().allMatch(WaveGroup::bossCategory)
                    : waveTypes.stream().allMatch(type -> type.groups().stream().allMatch(group -> WaveGroup.bossCategory(group.category())));
        }
    }

    /** One role-filtered species group inside a simultaneous stage. */
    public record WaveGroup(MobCategory category, int count, CombatRole role) {
        public WaveGroup(MobCategory category, int count) { this(category, count, CombatRole.ANY); }
        private static boolean bossCategory(MobCategory category) { return category == MobCategory.MINI_BOSS || category == MobCategory.BOSS; }
    }
    public record WaveStage(List<WaveGroup> groups) {
        public WaveStage { groups = List.copyOf(groups); }
    }
    public record WaveType(ResourceLocation id, List<WaveStage> sequence, int tier) {
        public WaveType { sequence = List.copyOf(sequence); }
        public WaveType(ResourceLocation id, List<WaveGroup> groups) { this(id, groups.stream().map(group -> new WaveStage(List.of(group))).toList(), 1); }
        public List<WaveGroup> groups() { return sequence.stream().flatMap(stage -> stage.groups().stream()).toList(); }
        public int count() { return groups().stream().mapToInt(WaveGroup::count).sum(); }
    }

    public record Enemy(ResourceLocation id, int count, MobCategory category, int stage, CombatRole role) {
        public Enemy(ResourceLocation id, int count, MobCategory category) { this(id, count, category, 0, CombatRole.ANY); }
        public Enemy(ResourceLocation id, int count) { this(id, count, null); }
    }

    public record RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount,
                              int weight, double chance, boolean rare, Map<ResourceLocation, Integer> enchantments) {
        public RewardEntry { enchantments = Map.copyOf(enchantments); }
        public RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount,
                           int weight, double chance, boolean rare) {
            this(id, item, minCount, maxCount, weight, chance, rare, Map.of());
        }
        public RewardEntry(ResourceLocation id, boolean item, int minCount, int maxCount,
                           int weight, double chance) {
            this(id, item, minCount, maxCount, weight, chance, false);
        }
    }

    public record Rewards(List<RewardEntry> entries, int rolls, int experience) {
        public Rewards { entries = List.copyOf(entries); }
    }
}
