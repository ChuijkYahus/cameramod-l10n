package net.mehvahdjukaar.vista.mixins;

import dev.nonamecrackers2.simpleclouds.client.renderer.SimpleCloudsRenderer;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.minecraft.client.multiplayer.ClientLevel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(SimpleCloudsRenderer.class)
public class CompatSimpleCloudsMixin {

    // Simple Clouds gates ALL its LevelRenderer hooks (renderBeforeLevel, renderAfterSky,
    // renderAfterLevel, the renderClouds/renderSnowAndRain overrides, weather and fog) on this one
    // static check. It keeps a single global singleton renderer with window-sized framebuffers and a
    // shared cloud mesh; Vista's nested TV/mirror feed re-enters LevelRenderer.renderLevel with a
    // different camera mid-frame, so Simple Clouds runs again and stomps that global state, making
    // the main view's clouds flicker. Closing the gate during the feed render disables Simple Clouds
    // for the TV pass only (TV falls back to vanilla clouds) and leaves the main pass intact.
    @Inject(method = "canRenderInDimension", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vista$disableInLiveFeed(@Nullable ClientLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            cir.setReturnValue(false);
        }
    }
}
