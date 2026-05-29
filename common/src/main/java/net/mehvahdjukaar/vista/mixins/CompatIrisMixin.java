package net.mehvahdjukaar.vista.mixins;

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

    // iris_off_hack=true: hand back a no-op VanillaRenderingPipeline so Iris stays
    // out of the feed pass entirely.
    @Inject(method = "preparePipeline", remap = false, at = @At("HEAD"), cancellable = true)
    private void vista$swapToVanillaPipeline(NamespacedId currentDimension,
                                             CallbackInfoReturnable<WorldRenderingPipeline> cir) {
        WorldRenderingPipeline modified = IrisCompat.getModifiedPipeline();
        if (modified != null) {
            this.pipeline = modified;
            cir.setReturnValue(modified);
        }
    }

    // iris_off_hack=false: force PipelineManager to keep a separate IrisRenderingPipeline
    // entry for the feed by rewriting the dimension namespace. Without this the single
    // shared per-dim pipeline gets its RenderTargets resized back to the feed canvas
    // size and then back to the main framebuffer size every frame, which reallocates
    // every gbuffer and is what produces the flicker + perf cliff.
    @ModifyVariable(method = "preparePipeline", at = @At("HEAD"), argsOnly = true, remap = false)
    private NamespacedId vista$rewriteDimensionForFeed(NamespacedId value) {
        if (IrisCompat.shouldSwapDimensionForFeed()) {
            return new NamespacedId(value.getNamespace(), "vista_live_feed_" + value.getName());
        }
        return value;
    }
}
