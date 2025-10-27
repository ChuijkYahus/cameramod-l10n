package net.mehvahdjukaar.vista.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AttributionAwareUpdateScheduler
 * <p>
 * Per-object token-bucket scheduler with adaptive refill rate based on the
 * *measured share* of frame time consumed by the scheduled updates themselves.
 * <p>
 * Key properties:
 * - Per-object base rate (updates/tick/object).
 * - Never go below a per-object minimum rate.
 * - Adaptation driven by update-time share, not FPS (optional FPS guardrail).
 * - Natural staggering via per-object random phase.
 * - O(1) per object; light housekeeping/eviction.
 * <p>
 * API:
 * - shouldUpdate(id, tick): ask permission to run the expensive update.
 * - runIfShouldUpdate(id, tick, runnable): convenience wrapper that times the update.
 * - onUpdateExecutedNanos(dtNanos): record the time an update took (called automatically by runIfShouldUpdate).
 * - onFrameEnd(frameMs): call once per frame to finalize time accounting and update the EMAs.
 */
public final class AdaptiveUpdateScheduler<ID> {

    // ---------- Per-object state ----------
    private static final class Entry {
        double tokens;
        long lastTick;
        final double phase; // [0,1) staggering
        long lastSeenTick;

        Entry(long tick) {
            this.tokens = 0.0;
            this.lastTick = tick;
            this.lastSeenTick = tick;
            this.phase = ThreadLocalRandom.current().nextDouble();
        }
    }

    private final Map<ID, Entry> state = new ConcurrentHashMap<>();

    // ---------- Rate knobs (immutable after build except where noted) ----------
    /**
     * Desired per-object rate when healthy (updates per tick per object).
     */
    private final double baseRatePerObj;
    /**
     * Hard per-object floor (never below this).
     */
    private final double minRatePerObj;
    /**
     * Token bucket cap to bound burstiness.
     */
    private final double maxBurst;

    // ---------- Attribution-based adaptation ----------
    /**
     * Target frame budget in ms (e.g., 16.666 for 60 FPS). Used only to compute share.
     */
    private final double targetFrameMs;
    /**
     * EMA smoothing for update time per frame (0,1].
     */
    private final double emaAlphaUpdate;

    /**
     * Smoothed ms per frame spent in scheduled updates.
     */
    private double emaUpdateMs = 0.0;
    /**
     * Accumulator for this frame's total update time (nanos).
     */
    private long accumUpdateNanosThisFrame = 0L;

    // ---------- Optional FPS guardrail (off by default) ----------
    private final boolean enableFpsGuard;
    private final double emaAlphaFrame;
    private double emaFrameMs;
    /**
     * Minimum scale when FPS guard is used (e.g., 0.2).
     */
    private final double minPerfScaleFps;

    // ---------- Housekeeping ----------
    private long currentTick = Long.MIN_VALUE;
    private final int evictionAfterTicks;

    private AdaptiveUpdateScheduler(double baseRatePerObj,
                                    double minRatePerObj,
                                    double maxBurst,
                                    double targetFrameMs,
                                    double emaAlphaUpdate,
                                    boolean enableFpsGuard,
                                    double emaAlphaFrame,
                                    double minPerfScaleFps,
                                    int evictionAfterTicks) {
        this.baseRatePerObj = reqPos(baseRatePerObj, "baseRatePerObj");
        this.minRatePerObj = reqPos(minRatePerObj, "minRatePerObj");
        if (maxBurst < 1.0) throw new IllegalArgumentException("maxBurst must be >= 1.0");
        this.maxBurst = maxBurst;
        this.targetFrameMs = reqPos(targetFrameMs, "targetFrameMs");
        this.emaAlphaUpdate = reqAlpha(emaAlphaUpdate, "emaAlphaUpdate");
        this.enableFpsGuard = enableFpsGuard;
        this.emaAlphaFrame = reqAlpha(emaAlphaFrame, "emaAlphaFrame");
        this.minPerfScaleFps = reqAlpha(minPerfScaleFps, "minPerfScaleFps");
        if (evictionAfterTicks < 1) throw new IllegalArgumentException("evictionAfterTicks must be >= 1");
        this.evictionAfterTicks = evictionAfterTicks;
        this.emaFrameMs = targetFrameMs; // initialized if guard is used
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
     * Convenience wrapper that will time the update and feed its duration back in.
     * Returns true if the update actually ran.
     */
    public void runIfShouldUpdate(ID id, Runnable update) {
        long tick = Minecraft.getInstance().level.getGameTime();
        rollTickIfNeeded(tick);
        final Entry e = state.computeIfAbsent(id, j -> new Entry(tick));
        refill(e, tick);
        e.lastSeenTick = tick;

        if (e.tokens >= 1.0) {
            e.tokens -= 1.0;
            long t0 = System.nanoTime();
            try {
                update.run();
            } finally {
                long dt = System.nanoTime() - t0;
                accumUpdateNanosThisFrame += dt;
            }
        }
    }

    public void onFrameEnd() {
        double frameMs = Minecraft.getInstance().getFrameTimeNs() * 1_000_000d;

        // Finalize this frame's update-time accounting
        double updateMsThisFrame = accumUpdateNanosThisFrame / 1_000_000.0;
        emaUpdateMs = (1.0 - emaAlphaUpdate) * emaUpdateMs + emaAlphaUpdate * updateMsThisFrame;
        accumUpdateNanosThisFrame = 0L;

        if (enableFpsGuard) {
            emaFrameMs = (1.0 - emaAlphaFrame) * emaFrameMs + emaAlphaFrame * frameMs;
        }
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

    private void refill(Entry e, long tick) {
        long dt = Math.max(0, tick - e.lastTick);
        if (dt == 0) return;

        double effRate = computeEffRate();

        // Staggering: slight fractional shift prevents phase locking.
        double effectiveDt = dt - 1 + e.phase;
        if (effectiveDt < 0) effectiveDt = 0;

        e.tokens = Math.min(maxBurst, e.tokens + effRate * effectiveDt);
        e.lastTick = tick;
    }

    private double computeEffRate() {
        double scaleUpdates = computeBudget(); // attribution-based
        double effRate = Math.max(minRatePerObj, baseRatePerObj * scaleUpdates);

        // Optional FPS guardrail (off by default). If on, we apply *additional* throttling.
        if (enableFpsGuard) {
            double scaleFps = Mth.clamp(targetFrameMs / Math.max(emaFrameMs, targetFrameMs), 0.0, 1.0);
            scaleFps = Math.max(minPerfScaleFps, scaleFps);
            effRate = Math.max(minRatePerObj, effRate * scaleFps);
        }
        return effRate;
    }

    private double computeBudget() {
        if (targetFrameMs <= 0) return 1.0;
        if (emaUpdateMs <= 0) return 1.0; // updates are effectively free → no throttling
        double s = targetFrameMs / emaUpdateMs; // proportional control
        return Mth.clamp(s, 0.0, 1.0);          // never boost above 1 here
    }

    // ---------- Builder ----------
    public static final class Builder {
        // Required
        private double baseRatePerObj;  // updates/tick/object at healthy perf
        private double minRatePerObj;   // hard floor
        // Optional with sensible defaults
        private double maxBurst = 2.0;
        private double frameBudgetMs = 1000f / 60 * 0.1; // 60 FPS, 10% budget
        private double emaAlphaUpdate = 0.2;   // smoothing for update time EMA
        private boolean enableFpsGuard = false;
        private double emaAlphaFrame = 0.2;    // smoothing for FPS EMA
        private double minPerfScaleFps = 0.2;  // never drop below 20% via FPS guard
        private int evictionAfterTicks = 20 * 5; // evict after 5 seconds at 20 TPS

        /**
         * Required: desired per-object base rate (updates per tick per object).
         */
        public Builder desiredUpdatesPerTick(double v) {
            if (v <= 0) throw new IllegalArgumentException("baseRatePerObj must be > 0");
            this.baseRatePerObj = v;
            return this;
        }

        public Builder desiredUpdatesTickInterval(int tickInterval) {
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

        public Builder minUpdatesTickInterval(int tickInterval) {
            return minUpdatesPerTick(1.0 / tickInterval);
        }

        /**
         * Max tokens per object (burst limiter). Default 2.0.
         */
        public Builder maxBurst(double v) {
            if (v < 1.0) throw new IllegalArgumentException("maxBurst must be >= 1.0");
            this.maxBurst = v;
            return this;
        }

        /**
         * Target frame time in ms (only used to compute share). Default 16.666.
         */
        public Builder targetFpsBudgetScale(int fps, double frameBudget) {
            this.frameBudgetMs = (1000d / fps) * frameBudget;
            return this;
        }

        public Builder targetMsBudget(double ms) {
            this.frameBudgetMs = ms;
            return this;
        }

        /**
         * EMA smoothing for update-time share. Default 0.2.
         */
        public Builder emaAlphaUpdate(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("emaAlphaUpdate must be in (0,1]");
            this.emaAlphaUpdate = v;
            return this;
        }

        /**
         * Enable an optional FPS-based guardrail (off by default).
         */
        public Builder enableFpsGuard(boolean enabled) {
            this.enableFpsGuard = enabled;
            return this;
        }

        /**
         * EMA smoothing for FPS guardrail. Default 0.2.
         */
        public Builder emaAlphaFrame(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("emaAlphaFrame must be in (0,1]");
            this.emaAlphaFrame = v;
            return this;
        }

        /**
         * Min scale when FPS guard engages. Default 0.2.
         */
        public Builder minPerfScaleFps(double v) {
            if (v <= 0 || v > 1) throw new IllegalArgumentException("minPerfScaleFps must be in (0,1]");
            this.minPerfScaleFps = v;
            return this;
        }

        /**
         * Evict objects not seen for this many ticks. Default 10k.
         */
        public Builder evictionAfterTicks(int v) {
            if (v < 1) throw new IllegalArgumentException("evictionAfterTicks must be >= 1");
            this.evictionAfterTicks = v;
            return this;
        }

        public <T> AdaptiveUpdateScheduler<T> build() {
            return new AdaptiveUpdateScheduler<>(
                    baseRatePerObj, minRatePerObj, maxBurst, frameBudgetMs,
                    emaAlphaUpdate, enableFpsGuard, emaAlphaFrame, minPerfScaleFps, evictionAfterTicks
            );
        }
    }

    public static Builder builder() {
        return new Builder();
    }

}
