package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public class PlayerMixin {

    @ModifyReturnValue(method = "isScoping", at = @At("RETURN"))
    public boolean vista$modifyIsScoping(boolean original) {
        return original || ViewFinderController.isZooming();
    }
}
