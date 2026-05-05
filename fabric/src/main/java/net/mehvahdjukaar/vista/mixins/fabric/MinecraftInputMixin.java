package net.mehvahdjukaar.vista.mixins.fabric;

import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftInputMixin {

    @Inject(method = "startUseItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"), cancellable = true)
    public void supp$onstartUseItem(CallbackInfo ci) {
        if (ViewFinderController.onPlayerUse()) {
            ci.cancel();
        }

    }
}
