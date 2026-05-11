package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.common.ICameraChunkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.HashSet;
import java.util.Set;

@Mixin(ServerPlayer.class)
public class ServerPlayerMixin implements ICameraChunkTracker {
    
    @Unique
    private final Set<BlockPos> vista$cameraPositions = new HashSet<>();
    
    @Override
    public Set<BlockPos> vista$getCameraPositions() {
        return vista$cameraPositions;
    }
    
    @Override
    public void vista$addCameraPosition(BlockPos pos) {
        vista$cameraPositions.add(pos);
    }
    
    @Override
    public void vista$removeCameraPosition(BlockPos pos) {
        vista$cameraPositions.remove(pos);
    }
}
