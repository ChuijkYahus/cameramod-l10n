package net.mehvahdjukaar.vista.mixins.compat;

import net.irisshaders.iris.pipeline.PipelineManager;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.mehvahdjukaar.vista.integration.iris.IrisCompat;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(value = PipelineManager.class, remap = false)
public class CompatIrisMixin  {

    @Shadow
    @Nullable
    private WorldRenderingPipeline pipeline;

    //kills iris pipeline. Why is this so hard
    //iris will actually shit itself if you dont do heavy mixins as its missing a lot of null checks
    @Inject(method = "preparePipeline", remap = false, at = @At("HEAD"), cancellable = true)
    private void vista$swapToVanillaPipeline(NamespacedId currentDimension,
                                             CallbackInfoReturnable<WorldRenderingPipeline> cir) {
        WorldRenderingPipeline modified = IrisCompat.getModifiedPipeline();
        if (modified != null) {
            this.pipeline = modified;
            cir.setReturnValue(modified);
        }
    }

    //unique pipeline for the feed. otherwise the shared one resizes all its gbuffers twice a frame, flicker and lag
    @ModifyVariable(method = "preparePipeline", at = @At("HEAD"), argsOnly = true, remap = false)
    private NamespacedId vista$rewriteDimensionForFeed(NamespacedId value) {
        if (IrisCompat.shouldSwapDimensionForFeed()) {
            return new NamespacedId(value.getNamespace(), "vista_live_feed_" + value.getName());
        }
        return value;
    }
}
