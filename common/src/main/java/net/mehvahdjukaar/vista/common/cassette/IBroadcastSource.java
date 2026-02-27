package net.mehvahdjukaar.vista.common.cassette;

import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.common.broadcast.BroadcastManager;
import net.mehvahdjukaar.vista.common.broadcast.IBroadcastLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IBroadcastSource {

    default void ensureLinked(Level level, IBroadcastLocation location) {
        if (level instanceof ServerLevel sl) {
            BroadcastManager.getInstance(sl)
                    .linkFeed(this.getUUID(), location);
        }
    }

    default void removeLink(Level level) {
        if (level instanceof ServerLevel sl) {
            BroadcastManager.getInstance(sl)
                    .unlinkFeed(this.getUUID());
        }
    }

    UUID getUUID();

    @Nullable IVideoSource getBroadcastVideo();
}
