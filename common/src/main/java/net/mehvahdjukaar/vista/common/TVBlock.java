package net.mehvahdjukaar.vista.common;

import com.mojang.serialization.MapCodec;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
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

import java.util.Arrays;
import java.util.Locale;

public class TVBlock extends HorizontalDirectionalBlock implements EntityBlock, Equipable {

    public static final MapCodec<TVBlock> CODEC = simpleCodec(TVBlock::new);
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final EnumProperty<TvConnection> CONNECTION = EnumProperty.create("connection", TvConnection.class);

    public TVBlock(Properties properties) {
        super(properties.lightLevel(state -> state.getValue(POWERED) ? 3 : 0));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(FACING, Direction.NORTH));
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
        if (powered != state.getValue(POWERED)) {
            if (powered) {
                level.setBlockAndUpdate(pos, state.setValue(POWERED, true));
            } else {
                level.setBlockAndUpdate(pos, state.setValue(POWERED, false));
            }
        }
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
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FACING);
        builder.add(POWERED);
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



    public enum TvConnection implements StringRepresentable {
        ISOLATED,
        CENTER,
        EDGE_TOP, EDGE_BOTTOM, EDGE_LEFT, EDGE_RIGHT,
        CORNER_UL, CORNER_UR, CORNER_DL, CORNER_DR;

        private static final int U = 1, D = 2, L = 4, R = 8;

        private static final TvConnection[] LUT = new TvConnection[16];
        static {
            Arrays.fill(LUT, null);

            // valid masks for solid-rectangle tiling
            LUT[0]              = ISOLATED;                  // ----

            LUT[U | R]          = CORNER_DR;                 // U+R   -> 1001b = 9
            LUT[U | L]          = CORNER_DL;                 // U+L   -> 0101b = 5
            LUT[D | R]          = CORNER_UR;                 // D+R   -> 1010b = 10
            LUT[D | L]          = CORNER_UL;                 // D+L   -> 0110b = 6

            LUT[D | L | R]      = EDGE_TOP;                  // -DLR  -> 1110b = 14 (missing U)
            LUT[U | L | R]      = EDGE_BOTTOM;               // ULR-  -> 1101b = 13 (missing D)
            LUT[U | D | R]      = EDGE_LEFT;                 // UDR-  -> 1011b = 11 (missing L)
            LUT[U | D | L]      = EDGE_RIGHT;                // UDL-  -> 0111b = 7  (missing R)

            LUT[U | D | L | R]  = CENTER;                    // UDLR  -> 1111b = 15
        }

        public static TvConnection get(boolean up, boolean down, boolean left, boolean right) {
            int mask = (up ? U : 0) | (down ? D : 0) | (left ? L : 0) | (right ? R : 0);
            TvConnection c = LUT[mask];
            if (c != null) return c;
            throw new IllegalArgumentException("Invalid pattern for square tiling (mask=" + mask + ")");
        }

        @Override
        public String getSerializedName() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

}


