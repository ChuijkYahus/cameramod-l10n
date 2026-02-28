package net.mehvahdjukaar.vista.common.view_finder;


import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.supplementaries.common.block.tiles.CannonBlockTile;
import net.mehvahdjukaar.vista.network.ServerBoundSyncViewFinderPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

//used to access a cannon position and rotation, be it in a block or an entity
public interface ViewFinderAccess {

    ViewFinderBlockEntity getInternalTile();

    TileOrEntityTarget makeNetworkTarget();

    void syncToServer(boolean removeOwner);

    Vec3 getGlobalPosition(float partialTicks);

    float getViewFinderGlobalYawOffset(float partialTicks);

    boolean stillValid(Player player);

    void updateClients();

    Restraint getPitchAndYawRestrains();

    default boolean shouldRotatePlayerFaceWhenManeuvering() {
        return false;
    }

    default boolean impedePlayerMovementWhenManeuvering() {
        return true;
    }

    default void setGlobalFacing(Vec3 direction) {
        setGlobalFacing(direction, false);
    }

    //shouldn't these be in cannon tile
    default void setGlobalFacing(Vec3 direction, boolean ignoreIfInvalid) {
        ViewFinderBlockEntity cannon = this.getInternalTile();
        float yaw = (float) (MthUtils.getYaw(direction) + this.getGlobalYawOffset(0));
        float pitch = (float) MthUtils.getPitch(direction);
        float oldYaw = cannon.getYaw(0);
        float oldPitch = cannon.getPitch(0);
        cannon.setYaw(this, yaw);
        cannon.setPitch(this, pitch);
        if (!ignoreIfInvalid) { //very ugly
            float newYaw = cannon.getYaw(0);
            float newPitch = cannon.getPitch(0);
            if (newYaw != yaw || newPitch != pitch) {
                //revert
                cannon.setYaw(this, oldYaw);
                cannon.setPitch(this, oldPitch);
            }
        }
    }

    default Vec3 getGlobalFacing(float partialTicks) {
        var cannon = this.getInternalTile();
        return Vec3.directionFromRotation(cannon.getPitch(partialTicks),
                cannon.getYaw(partialTicks) - this.getGlobalYawOffset(partialTicks));
    }

    float getGlobalYawOffset(float partialTicks);



    class Block implements ViewFinderAccess {
        private final ViewFinderBlockEntity blockEntity;

        public Block(ViewFinderBlockEntity tile) {
            this.blockEntity = tile;
        }

        @Override
        public float getGlobalYawOffset(float partialTicks) {
            return 0;
        }

        @Override
        public TileOrEntityTarget makeNetworkTarget() {
            return TileOrEntityTarget.of(this.blockEntity);
        }

        @Override
        public Vec3 getGlobalPosition(float ticks) {
            return blockEntity.getBlockPos().getCenter();
        }

        @Override
        public float getViewFinderGlobalYawOffset(float partialTicks) {
            return 0;
        }

        @Override
        public ViewFinderBlockEntity getInternalTile() {
            return this.blockEntity;
        }

        @Override
        public void updateClients() {
            var level = blockEntity.getLevel();
            level.sendBlockUpdated(blockEntity.getBlockPos(), blockEntity.getBlockState(), blockEntity.getBlockState(), 3);
        }

        @Override
        public void syncToServer(boolean removeOwner) {
            NetworkHelper.sendToServer(new ServerBoundSyncViewFinderPacket(
                    blockEntity.getYaw(), blockEntity.getPitch(), blockEntity.getZoomLevel(),
                    blockEntity.isLocked(),
                    removeOwner, TileOrEntityTarget.of(this.blockEntity)));
        }

        @Override
        public boolean stillValid(Player player) {
            Level level = player.level();
            float maxDist = 7;
            return !blockEntity.isRemoved() && level.getBlockEntity(blockEntity.getBlockPos()) == blockEntity &&
                    blockEntity.getBlockPos().distToCenterSqr(player.position()) < maxDist * maxDist;
        }

        public Restraint getPitchAndYawRestrains() {
            BlockState state = blockEntity.getBlockState();
            return switch (state.getValue(ViewFinderBlock.FACING).getOpposite()) {
                case NORTH -> new Restraint(-360, 360, -180, 180);
                case SOUTH -> new Restraint(-360, 360, -180, 180);
                case EAST -> new Restraint(-360, 360, -180, 180);
                case WEST -> new Restraint(-360, 360, -180, 180);
                case UP -> new Restraint(-360, 360, -180, 180);
                case DOWN -> new Restraint(-360, 360, -180, 180);
            };
        }

    }

    static ViewFinderAccess find(Level level, TileOrEntityTarget target) {
        var obj = target.getTarget(level);
        if (obj instanceof ViewFinderBlockEntity cannon) {
            return block(cannon);
        } else if (obj instanceof ViewFinderAccess cannon) {
            return cannon;
        }
        return null;
    }


    static ViewFinderAccess block(ViewFinderBlockEntity cannonBlockTile) {
        return new Block(cannonBlockTile);
    }

    record Restraint(float minYaw, float maxYaw, float minPitch, float maxPitch) {
    }


}
