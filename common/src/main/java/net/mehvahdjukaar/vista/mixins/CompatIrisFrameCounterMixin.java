package net.mehvahdjukaar.vista.mixins;

import net.irisshaders.iris.uniforms.SystemTimeUniforms;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

// While Vista is rendering a feed, return a feed-local counter instead of the global
// game-frame counter. Iris advances SystemTimeUniforms.COUNTER once per game frame
// (in iris$startFrame), but feeds typically render at a lower rate. If the counter
// jumps by N between feed renders, shader-pack TAA jitter cycles through N halton
// samples per feed step and the temporal resolve can't accumulate — every pixel
// wobbles. With a +1-per-feed counter, jitter advances one sub-pixel sample per
// feed render and TAA composite can blend two consecutive frames properly.
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
