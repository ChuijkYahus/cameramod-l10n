package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.animal.Parrot;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Parrot.class)
public abstract class ParrotMixin {

    @Shadow
    @Nullable
    private BlockPos jukebox;

    @ModifyExpressionValue(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/world/level/block/Block;)Z"))
    private boolean vista$keepDancingToTv(boolean isJukebox) {
        if (isJukebox) return true;
        Parrot parrot = (Parrot) (Object) this;
        return parrot.level().getBlockEntity(jukebox) instanceof TVBlockEntity tv && tv.isPlaying();
    }
}
