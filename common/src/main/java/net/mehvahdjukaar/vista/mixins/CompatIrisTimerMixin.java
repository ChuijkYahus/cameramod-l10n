package net.mehvahdjukaar.vista.mixins;

import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// Same reasoning as CompatIrisFrameCounterMixin but for the float side. Packs that
// implement procedural per-frame offsets via frameTimeCounter / frameTime instead of
// frameCounter would otherwise see wall-clock-from-game-start values that jump by
// the feed interval (~100 ms at 10 Hz) per observation — too coarse for any temporal
// resolve. Feed-local timer advances by real elapsed time between consecutive feed
// renders.
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
