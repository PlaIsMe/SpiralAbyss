package com.pla.spiral_abyss.encounter.data;

/** Datapack composition roles; equipment eligibility remains the source of bow/crossbow roles. */
public enum CombatRole {
    ANY, MELEE, RANGED;
    public String key() { return name().toLowerCase(java.util.Locale.ROOT); }
}
