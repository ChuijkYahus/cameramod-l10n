package net.mehvahdjukaar.camera_vision.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.camera_vision.client.LiveFeedRendererManager;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LevelRenderer.class)
public class LevelRendererMixin {

    @ModifyReturnValue(method = "shouldShowEntityOutlines", at = @At(value = "RETURN"))
    public boolean camera$disableEntityOutlines(boolean original) {
        if (LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED != null) {
            return false;
        }
        return original;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;isDetached()Z"),
    require = 1)
    public boolean camera$renderPlayer(boolean original) {
        if (LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED != null) {
            return true;
        }
        return original;
    }

    @ModifyExpressionValue(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getEntity()Lnet/minecraft/world/entity/Entity;",
    ordinal = 3),
            require = 1)
    public Entity camera$renderPlayer2(Entity original, @Local(ordinal = 0) Entity entity) {
        if (LiveFeedRendererManager.LIVE_FEED_BEING_RENDERED != null && entity instanceof LocalPlayer) {
            return entity;
        }
        return original;
    }

}
