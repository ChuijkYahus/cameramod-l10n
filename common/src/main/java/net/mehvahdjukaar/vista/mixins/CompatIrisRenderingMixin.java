package net.mehvahdjukaar.vista.mixins;

import net.irisshaders.iris.mixin.LevelRendererAccessor;
import net.irisshaders.iris.pipeline.IrisRenderingPipeline;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(IrisRenderingPipeline.class)
public class CompatIrisRenderingMixin {

    @Inject(method = "renderShadows", at = @At("HEAD"), cancellable = true)
    private void vista$cockblockIrisShadowGlobalStateMessBugs(LevelRendererAccessor worldRenderer, Camera playerCamera, CallbackInfo ci) {
        //we do this to prevent iris from rendering shadows when we are doing our custom rendering pass for the viewfinder
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            ci.cancel();
        }
    }
}
