package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Frustum.class)
public class FrustumMixin {

    @Inject(method = "offsetToFullyIncludeCameraCube", at = @At("HEAD"), cancellable = true)
    public void vista$skipOffsetToFullyIncludeCameraCube(int offset, CallbackInfoReturnable<Frustum> cir) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) {
            cir.setReturnValue((Frustum) (Object) this);
        }
    }
}
