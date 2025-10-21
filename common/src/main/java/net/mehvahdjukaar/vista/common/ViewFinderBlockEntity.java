package net.mehvahdjukaar.vista.common;

import net.mehvahdjukaar.moonlight.api.block.IOnePlayerInteractable;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.network.ClientBoundControlViewFinderPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class ViewFinderBlockEntity extends BlockEntity implements IOnePlayerInteractable {

    public Object ccHack = null;

    private final ViewFinderAccess selfAccess;

    private UUID myUUID;

    private float pitch = 0;
    private float prevPitch = 0;
    private float yaw = 0;
    private float prevYaw = 0;

    private int zoom = 1;
    private boolean locked = false;

    //not saved
    @Nullable
    private UUID controllingPlayer = null;

    public ViewFinderBlockEntity(BlockPos pos, BlockState state) {
        super(VistaMod.VIEWFINDER_TILE.get(), pos, state);

        this.myUUID = UUID.randomUUID();

        this.selfAccess = ViewFinderAccess.block(this);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ViewFinderBlockEntity tile) {
        tile.prevYaw = tile.yaw;
        tile.prevPitch = tile.pitch;
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
        this.yaw = tag.getFloat("yaw");
        this.pitch = tag.getFloat("pitch");
        this.locked = tag.getBoolean("locked");
        updateLink();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("UUID", this.myUUID);
        tag.putFloat("yaw", this.yaw);
        tag.putFloat("pitch", this.pitch);
        tag.putBoolean("locked", this.locked);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }


    public boolean tryInteracting(ServerPlayer player, BlockPos pos) {
        //same as super but sends custom packet
        if (!this.isOtherPlayerEditing(pos, player)) {
            // open gui (edit sign with empty hand)
            this.setPlayerWhoMayEdit(player.getUUID());
            NetworkHelper.sendToClientPlayer(player, new ClientBoundControlViewFinderPacket(TileOrEntityTarget.of(this)));
        }
        //always swing on fail
        return true;
    }

    public UUID getUUID() {
        return myUUID;
    }

    public float getYaw(float partialTicks) {
        return Mth.rotLerp(partialTicks, this.prevYaw, this.yaw);
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch(float partialTicks) {
        return Mth.rotLerp(partialTicks, this.prevPitch, this.pitch);
    }

    public float getPitch() {
        return pitch;
    }

    public int getZoomLevel() {
        return zoom;
    }

    public int getMaxZoom(){
        return 44;
    }

    public boolean isLocked() {
        return locked;
    }


    //cannon bs

    public void setAttributes(float yaw, float pitch, int zoom, boolean locked,
                              Player controllingPlayer, ViewFinderAccess access) {
        this.setYaw(access, yaw);
        this.setPitch(access, pitch);
        this.zoom = zoom;
        this.locked = locked;
    }


    public void setZoomLevel(int zoom) {
        this.zoom = zoom;
    }

    public void setPitch(ViewFinderAccess access, float relativePitch) {
        var r = access.getPitchAndYawRestrains();
        this.pitch = MthUtils.clampDegrees(relativePitch, r.minPitch(), r.maxPitch());
    }

    public void setYaw(ViewFinderAccess access, float relativeYaw) {
        var r = access.getPitchAndYawRestrains();
        this.yaw = MthUtils.clampDegrees(relativeYaw, r.minYaw(), r.maxYaw());
    }

    public void setGlobalYaw(ViewFinderAccess access, float relativeYaw) {
        //calculateyaw here
        float yawOffset = access.getCannonGlobalYawOffset(1);
        setYaw(access, relativeYaw);
    }

    // sets both prev and current yaw. Only makes sense to be called from render thread
    public void setRenderYaw(ViewFinderAccess access, float relativeYaw) {
        setYaw(access, relativeYaw);
        this.prevYaw = this.yaw;
    }

    public void setRenderPitch(ViewFinderAccess access, float pitch) {
        setPitch(access, pitch);
        this.prevPitch = this.pitch;
    }


    @Override
    public void setPlayerWhoMayEdit(@Nullable UUID uuid) {
        this.controllingPlayer = uuid;
    }

    @Override
    public UUID getPlayerWhoMayEdit() {
        return controllingPlayer;
    }


    public float getModifiedFOV(float startingFov, float modFov) {
        float spyglassZoom = 0.1f;
        float maxZoom = spyglassZoom / 5;
        float normalizedZoom = getNormalizedZoomFactor();
        return Mth.lerp(normalizedZoom, 1, maxZoom);
    }

    public float getNormalizedZoomFactor() {
        float normalizedZoom = (this.getZoomLevel() - 1f) / (this.getMaxZoom() - 1f);
        normalizedZoom = 1 - ((1 - normalizedZoom) * (1 - normalizedZoom)); //easing
        return normalizedZoom;
    }

}
