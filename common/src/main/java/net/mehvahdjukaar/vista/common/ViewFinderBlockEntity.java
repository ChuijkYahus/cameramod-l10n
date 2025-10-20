package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ViewFinderBlockEntity extends BlockEntity {

    public ViewFinderBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.VIEWFINDER_TILE.get(), pos, state);
    }

    public float getPitch(float partialTick) {
        return 0;
    }

    public float getYaw(float partialTick) {
        return 0;
    }
}
