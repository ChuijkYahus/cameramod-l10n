package net.mehvahdjukaar.camera_vision.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class TVBlock extends Block implements EntityBlock {

    public TVBlock(Properties properties) {
        super(properties);
    }


    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TVBlockEntity(pos, state);
    }
}
