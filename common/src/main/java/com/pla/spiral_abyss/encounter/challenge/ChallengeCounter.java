package com.pla.spiral_abyss.encounter.challenge;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Bounded objective progress with a permanent failure latch for the current wave. */
public final class ChallengeCounter {
    private final int target;
    private int progress;
    private boolean failed;
    private final Set<UUID> distinct = new HashSet<>();

    public ChallengeCounter(int target) {
        if (target <= 0) throw new IllegalArgumentException("Objective target must be positive");
        this.target = target;
    }
    public int progress() { return progress; }
    public int target() { return target; }
    public boolean failed() { return failed; }
    public boolean complete() { return !failed && progress >= target; }
    public void set(int amount) { progress = Math.max(0, Math.min(target, amount)); }
    public void increment() { if (progress < target) progress++; }
    public void incrementDistinct(UUID entity) { if (distinct.add(entity)) increment(); }
    public void fail() { failed = true; }
}
