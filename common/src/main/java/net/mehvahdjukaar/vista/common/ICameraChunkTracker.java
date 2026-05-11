package net.mehvahdjukaar.vista.common;

import net.minecraft.core.BlockPos;
import java.util.Set;

public interface ICameraChunkTracker {
    Set<BlockPos> vista$getCameraPositions();
    void vista$addCameraPosition(BlockPos pos);
    void vista$removeCameraPosition(BlockPos pos);
}
