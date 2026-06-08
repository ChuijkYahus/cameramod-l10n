package net.mehvahdjukaar.vista.mixins;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.mehvahdjukaar.moonlight.api.misc.OptionalMixin;
import net.mehvahdjukaar.supplementaries.common.block.tiles.EndermanSkullBlockTile;
import net.mehvahdjukaar.vista.common.mirror.MirrorReflectiveLook;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@OptionalMixin(value = "net.mehvahdjukaar.supplementaries.common.block.tiles.EndermanSkullBlockTile")
@Pseudo
@Mixin(EndermanSkullBlockTile.class)
public abstract class EndermanSkullBlockTileMixin {

    @ModifyExpressionValue(
            method = "isBeingWatched",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Player;pick(DFZ)Lnet/minecraft/world/phys/HitResult;")
    )
    private static HitResult vista$reflectivePick(HitResult original,
                                                  Level level, BlockPos pos,
                                                  net.minecraft.world.level.block.state.BlockState state,
                                                  @Local Player player) {
        if (original instanceof BlockHitResult bh && bh.getBlockPos().equals(pos)) return original;
        BlockHitResult mirror = MirrorReflectiveLook.tryHitThroughMirror(
                player, level, pos,
                MirrorReflectiveLook.MAX_DISTANCE, MirrorReflectiveLook.MAX_BOUNCES);
        return mirror != null ? mirror : original;
    }
}
