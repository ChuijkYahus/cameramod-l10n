package net.mehvahdjukaar.vista.client;

import net.mehvahdjukaar.vista.mixins.ClientChunkCacheMixin;
import net.minecraft.world.level.chunk.LevelChunk;

import java.util.Map;

/**
 * Interface injected onto {@link net.minecraft.client.multiplayer.ClientChunkCache}
 * by {@link ClientChunkCacheMixin} to expose Vista's pinned-chunk map for debug
 * rendering without requiring reflection.
 */
public interface IClientChunkCacheExt {

    /** Returns a live, read-only view of zone chunks Vista is holding outside the normal cache. */
    Map<Long, LevelChunk> vista$getPinnedChunks();
}
