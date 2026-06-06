package net.mehvahdjukaar.vista.common.mirror;

import net.mehvahdjukaar.vista.VistaMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

public class MirrorBlockEntity extends BlockEntity {

    private UUID id = UUID.randomUUID();

    public MirrorBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.MIRROR_TILE.get(), pos, state);
    }

    public UUID getId() {
        return id;
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.hasUUID("id")) {
            this.id = tag.getUUID("id");
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("id", id);
    }
}
