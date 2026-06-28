package net.mehvahdjukaar.vista.common.connection;

import net.mehvahdjukaar.moonlight.api.util.math.Direction2D;
import net.mehvahdjukaar.moonlight.api.util.math.Rect2D;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.Vec2;
import org.jetbrains.annotations.Nullable;

public interface IConnectedBlock {

    EnumProperty<ConnectionType> connectionProperty();

    int maxConnectedSize();

    boolean squareAspectRatio();

    AbstractGridAccess createGridAccess(Level level, BlockPos pos, BlockState state);

    default boolean connectionMatches(BlockState self, BlockState other) {
        return other.is((Block) this) &&
                other.getValue(HorizontalDirectionalBlock.FACING) == self.getValue(HorizontalDirectionalBlock.FACING);
    }

    default ConnectionType getTypeFromNeighbors(Level level, BlockPos pos, BlockState state) {
        if (maxConnectedSize() <= 1) return ConnectionType.SINGLE;
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        boolean up = isNeighborConnected(level, pos, state, Direction.UP);
        boolean down = isNeighborConnected(level, pos, state, Direction.DOWN);
        boolean left = isNeighborConnected(level, pos, state, facing.getClockWise());
        boolean right = isNeighborConnected(level, pos, state, facing.getCounterClockWise());
        return ConnectionType.fromConnections(up, down, left, right);
    }

    default boolean isNeighborConnected(Level level, BlockPos myPos, BlockState myState, Direction toDir) {
        BlockState neighbor = level.getBlockState(myPos.relative(toDir));
        if (!connectionMatches(myState, neighbor)) return false;
        Direction myFacing = myState.getValue(HorizontalDirectionalBlock.FACING);
        return neighbor.getValue(connectionProperty()).isConnected(toDir.getOpposite(), myFacing);
    }

    default boolean shouldHaveBlockEntity(BlockState state) {
        ConnectionType conn = state.getValue(connectionProperty());
        return conn.hasEdge(Direction2D.DOWN) && conn.hasEdge(Direction2D.LEFT);
    }

    default void enlargeConnection(BlockState state, Level level, BlockPos pos) {
        int maxSize = maxConnectedSize();
        if (maxSize <= 1) return;
        boolean squareOnly = squareAspectRatio();
        AbstractGridAccess gridAccess = createGridAccess(level, pos, state);
        Rect2D old = RectFinder.findMaxRect(gridAccess, Vec2i.ZERO, squareOnly);
        RectSelection newRec = RectFinder.findMaxExpandedRect(gridAccess, Vec2i.ZERO, maxSize, squareOnly);
        gridAccess.transform(old, newRec.selection(), newRec.touchedRect());
        gridAccess.applyChanges();
    }

    default void shrinkConnection(BlockState state, Level level, BlockPos pos) {
        if (state.getValue(connectionProperty()) == ConnectionType.SINGLE) return;
        int maxSize = maxConnectedSize();
        if (maxSize <= 1) return;
        boolean squareOnly = squareAspectRatio();

        AbstractGridAccess gridAccess = createGridAccess(level, pos, state);
        gridAccess.setAt(Vec2i.ZERO, state.getValue(connectionProperty()));
        Rect2D old = RectFinder.findMaxRect(gridAccess, Vec2i.ZERO, squareOnly);

        gridAccess.setAt(Vec2i.ZERO, null);
        Direction2D closestDir = closestDirToCenter(old);
        Vec2i newCenter = Vec2i.ZERO.offset(closestDir);
        Rect2D newRec = RectFinder.findMaxRect(gridAccess, newCenter, squareOnly);
        gridAccess.transform(old, newRec, old);
        gridAccess.applyChanges();
    }

    @Nullable
    default BlockEntity findMasterBlockEntity(LevelAccessor level, BlockPos pos, BlockState state) {
        ConnectionType type = state.getValue(connectionProperty());
        Direction facing = state.getValue(HorizontalDirectionalBlock.FACING);
        BlockPos current = pos;

        BlockEntity be = level.getBlockEntity(current);
        if (be != null) return be;

        while (type.isConnected(Direction.DOWN, facing)) {
            current = current.below();
            BlockState below = level.getBlockState(current);
            if (!connectionMatches(state, below)) return null;
            type = below.getValue(connectionProperty());
        }
        Direction myLeft = facing.getClockWise();
        while (type.isConnected(myLeft, facing)) {
            current = current.relative(myLeft);
            BlockState side = level.getBlockState(current);
            if (!connectionMatches(state, side)) return null;
            type = side.getValue(connectionProperty());
        }
        return level.getBlockEntity(current);
    }

    static Direction2D closestDirToCenter(Rect2D rect) {
        Vec2 center = rect.getCenter();
        Vec2 myPos = new Vec2(0.5f, 0.5f);
        Vec2 diff = center.add(myPos.scale(-1));
        return Direction2D.closest(diff);
    }
}
