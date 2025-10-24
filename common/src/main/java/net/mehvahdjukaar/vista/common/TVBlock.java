package net.mehvahdjukaar.vista.common;

import com.google.common.base.Preconditions;
import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class TVBlock extends HorizontalDirectionalBlock implements EntityBlock, Equipable {

    public static final MapCodec<TVBlock> CODEC = simpleCodec(TVBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<TvConnection> CONNECTION = EnumProperty.create("connection", TvConnection.class);

    public TVBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(POWERED) ? 3 : 0));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(CONNECTION, TvConnection.NONE)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(POWERED);
        builder.add(CONNECTION);
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return !level.isClientSide ? null : Utils.getTicker(blockEntityType, VistaMod.TV_TILE.get(), TVBlockEntity::clientTick);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        boolean powered = level.hasNeighborSignal(pos);

        if (neighborBlock == this) {
            BlockState neighborState = level.getBlockState(neighborPos);
            if (facingSameDir(neighborState, state)) {
                if (state.getValue(CONNECTION).isConnected(neighborState.getValue(CONNECTION))) {
                    powered |= neighborState.getValue(POWERED);
                }
            }
        }

        if (powered != state.getValue(POWERED)) {
            if (powered) {
                level.setBlockAndUpdate(pos, state.setValue(POWERED, true));
            } else {
                level.setBlockAndUpdate(pos, state.setValue(POWERED, false));
            }
        }
        //this.updateAllConnections(state, level, pos);
    }

    private static boolean facingSameDir(BlockState neighborState, BlockState state) {
        return neighborState.getValue(FACING) == state.getValue(FACING);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) this.updateAllConnections(state, level, pos);
    }

    @Nullable
    private TVBlockEntity getMasterBlockEntity(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TVBlockEntity tv) {
            return tv;
        }
        return null;
    }


    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(POWERED, powered)
                .setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TVBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {

        if (level.getBlockEntity(pos) instanceof TVBlockEntity tile) {
            return tile.interactWithPlayerItem(player, hand, stack);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }


    private void updateAllConnections(BlockState state, Level level, BlockPos pos) {
        if(!CommonConfigs.CONNECTED_TVS.get())return;
        TVGridAccess gridAccess = new TVGridAccess(level, pos, state);
        TvHelper.updateConnections(gridAccess);
        gridAccess.applyChanges();
    }

    private record Vec2i(int x, int y) {
    }

    public static class TVGridAccess implements TvHelper.GridAccess {

        private final BlockPos pos;
        private final Direction facing;
        private final Level level;

        private final Map<Vec2i, TvConnection> statesCache = new HashMap<>();
        private final Map<Vec2i, TvConnection> statesChanged = new HashMap<>();

        public TVGridAccess(Level level, BlockPos pos, BlockState state) {
            this.pos = pos;
            this.facing = state.getValue(FACING);
            this.level = level;
        }

        @Nullable
        @Override
        public TvConnection get(int left, int top) {
            Vec2i key = new Vec2i(left, top);
            if (statesChanged.containsKey(key)) {
                return statesChanged.get(key);
            }
            BlockPos target = relativePos(pos, facing, left, top, 0);
            BlockState bs = level.getBlockState(target);
            TvConnection value = null;
            if (bs.getBlock() instanceof TVBlock && bs.getValue(FACING) == facing) {
                value = bs.getValue(CONNECTION);
            }
            statesCache.put(key, value);
            return value;
        }

        @Override
        public void set(int left, int top, @Nullable TvConnection state) {
            Vec2i key = new Vec2i(left, top);
            TvConnection old = statesCache.get(key);
            if (old != state) {
                statesChanged.put(key, state);
            }
        }

        public void applyChanges() {
            for (var e : statesChanged.entrySet()) {
                Vec2i key = e.getKey();
                TvConnection conn = e.getValue();
                BlockPos target = relativePos(pos, facing, key.x, key.y, 0);
                BlockState bs = level.getBlockState(target);
                if (bs.getBlock() instanceof TVBlock && conn != null) {
                    level.setBlockAndUpdate(target, bs.setValue(CONNECTION, conn));
                }
            }
        }

    }

    @Deprecated(forRemoval = true)
    public static BlockPos relativePos(BlockPos pos, Direction normal, int left, int top, int forward) {
        Preconditions.checkArgument(normal.getAxis() != Direction.Axis.Y, "Normal direction cannot be vertical");
        if (forward != 0) {
            pos = pos.relative(normal, forward);
        }
        if (left != 0) {
            Direction leftDir = normal.getCounterClockWise();
            pos = pos.relative(leftDir, left);
        }
        if (top != 0) {
            pos = pos.above(top);
        }
        return pos;
    }

}


