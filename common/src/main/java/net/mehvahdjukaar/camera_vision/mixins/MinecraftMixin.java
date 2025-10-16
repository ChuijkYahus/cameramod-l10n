package net.mehvahdjukaar.camera_vision.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.pipeline.RenderTarget;
import net.mehvahdjukaar.camera_vision.CameraState;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @ModifyReturnValue(method = "getMainRenderTarget", at = @At(value = "RETURN"))
    public RenderTarget camera$setCameraTarget(RenderTarget original) {
        if (CameraState.TARGET != null) {
            return CameraState.TARGET;
        }
        return original;
    }
}
