package net.mehvahdjukaar.vista.mixins.compat;

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

    @Inject(method = "canRenderInDimension", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vista$disableInLiveFeed(@Nullable ClientLevel level, CallbackInfoReturnable<Boolean> cir) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            cir.setReturnValue(false);
        }
    }
}
