package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.chunk_tracking.IChunkViewWithZones;
import net.mehvahdjukaar.vista.common.chunk_tracking.ServerCameraChunkManager;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ChunkMap.class)
public class ChunkMapMixin {

    @Inject(method = "applyChunkTrackingView", at = @At("HEAD"))
    private void vista$attachZonesToView(ServerPlayer player, ChunkTrackingView view, CallbackInfo ci) {
        if (view instanceof IChunkViewWithZones viewWithZones) {
            viewWithZones.vista$setExtraZones(VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player));
        }
    }

    @Inject(method = "applyChunkTrackingView", at = @At("RETURN"))
    private void vista$sendZoneChunks(ServerPlayer player, ChunkTrackingView view, CallbackInfo ci) {
        ServerCameraChunkManager.sendLoadedZoneChunks((ChunkMap) (Object) this, player, view);
    }

    @ModifyReturnValue(method = "isChunkTracked", at = @At("RETURN"))
    private boolean vista$trackZoneChunks(boolean original, ServerPlayer player, int x, int z) {
        return original || VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player).containsChunk(x, z);
    }

    @Inject(method = "dropChunk", at = @At("HEAD"), cancellable = true)
    private static void vista$keepZoneChunks(ServerPlayer player, ChunkPos chunkPos, CallbackInfo ci) {
        if (VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player).containsChunk(chunkPos.x, chunkPos.z)) ci.cancel();
    }
}
