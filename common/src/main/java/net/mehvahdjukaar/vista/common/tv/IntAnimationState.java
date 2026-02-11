package net.mehvahdjukaar.vista.common.tv;

import java.util.Objects;

/**
 * Integer-only animation controller.
 * Valid only when one duration is an exact multiple of the other.
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

        @Override
        public float getValue(float partialTick) {
            return 0;
        }
    };

    private final int maxTick;
    private final int forwardStep;
    private final int backwardStep;

    private int currentTick;
    private int prevTick;

    public IntAnimationState(int turnOnTime, int turnOffTime) {
        if (turnOnTime <= 0 || turnOffTime <= 0) {
            throw new IllegalArgumentException("Times must be positive");
        }

        int max = Math.max(turnOnTime, turnOffTime);
        int min = Math.min(turnOnTime, turnOffTime);

        if (max % min != 0) {
            throw new IllegalArgumentException(
                    "Invalid animation ratio: one time must be an exact multiple of the other"
            );
        }

        int ratio = max / min;

        if (turnOnTime >= turnOffTime) {
            this.forwardStep = 1;
            this.backwardStep = ratio;
        } else {
            this.forwardStep = ratio;
            this.backwardStep = 1;
        }

        this.maxTick = turnOnTime * forwardStep;
        this.currentTick = 0;
        this.prevTick = 0;
    }

    public boolean isDecreasing() {
        return currentTick < prevTick;
    }

    public boolean isIncreasing() {
        return currentTick > prevTick;
    }

    /** Call each tick while turning on */
    public void increment() {
        prevTick = currentTick;
        currentTick = Math.min(maxTick, currentTick + forwardStep);
    }

    /** Call each tick while turning off */
    public void decrement() {
        prevTick = currentTick;
        currentTick = Math.max(0, currentTick - backwardStep);
    }

    /** Normalized animation value in [0,1] */
    public float getValue(float partialTick) {
        float interpolated = prevTick + (currentTick - prevTick) * partialTick;
        return interpolated / maxTick;
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
        return "Anim[" + currentTick + "->" + prevTick + "]";
    }
}
