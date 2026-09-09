package net.mehvahdjukaar.vista.mixins.compat;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import foundry.veil.api.client.render.CullFrustum;
import net.mehvahdjukaar.moonlight.api.misc.OptionalMixin;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@OptionalMixin(value = "foundry.veil.mixin.pipeline.client.PipelineLevelRendererMixin")
@Pseudo
@Mixin(value = LevelRenderer.class, priority = 1500)
public abstract class CompatVeilMixin {

    @TargetHandler(
            mixin = "foundry.veil.mixin.pipeline.client.PipelineLevelRendererMixin",
            name = "blit"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/VeilRenderSystem;drawLights(Lnet/minecraft/util/profiling/ProfilerFiller;Lfoundry/veil/api/client/render/CullFrustum;)Z"),
            require = 0
    )
    private boolean vista$skipVeilLightPassInFeeds(ProfilerFiller profiler, CullFrustum cullFrustum,
                                                   Operation<Boolean> original) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) return false;
        return original.call(profiler, cullFrustum);
    }

    // Veil 4.4.0+ variant of the above
    @TargetHandler(
            mixin = "foundry.veil.mixin.pipeline.client.PipelineLevelRendererMixin",
            name = "blit"
    )
    @WrapOperation(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/VeilRenderSystem;drawLights(Lnet/minecraft/util/profiling/ProfilerFiller;Lfoundry/veil/api/client/render/CullFrustum;Z)Z"),
            require = 0
    )
    private boolean vista$skipVeilLightPassInFeeds44(ProfilerFiller profiler, CullFrustum cullFrustum,
                                                     boolean renderInscattering, Operation<Boolean> original) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) return false;
        return original.call(profiler, cullFrustum, renderInscattering);
    }

}
