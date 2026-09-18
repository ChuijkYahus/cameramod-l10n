package net.mehvahdjukaar.vista.mixins.compat;

import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

//DH removes vanilla fog assuming its LODs are behind it. no LODs in feeds so keep the fog there
@Pseudo
@Mixin(targets = {
        "com.seibel.distanthorizons.common.commonMixins.MixinVanillaFogCommon_neoforge",
        "com.seibel.distanthorizons.common.commonMixins.MixinVanillaFogCommon_fabric"
}, remap = false)
public class HackShushDistantHorizonsFogMixin {

    @Inject(method = "cancelFog", at = @At("HEAD"), cancellable = true, remap = false)
    private static void vista$keepVanillaFogInFeeds(CallbackInfoReturnable<Boolean> cir) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) cir.setReturnValue(false);
    }
}
