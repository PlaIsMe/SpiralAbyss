package com.pla.spiral_abyss.encounter.data;
import java.util.Locale;

/** Datapack composition roles; equipment eligibility remains the source of bow/crossbow roles. */
public enum CombatRole {
    ANY, MELEE, RANGED;
    public String key() { return name().toLowerCase(Locale.ROOT); }
}
