package net.mehvahdjukaar.camera_vision.common;

import net.mehvahdjukaar.camera_vision.CameraVision;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class TVBlockEntity extends BlockEntity {

    private static final ResourceLocation CURRENT_CAMERA_ID = CameraVision.res("test");

    public TVBlockEntity(BlockPos pos, BlockState state) {
        super(CameraVision.TV_TILE.get(), pos, state);
    }

    public ResourceLocation getLinkedCamera() {
        return CURRENT_CAMERA_ID;
    }


}
