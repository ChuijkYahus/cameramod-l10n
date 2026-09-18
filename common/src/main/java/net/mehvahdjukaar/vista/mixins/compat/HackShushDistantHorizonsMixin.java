package net.mehvahdjukaar.vista.mixins.compat;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// DH assumes every level render is the player's
@Pseudo
@Mixin(value = ClientApi.class, remap = false)
public class HackShushDistantHorizonsMixin {

    @Inject(method = {"renderLods", "renderDeferredLodsForShaders", "renderFadeOpaque", "renderFadeTransparent"},
            at = @At("HEAD"), cancellable = true, remap = false)
    private void vista$killAssumptions(CallbackInfo ci) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) ci.cancel();
    }
}
