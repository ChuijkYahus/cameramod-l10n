package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.expression.Definition;
import com.llamalad7.mixinextras.expression.Expression;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
public class GameRendererMixin {

    @Definition(id = "fov", field = "Lnet/minecraft/client/renderer/GameRenderer;fov:F")
    @Expression("this.fov < 0.1")
    @ModifyExpressionValue(method = "tickFov", at = @At("MIXINEXTRAS:EXPRESSION"))
    private boolean vista$uncapFog(boolean original) {
        if (!original) {
            return false;
        }
        return !ViewFinderController.isActive();
    }
}
