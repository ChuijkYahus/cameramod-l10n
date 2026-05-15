package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.common.ExtraChunkViewData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes {@code ClientChunkCache.Storage.inRange(x, z)} return {@code true} for any
 * chunk belonging to a camera zone. Without this the storage rejects slot writes for
 * chunks outside the normal view range, which would prevent zone chunks from being
 * stored in the circular buffer at all.
 */
@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public class ClientChunkCacheStorageMixin {

    @Inject(method = "inRange", at = @At("HEAD"), cancellable = true)
    private void vista$alwaysInRangeForPinnedZone(int x, int z, CallbackInfoReturnable<Boolean> cir) {
        if (ExtraChunkViewData.CLIENT_INSTANCE.containsChunk(x, z)) {
            cir.setReturnValue(true);
        }
    }
}
