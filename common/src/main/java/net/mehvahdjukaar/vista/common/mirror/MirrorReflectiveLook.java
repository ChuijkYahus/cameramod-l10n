package net.mehvahdjukaar.vista.common.mirror;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Shared helper for blocks whose tick logic does {@code player.pick(...)} to detect being looked
 * at (vanilla enderman heads, Supplementaries enderman heads, Quark ender watchers). If the
 * direct pick misses the target, this allows the gaze ray to bounce off {@link MirrorBlock}s
 * and resume in the reflected direction.
 */
public final class MirrorReflectiveLook {

    public static final double MAX_DISTANCE = 64.0;
    public static final int MAX_BOUNCES = 1;
    private static final double EPSILON = 1.0e-3;

    private MirrorReflectiveLook() {}

    /**
     * Casts a ray from the player's eyes that may bounce off mirrors, and returns the final
     * hit if it lands on {@code target}, otherwise null.
     */
    @Nullable
    public static BlockHitResult tryHitThroughMirror(Player player, Level level, BlockPos target,
                                                     double maxDistance, int maxBounces) {
        Vec3 origin = player.getEyePosition(1.0F);
        Vec3 dir = player.getViewVector(1.0F).normalize();
        return bouncePick(level, player, origin, dir, maxDistance, maxBounces, target);
    }

    @Nullable
    private static BlockHitResult bouncePick(Level level, Player player, Vec3 origin, Vec3 dir,
                                             double remaining, int bouncesLeft, BlockPos target) {
        if (remaining <= 0) return null;
        Vec3 end = origin.add(dir.scale(remaining));
        BlockHitResult hit = level.clip(new ClipContext(origin, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player));
        if (hit.getType() == HitResult.Type.MISS) return null;

        BlockPos hitPos = hit.getBlockPos();
        if (hitPos.equals(target)) return hit;
        if (bouncesLeft <= 0) return null;

        BlockState state = level.getBlockState(hitPos);
        if (!(state.getBlock() instanceof MirrorBlock)) return null;

        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        if (hit.getDirection() != facing) return null;

        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 reflectedDir = dir.subtract(normal.scale(2 * dir.dot(normal))).normalize();

        Vec3 hitLoc = hit.getLocation();
        double consumed = hitLoc.subtract(origin).length();
        Vec3 nextOrigin = hitLoc.add(reflectedDir.scale(EPSILON));

        return bouncePick(level, player, nextOrigin, reflectedDir, remaining - consumed, bouncesLeft - 1, target);
    }
}
