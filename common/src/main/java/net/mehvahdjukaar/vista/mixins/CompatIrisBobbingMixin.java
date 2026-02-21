package net.mehvahdjukaar.vista.mixins;

import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.misc.OptionalMixin;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@OptionalMixin(value = "net.irisshaders.iris.pipeline.PipelineManager")
@Mixin(GameRenderer.class)
public class CompatIrisBobbingMixin {

    @Inject(method = "bobView", at = @At(value = "HEAD"), cancellable = true)
    private void vista$skipBobViewDuringFeed(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (CompatHandler.IRIS && IrisCompat.shouldSkipBobbing()) {
            ci.cancel();
        }
    }

    @Inject(method = "bobHurt", at = @At(value = "HEAD"), cancellable = true)
    private void vista$skipBobHurtDuringFeed(PoseStack poseStack, float partialTicks, CallbackInfo ci) {
        if (CompatHandler.IRIS && IrisCompat.shouldSkipBobbing()) {
            ci.cancel();
        }
    }
}
