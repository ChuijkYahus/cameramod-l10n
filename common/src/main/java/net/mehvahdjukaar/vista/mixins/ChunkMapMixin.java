package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.mixins.accessor.ChunkMapAccessor;
import net.mehvahdjukaar.vista.common.ICameraChunkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Inject(method = "updateChunkTracking", at = @At("TAIL"))
    private void vista$updateCameraChunkTracking(ServerPlayer player, CallbackInfo ci) {
        if (player instanceof ICameraChunkTracker tracker) {
            Set<BlockPos> cameraPositions = tracker.vista$getCameraPositions();
            if (!cameraPositions.isEmpty()) {
                ChunkMapAccessor accessor = (ChunkMapAccessor) this;
                
                int viewDistance = accessor.vista$getPlayerViewDistance(player);
                int cameraViewDistance = Math.min(4, viewDistance);
                
                for (BlockPos cameraPos : cameraPositions) {
                    ChunkPos chunkPos = new ChunkPos(cameraPos);
                    ChunkTrackingView trackingView = ChunkTrackingView.of(
                        chunkPos,
                        cameraViewDistance
                    );
                    accessor.vista$applyChunkTrackingView(player, trackingView);
                }
            }
        }
    }
}
