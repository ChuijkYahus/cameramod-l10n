package net.mehvahdjukaar.vista.mixins;

import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Pseudo
@Mixin(value = PipelineManager.class, remap = false)
public class CompatIrisMixin  {

    @Shadow
    @Nullable
    private WorldRenderingPipeline pipeline;

    @ModifyVariable(method = "preparePipeline", at = @At("HEAD"), argsOnly = true, remap = false)
    public NamespacedId modifyPipeline(NamespacedId value) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            return new NamespacedId(value.getNamespace(), "vista_live_feed_" + value.getName());
        }
        return value;
    }

    /*
    @Inject(method = "preparePipeline", remap = false, at = @At("HEAD"), cancellable = true)
    private void vista$preparePipeline(NamespacedId currentDimension,
                                       CallbackInfoReturnable<WorldRenderingPipeline> cir) {
        var modified = IrisCompat.getModifiedPipeline();
        if (modified != null) {
            this.pipeline = modified;
            cir.setReturnValue(modified);
        }
    }*/
}
