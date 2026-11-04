package com.pla.spiral_abyss.encounter;

/** Fixed combat limits; stars come from the cycle's three rolled objectives. */
public final class WaveChallenges {
    public static final int WAVE_COUNT = 3;
    public static final int TIME_LIMIT_TICKS = 5 * 60 * 20;
    public static final int TRANSITION_TICKS = 5 * 20;

    private WaveChallenges() {}

    public static boolean beforeDeadline(int elapsedTicks, int seconds) {
        return elapsedTicks >= 0 && seconds > 0 && (long) elapsedTicks < (long) seconds * 20;
    }

    public static int countdownSeconds(int ticks) {
        return Math.max(0, (ticks + 19) / 20);
    }
}
