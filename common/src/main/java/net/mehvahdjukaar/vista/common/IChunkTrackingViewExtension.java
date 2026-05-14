package net.mehvahdjukaar.vista.common;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public interface IChunkTrackingViewExtension {
    /**
     * Call this from your mod initializer to set the extra chunks.
     */
      void vista$setHardcodedChunks(Collection<ChunkPos> chunks);

      void vista$setExtraRadius(int radius) ;
}
