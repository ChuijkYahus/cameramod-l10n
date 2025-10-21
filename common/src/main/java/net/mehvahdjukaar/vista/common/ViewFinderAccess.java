package net.mehvahdjukaar.vista.common;


import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
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

    Vec3 getCannonGlobalPosition(float partialTicks);

    float getCannonGlobalYawOffset(float partialTicks);

    Vec3 getCannonGlobalOffset();

    boolean stillValid(Player player);

    void updateClients();

    default Vec3 getCannonGlobalFacing(float partialTicks) {
        ViewFinderBlockEntity cannon = this.getInternalTile();
        return Vec3.directionFromRotation(cannon.getPitch(partialTicks),
                cannon.getYaw(partialTicks) - this.getCannonGlobalYawOffset(partialTicks)).scale(-1);
    }

    Restraint getPitchAndYawRestrains();

    default boolean shouldRotatePlayerFaceWhenManeuvering() {
        return false;
    }

    default boolean impedePlayerMovementWhenManeuvering() {
        return true;
    }

    class Block implements ViewFinderAccess {
        private final ViewFinderBlockEntity blockEntity;

        public Block(ViewFinderBlockEntity tile) {
            this.blockEntity = tile;
        }

        @Override
        public TileOrEntityTarget makeNetworkTarget() {
            return TileOrEntityTarget.of(this.blockEntity);
        }

        @Override
        public Vec3 getCannonGlobalPosition(float ticks) {
            return blockEntity.getBlockPos().getCenter();
        }

        @Override
        public Vec3 getCannonGlobalOffset() {
            return new Vec3(0.5, 0.5, 0.5);
        }

        @Override
        public float getCannonGlobalYawOffset(float partialTicks) {
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
            return new Block(cannon);
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
