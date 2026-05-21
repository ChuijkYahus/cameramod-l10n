package net.mehvahdjukaar.vista.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public final class AdaptiveUpdateScheduler<ID> {

    // ------------------------------------------------------------
    // Per-object state
    // ------------------------------------------------------------

    private final class Entry {
        double phase01;
        long lastTickSeen;

        Entry(ID id, long tick) {
            this.phase01 = stablePhaseFromId(id);
            this.lastTickSeen = tick;
        }
    }

    private final Map<ID, Entry> entries = new HashMap<>();

    // ------------------------------------------------------------
    // Config
    // ------------------------------------------------------------

    private volatile double baseUpdateRatePerTick;
    private final double minUpdateRatePerTick;

    private final double updateTimeTargetMs;
    private final double updateTimeSmoothingTimeWindowMs;

    private final double scaleSmoothingTimeConstantMs;
    private final double maxScaleChangePerFrame;

    private final boolean useFpsGuard;
    private final double fpsGuardTargetFrameMs;
    private final double fpsEmaAlpha;
    private final double minFpsScale;

    private final int evictAfterTicks;

    // ------------------------------------------------------------
    // Runtime state
    // ------------------------------------------------------------

    public volatile double smoothedAverageUpdateTimeMs = 0.0;
    private volatile double smoothedAverageFrameTimeMs;

    private long thisFrameAccumulatedUpdateTimeNano = 0L;

    private double smoothedBudgetScale = 1.0;

    private long currentTick = Long.MIN_VALUE;

    /**
     * IMPORTANT:
     * Frozen once per scheduler tick.
     * ALL objects use identical rate for the entire tick.
     */
    private double effectiveRateThisTick = 1.0;

    // ------------------------------------------------------------
    // Construction
    // ------------------------------------------------------------

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
            int evictAfterTicks
    ) {
        this.baseUpdateRatePerTick = requirePos(baseRatePerTick, "baseRatePerTick");
        this.minUpdateRatePerTick = requirePos(minRatePerTick, "minRatePerTick");

        this.updateTimeTargetMs = requirePos(targetBudgetMs, "targetBudgetMs");
        this.updateTimeSmoothingTimeWindowMs = requirePos(smoothingTimeConstantMs, "smoothingTimeConstantMs");

        this.scaleSmoothingTimeConstantMs = requirePos(scaleSmoothingTimeConstantMs, "scaleSmoothingTimeConstantMs");
        this.maxScaleChangePerFrame = requirePos(maxScaleChangePerFrame, "maxScaleChangePerFrame");

        this.useFpsGuard = useFpsGuard;

        this.fpsGuardTargetFrameMs =
                useFpsGuard
                        ? requirePos(fpsGuardTargetFrameMs, "fpsGuardTargetFrameMs")
                        : 16.667;

        this.fpsEmaAlpha = requireAlpha(fpsEmaAlpha, "fpsEmaAlpha");
        this.minFpsScale = requireAlpha(minFpsScale, "minFpsScale");

        this.smoothedAverageFrameTimeMs = this.fpsGuardTargetFrameMs;

        if (evictAfterTicks < 1) {
            throw new IllegalArgumentException("evictAfterTicks must be >= 1");
        }

        this.evictAfterTicks = evictAfterTicks;
    }

    // ------------------------------------------------------------
    // Public API
    // ------------------------------------------------------------

    public void tryRunUpdate(ID id, Runnable update) {
        long tick = Minecraft.getInstance().level.getGameTime();

        onNewTick(tick);

        Entry e = entries.computeIfAbsent(id, k -> new Entry(id, tick));
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

    public void runIfShouldUpdate(ID id, Runnable update) {
        tryRunUpdate(id, update);
    }

    /**
     * Force exactly one immediate future update.
     *
     * Preserves phase continuity better than snapping to 1-epsilon.
     */
    public void forceUpdateNextTick(ID id) {
        long tick = Minecraft.getInstance().level.getGameTime();

        Entry e = entries.computeIfAbsent(id, k -> new Entry(id, tick));
        e.lastTickSeen = tick;

        e.phase01 = 0.999999999999;
    }

    public void onEndOfFrame() {

        double lastFrameMs =
                Math.max(0.001,
                        Minecraft.getInstance().getFrameTimeNs() / 1_000_000.0);

        // --------------------------------------------------------
        // Smooth measured update cost
        // --------------------------------------------------------

        double updateMsThisFrame =
                thisFrameAccumulatedUpdateTimeNano / 1_000_000.0;

        double alpha =
                1.0 - Math.exp(
                        -lastFrameMs / updateTimeSmoothingTimeWindowMs
                );

        smoothedAverageUpdateTimeMs =
                (1.0 - alpha) * smoothedAverageUpdateTimeMs
                        + alpha * updateMsThisFrame;

        thisFrameAccumulatedUpdateTimeNano = 0L;

        // --------------------------------------------------------
        // Smooth budget scale
        // --------------------------------------------------------

        double rawScale =
                (smoothedAverageUpdateTimeMs <= 0.0)
                        ? 1.0
                        : Mth.clamp(
                        updateTimeTargetMs / smoothedAverageUpdateTimeMs,
                        0.0,
                        1.0
                );

        double beta =
                1.0 - Math.exp(
                        -lastFrameMs / scaleSmoothingTimeConstantMs
                );

        double targetScale =
                (1.0 - beta) * smoothedBudgetScale
                        + beta * rawScale;

        double delta =
                Mth.clamp(
                        targetScale - smoothedBudgetScale,
                        -maxScaleChangePerFrame,
                        +maxScaleChangePerFrame
                );

        smoothedBudgetScale += delta;

        // --------------------------------------------------------
        // FPS guard
        // --------------------------------------------------------

        if (useFpsGuard) {
            smoothedAverageFrameTimeMs =
                    (1.0 - fpsEmaAlpha) * smoothedAverageFrameTimeMs
                            + fpsEmaAlpha * lastFrameMs;
        }
    }

    // ------------------------------------------------------------
    // Tick management
    // ------------------------------------------------------------

    private void onNewTick(long tick) {

        if (tick == currentTick) {
            return;
        }

        currentTick = tick;

        // IMPORTANT:
        // Freeze exact rate once for ALL objects this tick.
        effectiveRateThisTick = computeEffectiveUpdateRate();

        if ((tick & 0xFF) == 0) {
            evictStale(tick);
        }
    }

    private void evictStale(long tick) {

        long cutoff = tick - evictAfterTicks;

        Iterator<Map.Entry<ID, Entry>> it =
                entries.entrySet().iterator();

        while (it.hasNext()) {

            Map.Entry<ID, Entry> en = it.next();

            if (en.getValue().lastTickSeen < cutoff) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------
    // Scheduling
    // ------------------------------------------------------------

    private boolean stepPhaseAndGrant(Entry e) {

        double newPhase = e.phase01 + effectiveRateThisTick;

        if (newPhase >= 1.0) {
            e.phase01 = newPhase - 1.0;
            return true;
        }

        e.phase01 = newPhase;
        return false;
    }

    private double computeEffectiveUpdateRate() {

        double scaled =
                Math.max(
                        minUpdateRatePerTick,
                        baseUpdateRatePerTick * smoothedBudgetScale
                );

        if (useFpsGuard) {

            double fpsScale =
                    Mth.clamp(
                            fpsGuardTargetFrameMs /
                                    Math.max(
                                            smoothedAverageFrameTimeMs,
                                            fpsGuardTargetFrameMs
                                    ),
                            0.0,
                            1.0
                    );

            fpsScale = Math.max(minFpsScale, fpsScale);

            scaled =
                    Math.max(
                            minUpdateRatePerTick,
                            scaled * fpsScale
                    );
        }

        return Mth.clamp(scaled, 0.0, 1.0);
    }

    // ------------------------------------------------------------
    // Hash → stable phase
    // ------------------------------------------------------------

    private static double stablePhaseFromId(Object id) {

        int x = (id == null) ? 0 : id.hashCode();

        long z = mix64(x);

        long mant = z >>> 11;

        return mant * (1.0 / (1L << 53));
    }

    private static long mix64(int x) {

        long z =
                (x * 0x9E3779B9L)
                        ^ 0xBF58476D1CE4E5B9L;

        z =
                (z ^ (z >>> 30))
                        * 0xBF58476D1CE4E5B9L;

        z =
                (z ^ (z >>> 27))
                        * 0x94D049BB133111EBL;

        return z ^ (z >>> 31);
    }

    // ------------------------------------------------------------
    // Validation
    // ------------------------------------------------------------

    private static double requirePos(double v, String name) {
        if (v <= 0) {
            throw new IllegalArgumentException(name + " must be > 0");
        }
        return v;
    }

    private static double requireAlpha(double v, String name) {
        if (v <= 0 || v > 1) {
            throw new IllegalArgumentException(name + " must be in (0,1]");
        }
        return v;
    }
}
