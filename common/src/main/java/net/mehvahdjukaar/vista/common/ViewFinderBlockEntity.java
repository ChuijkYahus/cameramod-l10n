package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class ViewFinderBlockEntity extends BlockEntity {

    private UUID myUUID;

    public ViewFinderBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.VIEWFINDER_TILE.get(), pos, state);

        this.myUUID = UUID.randomUUID();
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        updateLink();
    }

    private void updateLink() {
        if (level instanceof ServerLevel sl) {
            ViewFinderConnection.get(sl)
                    .linkFeed(this.myUUID, new GlobalPos(level.dimension(), this.worldPosition));
        }
    }

    private void removeLink() {
        if (level instanceof ServerLevel sl) {
            ViewFinderConnection.get(sl)
                    .unlinkFeed(this.myUUID);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.myUUID = tag.getUUID("UUID");
        updateLink();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("UUID", this.myUUID);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }

    public float getPitch(float partialTick) {
        return 20;
    }

    public float getYaw(float partialTick) {
        return 0;
    }

    public UUID getUUID() {
        return myUUID;
    }
}
