package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.PinnedChunks;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public class ClientChunkCacheStorageMixin {

    @Shadow
    @Final
    AtomicReferenceArray<LevelChunk> chunks;
    @Shadow
    @Final
    int chunkRadius;
    @Shadow
    volatile int viewCenterX;
    @Shadow
    volatile int viewCenterZ;

    @Inject(method = "inRange", at = @At("HEAD"), cancellable = true)
    private void vista$alwaysInRangeForPinnedZone(int x, int z, CallbackInfoReturnable<Boolean> cir) {
        if (VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(x, z)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "replace(ILnet/minecraft/world/level/chunk/LevelChunk;)V",
            at = @At("HEAD"), cancellable = true)
    private void vista$keepZoneChunksOutOfArray(int chunkIndex, LevelChunk chunk, CallbackInfo ci) {
        if (chunk != null) {
            ChunkPos pos = chunk.getPos();
            if (vista$isFarZoneChunk(pos)) {
                PinnedChunks.pin(chunk);
                ci.cancel();
                return;
            }
            PinnedChunks.unpin(pos.x, pos.z);
        }

        LevelChunk evicted = this.chunks.get(chunkIndex);
        if (evicted != null && evicted != chunk && vista$isFarZoneChunk(evicted.getPos())) {
            PinnedChunks.pin(evicted);
        }
    }

    @Unique
    private boolean vista$isFarZoneChunk(ChunkPos pos) {
        return !vista$inNormalRange(pos.x, pos.z)
                && VistaModClient.CLIENT_EXTRA_CHUNK_VIEW_DATA.containsChunk(pos.x, pos.z);
    }

    @Unique
    private boolean vista$inNormalRange(int x, int z) {
        return Math.abs(x - this.viewCenterX) <= this.chunkRadius
                && Math.abs(z - this.viewCenterZ) <= this.chunkRadius;
    }
}
