package net.mehvahdjukaar.vista.integration.computer_craft;

import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

public class CassetteBurnerBlockEntity extends ItemDisplayTile {

    public final CassetteBurnerPeripheral peripheral;

    protected CassetteBurnerBlockEntity( BlockPos pos, BlockState state) {
        super(CCCompat.CASSETTE_BURNER_TILE.get(), pos, state , 1);
        this.peripheral = new CassetteBurnerPeripheral(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return new CompoundTag();
    }

    @Override
    public boolean needsToUpdateClientWhenChanged() {
        return false;
    }

    public String getUrl() {
        return "null";
    }

    public boolean setUrl(String url) {

        return false;
    }
}
