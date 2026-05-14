package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.server.level.ChunkTrackingView;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

@Mixin(ChunkTrackingView.Positioned.class)
public abstract class ChunkTrackingViewMixin implements ChunkTrackingView {

    @Shadow
    public abstract ChunkPos center();

    @Shadow
    public abstract int viewDistance();

    @Inject(method = "contains(IIZ)Z", at = @At("HEAD"), cancellable = true)
    private void vista$alwaysContainChunk00(int x, int z, boolean includeOuter, CallbackInfoReturnable<Boolean> cir) {
        if (x == 0 && z == 0) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "forEach", at = @At("RETURN"))
    private void vista$forceIncludeChunk00(Consumer<ChunkPos> action, CallbackInfo ci) {
        // Only add if it's not already in the normal view
        ChunkPos center = this.center();
        int viewDistance = this.viewDistance();
        if (!ChunkTrackingView.isWithinDistance(center.x, center.z, viewDistance, 0, 0, true)) {
            action.accept(new ChunkPos(0, 0));
        }
    }
}