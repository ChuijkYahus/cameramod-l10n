package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {

    @WrapWithCondition(method = "turnPlayer",
            at = @At(target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V", value = "INVOKE")
    )
    protected boolean vista$onMoveMouse(LocalPlayer instance, double yRot, double xRot) {
        return !ViewFinderController.onPlayerRotated(yRot, xRot);
    }
}
