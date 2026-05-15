package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import net.mehvahdjukaar.vista.common.IChunkViewWithZones;
import net.mehvahdjukaar.vista.mixins.accessor.ChunkMapAccessor;
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
            zv.vista$setExtraZones(VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player));
        }
    }

    /**
     * After the normal diff runs, sweep through the player's zone chunks and
     * force-queue any that are now sendable but were not inside the normal view.
     * This catches chunks that were not yet loaded when the camera first registered
     * (so markChunkPendingToSend silently failed in the packet handler) and became
     * available later.
     */
    @Inject(method = "applyChunkTrackingView", at = @At("RETURN"))
    private void vista$flushPendingZoneChunks(ServerPlayer player, ChunkTrackingView view, CallbackInfo ci) {
        ExtraChunkViewData data = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player);
        if (data.getZones().isEmpty()) return;
        ChunkMapAccessor self = (ChunkMapAccessor) (Object) this;
        int flushed = 0;
        for (ChunkPos pos : data.getAllChunks()) {
            // Only queue chunks outside normal view that haven't been queued yet
            if (!view.isInViewDistance(pos.x, pos.z) && !data.isZoneChunkQueued(pos)
                    && self.vista$getChunkToSend(pos.toLong()) != null) {
                self.vista$markChunkPendingToSend(player, pos);
                data.markZoneChunkQueued(pos);
                flushed++;
            }
        }
        if (flushed > 0) {
            VistaMod.LOGGER.info("[Vista/Chunks] applyChunkTrackingView flushed {} zone chunks for {}", flushed, player.getName().getString());
        }
    }

    /**
     * Prevents the server from sending a chunk-forget packet to the client for any
     * chunk that still belongs to the player's camera zones.
     */
    @Inject(method = "dropChunk", at = @At("HEAD"), cancellable = true)
    private static void vista$preventDropCameraZoneChunk(ServerPlayer player, ChunkPos chunkPos, CallbackInfo ci) {
        ExtraChunkViewData data = VistaMod.EXTRA_VIEW_AREAS.getOrCreate(player);
        if (data != null && data.containsChunk(chunkPos.x, chunkPos.z)) {
            ci.cancel();
        }
    }
}
