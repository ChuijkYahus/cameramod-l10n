package net.mehvahdjukaar.vista.common.mirror;

import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.moonlight.api.block.IOptionalEntityBlock;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.moonlight.api.util.math.Rect2D;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.tv.PowerState;
import net.mehvahdjukaar.vista.common.tv.connection.AbstractGridAccess;
import net.mehvahdjukaar.vista.common.tv.connection.ConnectionType;
import net.mehvahdjukaar.vista.common.tv.connection.GridTile;
import net.mehvahdjukaar.vista.common.tv.connection.IConnectedBlock;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class MirrorBlock extends HorizontalDirectionalBlock implements EntityBlock, IOptionalEntityBlock, IConnectedBlock {

    public static final MapCodec<MirrorBlock> CODEC = simpleCodec(MirrorBlock::new);
    public static final EnumProperty<ConnectionType> CONNECTION = ConnectionType.STATE_PROPERTY;

    public MirrorBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(CONNECTION, ConnectionType.SINGLE));
    }

    @Override
    protected MapCodec<MirrorBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, CONNECTION);
    }

    @Override
    public EnumProperty<ConnectionType> connectionProperty() {
        return CONNECTION;
    }

    @Override
    public int maxConnectedSize() {
        return CommonConfigs.MAX_CONNECTED_MIRROR_SIZE.get();
    }

    @Override
    public boolean squareAspectRatio() {
        return CommonConfigs.MIRROR_SQUARE_ASPECT_RATIO.get();
    }

    @Override
    public AbstractGridAccess createGridAccess(Level level, BlockPos pos, BlockState state) {
        return new MirrorGridAccess(level, pos, state, this);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        ConnectionType type = getTypeFromNeighbors(context.getLevel(), context.getClickedPos(), facing);
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(CONNECTION, type);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    public boolean shouldHaveBlockEntity(BlockStateBase state) {
        return IConnectedBlock.super.shouldHaveBlockEntity((BlockState) state);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return shouldHaveBlockEntity(state) ? new MirrorBlockEntity(pos, state) : null;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return Utils.getTicker(blockEntityType, VistaMod.MIRROR_TILE.get(), MirrorBlockEntity::onTick);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(placer instanceof Player p) || !p.isSecondaryUseActive()) enlargeConnection(state, level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) shrinkConnection(state, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Nullable
    public static MirrorBlockEntity getMasterBlockEntity(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof MirrorBlock mirror)) return null;
        BlockEntity be = mirror.findMasterBlockEntity(level, pos, state);
        return be instanceof MirrorBlockEntity m ? m : null;
    }

    public static class MirrorGridAccess extends AbstractGridAccess {

        public MirrorGridAccess(Level level, BlockPos pos, BlockState state, Block owner) {
            super(level, pos, state, owner);
        }

        @Override
        protected EnumProperty<ConnectionType> connectionProperty() {
            return CONNECTION;
        }

        @Override
        protected GridTile readTile(BlockPos target, BlockState bs) {
            ConnectionType t = bs.getValue(CONNECTION);
            boolean hasBe = level.getBlockEntity(target) instanceof MirrorBlockEntity;
            return new GridTile(t, hasBe, PowerState.OFF);
        }

        @Override
        protected GridTile buildTile(Vec2i key, @Nullable ConnectionType type, boolean setPower) {
            return new GridTile(type, false, PowerState.OFF);
        }

        @Override
        protected void onMasterApplied(BlockPos target, Rect2D rect) {
            if (level.getBlockEntity(target) instanceof MirrorBlockEntity mirror) {
                mirror.setConnectionSize(rect.getSize());
                mirror.setChanged();
            }
        }
    }
}
