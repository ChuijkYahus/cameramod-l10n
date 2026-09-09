package net.mehvahdjukaar.vista.common.broadcast;

import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ryanhcode.sable.companion.SableCompanion;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.vista.VistaModClient;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Position;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Optional;

public final class LevelBEBroadcastLocation implements IBroadcastLocation {

    public static final BroadcastLocationType TYPE = new BroadcastLocationType(
            RecordCodecBuilder.<LevelBEBroadcastLocation>mapCodec(i -> i.group(
                    GlobalPos.MAP_CODEC.forGetter(l -> l.globalPos),
                    BlockPos.CODEC.optionalFieldOf("sub_level_anchor")
                            .forGetter(l -> Optional.ofNullable(l.subLevelAnchor))
            ).apply(i, (pos, anchor) -> new LevelBEBroadcastLocation(pos, anchor.orElse(null)))),
            GlobalPos.STREAM_CODEC.map(LevelBEBroadcastLocation::new, LevelBEBroadcastLocation::globalPos)
    );

    private final GlobalPos globalPos;
    @Nullable
    private BlockPos subLevelAnchor;

    public LevelBEBroadcastLocation(GlobalPos globalPos) {
        this(globalPos, null);
    }

    private LevelBEBroadcastLocation(GlobalPos globalPos, @Nullable BlockPos subLevelAnchor) {
        this.globalPos = globalPos;
        this.subLevelAnchor = subLevelAnchor;
    }

    public static LevelBEBroadcastLocation of(Level level, BlockPos pos) {
        BlockPos anchor = null;
        if (!level.isClientSide() && SableCompanion.INSTANCE.isInPlotGrid(level, pos)) {
            anchor = projectToWorldAnchor(level, pos);
        }
        return new LevelBEBroadcastLocation(GlobalPos.of(level.dimension(), pos), anchor);
    }

    public static LevelBEBroadcastLocation of(BlockEntity be) {
        return of(be.getLevel(), be.getBlockPos());
    }

    public GlobalPos globalPos() {
        return globalPos;
    }

    @Override
    public TriResult<IBroadcastSource> get(boolean isClient) {
        MinecraftServer server = PlatHelper.getCurrentServer();
        Level otherLevel = isClient
                ? VistaModClient.getLocalLevelByDimension(globalPos.dimension())
                : (server == null ? null : server.getLevel(globalPos.dimension()));

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

    @Override
    public @Nullable GlobalPos getChunkSendPosition() {
        MinecraftServer server = PlatHelper.getCurrentServer();
        Level level = server == null ? null : server.getLevel(globalPos.dimension());
        if (level == null || !SableCompanion.INSTANCE.isInPlotGrid(level, globalPos.pos())) {
            return globalPos; // normal in-world block entity: fast path
        }
        BlockPos anchor = projectToWorldAnchor(level, globalPos.pos());
        if (anchor != null) {
            if (!anchor.equals(this.subLevelAnchor)) {
                this.subLevelAnchor = anchor;
                BroadcastManager.getInstance(level).setDirty();
            }
            return GlobalPos.of(globalPos.dimension(), anchor);
        }
        return subLevelAnchor == null ? null : GlobalPos.of(globalPos.dimension(), subLevelAnchor);
    }

    @Nullable
    private static BlockPos projectToWorldAnchor(Level level, BlockPos plotPos) {
        Vec3 world = SableCompanion.INSTANCE.projectOutOfSubLevel(level, (Position) Vec3.atCenterOf(plotPos));
        if (SableCompanion.INSTANCE.isInPlotGrid(level, (Position) world)) return null;
        return new ChunkPos(BlockPos.containing(world)).getWorldPosition();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LevelBEBroadcastLocation other && globalPos.equals(other.globalPos);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(globalPos);
    }

    @Override
    public String toString() {
        return "LevelBEBroadcastLocation[" + globalPos + (subLevelAnchor != null ? ", anchor=" + subLevelAnchor : "") + "]";
    }
}
