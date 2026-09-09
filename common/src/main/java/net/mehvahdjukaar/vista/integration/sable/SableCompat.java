package net.mehvahdjukaar.vista.integration.sable;

import dev.ryanhcode.sable.companion.SableCompanion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Position;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public class SableCompat {

    public static boolean isOnSubLevel(Level level, BlockPos pos) {
        return SableCompanion.INSTANCE.isInPlotGrid(level, pos);
    }

    @Nullable
    public static ChunkPos projectChunkOutOfSubLevel(Level level, BlockPos plotPos) {
        Vec3 world = SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) Vec3.atCenterOf(plotPos));
        if (SableCompanion.INSTANCE.isInPlotGrid(level, world)) return null;
        return new ChunkPos(BlockPos.containing(world));
    }
}
