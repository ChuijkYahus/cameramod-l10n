package net.mehvahdjukaar.vista.common.projector;

import net.mehvahdjukaar.moonlight.api.client.IScreenProvider;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.client.ui.SignalProjectorScreen;
import net.mehvahdjukaar.vista.common.cassette.IBroadcastProvider;
import net.mehvahdjukaar.vista.integration.CompatHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class SignalProjectorBlockEntity extends BlockEntity implements IScreenProvider, IBroadcastProvider {

    public Object ccPeripheral;

    private String url = "";
    private UUID myUUID;

    public SignalProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.SIGNAL_PROJECTOR_TILE.get(), pos, state);
        this.myUUID = UUID.randomUUID();
    }


    @Override
    public void openScreen(Level level, Player player, Direction direction, Vec3 pos) {
        SignalProjectorScreen.open(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public @Nullable Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        this.ensureLinked();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.myUUID = tag.getUUID("UUID");
        this.url = tag.getString("url");
        this.ensureLinked();

        
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("UUID", myUUID);
        tag.putString("url", url);
    }

    @Override
    public UUID getUUID() {
        return  myUUID;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public boolean canBeEditedBy(Player player) {
        return CompatHandler.COMPUTER_CRAFT || player.canUseGameMasterBlocks();
    }
}
