package net.mehvahdjukaar.vista.common.cassette;

import net.mehvahdjukaar.vista.common.LiveFeedConnectionManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;

import java.util.UUID;

public interface IFeedProvider {

    default void ensureLinked() {
        if (this.getLevel() instanceof ServerLevel sl) {
            LiveFeedConnectionManager.getInstance(sl)
                    .linkFeed(this.getUUID(), new GlobalPos(sl.dimension(), this.getBlockPos()));
        }
    }

    default void removeLink() {
        if (getLevel() instanceof ServerLevel sl) {
            LiveFeedConnectionManager.getInstance(sl)
                    .unlinkFeed(this.getUUID());
        }
    }

    BlockPos getBlockPos();

    UUID getUUID();

    Object getLevel();
}
