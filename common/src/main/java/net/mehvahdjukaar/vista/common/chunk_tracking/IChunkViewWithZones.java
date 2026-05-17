package net.mehvahdjukaar.vista.common.chunk_tracking;

import net.mehvahdjukaar.vista.mixins.ChunkMapMixin;
import net.mehvahdjukaar.vista.mixins.ChunkTrackingViewMixin;

/**
 * Duck-typed interface injected into {@link net.minecraft.server.level.ChunkTrackingView.Positioned}
 * by {@link ChunkTrackingViewMixin}.
 *
 * Carrying the zone data directly on the view object means:
 *  - When a new Positioned is created and passed to {@code applyChunkTrackingView}, its zones
 *    are set by {@link ChunkMapMixin} before {@code difference} runs.
 *  - The same Positioned instance is later stored via {@code player.setChunkTrackingView},
 *    so when it is retrieved as the "old view" on the next call it already has zones attached.
 *  - {@code ChunkTrackingView.difference} keeps both views as {@code Positioned}, preserving
 *    the bounding-box fast path and avoiding any per-move re-sends of zone chunks.
 */
public interface IChunkViewWithZones {
    ExtraChunkViewData vista$getExtraZones();
    void vista$setExtraZones(ExtraChunkViewData data);
}
