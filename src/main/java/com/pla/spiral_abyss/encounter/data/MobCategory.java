package com.pla.spiral_abyss.encounter.data;

/** Datapack category names; pool contents live in mob_pools/*.json. */
public enum MobCategory {
    FODDER_MOB("fodder_mob"),
    COMMON_MOB("common_mob"),
    CHAMPION_MOB("champion_mob"),
    MINI_BOSS("mini_boss"),
    BOSS("boss");

    private final String key;
    MobCategory(String key) { this.key = key; }
    public String key() { return key; }
}
