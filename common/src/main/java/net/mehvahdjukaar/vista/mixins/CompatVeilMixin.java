package net.mehvahdjukaar.vista.mixins;

import com.bawnorton.mixinsquared.TargetHandler;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import foundry.veil.mixin.pipeline.client.PipelineLevelRendererMixin;
import net.mehvahdjukaar.moonlight.api.misc.OptionalMixin;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
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
    @ModifyExpressionValue(
            method = "@MixinSquared:Handler",
            at = @At(
                    value = "INVOKE",
                    target = "Lfoundry/veil/api/client/render/VeilLevelPerspectiveRenderer;isRenderingPerspective()Z")
    )
    private boolean vista$incorrectlyCancelVeilLightsJustLikeVeilItselfDoesHack(boolean original) {
        return original || VistaLevelRenderer.isRenderingLiveFeed();
    }

}
