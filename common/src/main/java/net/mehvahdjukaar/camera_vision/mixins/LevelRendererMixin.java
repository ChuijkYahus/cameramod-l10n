package net.mehvahdjukaar.camera_vision.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.mehvahdjukaar.camera_vision.CameraState;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyReturnValue(method = "shouldShowEntityOutlines", at = @At(value = "RETURN"))
    public boolean camera$disableEntityOutlines(boolean original) {
        if (CameraState.NO_OUTLINE) {
            return false;
        }
        return original;
    }

    @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSectionLayer(Lnet/minecraft/client/renderer/RenderType;DDDLorg/joml/Matrix4f;Lorg/joml/Matrix4f;)V"))
    public boolean camera$skipRenderSectionLayer(LevelRenderer levelRenderer, net.minecraft.client.renderer.RenderType renderType, double d0, double d1, double d2, org.joml.Matrix4f matrix4f, org.joml.Matrix4f matrix4f1) {
        if (CameraState.SKIP_WORLD_RENDERING) {
            return false;
        }
        return true;
    }

    @WrapWithCondition(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V"))
    public boolean camera$sky(LevelRenderer instance, Matrix4f f8, Matrix4f f9, float j, Camera f3, boolean f4, Runnable f5) {
        if (CameraState.SKIP_SKY) {
            return false;
        }
        return true;
    }

}
