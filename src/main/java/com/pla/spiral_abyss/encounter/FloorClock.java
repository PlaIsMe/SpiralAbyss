package com.pla.spiral_abyss.encounter;

/** One combat budget per attempt; inter-wave delays never spend that budget. */
public final class FloorClock {
    private int remainingTicks;
    public void start(int ticks) { remainingTicks = Math.max(0, ticks); }
    public int remainingTicks() { return remainingTicks; }
    public int remainingSeconds() { return (remainingTicks + 19) / 20; }
    public boolean tick(boolean fighting) {
        if (fighting && remainingTicks > 0) remainingTicks--;
        return remainingTicks == 0;
    }
    public void clear() { remainingTicks = 0; }
}
