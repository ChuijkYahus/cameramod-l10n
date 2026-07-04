package net.mehvahdjukaar.vista.integration.create;

import net.mehvahdjukaar.candlelight.api.PlatformImpl;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.broadcast.BroadcastManager;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.UUID;

/**
 * Create integration: lets view finders be controlled while mounted on trains and contraptions, and keeps their
 * broadcast feed linked to TVs while riding a moving contraption.
 *
 * <p>Create 6 is multiloader, so all of this logic lives in the common module. It never references Create
 * directly (a contraption is only ever a vanilla {@link Entity} here); the Create API calls and behaviour
 * registration are hard-cast in the platform impl of the {@link PlatformImpl} methods below. Those platform
 * behaviours call back into the callbacks below.
 *
 * <p>Aim sync uses {@link SyncContraptionViewFinderPacket}: a contraption has no live server-side block entity, so
 * the client's aim change is relayed by the server to everyone tracking the contraption (updating their render
 * block entities live) and baked into the contraption's stored StructureBlockInfo NBT via
 * {@link #persistViewFinderAim} (so it survives a render rebuild, a new viewer, and save/load while assembled).
 */
public class CreateCompat {

    public static void setup() {
        registerViewFinderBehaviours(VistaMod.VIEWFINDER.get());
    }

    /**
     * Client-side callback from the contraption interaction behaviour: if the clicked block is a view finder,
     * enter camera control. Returns whether the interaction was handled.
     */
    public static boolean onContraptionInteractClient(@Nullable BlockEntity be, Entity contraption, BlockPos localPos) {
        if (be instanceof ViewFinderBlockEntity vf) {
            CreateClientCompat.startControlling(vf, contraption, localPos);
            return true;
        }
        return false;
    }

    /**
     * Server-side callback from the movement behaviour: (re)register the view finder's feed while it moves.
     */
    public static void linkContraptionFeed(Level world, Entity contraption, BlockPos localPos,
                                           @Nullable CompoundTag blockEntityData) {
        if (world.isClientSide()) return;
        if (blockEntityData == null || !blockEntityData.hasUUID("UUID")) return;
        UUID feedId = blockEntityData.getUUID("UUID");
        // linkFeed is a no-op when unchanged, so calling it every tick is cheap
        BroadcastManager.getInstance(world).linkFeed(feedId,
                new ContraptionBroadcastLocation(world.dimension(), contraption.getUUID(), localPos));
    }

    /**
     * Server-side callback from the movement behaviour: drop the contraption feed link when it stops moving.
     */
    public static void unlinkContraptionFeed(Level world, Entity contraption, BlockPos localPos) {
        if (world.isClientSide()) return;
        // remove by value: if the block already re-registered a world location on disassembly, this is a no-op
        BroadcastManager.getInstance(world).unlinkFeed(
                new ContraptionBroadcastLocation(world.dimension(), contraption.getUUID(), localPos));
    }

    // === platform bridge: everything below is hard-cast to Create's AbstractContraptionEntity in the impl ===

    @PlatformImpl
    public static void registerViewFinderBehaviours(Block viewFinder) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static Vec3 contraptionPosToGlobalPos(Entity contraption, Vec3 localVec, float partialTicks) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static Quaternionf getContraptionRotation(Entity contraption, float partialTicks) {
        throw new AssertionError();
    }

    @PlatformImpl
    public static Vec3 getContactPointMotion(Entity contraption, Vec3 worldPoint) {
        throw new AssertionError();
    }

    @PlatformImpl
    @Nullable
    public static BlockEntity getClientBlockEntity(Entity contraption, BlockPos localPos) {
        throw new AssertionError();
    }

    @PlatformImpl
    @Nullable
    public static Entity findContraption(Level level, UUID contraptionId) {
        throw new AssertionError();
    }

    /**
     * Bake a view finder's aim into the contraption's stored block NBT so it persists past the live render.
     */
    @PlatformImpl
    public static void persistViewFinderAim(Entity contraption, BlockPos localPos, Quaternionf localRot,
                                            int zoom, boolean locked) {
        throw new AssertionError();
    }
}
