package net.mehvahdjukaar.vista.common.tv;

import java.util.Objects;

/**
 * AnimationHolder with forward and backward durations.
 * Enforces integer-only steps and clean divisions.
 */
public class IntAnimationState {

    // unmodifiable instance
    public static final IntAnimationState NO_ANIM = new IntAnimationState(1,1){
        @Override
        public void increment() {
        }
        @Override
        public void decrement() {
        }
    };

    private final int turnOnTime;      // total ticks to fully turn on
    private final int forwardStep;     // step per tick
    private final int backwardStep;    // step per tick

    private int currentTick = 0;
    private int prevTick = 0;

    public IntAnimationState(int turnOnTime, int turnOffTime) {
        if (turnOnTime <= 0 || turnOffTime <= 0)
            throw new IllegalArgumentException("Both turnOnTime and turnOffTime must be positive");

        this.turnOnTime = turnOnTime;

        // Compute steps per tick
        this.forwardStep = 1; // always 1 for minimal storage

        // Compute backward step as integer
        float computedBackwardStep = (float) turnOnTime / turnOffTime;
        if (Math.abs(Math.round(computedBackwardStep) - computedBackwardStep) > 1e-6) {
            throw new IllegalArgumentException("turnOnTime / turnOffTime must produce integer backward step");
        }
        this.backwardStep = Math.round(computedBackwardStep);

        this.currentTick = 0;
        this.prevTick = 0;
    }

    public boolean isDecreasing() {
        return currentTick < prevTick;
    }

    public boolean isIncreasing() {
        return currentTick > prevTick;
    }

    /**
     * Call each tick while turning on
     */
    public void increment() {
        prevTick = currentTick;
        currentTick = Math.min(turnOnTime, currentTick + forwardStep);
    }

    /**
     * Call each tick while turning off
     */
    public void decrement() {
        prevTick = currentTick;
        currentTick = Math.max(0, currentTick - backwardStep);
    }

    /**
     * Returns normalized value [0,1], partialTick for interpolation
     */
    public float getValue(float partialTick) {
        float interpolated = prevTick + (currentTick - prevTick) * partialTick;
        return Math.max(0f, Math.min(interpolated / turnOnTime, 1f));
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof IntAnimationState that)) return false;
        return currentTick == that.currentTick && prevTick == that.prevTick;
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentTick, prevTick);
    }

    @Override
    public String toString() {
        return String.format("AnimationHolder[tick=%d/%d]", currentTick, turnOnTime);
    }
}
