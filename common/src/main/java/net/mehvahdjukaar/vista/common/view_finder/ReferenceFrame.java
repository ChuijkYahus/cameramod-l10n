package net.mehvahdjukaar.vista.common.view_finder;

import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

public interface ReferenceFrame {
    Vec3 position(float partialTicks);

    Quaternionf getRotation(float partialTicks);

    Vec3 velocity();

    TileOrEntityTarget makeNetworkTarget();

    boolean isStillValid(Player player);

}

