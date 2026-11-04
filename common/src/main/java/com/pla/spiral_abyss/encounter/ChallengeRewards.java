package com.pla.spiral_abyss.encounter;

import java.util.*;
import java.util.function.Function;

/** Claim-time policy shared by every chest; an absent participant contributes nothing. */
public final class ChallengeRewards {
    private ChallengeRewards() {}

    public static List<UUID> eligible(Set<UUID> captured, Collection<UUID> present,
            Function<UUID, FloorProgress> progress, int floor, int tier, int[] attemptStars) {
        if (tier < 1 || tier > 3) return List.of();
        return present.stream().distinct().filter(captured::contains).filter(id -> {
            FloorProgress personal = progress.apply(id);
            return !personal.tierClaimed(floor, tier)
                    && Arrays.stream(personal.projectedStars(floor, attemptStars)).sum() >= tier * 3;
        }).toList();
    }

    /** Preserve the floor's XP budget across three tiers per eligible player. */
    public static double scale(int eligiblePlayers) { return Math.max(0, eligiblePlayers) / 3.0; }

    /** Copper, iron and golden chests try one, two and three tables per live claimant. */
    public static int rollCount(int tier, int eligiblePlayers) {
        if (tier < 1 || tier > 3 || eligiblePlayers <= 0) return 0;
        return Math.multiplyExact(tier, eligiblePlayers);
    }
}
