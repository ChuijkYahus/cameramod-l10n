package net.mehvahdjukaar.vista.mixins;

import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(PipelineManager.class)
public class CompatIrisMixin {

    @Inject(method = "preparePipeline", at = @At("HEAD"), cancellable = true)
    private void vista$shushIris(NamespacedId currentDimension, CallbackInfoReturnable<WorldRenderingPipeline> cir) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            cir.setReturnValue(IrisCompat.getVistaPipeline());
        }
    }
}
