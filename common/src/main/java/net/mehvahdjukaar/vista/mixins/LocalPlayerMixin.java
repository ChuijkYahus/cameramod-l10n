package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.vista.VistaModClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.Input;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin extends AbstractClientPlayer {

    protected LocalPlayerMixin(ClientLevel clientLevel, GameProfile gameProfile) {
        super(clientLevel, gameProfile);
    }

    @WrapOperation(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/Input;tick(ZF)V"))
    public void suppl$preventMovementWhileOperatingCannon(Input instance, boolean isSneaking, float sneakingSpeedMultiplier, Operation<Void> original) {
        original.call(instance, isSneaking, sneakingSpeedMultiplier);
        VistaModClient.modifyInputUpdate(instance, (LocalPlayer) (Object) this);
    }
}