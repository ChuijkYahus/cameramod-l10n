package net.mehvahdjukaar.vista.mixins;

import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import software.bernie.geckolib.animatable.GeoAnimatable;
import software.bernie.geckolib.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

@Pseudo
@Mixin(GeoModel.class)
public class CompatGeckoLibMixin<T extends GeoAnimatable> {

    @Inject(method = "handleAnimations", at = @At("HEAD"), cancellable = true)
    public void vista$blockGeckoStateMachineStuffThatShouldProbablyNotEvenExist(T animatable, long instanceId, AnimationState<T> animationState, float partialTick, CallbackInfo ci) {
        if (VistaLevelRenderer.isRenderingLiveFeed()) ci.cancel();
    }
}
