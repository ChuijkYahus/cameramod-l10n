package net.mehvahdjukaar.vista.mixins.compat;

import net.mehvahdjukaar.moonlight.api.misc.OptionalMixin;
import net.mehvahdjukaar.vista.common.mob_gaze.GazeRedirect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@OptionalMixin(value = "com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking")
@Pseudo
@Mixin(targets = "com.blackgear.vanillabackport.common.level.entities.mob.monster.creaking.Creaking", remap = false)
public class CompatVanillaBackportCreakingMixin {

    @Inject(method = "checkCanMove", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void vista$freezeWhenWatchedOnScreen(CallbackInfoReturnable<Boolean> cir) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (GazeRedirect.isWatchedThroughScreens(self.level(), self)) {
            cir.setReturnValue(false);
        }
    }
}
