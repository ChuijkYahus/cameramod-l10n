package net.mehvahdjukaar.vista.common.mirror;

import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class MirrorBlockEntity extends BlockEntity {

    private UUID id = UUID.randomUUID();
    private Vec2i connectedMirrorsAmount = Vec2i.ONE;

    public MirrorBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.MIRROR_TILE.get(), pos, state);
    }

    public UUID getId() {
        return id;
    }

    public Vec2i getConnectedCount() {
        return connectedMirrorsAmount;
    }

    public void setConnectionSize(Vec2i size) {
        this.connectedMirrorsAmount = size;
    }

    public Vec2i getScreenPixelSize() {
        return new Vec2i(connectedMirrorsAmount.x() * 16, connectedMirrorsAmount.y() * 16);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("id")) {
            this.id = tag.getUUID("id");
        }
        this.connectedMirrorsAmount = new Vec2i(
                Math.max(1, tag.getInt("ConnectionWidth")),
                Math.max(1, tag.getInt("ConnectionHeight")));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("id", id);
        tag.putInt("ConnectionWidth", connectedMirrorsAmount.x());
        tag.putInt("ConnectionHeight", connectedMirrorsAmount.y());
    }
}
