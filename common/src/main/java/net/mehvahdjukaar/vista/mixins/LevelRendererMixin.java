package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.vista.client.LevelRendererCameraState;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.minecraft.client.Camera;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyReturnValue(method = "shouldShowEntityOutlines", at = @At(value = "RETURN"))
    public boolean vista$disableEntityOutlines(boolean original) {
        if (LiveFeedTexturesManager.getLifeFeedBeingRendered() != null) {
            return false;
        }
        return original;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"),
            require = 1)
    public boolean vista$isCameraDetached(boolean original) {
        if (LiveFeedTexturesManager.getLifeFeedBeingRendered() != null) {
            return true;
        }
        return original;
    }

    //idk why this was needed
    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;",
            ordinal = 3), require = 1)
    public Entity vista$getActualPlayer(Entity original, @Local(ordinal = 0) Entity entity) {
        if (LiveFeedTexturesManager.getLifeFeedBeingRendered() != null && entity instanceof LocalPlayer) {
            return entity;
        }
        return original;
    }

    @Inject(method = "setupRender", at = @At("HEAD"), cancellable = true)
    public void vista$alterSetupRender(Camera camera, Frustum frustum, boolean hasCapturedFrustum, boolean isSpectator, CallbackInfo ci) {
        LevelRendererCameraState cameraState = LiveFeedTexturesManager.getLiveFeedCameraState();
        if (cameraState != null) {
            LevelRendererCameraState.setupRender((LevelRenderer) (Object) this, camera, frustum, hasCapturedFrustum, isSpectator);
            ci.cancel();
        }
    }

}
