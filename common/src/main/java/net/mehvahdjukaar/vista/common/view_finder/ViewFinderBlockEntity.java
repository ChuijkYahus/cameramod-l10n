package net.mehvahdjukaar.vista.common.view_finder;

import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.moonlight.api.block.IOneUserInteractable;
import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.moonlight.api.util.FakePlayerManager;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.vista.VistaMod;
import net.mehvahdjukaar.vista.common.EndermanFreezeWhenLookedAtThroughTVGoal;
import net.mehvahdjukaar.vista.common.LiveFeedConnectionManager;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.common.tv.TVSpectatorView;
import net.mehvahdjukaar.vista.network.ClientBoundControlViewFinderPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ViewFinderBlockEntity extends ItemDisplayTile implements IOneUserInteractable {

    public static final int MAX_ZOOM = 44;

    public static final float NEAR_PLANE = 0.05f;
    public static final float BASE_FOV = 70;

    public Object ccHack = null;

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
            LiveFeedConnectionManager.getInstance(sl)
                    .linkFeed(this.myUUID, new GlobalPos(level.dimension(), this.worldPosition));
        }
    }

    private void removeLink() {
        if (level instanceof ServerLevel sl) {
            LiveFeedConnectionManager.getInstance(sl)
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
        this.zoom = tag.getInt("zoom");
        updateLink();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putUUID("UUID", this.myUUID);
        tag.putFloat("yaw", this.yaw);
        tag.putFloat("pitch", this.pitch);
        tag.putBoolean("locked", this.locked);
        tag.putInt("zoom", this.zoom);
    }

    @Override
    protected Component getDefaultName() {
        return Component.literal("View Finder");
    }

    @Override
    public boolean canPlaceItem(int index, ItemStack stack) {
        return stack.getItem() instanceof BlockItem bi &&
                (bi.getBlock() instanceof StainedGlassPaneBlock || bi.getBlock() instanceof SkullBlock);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
    }


    public ItemInteractionResult tryInteracting(Player player, InteractionHand hand, ItemStack stack, BlockPos pos) {
        ItemInteractionResult itemAdd = this.interactWithPlayerItem(player, hand, stack);
        if (itemAdd.consumesAction()) {
            return itemAdd;
        }
        //same as super but sends custom packet
        if (player instanceof ServerPlayer sp && this.canBeUsedBy(pos, player)) {
            // open gui (edit sign with empty hand)
            this.setCurrentUser(player.getUUID());
            NetworkHelper.sendToClientPlayer(sp, new ClientBoundControlViewFinderPacket(TileOrEntityTarget.of(this)));
        }
        //always swing on fail
        return ItemInteractionResult.SUCCESS;
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


    public void setLocked(boolean b) {
        this.locked = b;
    }

    @Override
    public void setCurrentUser(@Nullable UUID uuid) {
        this.controllingPlayer = uuid;
    }

    @Override
    public @Nullable UUID getCurrentUser() {
        return controllingPlayer;
    }

    public float getFOVModifier() {
        float spyglassZoom = 0.1f;
        float maxZoom = spyglassZoom / 5;
        float normalizedZoom = getNormalizedZoomFactor();
        return Mth.lerp(normalizedZoom, 1, maxZoom);
    }

    public float getNormalizedZoomFactor() {
        float normalizedZoom = (this.getZoomLevel() - 1f) / (ViewFinderBlockEntity.MAX_ZOOM - 1f);
        normalizedZoom = 1 - ((1 - normalizedZoom) * (1 - normalizedZoom)); //easing
        return normalizedZoom;
    }

    private static final GameProfile VIEW_FINDER_PLAYER = new GameProfile(UUID.fromString("33242C44-27d9-1f22-3d27-99D2C45d1378"),
            "[VIEW_FINDER_ENDERMAN_PLAYER]");

    public boolean angerEndermenBeingLookedAt(List<TVSpectatorView> views, int range,
                                              float screenW, float screenH, TVBlockEntity fromTV) {
        if (views.isEmpty()) return false;
        Vec3 lensCenter = Vec3.atCenterOf(worldPosition);
        double rangeSq = (double) range * (double) range;

        AABB aabb = new AABB(worldPosition).inflate(range);
        List<EnderMan> enderMen = level.getEntitiesOfClass(EnderMan.class, aabb, em ->
                em.distanceToSqr(lensCenter.x, lensCenter.y, lensCenter.z) < rangeSq
        );
        if (enderMen.isEmpty()) return false;
        boolean anyAnger = false;

        List<EndermanLookResult> lookResults = computeEndermanLookedAt(views, screenW, screenH, enderMen);
        for (var r : lookResults) {
            if (EndermanFreezeWhenLookedAtThroughTVGoal.anger(r.enderman, r.player, this, fromTV)) {
                anyAnger = true;
            }
        }
        return anyAnger;
    }

    private record EndermanLookResult(Player player, EnderMan enderman) {
    }


    public boolean isEndermanBeingLookedAt(TVSpectatorView view, float screenW, float screenH, EnderMan man) {
        return !computeEndermanLookedAt(List.of(view), screenW, screenH, List.of(man)).isEmpty();
    }

    private List<EndermanLookResult> computeEndermanLookedAt(List<TVSpectatorView> views, float screenW, float screenH,
                                                             List<EnderMan> enderMen) {
        List<EndermanLookResult> lookResults = new ArrayList<>();
        Vec3 lensFacing = Vec3.directionFromRotation(this.pitch, this.yaw).normalize();
        Vec3 lensCenter = Vec3.atCenterOf(worldPosition);

        // Prepare fake player once
        Player fakePlayer = FakePlayerManager.get(VIEW_FINDER_PLAYER, level);
        float eyeH = fakePlayer.getEyeHeight();


        // For each view result: map local (x,y) -> destination world point, orient fake player, notify endermen
        for (TVSpectatorView vr : views) {
            // local offsets on source screen (meters)
            float localX = -vr.localHit().x; //flip since tv faces the other way
            float localY = vr.localHit().y;

            // If your hitPos is *normalized* in [-0.5..0.5], convert here:
            // localX *= screenSideLength; localY *= screenSideLength;

            // Map local offset onto the destination screen:
            Vec3 t = lensCenter.add(lensFacing.scale(-NEAR_PLANE));
            // Place fake player's eye at the destination screen center height (adjust if you want different origin)
            fakePlayer.setPos(t.x, t.y - eyeH, t.z);

            // Compute look vector from fake eye to mapped hit
            Vec3 look = pixelRayDir(localX, localY, screenW, screenH);

            //TODO:better math here
            // Convert look vector to yaw/pitch (Minecraft convention)
            //flip look since its inverted
            float yRot = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) + 90);
            double horiz = Math.sqrt(look.x * look.x + look.z * look.z);
            float xRot = (float) (-Math.toDegrees(Math.atan2(look.y, horiz)));

            fakePlayer.setYRot(yRot + this.yaw);
            fakePlayer.setYHeadRot(yRot + this.yaw);
            fakePlayer.setXRot(xRot + this.pitch);

            // Iterate endermen found in AABB and apply tighter checks before calling isLookingAtMe
            for (EnderMan man : enderMen) {
                // Now the enderman is in range and in front: trigger the "looking at fake player"
                if (man.isLookingAtMe(fakePlayer)) {
                    lookResults.add(new EndermanLookResult(vr.player(), man));
                }
            }
        }

        return lookResults;
    }


    private Vec3 pixelRayDir(float px, float py, float screenWidth, float screenHeight) {
        // 1) NDC coords in [-1, 1]
        float fovRad = BASE_FOV * getFOVModifier() * Mth.DEG_TO_RAD;
        float ndcX = (2.0f * px) / screenWidth;
        float ndcY = (2.0f * py) / screenHeight; // flip Y if needed by your convention

        // 2) camera-space ray direction (z = -1 for right-handed camera looking down -Z)
        float aspect = screenWidth / screenHeight;
        float tanHalfFov = (float) Math.tan(fovRad * 0.5f);

        float camX = ndcX * aspect * tanHalfFov;
        float camY = ndcY * tanHalfFov;
        // camera-space direction
        Vector3f dirCam = new Vector3f(camX, camY, -1.0f).normalize();

        // 3) rotate by camera orientation to world space (no translation)
        // cameraRotation should represent camera->world rotation (inverse of view rotation)

        // 4) return Minecraft Vec3
        return new Vec3(dirCam.x, dirCam.y, dirCam.z);
    }

}
