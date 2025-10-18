package net.mehvahdjukaar.camera_vision.common;

import net.mehvahdjukaar.camera_vision.CameraVision;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class ViewFinderBlockEntity extends BlockEntity {

    public ViewFinderBlockEntity(BlockPos pos, BlockState state) {
        super(CameraVision.VIEW_FINDER_TILE.get(), pos, state);
    }
}
