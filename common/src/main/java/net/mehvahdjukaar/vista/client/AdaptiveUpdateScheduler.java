package net.mehvahdjukaar.vista.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * StaggeredBudgetUpdateScheduler
 *
 * Deterministic staggered, per-object scheduler using a phase accumulator:
 *  • Each object holds a phase in [0,1). Each tick: phase += effectiveRate + ε; if phase >= 1 → wrap once and grant.
 *  • Phase is seeded from a stable hash of the ID → persistent, uniform staggering even if objects start together.
 *
 * Adaptation by attribution:
 *  • targetBudgetMs = desired ms per *rendered frame* spent in these updates.
 *  • We smooth the measured update time with a time-constant EMA (frame-time aware), then
 *    compute a raw scale = clamp(targetBudgetMs / smoothedUpdateMs, 0..1).
 *  • The scale itself is smoothed (own time constant) and rate-limited per frame to avoid twitch.
 *
 * Optional FPS guardrail (off by default) scales down further if overall FPS is poor.
 *
 * API:
 *  • tryRunUpdate(id, runnable): run at most once per tick per object (no catch-up) and count time to budget.
 *  • runIfShouldUpdate(id, runnable): alias to tryRunUpdate for compatibility.
 *  • forceUpdateNextTick(id): guarantee a grant on the next tick (counts toward budget when it runs).
 *  • onEndOfFrame(): call once per rendered frame to finalize EMAs.
 */
public final class AdaptiveUpdateScheduler<ID> {

    // ---------- Per-object state ----------
    private final class Entry {

        double phase01;    // phase in [0,1)
        long lastTickSeen;
        long lastTickTouched;
        Entry(ID id, long tick) {
            this.phase01 = stablePhaseFromId(id);
            this.lastTickSeen = tick;
            this.lastTickTouched = tick;
        }

    }
    private final Map<ID, Entry> entries = new ConcurrentHashMap<>();


    // ---------- Rate knobs ----------
    /** Desired per-object rate (updates per TICK per object) when healthy. */
    private volatile double baseUpdateRatePerTick;
    /** Per-object minimum rate (never goes below this). */
    private final double minUpdateRatePerTick;

    // ---------- Budget feedback (attribution) ----------
    /** Max milliseconds per FRAME we allow these updates to consume. */
    private final double updateTimeTargetMs;
    /** EMA time constant (ms) for the measured update-time signal. */
    private final double updateTimeSmoothingTimeWindowMs;



    /** Smoothed ms per frame spent in scheduled updates. */
    public volatile double smoothedAverageUpdateTimeMs = 0.0;

    /** Accumulator for this frame's total update time (nanos). */
    private long thisFrameAccumulatedUpdateTimeNano = 0L;


    // ---------- Smoothed budget scale + rate limit ----------
    /** EMA time constant (ms) for the scale itself (slower than update-time EMA recommended). */
    private final double scaleSmoothingTimeConstantMs;
    /** Max allowed absolute change of the smoothed scale per frame (e.g., 0.08 → ±8%). */
    private final double maxScaleChangePerFrame;

    /** Smoothed scale (multiplier in [0,1]) applied to baseRatePerTick. */
    private double smoothedBudgetScale = 1.0;

    // ---------- Optional FPS guardrail ----------

    private final boolean useFpsGuard;
    /** Target frame time for the guard (ms). */
    private final double fpsGuardTargetFrameMs;
    /** EMA alpha for FPS guard (simple fixed-alpha smoothing is sufficient). */
    private final double fpsEmaAlpha;
    /** Smoothed observed frame time (ms). */
    private volatile double smoothedAverageFrameTimeMs;
    /** Minimum FPS scale when guard is enabled. */
    private final double minFpsScale;

    // ---------- Staggering helpers ----------
    /** Tiny irrational micro-jitter added each tick to avoid long-term rational lock-in. */
    private static final double GOLDEN_EPS = (1.0 / 1024.0) * 0.0001; // ≈ 9.77e-8
    // ---------- Housekeeping ----------

    private long currentTick = Long.MIN_VALUE;
    private final int evictAfterTicks;
    // ---------- Construction ----------

    private AdaptiveUpdateScheduler(
            double baseRatePerTick,
            double minRatePerTick,
            double targetBudgetMs,
            double smoothingTimeConstantMs,
            double scaleSmoothingTimeConstantMs,
            double maxScaleChangePerFrame,
            boolean useFpsGuard,
            double fpsGuardTargetFrameMs,
            double fpsEmaAlpha,
            double minFpsScale,
            int evictAfterTicks) {

        this.baseUpdateRatePerTick = requirePos(baseRatePerTick, "baseRatePerTick");
        this.minUpdateRatePerTick = requirePos(minRatePerTick, "minRatePerTick");
        this.updateTimeTargetMs = requirePos(targetBudgetMs, "targetBudgetMs");
        this.updateTimeSmoothingTimeWindowMs = requirePos(smoothingTimeConstantMs, "smoothingTimeConstantMs");
        this.scaleSmoothingTimeConstantMs = requirePos(scaleSmoothingTimeConstantMs, "scaleSmoothingTimeConstantMs");
        this.maxScaleChangePerFrame = requirePos(maxScaleChangePerFrame, "maxScaleChangePerFrame");

        this.useFpsGuard = useFpsGuard;
        this.fpsGuardTargetFrameMs = useFpsGuard ? requirePos(fpsGuardTargetFrameMs, "fpsGuardTargetFrameMs") : 16.667;
        this.fpsEmaAlpha = requireAlpha(fpsEmaAlpha, "fpsEmaAlpha");
        this.minFpsScale = requireAlpha(minFpsScale, "minFpsScale");
        this.smoothedAverageFrameTimeMs = this.fpsGuardTargetFrameMs;

        if (evictAfterTicks < 1) throw new IllegalArgumentException("evictAfterTicks must be >= 1");
        this.evictAfterTicks = evictAfterTicks;
    }
    private static double requirePos(double v, String name) {
        if (v <= 0) throw new IllegalArgumentException(name + " must be > 0");
        return v;
    }

    private static double requireAlpha(double v, String name) {
        if (v <= 0 || v > 1) throw new IllegalArgumentException(name + " must be in (0,1]");
        return v;
    }


    // ------------------ Public API ------------------

    public double getAverageUpdateTimeMs() {
        return smoothedAverageUpdateTimeMs;
    }

    /**
     * Preferred call: decides this tick and, if granted, runs and times the update.
     * Guarantees at most ONE update per tick per object (no catch-up).
     */
    public void tryRunUpdate(ID id, Runnable update) {
        long tick = Minecraft.getInstance().level.getGameTime();
        onNewTick(tick);
        final Entry e = entries.computeIfAbsent(id, j -> new Entry(id, tick));
        e.lastTickSeen = tick;

        if (stepPhaseAndGrant(e)) {
            long t0 = System.nanoTime();
            try {
                update.run();
            } finally {
                thisFrameAccumulatedUpdateTimeNano += (System.nanoTime() - t0);
            }
        }
    }

    /** Compatibility alias (same behavior as tryRunUpdate). */
    public void runIfShouldUpdate(ID id, Runnable update) {
        tryRunUpdate(id, update);
    }

    /** Force the object to be granted once on the NEXT tick (counts toward budget when it runs). */
    public void forceUpdateNextTick(ID id) {
        long tick = Minecraft.getInstance().level.getGameTime();
        final Entry e = entries.computeIfAbsent(id, j -> new Entry(id, tick));
        e.lastTickSeen = tick;
        e.lastTickTouched = tick;
        // set phase just below 1 so the next tick's step will wrap once
        e.phase01 = Math.nextAfter(1.0, Double.NEGATIVE_INFINITY);
    }

    /** Call once at the end of each rendered frame. */
    public void onEndOfFrame() {
        // ns → ms
        /** Last frame duration (ms); set in onEndOfFrame(), reused elsewhere for consistent smoothing. */
        double lastFrameMs = Math.max(0.001, Minecraft.getInstance().getFrameTimeNs() / 1_000_000.0);

        // 1) Smooth the measured update time with time-constant EMA
        double updateMsThisFrame = thisFrameAccumulatedUpdateTimeNano / 1_000_000.0;
        double alphaTime = 1.0 - Math.exp(-lastFrameMs / updateTimeSmoothingTimeWindowMs);
        smoothedAverageUpdateTimeMs = (1.0 - alphaTime) * smoothedAverageUpdateTimeMs + alphaTime * updateMsThisFrame;
        thisFrameAccumulatedUpdateTimeNano = 0L;

        // 2) Smooth & limit the budget scale
        double rawScale = (updateTimeTargetMs <= 0 || smoothedAverageUpdateTimeMs <= 0) ? 1.0
                : Mth.clamp(updateTimeTargetMs / smoothedAverageUpdateTimeMs, 0.0, 1.0);
        double betaScale = 1.0 - Math.exp(-lastFrameMs / scaleSmoothingTimeConstantMs);
        double targetScale = (1.0 - betaScale) * smoothedBudgetScale + betaScale * rawScale;
        double delta = Mth.clamp(targetScale - smoothedBudgetScale,
                -maxScaleChangePerFrame, +maxScaleChangePerFrame);
        smoothedBudgetScale += delta;

        // 3) Optional FPS guardrail
        if (useFpsGuard) {
            smoothedAverageFrameTimeMs = (1.0 - fpsEmaAlpha) * smoothedAverageFrameTimeMs + fpsEmaAlpha * lastFrameMs;
        }
    }

    /** Change the base per-object update rate at runtime. */
    public void setBaseRatePerTick(double v) {
        this.baseUpdateRatePerTick = requirePos(v, "baseRatePerTick");
    }

    // ------------------ Internals ------------------

    private void onNewTick(long tick) {
        if (tick != currentTick) {
            if (currentTick != Long.MIN_VALUE && (tick & 0xFF) == 0) {
                evictStale(tick);
            }
            currentTick = tick;
        }
    }

    private void evictStale(long tick) {
        long cutoff = tick - evictAfterTicks;
        entries.entrySet().removeIf(en -> en.getValue().lastTickSeen < cutoff);
    }

    /** Advance phase by (effectiveRate + ε) and grant at most once. */
    private boolean stepPhaseAndGrant(Entry e) {
        double effRate = computeEffectiveUpdateRate();
        effRate = Mth.clamp(effRate, 0.0, 1.0);

        // add tiny irrational jitter to avoid phase-lock when effRate is rational
        double step = effRate + GOLDEN_EPS;

        double newPhase = e.phase01 + step;
        boolean granted = newPhase >= 1.0;
        if (granted) newPhase -= 1.0; // single wrap → single grant
        e.phase01 = newPhase;

        e.lastTickTouched = currentTick;
        return granted;
    }

    private double computeEffectiveUpdateRate() {
        // Budget-based scale (already smoothed and rate-limited in onEndOfFrame)
        double scaled = Math.max(minUpdateRatePerTick, baseUpdateRatePerTick * smoothedBudgetScale);

        if (useFpsGuard) {
            double fpsScale = Mth.clamp(fpsGuardTargetFrameMs / Math.max(smoothedAverageFrameTimeMs, fpsGuardTargetFrameMs), 0.0, 1.0);
            fpsScale = Math.max(minFpsScale, fpsScale);
            scaled = Math.max(minUpdateRatePerTick, scaled * fpsScale);
        }
        return scaled;
    }

    // Stable per-ID phase in [0,1). Mix a 32-bit hashCode into 64 bits, then map to [0,1).
    private static double stablePhaseFromId(Object id) {
        int x = (id == null) ? 0 : id.hashCode();
        long z = mix64(x);
        long mant = (z >>> 11); // top 53 bits
        return mant * (1.0 / (1L << 53));
    }

    // SplitMix64-like mixer for 32→64 bit hashing
    private static long mix64(int x) {
        long z = (x * 0x9E3779B9L) ^ 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    // ---------- Builder ----------
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
