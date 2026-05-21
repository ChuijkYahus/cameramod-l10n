package net.mehvahdjukaar.vista.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import java.util.HashMap;
import java.util.Map;

/**
 * AdaptiveUpdateScheduler
 * <p>
 * Driven entirely by render frames to eliminate Tick-vs-Frame desynchronization.
 * Guarantees uniform update rates across all objects for perfect convergence and fairness.
 */
public final class AdaptiveUpdateScheduler<ID> {

    private final class Entry {
        double phase01;
        long lastFrameSeen;

        Entry(ID id, long frame) {
            this.phase01 = stablePhaseFromId(id);
            this.lastFrameSeen = frame;
        }
    }

    private final Map<ID, Entry> entries = new HashMap<>();

    // Config (Normalized to per-frame values)
    private final double baseUpdateRatePerFrame;
    private final double minUpdateRatePerFrame;
    private final double updateTimeTargetMs;
    private final double updateTimeSmoothingWindowMs;
    private final double scaleSmoothingTimeConstantMs;
    private final double maxScaleChangePerFrame;

    private final boolean useFpsGuard;
    private final double fpsGuardTargetFrameMs;
    private final double fpsEmaAlpha;
    private final double minFpsScale;
    private final int evictAfterFrames;

    // Runtime State
    public volatile double smoothedAverageUpdateTimeMs = 0.0;
    private volatile double smoothedAverageFrameTimeMs;
    private long thisFrameAccumulatedUpdateTimeNano = 0L;
    private double smoothedBudgetScale = 1.0;

    // Core Frame Drivers
    private long currentFrame = 0L;
    private double effectiveRateThisFrame = 1.0;

    public AdaptiveUpdateScheduler(
            double baseRatePerFrame,         // e.g., 0.1 means update once every 10 frames baseline
            double minRatePerFrame,          // e.g., 0.01 means minimum once every 100 frames
            double targetBudgetMs,           // Max ms to spend per frame on these updates
            double smoothingWindowMs,
            double scaleSmoothingTimeConstantMs,
            double maxScaleChangePerFrame,
            boolean useFpsGuard,
            double fpsGuardTargetFrameMs,
            double fpsEmaAlpha,
            double minFpsScale,
            int evictAfterFrames
    ) {
        this.baseUpdateRatePerFrame = requirePos(baseRatePerFrame, "baseRatePerFrame");
        this.minUpdateRatePerFrame = requirePos(minRatePerFrame, "minRatePerFrame");
        this.updateTimeTargetMs = requirePos(targetBudgetMs, "targetBudgetMs");
        this.updateTimeSmoothingWindowMs = requirePos(smoothingWindowMs, "smoothingWindowMs");
        this.scaleSmoothingTimeConstantMs = requirePos(scaleSmoothingTimeConstantMs, "scaleSmoothingTimeConstantMs");
        this.maxScaleChangePerFrame = requirePos(maxScaleChangePerFrame, "maxScaleChangePerFrame");
        this.useFpsGuard = useFpsGuard;
        this.fpsGuardTargetFrameMs = useFpsGuard ? requirePos(fpsGuardTargetFrameMs, "fpsGuardTargetFrameMs") : 16.667;
        this.fpsEmaAlpha = requireAlpha(fpsEmaAlpha, "fpsEmaAlpha");
        this.minFpsScale = requireAlpha(minFpsScale, "minFpsScale");
        this.evictAfterFrames = evictAfterFrames;

        this.smoothedAverageFrameTimeMs = this.fpsGuardTargetFrameMs;
    }

    /**
     * Attempts to update an object, tracking budgets globally.
     *
     * @param id        The unique identifier of the object.
     * @param isVisible True if the object is actively in the player's view frustum.
     * @param update    The expensive logic to run.
     */
    public void runIfShouldUpdate(ID id, boolean isVisible, Runnable update) {
        // Enforce fairness: completely skip phase accumulation if the object is hidden
        if (!isVisible) {
            return;
        }

        // Fetch or create entry without allocating a lambda capture on every call
        Entry e = entries.get(id);
        if (e == null) {
            e = new Entry(id, currentFrame);
            entries.put(id, e);
        }
        e.lastFrameSeen = currentFrame;

        if (stepPhaseAndGrant(e)) {
            long t0 = System.nanoTime();
            try {
                update.run();
            } finally {
                thisFrameAccumulatedUpdateTimeNano += (System.nanoTime() - t0);
            }
        }
    }

    private boolean stepPhaseAndGrant(Entry e) {
        // Every visible object increments by the EXACT same scale this frame,
        // guaranteeing convergence to an identical frequency.
        double newPhase = e.phase01 + effectiveRateThisFrame;

        if (newPhase >= 1.0) {
            e.phase01 = newPhase - 1.0; // Wrap around to preserve alignment integrity
            return true;
        }

        e.phase01 = newPhase;
        return false;
    }

    /**
     * Must be called exactly once at the absolute END of every rendered frame.
     */
    public void onEndOfFrame() {
        double lastFrameMs = Math.max(0.001, Minecraft.getInstance().getFrameTimeNs() / 1_000_000.0);

        // 1. Smooth the actual time spent executing updates during this specific frame
        double updateMsThisFrame = thisFrameAccumulatedUpdateTimeNano / 1_000_000.0;
        double alpha = 1.0 - Math.exp(-lastFrameMs / updateTimeSmoothingWindowMs);

        smoothedAverageUpdateTimeMs = (1.0 - alpha) * smoothedAverageUpdateTimeMs + alpha * updateMsThisFrame;
        thisFrameAccumulatedUpdateTimeNano = 0L; // Reset frame accumulator

        // 2. Compute how much headroom we have against our budget target
        double rawScale = (smoothedAverageUpdateTimeMs <= 0.0)
                ? 1.0
                : Mth.clamp(updateTimeTargetMs / smoothedAverageUpdateTimeMs, 0.0, 1.0);

        double beta = 1.0 - Math.exp(-lastFrameMs / scaleSmoothingTimeConstantMs);
        double targetScale = (1.0 - beta) * smoothedBudgetScale + beta * rawScale;

        // Rate limit changes to avoid jittery frame pacing
        double delta = Mth.clamp(targetScale - smoothedBudgetScale, -maxScaleChangePerFrame, +maxScaleChangePerFrame);
        smoothedBudgetScale += delta;

        // 3. Process FPS Guardrail if enabled
        if (useFpsGuard) {
            smoothedAverageFrameTimeMs = (1.0 - fpsEmaAlpha) * smoothedAverageFrameTimeMs + fpsEmaAlpha * lastFrameMs;
        }

        // 4. Advance frame clock and recalculate the frozen rate for the upcoming frame
        currentFrame++;
        effectiveRateThisFrame = computeEffectiveUpdateRate();

        // Evict stale entries periodically
        if ((currentFrame & 0xFF) == 0) {
            long cutoff = currentFrame - evictAfterFrames;
            entries.entrySet().removeIf(en -> en.getValue().lastFrameSeen < cutoff);
        }
    }

    private double computeEffectiveUpdateRate() {
        double scaled = Math.max(minUpdateRatePerFrame, baseUpdateRatePerFrame * smoothedBudgetScale);

        if (useFpsGuard) {
            double fpsScale = Mth.clamp(
                    fpsGuardTargetFrameMs / Math.max(smoothedAverageFrameTimeMs, fpsGuardTargetFrameMs),
                    0.0, 1.0
            );
            fpsScale = Math.max(minFpsScale, fpsScale);
            scaled = Math.max(minUpdateRatePerFrame, scaled * fpsScale);
        }

        return Mth.clamp(scaled, 0.0, 1.0);
    }

    public void forceUpdateNextTick(ID id) {
        Entry e = entries.get(id);
        if (e == null) {
            e = new Entry(id, currentFrame);
            entries.put(id, e);
        }
        e.lastFrameSeen = currentFrame;
        e.phase01 = 0.999999;
    }

    private static double stablePhaseFromId(Object id) {
        int x = (id == null) ? 0 : id.hashCode();
        long z = mix64(x);
        return (z >>> 11) * (1.0 / (1L << 53));
    }

    private static long mix64(int x) {
        long z = (x * 0x9E3779B9L) ^ 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    private static double requirePos(double v, String name) {
        if (v <= 0) throw new IllegalArgumentException(name + " must be > 0");
        return v;
    }

    private static double requireAlpha(double v, String name) {
        if (v <= 0 || v > 1) throw new IllegalArgumentException(name + " must be in (0,1]");
        return v;
    }

    public static final class Builder {
        // Required
        private double baseRatePerTick;    // updates/tick/object at healthy perf
        private double minRatePerTick;     // hard floor

        // Budget
        private double updateTimeTargetMs = 5.0;          // e.g., 5 ms per frame for these updates
        private double updateTimeSmoothingTimeWindowMs = 300; // EMA tau for update time (ms). time window in real time for decay

        // Scale smoothing
        private double scaleSmoothingTimeWindowMs = 350; // EMA tau for budget scale (ms)
        private double maxScaleChangePerFrame = 0.08;      // ≤ 8% change per frame

        // FPS guard (optional)
        private boolean useFpsGuard = false;
        private double fpsGuardTargetFrameMs = 16.667; // 60 FPS target
        private double fpsEmaAlpha = 0.2;
        private double minFpsScale = 0.2; // min 20% scale when guard engages

        // Eviction
        private int evictAfterTicks = 20 * 5; // evict after ~5s at 20 TPS

        /** Desired per-object base rate (updates per tick per object). */
        public Builder baseRatePerTick(double v) {
            if (v <= 0) throw new IllegalArgumentException("baseRatePerTick must be > 0");
            this.baseRatePerTick = v; return this;
        }
        /** Convenience: desired update every N ticks. */
        public Builder basePeriodTicks(int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException("ticks must be > 0");
            return baseRatePerTick(1.0 / ticks);
        }

        public Builder baseFps(double fps) {
            if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
            return baseRatePerTick(fps / 20.0); // assuming 20 TPS
        }

        /** Minimum per-object rate (never below). */
        public Builder minRatePerTick(double v) {
            if (v <= 0) throw new IllegalArgumentException("minRatePerTick must be > 0");
            this.minRatePerTick = v; return this;
        }
        /** Convenience: minimum update every N ticks. */
        public Builder minPeriodTicks(int ticks) {
            if (ticks <= 0) throw new IllegalArgumentException("ticks must be > 0");
            return minRatePerTick(1.0 / ticks);
        }

        public Builder minFps(double fps) {
            if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
            return minRatePerTick(fps / 20.0); // assuming 20 TPS
        }

        /** Direct ms budget per frame for all scheduled updates. */
        public Builder targetBudgetMs(double v) {
            if (v <= 0) throw new IllegalArgumentException("targetBudgetMs must be > 0");
            this.updateTimeTargetMs = v; return this;
        }

        /** Convenience: budget from FPS + share (0..1]. */
        public Builder targetBudgetFromFps(double fps, double share) {
            if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
            if (share <= 0 || share > 1) throw new IllegalArgumentException("share must be in (0,1]");
            return targetBudgetMs((1000.0 / fps) * share);
        }

        /** Update-time smoothing time-constant (ms). */
        public Builder smoothingTimeConstantMs(double v) {
            if (v <= 0) throw new IllegalArgumentException("smoothingTimeConstantMs must be > 0");
            this.updateTimeSmoothingTimeWindowMs = v; return this;
        }

        /** Budget-scale smoothing time-constant (ms). */
        public Builder scaleSmoothingTimeConstantMs(double v) {
            if (v <= 0) throw new IllegalArgumentException("scaleSmoothingTimeConstantMs must be > 0");
            this.scaleSmoothingTimeWindowMs = v; return this;
        }

        /** Max allowed scale change per frame (0..1]. */
        public Builder maxScaleChangePerFrame(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("maxScaleChangePerFrame must be in (0,1]");
            this.maxScaleChangePerFrame = v; return this;
        }

        /** FPS target for the guard (e.g., 60 → 16.667 ms). */
        public Builder guardTargetFps(double fps) {
            if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
            this.fpsGuardTargetFrameMs = 1000.0 / fps;
            this.useFpsGuard = true;
            return this;

        }
        /** FPS guard smoothing alpha (fixed-alpha is fine). */
        public Builder fpsGuardAlpha(double alpha) {
            if (alpha <= 0 || alpha > 1) throw new IllegalArgumentException("fpsEmaAlpha must be in (0,1]");
            this.fpsEmaAlpha = alpha; return this;
        }
        /** Minimum FPS scale when guard engages. */
        public Builder minFpsScale(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("minFpsScale must be in (0,1]");
            this.minFpsScale = v; return this;
        }

        /** Evict objects not seen for this many ticks. */
        public Builder evictAfterTicks(int v) {
            if (v < 1) throw new IllegalArgumentException("evictAfterTicks must be >= 1");
            this.evictAfterTicks = v; return this;
        }

        public <T> AdaptiveUpdateScheduler<T> build() {
            return new AdaptiveUpdateScheduler<>(
                    baseRatePerTick, minRatePerTick,
                    updateTimeTargetMs,
                    updateTimeSmoothingTimeWindowMs,
                    scaleSmoothingTimeWindowMs,
                    maxScaleChangePerFrame,
                    useFpsGuard, fpsGuardTargetFrameMs, fpsEmaAlpha, minFpsScale,
                    evictAfterTicks
            );
        }
    }

    public static Builder builder() { return new Builder(); }
}
