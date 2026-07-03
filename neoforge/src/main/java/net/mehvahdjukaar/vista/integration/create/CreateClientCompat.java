package net.mehvahdjukaar.vista.integration.create;

import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.mehvahdjukaar.vista.client.ViewFinderController;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.core.BlockPos;

/**
 * Client-only entry points for the Create integration. Isolated from {@link ViewFinderMovingInteraction}
 * (which runs on both sides) so that referencing client classes never loads them on a dedicated server.
 */
public class CreateClientCompat {

    public static void startControlling(ViewFinderBlockEntity vf, AbstractContraptionEntity contraption, BlockPos localPos) {
        // rebind the view finder to the moving structure so position/rotation/velocity track the contraption
        vf.setReferenceFrame(new ContraptionReferenceFrame(contraption, localPos));
        ViewFinderController.startControlling(vf);
    }
}
