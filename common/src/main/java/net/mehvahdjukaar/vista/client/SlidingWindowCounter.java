package net.mehvahdjukaar.vista.client;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class SlidingWindowCounter<K> {

    private static final class Bucket<K> {
        final Map<K, Integer> counts = new HashMap<>();
    }

    private final Bucket<K>[] ring;
    private final int bucketCount;
    private final long bucketDurationNanos;

    private final ConcurrentHashMap<K, AtomicInteger> totals = new ConcurrentHashMap<>();

    private volatile long lastTick;

    public SlidingWindowCounter(Duration expireWindow, Duration resolution) {
        this(expireWindow.toNanos(),
                TimeUnit.NANOSECONDS,
                (int) resolution.toMillis());
    }

    @SuppressWarnings("unchecked")
    public SlidingWindowCounter(long window,
                                TimeUnit unit,
                                int resolutionMillis) {

        long windowNanos = unit.toNanos(window);
        this.bucketDurationNanos =
                TimeUnit.MILLISECONDS.toNanos(resolutionMillis);

        if (windowNanos <= 0 || resolutionMillis <= 0)
            throw new IllegalArgumentException();

        this.bucketCount = (int) (windowNanos / bucketDurationNanos);
        if (bucketCount <= 0)
            throw new IllegalArgumentException("Resolution too large for window");

        this.ring = (Bucket<K>[]) new Bucket[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            ring[i] = new Bucket<>();
        }

        this.lastTick = currentTick();
    }

    public void record(K key) {
        advance();

        int index = (int) (currentTick() % bucketCount);
        Bucket<K> bucket = ring[index];

        bucket.counts.merge(key, 1, Integer::sum);
        totals.computeIfAbsent(key, k -> new AtomicInteger())
                .incrementAndGet();
    }

    public int getCount(K key) {
        advance();
        AtomicInteger v = totals.get(key);
        return v == null ? 0 : v.get();
    }

    public Map<K, Integer> snapshot() {
        advance();
        Map<K, Integer> result = new HashMap<>();
        totals.forEach((k, v) -> {
            int count = v.get();
            if (count > 0) result.put(k, count);
        });
        return result;
    }

    private long currentTick() {
        return System.nanoTime() / bucketDurationNanos;
    }

    private synchronized void advance() {
        long now = currentTick();
        long diff = now - lastTick;
        if (diff <= 0) return;

        long steps = Math.min(diff, bucketCount);

        for (long i = 1; i <= steps; i++) {
            int index = (int) ((lastTick + i) % bucketCount);
            Bucket<K> bucket = ring[index];

            for (Map.Entry<K, Integer> e : bucket.counts.entrySet()) {
                AtomicInteger total = totals.get(e.getKey());
                if (total != null) {
                    total.addAndGet(-e.getValue());
                }
            }

            bucket.counts.clear();
        }

        lastTick = now;
    }
}

