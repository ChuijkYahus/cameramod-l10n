package net.mehvahdjukaar.vista.common.chunk_tracking;

import net.minecraft.world.level.ChunkPos;

import java.util.Collection;

public interface IChunkTrackingViewExtension {
    /**
     * Call this from your mod initializer to set the extra chunks.
     */
      void vista$setHardcodedChunks(Collection<ChunkPos> chunks);

      void vista$setExtraRadius(int radius) ;
}
