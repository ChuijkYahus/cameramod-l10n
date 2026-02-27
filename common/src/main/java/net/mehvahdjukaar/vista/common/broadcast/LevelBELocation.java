package net.mehvahdjukaar.vista.common.broadcast;

import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public record LevelBELocation(GlobalPos globalPos) implements IBroadcastLocation {

    public static final BroadcastLocationType TYPE = new BroadcastLocationType(
            GlobalPos.MAP_CODEC.xmap(LevelBELocation::new, LevelBELocation::globalPos),
            GlobalPos.STREAM_CODEC.map(LevelBELocation::new, LevelBELocation::globalPos)
    );

    public static LevelBELocation of(Level level, BlockPos pos) {
        return new LevelBELocation(GlobalPos.of(level.dimension(), pos));
    }

    public static LevelBELocation of(BlockEntity be){
        return of(be.getLevel(), be.getBlockPos());
    }

    @Override
    public TriResult<IBroadcastSource> get(boolean isClient) {
        Level otherLevel = isClient
                ? VistaModClient.getLocalLevelByDimension(globalPos.dimension())
                : PlatHelper.getCurrentServer().getLevel(globalPos.dimension());

        if (otherLevel != null && otherLevel.isLoaded(globalPos.pos())) {
            if (otherLevel.getBlockEntity(globalPos.pos()) instanceof IBroadcastSource provider) {
                return TriResult.valid(provider);
            } else if (!isClient) {
                return TriResult.invalid();
            }
        }
        return TriResult.empty();
    }

    @Override
    public BroadcastLocationType type() {
        return TYPE;
    }

    @Override
    public MutableComponent getTooltipComponent(Level level) {
        if (level.dimension() == globalPos.dimension()) {
            BlockPos pos = globalPos.pos();
            return Component.translatable("tooltip.vista.hollow_cassette.linked",
                    pos.getX(), pos.getY(), pos.getZ());
        } else {
            return Component.translatable("tooltip.vista.hollow_cassette.linked_away", globalPos.dimension());
        }
    }

}
