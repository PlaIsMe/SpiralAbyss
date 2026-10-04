package com.pla.spiral_abyss.encounter;

/** One combat budget per wave; inter-wave delays never spend that budget. */
public final class FloorClock {
    private int remainingTicks;
    private int durationTicks;
    public void start(int ticks) { durationTicks = remainingTicks = Math.max(0, ticks); }
    public int remainingTicks() { return remainingTicks; }
    public int elapsedTicks() { return durationTicks - remainingTicks; }
    public int remainingSeconds() { return (remainingTicks + 19) / 20; }
    public boolean tick(boolean fighting) {
        if (fighting && remainingTicks > 0) remainingTicks--;
        return remainingTicks == 0;
    }
    public void clear() { durationTicks = remainingTicks = 0; }
}
