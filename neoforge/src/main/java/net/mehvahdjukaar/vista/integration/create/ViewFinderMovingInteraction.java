package net.mehvahdjukaar.vista.integration.create;

import com.simibubi.create.api.behaviour.interaction.MovingInteractionBehaviour;
import com.simibubi.create.content.contraptions.AbstractContraptionEntity;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Routes right-clicks on a view finder that is part of a moving contraption into camera control.
 * Create fires this on both sides (client locally, then again on the server via ContraptionInteractionPacket).
 */
public class ViewFinderMovingInteraction extends MovingInteractionBehaviour {

    @Override
    public boolean handlePlayerInteraction(Player player, InteractionHand activeHand, BlockPos localPos,
                                           AbstractContraptionEntity contraptionEntity) {
        if (contraptionEntity.level().isClientSide) {
            // the contraption keeps a live client-side block entity in its virtual render level
            BlockEntity be = contraptionEntity.getContraption().getBlockEntityClientSide(localPos);
            if (be instanceof ViewFinderBlockEntity vf) {
                // client-only logic kept in a separate class so the server never links it
                CreateClientCompat.startControlling(vf, contraptionEntity, localPos);
                return true;
            }
            return false;
        }
        // server side: no authoritative live BE exists here; accept the interaction so it isn't
        // treated as a miss. Aim changes arrive afterwards via the sync packet.
        return true;
    }
}
