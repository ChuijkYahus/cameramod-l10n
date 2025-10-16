package net.mehvahdjukaar.camera_vision.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.camera_vision.CameraState;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyReturnValue(method = "shouldShowEntityOutlines", at = @At(value = "RETURN"))
    public boolean camera$disableEntityOutlines(boolean original) {
        if (CameraState.noOutline) {
            return false;
        }
        return original;
    }

}
