package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.moonlight.api.misc.RollingBuffer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AdaptiveUpdateScheduler
 * <p>
 * Deterministic staggered per-object scheduler using a phase accumulator:
 * - Each object has phase in [0,1). Each tick: phase += effRate + ε; if (phase >= 1) { phase -= 1; grant }.
 * - Phase is seeded from a stable hash of the ID → strong, deterministic staggering.
 * - effRate adapts via attribution-based control: scale = clamp(targetBudgetMs / emaUpdateMs, 0..1).
 * - Optional FPS guardrail (off by default).
 * <p>
 * Extras:
 * - forceNextUpdate(id): ensures next tick grants once (counts toward budget).
 * - runForcedNow(id, runnable, countTowardsBudget): run immediately (optionally counted).
 */
public final class AdaptiveUpdateScheduler<ID> {

    // ---------- Per-object state ----------
    private final class Entry {
        double phase;   // [0,1)
        long lastTick;
        long lastSeenTick;

        Entry(ID id, long tick) {
            this.phase = stablePhaseFromId(id);
            this.lastTick = tick;
            this.lastSeenTick = tick;
        }

        // Stable per-ID phase in [0,1). Uses a 64-bit mix to avoid clustering from poor hashCodes.
        private double stablePhaseFromId(ID id) {
            long h = mix64(id == null ? 0 : id.hashCode());
            // Convert top 53 bits to a double in [0,1)
            long mant = (h >>> 11); // keep 53 bits
            return mant * (1.0 / (1L << 53));
        }

        // SplitMix64 mix from a 32-bit input to 64-bit
        private static long mix64(int x) {
            long z = (x * 0x9E3779B9L) ^ 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
            z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
            return z ^ (z >>> 31);
        }
    }

    private final Map<ID, Entry> state = new ConcurrentHashMap<>();

    // ---------- Rate knobs ----------
    /**
     * Desired per-object rate when healthy (updates per tick per object).
     */
    private final double baseRatePerObj;
    /**
     * Hard per-object floor (never below this).
     */
    private final double minRatePerObj;

    // ---------- Attribution-based adaptation (single budget) ----------
    /**
     * Target time budget (ms) you allow these updates to consume per frame.
     */
    private final double targetBudgetMs;
    /**
     * EMA smoothing for per-frame update time (0,1].
     */
    private final double emaAlphaUpdate;
    /**
     * Smoothed ms per frame spent in scheduled updates.
     */
    private volatile double emaUpdateMs = 0.0;
    /**
     * Accumulator for this frame's total update time (nanos).
     */
    private long accumUpdateNanosThisFrame = 0L;

    // ---------- Optional FPS guardrail (OFF by default) ----------
    private final boolean enableFpsGuard;
    /**
     * Target frame time for FPS guard (ms), e.g., 16.667 for 60 FPS.
     */
    private final double guardTargetFrameMs;
    /**
     * EMA smoothing for FPS guardrail.
     */
    private final double emaAlphaFrame;
    /**
     * Smoothed observed frame time (ms).
     */
    private volatile double emaFrameMs;
    /**
     * Minimum scale when FPS guard is used (e.g., 0.2).
     */
    private final double minPerfScaleFps;

    // ---------- Staggering helpers ----------
    /**
     * Tiny irrational micro-jitter added each tick to avoid rational locking.
     */
    private static final double GOLDEN_EPS = 1.0 / 1024.0 * 0.0001; // ~1e-4/1024 ≈ 9.7e-8

    // ---------- Housekeeping ----------
    private long currentTick = Long.MIN_VALUE;
    private final int evictionAfterTicks;

    private AdaptiveUpdateScheduler(double baseRatePerObj,
                                    double minRatePerObj,
                                    double targetBudgetMs,
                                    double emaAlphaUpdate,
                                    boolean enableFpsGuard,
                                    double guardTargetFrameMs,
                                    double emaAlphaFrame,
                                    double minPerfScaleFps,
                                    int evictionAfterTicks) {
        this.baseRatePerObj = reqPos(baseRatePerObj, "baseRatePerObj");
        this.minRatePerObj = reqPos(minRatePerObj, "minRatePerObj");

        this.targetBudgetMs = reqPos(targetBudgetMs, "targetBudgetMs");
        this.emaAlphaUpdate = reqAlpha(emaAlphaUpdate, "emaAlphaUpdate");

        this.enableFpsGuard = enableFpsGuard;
        this.guardTargetFrameMs = enableFpsGuard ? reqPos(guardTargetFrameMs, "guardTargetFrameMs") : 16.667;
        this.emaAlphaFrame = reqAlpha(emaAlphaFrame, "emaAlphaFrame");
        this.minPerfScaleFps = reqAlpha(minPerfScaleFps, "minPerfScaleFps");
        this.emaFrameMs = this.guardTargetFrameMs;

        if (evictionAfterTicks < 1) throw new IllegalArgumentException("evictionAfterTicks must be >= 1");
        this.evictionAfterTicks = evictionAfterTicks;
    }

    private static double reqPos(double v, String name) {
        if (v <= 0) throw new IllegalArgumentException(name + " must be > 0");
        return v;
    }

    private static double reqAlpha(double v, String name) {
        if (v <= 0 || v > 1) throw new IllegalArgumentException(name + " must be in (0,1]");
        return v;
    }

    // ------------------ Public API ------------------

    /**
     * Decide and (if granted) run and time the update.
     * Guarantees at most ONE update per tick per object (no catch-up).
     */
    public void runIfShouldUpdate(ID id, Runnable update) {
        long tick = Minecraft.getInstance().level.getGameTime();
        rollTickIfNeeded(tick);
        final Entry e = state.computeIfAbsent(id, j -> new Entry(id, tick));
        e.lastSeenTick = tick;

        if (stepAndGrant(e)) {
            long t0 = System.nanoTime();
            try {
                update.run();
            } finally {
                long dt = System.nanoTime() - t0;
                accumUpdateNanosThisFrame += dt;
            }
        }
    }

    /**
     * Prime an object so the next tick will grant exactly one update (counts toward budget).
     */
    public void forceNextUpdate(ID id) {
        long tick = Minecraft.getInstance().level.getGameTime();
        final Entry e = state.computeIfAbsent(id, j -> new Entry(id, tick));
        e.lastSeenTick = tick;
        e.lastTick = tick;
        // put phase just below 1 so next step wraps
        e.phase = Math.nextAfter(1.0, Double.NEGATIVE_INFINITY);
    }

    /**
     * Call once at the end of each rendered frame.
     */
    public void onFrameEnd() {
        // getFrameTimeNs() is ns of last frame
        double frameMs = Minecraft.getInstance().getFrameTimeNs() / 1_000_000.0;

        // finalize this frame's update-time accounting
        double updateMsThisFrame = accumUpdateNanosThisFrame / 1_000_000.0;
        emaUpdateMs = ((1.0 - emaAlphaUpdate) * emaUpdateMs + emaAlphaUpdate * updateMsThisFrame);
        accumUpdateNanosThisFrame = 0L;

        if (enableFpsGuard) {
            emaFrameMs = (1.0 - emaAlphaFrame) * emaFrameMs + emaAlphaFrame * frameMs;
        }
    }

    /**
     * Diagnostics
     */
    public double getEmaUpdateMs() {
        return emaUpdateMs;
    }

    public double getEmaFrameMs() {
        return emaFrameMs;
    }

    // ------------------ Internals ------------------

    private void rollTickIfNeeded(long tick) {
        if (tick != currentTick) {
            if (currentTick != Long.MIN_VALUE && (tick & 0xFF) == 0) {
                evictStale(tick);
            }
            currentTick = tick;
        }
    }

    private void evictStale(long tick) {
        long cutoff = tick - evictionAfterTicks;
        state.entrySet().removeIf(en -> en.getValue().lastSeenTick < cutoff);
    }

    /**
     * Advance phase by effective rate + epsilon and determine if we grant exactly one update.
     * We compute effRate once per object per tick to avoid inconsistencies.
     */
    private boolean stepAndGrant(Entry e) {
        double effRate = computeEffRate();
        effRate = Mth.clamp(effRate, 0.0, 1.0);

        // Add tiny irrational jitter to avoid lock-in when effRate is rational.
        double step = effRate + GOLDEN_EPS;

        double newPhase = e.phase + step;
        boolean grant = newPhase >= 1.0;
        if (grant) newPhase -= 1.0; // wrap once; no catch-up
        e.phase = newPhase;

        e.lastTick = currentTick;
        return grant;
    }

    private double computeEffRate() {
        // Attribution-based scaling: keep total updates within the ms budget.
        double scaleBudget = computeScaleFromBudget();
        double eff = Math.max(minRatePerObj, baseRatePerObj * scaleBudget);

        // Optional FPS guardrail: additionally scale by FPS health.
        if (enableFpsGuard) {
            double scaleFps = Mth.clamp(guardTargetFrameMs / Math.max(emaFrameMs, guardTargetFrameMs), 0.0, 1.0);
            scaleFps = Math.max(minPerfScaleFps, scaleFps);
            eff = Math.max(minRatePerObj, eff * scaleFps);
        }
        return eff;
    }

    private double computeScaleFromBudget() {
        if (targetBudgetMs <= 0) return 1.0;
        if (emaUpdateMs <= 0) return 1.0; // updates are effectively free → no throttling
        double s = targetBudgetMs / emaUpdateMs; // proportional control
        return Mth.clamp(s, 0.0, 1.0);          // never boost above 1 here
    }



    // ---------- Builder ----------
    public static final class Builder {
        // Required
        private double baseRatePerObj;   // updates/tick/object at healthy perf
        private double minRatePerObj;    // hard floor

        // Budget controller
        private double targetBudgetMs = 5.0;   // default 5 ms per frame for these updates
        private double emaAlphaUpdate = 0.2;   // smoothing for update-time EMA

        // Optional FPS guardrail
        private boolean enableFpsGuard = false;
        private double guardTargetFrameMs = 16.667; // 60 FPS
        private double emaAlphaFrame = 0.2;
        private double minPerfScaleFps = 0.2;

        // Eviction
        private int evictionAfterTicks = 20 * 5; // evict after 5 seconds at 20 TPS

        /**
         * Required: desired per-object base rate (updates per tick per object).
         */
        public Builder desiredUpdatesPerTick(double v) {
            if (v <= 0) throw new IllegalArgumentException("baseRatePerObj must be > 0");
            this.baseRatePerObj = v;
            return this;
        }

        /**
         * Convenience: desired update every N ticks.
         */
        public Builder desiredUpdatesTickInterval(int tickInterval) {
            if (tickInterval <= 0) throw new IllegalArgumentException("tickInterval must be > 0");
            return desiredUpdatesPerTick(1.0 / tickInterval);
        }

        /**
         * Required: minimum per-object rate (never go below).
         */
        public Builder minUpdatesPerTick(double v) {
            if (v <= 0) throw new IllegalArgumentException("minRatePerObj must be > 0");
            this.minRatePerObj = v;
            return this;
        }

        /**
         * Convenience: minimum update every N ticks.
         */
        public Builder minUpdatesTickInterval(int tickInterval) {
            if (tickInterval <= 0) throw new IllegalArgumentException("tickInterval must be > 0");
            return minUpdatesPerTick(1.0 / tickInterval);
        }

        /**
         * Direct ms budget per frame for all scheduled updates (recommended).
         */
        public Builder targetBudgetMs(double v) {
            if (v <= 0) throw new IllegalArgumentException("targetBudgetMs must be > 0");
            this.targetBudgetMs = v;
            return this;
        }

        /**
         * Convenience: compute ms budget from FPS and share (0..1].
         */
        public Builder targetBudgetFromFps(double fps, double share) {
            if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
            if (share <= 0 || share > 1) throw new IllegalArgumentException("share must be in (0,1]");
            double frameMs = 1000.0 / fps;
            return targetBudgetMs(frameMs * share);
        }

        /**
         * EMA smoothing for update-time budget controller.
         */
        public Builder emaAlphaUpdate(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("emaAlphaUpdate must be in (0,1]");
            this.emaAlphaUpdate = v;
            return this;
        }

        /**
         * Enable optional FPS guardrail (separate from budget logic).
         */
        public Builder enableFpsGuard(boolean enabled) {
            this.enableFpsGuard = enabled;
            return this;
        }

        /**
         * Sets the FPS target for the guardrail (e.g., 60 → 16.667 ms).
         */
        public Builder guardTargetFps(double fps) {
            if (fps <= 0) throw new IllegalArgumentException("fps must be > 0");
            this.guardTargetFrameMs = 1000.0 / fps;
            return this;
        }

        /**
         * EMA smoothing for FPS guardrail.
         */
        public Builder emaAlphaFrame(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("emaAlphaFrame must be in (0,1]");
            this.emaAlphaFrame = v;
            return this;
        }

        /**
         * Minimum scale when FPS guard engages.
         */
        public Builder minPerfScaleFps(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("minPerfScaleFps must be in (0,1]");
            this.minPerfScaleFps = v;
            return this;
        }

        /**
         * Evict objects not seen for this many ticks.
         */
        public Builder evictionAfterTicks(int v) {
            if (v < 1) throw new IllegalArgumentException("evictionAfterTicks must be >= 1");
            this.evictionAfterTicks = v;
            return this;
        }

        public <T> AdaptiveUpdateScheduler<T> build() {
            return new AdaptiveUpdateScheduler<>(
                    baseRatePerObj, minRatePerObj,
                    targetBudgetMs, emaAlphaUpdate,
                    enableFpsGuard, guardTargetFrameMs, emaAlphaFrame, minPerfScaleFps,
                    evictionAfterTicks
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }
}
