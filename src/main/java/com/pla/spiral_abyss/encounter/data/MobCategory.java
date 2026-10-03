package com.pla.spiral_abyss.encounter.data;

import java.util.List;

/** Datapack category names and the initial configurable mob pools. */
public enum MobCategory {
    FODDER_MOB("fodder_mob", "zombie", "skeleton", "husk", "stray", "zombie_villager", "drowned"),
    COMMON_MOB("common_mob", "creeper", "spider", "pillager", "piglin", "cave_spider",  "wither_skeleton", "blaze", "witch", "phantom"),
    CHAMPION_MOB("champion_mob", "vindicator", "vex", "piglin_brute", "ravager", "ghast", "shulker", "guardian"),
    MINI_BOSS("mini_boss", "evoker", "enderman"),
    BOSS("boss", "warden", "wither");

    private final String key;
    private final List<String> defaults;
    MobCategory(String key, String... mobs) {
        this.key = key;
        this.defaults = java.util.Arrays.stream(mobs).map(id -> "minecraft:" + id).toList();
    }
    public String key() { return key; }
    public List<String> defaults() { return defaults; }
}
