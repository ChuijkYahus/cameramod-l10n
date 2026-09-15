package net.mehvahdjukaar.vista.mixins.compat;

import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// sable overwrites yRot/xRot inside setRotation with the euler angles of its ship-rotated quaternion,
// so getYRot no longer returns what was set making it asymmetric
// for https://github.com/ryanhcode/sable/issues/1563
@Mixin(Camera.class)
public abstract class HackFixSableCameraAnglesMixin {

    @Shadow
    private float yRot;
    @Shadow
    private float xRot;

    @Inject(method = "setRotation(FF)V", at = @At("TAIL"))
    public void hackFixSable_1563(float yRot, float xRot, CallbackInfo ci) {
        this.yRot = yRot;
        this.xRot = xRot;
    }
}
