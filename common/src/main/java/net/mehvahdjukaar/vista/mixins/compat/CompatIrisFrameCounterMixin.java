package net.mehvahdjukaar.vista.mixins.compat;

import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Iris advances its frame counter once per game frame, but feeds render slower, so the counter jumps by N between feed renders.
@Pseudo
@Mixin(value = SystemTimeUniforms.FrameCounter.class, remap = false)
public class CompatIrisFrameCounterMixin {

    @Inject(method = "getAsInt", at = @At("HEAD"), cancellable = true, remap = false)
    private void vista$feedScopedFrameCounter(CallbackInfoReturnable<Integer> cir) {
        if (IrisCompat.isFeedRendering()) {
            cir.setReturnValue(IrisCompat.getFeedFrameCounter());
        }
    }
}
