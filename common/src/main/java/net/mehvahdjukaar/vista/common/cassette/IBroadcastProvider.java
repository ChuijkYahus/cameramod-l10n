package net.mehvahdjukaar.vista.common.cassette;

import net.mehvahdjukaar.vista.client.video_source.IVideoSource;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IBroadcastProvider {

    default void ensureLinked() {
        if (this.getLevel() instanceof ServerLevel sl) {
            BroadcastManager.getInstance(sl)
                    .linkFeed(this.getUUID(), new GlobalPos(sl.dimension(), this.getBlockPos()));
        }
    }

    default void removeLink() {
        if (getLevel() instanceof ServerLevel sl) {
            BroadcastManager.getInstance(sl)
                    .unlinkFeed(this.getUUID());
        }
    }

    BlockPos getBlockPos();

    UUID getUUID();

    Object getLevel();


    @Nullable IVideoSource getBroadcastVideoSource();
}
