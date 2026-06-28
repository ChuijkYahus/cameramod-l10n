package net.mehvahdjukaar.vista.common.tv;


import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.moonlight.api.block.IOptionalEntityBlock;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.moonlight.api.util.math.Rect2D;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.connection.*;
import net.mehvahdjukaar.vista.configs.CommonConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Equipable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class TVBlock extends HorizontalDirectionalBlock implements EntityBlock, Equipable, IOptionalEntityBlock, WorldlyContainerHolder, IConnectedBlock {

    public static final MapCodec<TVBlock> CODEC = simpleCodec(TVBlock::new);
    public static final EnumProperty<PowerState> POWER_STATE = EnumProperty.create("powered", PowerState.class);
    public static final EnumProperty<ConnectionType> CONNECTION = ConnectionType.STATE_PROPERTY;

    public TVBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(POWER_STATE).isOn() ? 3 : 0));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWER_STATE, PowerState.OFF)
                .setValue(CONNECTION, ConnectionType.SINGLE)
                .setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(POWER_STATE);
        builder.add(CONNECTION);
    }

    @Override
    public EnumProperty<ConnectionType> connectionProperty() {
        return CONNECTION;
    }

    @Override
    public int maxConnectedSize() {
        return CommonConfigs.TV_MAX_CONNECTED_TV_SIZE.get();
    }

    @Override
    public boolean squareAspectRatio() {
        return CommonConfigs.TV_SQUARE_ASPECT_RATIO.get();
    }

    @Override
    public AbstractGridAccess createGridAccess(Level level, BlockPos pos, BlockState state) {
        return new TVGridAccess(level, pos, state, this);
    }

    @Override
    public WorldlyContainer getContainer(BlockState state, LevelAccessor level, BlockPos pos) {
        BlockEntity master = findMasterBlockEntity(level, pos, state);
        if (master instanceof WorldlyContainer wc) return wc;
        return EmptyWorldlyContainer.INSTANCE;
    }

    @Override
    public EquipmentSlot getEquipmentSlot() {
        return EquipmentSlot.HEAD;
    }

    @Override
    public @Nullable <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return Utils.getTicker(blockEntityType, VistaMod.TV_TILE.get(), TVBlockEntity::onTick);
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = this.defaultBlockState()
                .setValue(POWER_STATE, PowerState.direct(powered))
                .setValue(FACING, facing);
        ConnectionType type = getTypeFromNeighbors(context.getLevel(), context.getClickedPos(), state);
        return state.setValue(CONNECTION, type);
    }

    @Override
    protected boolean triggerEvent(BlockState state, Level level, BlockPos pos, int id, int param) {
        if (id == 1) {
            if (level.isClientSide) {
                if (level.getBlockEntity(pos) instanceof TVBlockEntity tile) {
                    tile.updateEndermanLookAnimation(param);
                    return true;
                }
            } else return true;
        }
        return super.triggerEvent(state, level, pos, id, param);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity var5 = level.getBlockEntity(pos);
        if (var5 instanceof TVBlockEntity tv) {
            return tv.getComparatorOutput();
        } else {
            return 0;
        }
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hitResult) {
        BlockEntity master = findMasterBlockEntity(level, pos, state);
        if (master instanceof TVBlockEntity masterTile) {
            return masterTile.interactWithPlayerItem(player, hand, stack, 0, hitResult);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public boolean shouldHaveBlockEntity(BlockStateBase state) {
        return IConnectedBlock.super.shouldHaveBlockEntity((BlockState) state);
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return shouldHaveBlockEntity(state) ? new TVBlockEntity(pos, state) : null;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (neighborBlock == this) return;
        boolean powered = level.hasNeighborSignal(pos);
        PowerState oldPower = state.getValue(POWER_STATE);
        PowerState newPower = PowerState.direct(powered);
        if (newPower != oldPower)
            level.setBlock(pos, state.setValue(POWER_STATE, newPower),
                    Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_CLIENTS | Block.UPDATE_NONE);
        if (oldPower.isOn() != newPower.isOn()) {
            TVGridAccess gridAccess = new TVGridAccess(level, pos, state, this);
            Rect2D old = RectFinder.findMaxRect(gridAccess, Vec2i.ZERO, false);
            gridAccess.transform(old, old, null);
            gridAccess.applyChanges();
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock())) shrinkConnection(state, level, pos);
        Containers.dropContentsOnDestroy(state, newState, level, pos);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!(placer instanceof Player p) || !p.isSecondaryUseActive()) enlargeConnection(state, level, pos);
    }

    //TODO: make blockstate?
    private static boolean hasCassette(BlockPos pos, Level level) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof TVBlockEntity tv) {
            return !tv.isEmpty();
        }
        return false;
    }

    public static class TVGridAccess extends AbstractGridAccess {

        private ItemStack cassetteTransfer = null;

        public TVGridAccess(Level level, BlockPos pos, BlockState state, Block owner) {
            super(level, pos, state, owner);
        }

        @Override
        protected EnumProperty<ConnectionType> connectionProperty() {
            return CONNECTION;
        }

        @Override
        protected GridTile readTile(BlockPos target, BlockState bs) {
            ConnectionType t = bs.getValue(CONNECTION);
            PowerState hasPower = bs.getValue(POWER_STATE);
            boolean hasBe = hasCassette(target, level);
            return new GridTile(t, hasBe, hasPower);
        }

        @Override
        protected GridTile buildTile(Vec2i key, @Nullable ConnectionType type, boolean setPower) {
            GridTile old = getAt(key);
            return GridTile.of(type, PowerState.indirect(old.powerState(), setPower));
        }

        @Override
        protected BlockState writeState(BlockState state, GridTile tile) {
            return state.setValue(POWER_STATE, tile.powerState());
        }

        @Override
        public void planBeMove(@Nullable Rect2D fromRec, Rect2D toRec) {
            super.planBeMove(fromRec, toRec);
            if (fromRec == null) return;
            BlockPos target = targetPos(fromRec.bottomLeft());
            if (level.getBlockEntity(target) instanceof TVBlockEntity tv) {
                cassetteTransfer = tv.getDisplayedItem().copy();
                tv.clearContent();
                tv.setConnectionSize(Vec2i.ONE);
            }
        }

        @Override
        protected void onMasterApplied(BlockPos target, Rect2D rect) {
            if (level.getBlockEntity(target) instanceof TVBlockEntity tv) {
                if (cassetteTransfer != null) tv.setDisplayedItem(cassetteTransfer);
                tv.setChanged();
                tv.setConnectionSize(rect.getSize());
            }
        }
    }
}
