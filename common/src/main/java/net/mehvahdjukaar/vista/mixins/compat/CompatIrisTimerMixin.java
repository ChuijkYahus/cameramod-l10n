package net.mehvahdjukaar.vista.mixins.compat;

import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Same reasoning as CompatIrisFrameCounterMixin
@Pseudo
@Mixin(value = SystemTimeUniforms.Timer.class, remap = false)
public class CompatIrisTimerMixin {

    @Inject(method = "getFrameTimeCounter", at = @At("HEAD"), cancellable = true, remap = false)
    private void vista$feedFrameTimeCounter(CallbackInfoReturnable<Float> cir) {
        if (IrisCompat.isFeedRendering()) {
            cir.setReturnValue(IrisCompat.getFeedFrameTimeCounter());
        }
    }

    @Inject(method = "getLastFrameTime", at = @At("HEAD"), cancellable = true, remap = false)
    private void vista$feedLastFrameTime(CallbackInfoReturnable<Float> cir) {
        if (IrisCompat.isFeedRendering()) {
            cir.setReturnValue(IrisCompat.getFeedLastFrameTime());
        }
    }
}
