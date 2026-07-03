package net.mehvahdjukaar.vista.integration.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import net.mehvahdjukaar.vista.VistaMod;

/**
 * Create integration: lets view finders be controlled while mounted on trains and contraptions.
 *
 * <p>NOT yet handled (aim sync / persistence): when a player aims a view finder on a contraption the
 * change is applied to the client-side contraption block entity only. Syncing it to the server and other
 * players (and persisting it) needs a custom packet carrying the contraption entity + local BlockPos,
 * because a contraption stores blocks as StructureBlockInfo NBT with no live server-side block entity, and
 * Create's built-in block-change packet only carries block state, not NBT. See {@code SyncViewFinderPacket}.
 */
public class CreateCompat {

    public static void setup() {
        // makes right-clicking the view finder on a moving contraption enter camera control
        MovingInteractionBehaviour.REGISTRY.register(VistaMod.VIEWFINDER.get(), new ViewFinderMovingInteraction());
    }
}
