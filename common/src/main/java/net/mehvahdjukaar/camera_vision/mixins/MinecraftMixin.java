package net.mehvahdjukaar.camera_vision.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.camera_vision.client.CameraRendererManager;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "<clinit>" , at = @At("TAIL"))
    private static void onClinit(CallbackInfo ci) {
        int aa = 1;
    }

    @ModifyReturnValue(method = "getMainRenderTarget", at = @At(value = "RETURN"))
    public RenderTarget camera$setCameraTarget(RenderTarget original) {
        if (CameraRendererManager.CAMERA_CANVAS != null) {
            return CameraRendererManager.CAMERA_CANVAS;
        }
        return original;
    }
}
