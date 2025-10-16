package net.mehvahdjukaar.camera_vision.common;

import net.mehvahdjukaar.camera_vision.CameraVision;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TVBlockEntity extends BlockEntity {
    public TVBlockEntity(BlockPos pos, BlockState state) {
        super(CameraVision.TV_TILE.get(), pos, state);
    }
}
