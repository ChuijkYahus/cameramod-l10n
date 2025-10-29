package net.mehvahdjukaar.vista.mixins.fabric;

import net.fabricmc.fabric.mixin.event.lifecycle.client.MinecraftClientMixin;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    @Inject(method = "runTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;render(Lnet/minecraft/client/DeltaTracker;Z)V",
    shift = At.Shift.AFTER))
    public void vista$whyIsntThereAnEventForThis(boolean renderLevel, CallbackInfo ci) {
        VistaModClient.onRenderTickEnd((Minecraft) (Object) this);
    }

}
