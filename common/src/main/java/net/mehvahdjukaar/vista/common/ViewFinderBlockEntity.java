package net.mehvahdjukaar.vista.common;

import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.moonlight.api.block.IOnePlayerInteractable;
import net.mehvahdjukaar.moonlight.api.block.ItemDisplayTile;
import net.mehvahdjukaar.moonlight.api.misc.TileOrEntityTarget;
import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.mehvahdjukaar.moonlight.api.util.FakePlayerManager;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.vista.VistaMod;
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
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.UUID;

public class ViewFinderBlockEntity extends ItemDisplayTile implements IOnePlayerInteractable {

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
        if (player instanceof ServerPlayer sp && !this.isOtherPlayerEditing(pos, player)) {
            // open gui (edit sign with empty hand)
            this.setPlayerWhoMayEdit(player.getUUID());
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

    public int getMaxZoom() {
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


    public void setLocked(boolean b) {
        this.locked = b;
    }

    @Override
    public void setPlayerWhoMayEdit(@Nullable UUID uuid) {
        this.controllingPlayer = uuid;
    }

    @Override
    public UUID getPlayerWhoMayEdit() {
        return controllingPlayer;
    }


    public float getFOVModifier() {
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

    private static final GameProfile VIEW_FINDER_PLAYER = new GameProfile(UUID.fromString("33242C44-27d9-1f22-3d27-99D2C45d1378"),
            "[VIEW_FINDER_ENDERMAN_PLAYER]");

    public boolean angerEnderMen(List<TvViewHitResult> views, int range,
                                 float screenW, float screenH) {
        if (views.isEmpty()) return false;
        final double EPS = 1e-6;

        Vec3 lensFacing = Vec3.directionFromRotation(this.pitch, this.yaw).normalize();
        Vec3 lensCenter = Vec3.atCenterOf(worldPosition).add(lensFacing.scale(0.5));
        double rangeSq = (double) range * (double) range;

        AABB aabb = new AABB(worldPosition).inflate(range);
        List<EnderMan> enderMen = level.getEntitiesOfClass(EnderMan.class, aabb, em ->
                em.distanceToSqr(lensCenter.x, lensCenter.y, lensCenter.z) < rangeSq
        );
        if (enderMen.isEmpty()) return false;

        // Destination screen geometry (receiver)
        Vec3 worldUp = new Vec3(0, 1, 0);

        // destRight = destNormal × worldUp  (safe because destNormal is horizontal in your scenario OR robust fallback provided)
        Vec3 lensRight = lensFacing.cross(worldUp);
        double drLen = lensRight.length();
        if (drLen < EPS) lensRight = new Vec3(1, 0, 0);
        else lensRight = lensRight.scale(1.0 / drLen);
        Vec3 destUp = lensRight.cross(lensFacing).normalize();

        // destCenter: block center offset half a block along the outward normal (same convention as producer)

        // Prepare fake player once
        Player fakePlayer = FakePlayerManager.get(VIEW_FINDER_PLAYER, level);
        float eyeH = fakePlayer.getEyeHeight();

        boolean anyAnger = false;

        // For each view result: map local (x,y) -> destination world point, orient fake player, notify endermen
        for (TvViewHitResult vr : views) {
            // local offsets on source screen (meters)
            float localX = -vr.localHit().x; //flip since tv faces the other way
            float localY = vr.localHit().y;

            // If your hitPos is *normalized* in [-0.5..0.5], convert here:
            // localX *= screenSideLength; localY *= screenSideLength;

            // Map local offset onto the destination screen:
            Vec3 lensRelative = lensCenter.add(lensRight.scale(localX)).add(destUp.scale(localY));

            // Place fake player's eye at the destination screen center height (adjust if you want different origin)
            fakePlayer.setPos(lensRelative.x, lensRelative.y - eyeH, lensRelative.z);

            // Compute look vector from fake eye to mapped hit
            Vec3 look = pixelRayDirWorld(localX, localY, screenW, screenH);

            //TODO:better math here
            // Convert look vector to yaw/pitch (Minecraft convention)
            //flip look since its inverted
            float yRot = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) + 90);
            double horiz = Math.sqrt(look.x * look.x + look.z * look.z);
            float xRot = (float) (-Math.toDegrees(Math.atan2(look.y, horiz)));

            fakePlayer.setYRot(yRot);
            fakePlayer.setXRot(xRot);
            fakePlayer.setYHeadRot(yRot);
            this.fakePlayer = fakePlayer;

            // Iterate endermen found in AABB and apply tighter checks before calling isLookingAtMe
            for (EnderMan em : enderMen) {
                // Now the enderman is in range and in front: trigger the "looking at fake player"
                if (em.isLookingAtMe(fakePlayer)) {
                    anyAnger = true;
                    break;
                }
            }
        }

        return anyAnger;
    }


    /**
     * Computes a normalized world-space ray direction for a given pixel.
     *
     * @param px pixel x (-0.5..0.5)
     * @param py pixel y (-0.5..0.5)
     * @return normalized world-space direction vector
     */
    public Vec3 pixelRayDirWorld(float px, float py, float screenWidth, float screenHeight) {

        // 1) Convert pixel to Normalized Device Coordinates from -1 to 1
        float nx = (2 * px) / screenWidth;
        float ny = (2 * py) / screenHeight;

        Matrix4f projView = new Matrix4f().perspective(BASE_FOV * getFOVModifier() * Mth.DEG_TO_RAD,
                screenWidth / screenHeight, ViewFinderBlockEntity.NEAR_PLANE, 10);

        // 2) Inverse of projection * view
        Matrix4f invPV = projView.invert();

        // 3) Define clip-space positions for near and far plane
        Vector4f clipNear = new Vector4f(nx, ny, -1.0f, 1.0f);
        Vector4f clipFar = new Vector4f(nx, ny, 1.0f, 1.0f);

        // 4) Transform to world space
        Vector4f worldNear4 = invPV.transform(clipNear);
        Vector4f worldFar4 = invPV.transform(clipFar);

        // 5) Perspective divide
        Vector3f worldNear = new Vector3f(worldNear4.x / worldNear4.w, worldNear4.y / worldNear4.w, worldNear4.z / worldNear4.w);
        Vector3f worldFar = new Vector3f(worldFar4.x / worldFar4.w, worldFar4.y / worldFar4.w, worldFar4.z / worldFar4.w);

        // 6) Ray direction (normalized)
        return new Vec3(worldFar.sub(worldNear).normalize());
    }


    public Player fakePlayer = null;
}
