package net.mehvahdjukaar.vista.client.renderer;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks RenderSection indices that are "pinned" to a fixed world position
 * outside the normal camera-relative torus buffer. These sections always
 * remain at their creation origin regardless of camera movement.
 */
public class PinnedSections {

    private static final Set<Integer> PINNED_INDICES = ConcurrentHashMap.newKeySet();

    public static void register(int index) {
        PINNED_INDICES.add(index);
    }

    public static void clear() {
        PINNED_INDICES.clear();
    }

    public static boolean isPinned(int index) {
        return PINNED_INDICES.contains(index);
    }

    public static Set<Integer> getAll() {
        return Collections.unmodifiableSet(PINNED_INDICES);
    }
}
