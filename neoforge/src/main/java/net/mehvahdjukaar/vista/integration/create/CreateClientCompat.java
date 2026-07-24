package net.mehvahdjukaar.vista.integration.create;

import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public class CreateClientCompat {

    public static void startControlling(ViewFinderBlockEntity vf, Entity contraption, BlockPos localPos) {
        // rebind the view finder to the moving structure so position/rotation/velocity track the contraption
        vf.setReferenceFrame(new ContraptionReferenceFrame(contraption, localPos));
        ViewFinderController.startControlling(vf);
    }
}
