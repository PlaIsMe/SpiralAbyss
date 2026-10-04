package com.pla.spiral_abyss.encounter;

/** Shared, server-authoritative wave limits and elapsed-time challenge thresholds. */
public final class WaveChallenges {
    public static final int WAVE_COUNT = 3;
    public static final int TIME_LIMIT_TICKS = 5 * 60 * 20;
    public static final int TRANSITION_TICKS = 5 * 20;

    private WaveChallenges() {}

    /** Each threshold is strict: exactly one minute earns two stars, not three. */
    public static int stars(int elapsedTicks) {
        if (elapsedTicks < 0) return 0;
        if (elapsedTicks < 60 * 20) return 3;
        if (elapsedTicks < 2 * 60 * 20) return 2;
        if (elapsedTicks < 3 * 60 * 20) return 1;
        return 0;
    }

    public static int countdownSeconds(int ticks) {
        return Math.max(0, (ticks + 19) / 20);
    }
}
