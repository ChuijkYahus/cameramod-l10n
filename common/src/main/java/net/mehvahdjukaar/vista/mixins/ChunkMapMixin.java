package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import net.mehvahdjukaar.vista.common.IChunkViewWithZones;
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

    /**
     * Attach the player's zone data to the incoming view before {@code difference}
     * runs. Because {@code player.setChunkTrackingView} stores the same Positioned
     * instance, the field survives into the next call as the "old view", so old and
     * new carry identical zone sets → no spurious re-sends on every section change.
     */
    @Inject(method = "applyChunkTrackingView", at = @At("HEAD"))
    private void vista$attachZonesToView(ServerPlayer player, ChunkTrackingView view, CallbackInfo ci) {
        if (view instanceof IChunkViewWithZones zv) {
            zv.vista$setExtraZones(VistaMod.TRACKED_CAMERAS_ATTACH.getOrCreate(player));
        }
    }

    /**
     * Prevents the server from sending a chunk-forget packet to the client for any
     * chunk that still belongs to the player's camera zones.
     */
    @Inject(method = "dropChunk", at = @At("HEAD"), cancellable = true)
    private static void vista$preventDropCameraZoneChunk(ServerPlayer player, ChunkPos chunkPos, CallbackInfo ci) {
        ExtraChunkViewData data = VistaMod.TRACKED_CAMERAS_ATTACH.getOrCreate(player);
        if (data != null && data.containsChunk(chunkPos.x, chunkPos.z)) {
            ci.cancel();
        }
    }
}
