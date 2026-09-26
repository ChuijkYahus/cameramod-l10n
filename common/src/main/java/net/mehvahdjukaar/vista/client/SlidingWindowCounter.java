package net.mehvahdjukaar.vista.client;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SlidingWindowCounter<K> {

    private final int bucketCount;
    private final long bucketDurationNanos;
    private final Map<K, Integer>[] buckets;

    private final ConcurrentHashMap<K, AtomicInteger> totals = new ConcurrentHashMap<>();

    private volatile long lastTick;

    @SuppressWarnings("unchecked")
    public SlidingWindowCounter(Duration expireWindow, Duration resolution) {
        this.bucketDurationNanos = resolution.toNanos();
        this.bucketCount = (int) (expireWindow.toNanos() / bucketDurationNanos);
        this.buckets = new Map[bucketCount];
        for (int i = 0; i < bucketCount; i++) {
            buckets[i] = new HashMap<>();
        }
        this.lastTick = currentTick();
    }

    public void record(K key) {
        advance();

        int index = (int) (currentTick() % bucketCount);
        buckets[index].merge(key, 1, Integer::sum);
        totals.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
    }

    public int getCount(K key) {
        advance();
        AtomicInteger v = totals.get(key);
        return v == null ? 0 : v.get();
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
            Map<K, Integer> bucket = buckets[index];

            for (Map.Entry<K, Integer> e : bucket.entrySet()) {
                AtomicInteger total = totals.get(e.getKey());
                if (total != null) {
                    total.addAndGet(-e.getValue());
                }
            }

            bucket.clear();
        }

        lastTick = now;
    }
}

