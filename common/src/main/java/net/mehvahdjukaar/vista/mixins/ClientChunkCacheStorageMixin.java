package net.mehvahdjukaar.vista.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(targets = "net.minecraft.client.multiplayer.ClientChunkCache$Storage")
public class ClientChunkCacheStorageMixin {
    
    @Inject(method = "inRange", at = @At("HEAD"), cancellable = true)
    private void vista$alwaysInRangeFor00(int x, int z, CallbackInfoReturnable<Boolean> cir) {
        if (x == 0 && z == 0) {
            cir.setReturnValue(true);
        }
    }
}
