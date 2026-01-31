package net.mehvahdjukaar.vista.common.tv;

import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.moonlight.api.util.FakePlayerManager;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.vista.common.BroadcastManager;
import net.mehvahdjukaar.vista.common.EndermanFreezeWhenLookedAtThroughTVGoal;
import net.mehvahdjukaar.vista.common.view_finder.EndermanLookResult;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TVEndermanObservationController {

    private static final float PLAYERS_TO_TV_DIST_SQ = 20;
    private static final float ENDERMEN_TO_CAMERA_DIST_SQ = 20;
    private static final GameProfile VIEW_FINDER_PLAYER = new GameProfile(UUID.fromString("33242C44-27d9-1f22-3d27-99D2C45d1378"),
            "[VIEW_FINDER_ENDERMAN_PLAYER]");

    private final UUID broadcastUUID;
    private final TVBlockEntity myTv;

    public TVEndermanObservationController(UUID broadcastUUID, TVBlockEntity tv) {
        this.broadcastUUID = broadcastUUID;
        this.myTv = tv;
    }

    public boolean isPlayerLookingAtEnderman(EnderMan enderMan, Player player) {
        TVSpectatorView view = getPlayerLookView(player);
        if (view == null) {
            return false;
        }
        Level level = myTv.getLevel();
        ViewFinderBlockEntity viewFinder = BroadcastManager.findLinkedViewFinder(level, broadcastUUID);
        if (viewFinder == null) return false;
        return !computeEndermanLookedAt(viewFinder, List.of(view), List.of(enderMan)).isEmpty();
    }

    public boolean tick() {
        Level level = myTv.getLevel();
        ViewFinderBlockEntity viewFinder = BroadcastManager.findLinkedViewFinder(level, broadcastUUID);
        if (viewFinder != null) {
            List<TVSpectatorView> doomScrollingPlayers = getPlayersLookView(level.players());

            return angerEntitiesBeingLookedAt(viewFinder, doomScrollingPlayers);
        }
        return false;
    }

    private List<TVSpectatorView> getPlayersLookView(Collection<? extends Player> players) {
        if (players.isEmpty()) return List.of();
        List<TVSpectatorView> result = new ArrayList<>();

        BlockState state = myTv.getBlockState();
        Direction facing = state.getValue(TVBlock.FACING);
        float screenW = myTv.getScreenPixelWidth() / 16f;
        float screenH = myTv.getScreenPixelHeight() / 16f;
        // Screen center: block center offset half a block in facing direction
        Vec2 relativeCenter = myTv.getScreenBlockCenter();
        Vec3 screenCenterPos = myTv.getBlockPos().getCenter()
                .add(MthUtils.rotateVec3(new Vec3(relativeCenter.x, relativeCenter.y, 0.5), facing.getOpposite()));

        // Screen normal (points outward from screen). facing is horizontal => normal.y == 0
        Vec3 screenNormal = new Vec3(facing.step()).normalize();

        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = screenNormal.cross(up);

        for (Player player : players) {
            TVSpectatorView viewResult = getPlayerHit(player, screenCenterPos, screenNormal, right, up, screenW, screenH);
            if (viewResult != null) result.add(viewResult);
        }

        return result;
    }

    @Nullable
    public TVSpectatorView getPlayerLookView(Player player) {
        return getPlayersLookView(List.of(player)).stream().findFirst().orElse(null);
    }

    @Nullable
    private static TVSpectatorView getPlayerHit(Player player, Vec3 screenCenterPos, Vec3 screenNormal,
                                                Vec3 normalRight, Vec3 normalUp, float sideWidth, float sideHeight) {

        final double EPS = 1e-6;
        Vec3 eyePos = player.getEyePosition(1.0F);

        // 1) radius check (cheap, no sqrt)
        Vec3 eyeToCenter = screenCenterPos.subtract(eyePos);
        double distSq = eyeToCenter.lengthSqr();
        if (distSq > (PLAYERS_TO_TV_DIST_SQ * PLAYERS_TO_TV_DIST_SQ)) return null;
        eyeToCenter = eyeToCenter.scale(1.0 / Math.sqrt(distSq)); // normalize

        // 2) half-circle in front check (cheap): player must be in front half-space of screen
        double frontDot = eyeToCenter.dot(screenNormal);
        if (frontDot > 0.0) return null;

        // 3) get view vector (normalize for stable intersection math)
        Vec3 playerView = player.getViewVector(1.0F).normalize();

        // 4) ray-plane intersection
        double denom = playerView.dot(screenNormal);
        if (Math.abs(denom) < EPS) return null;

        double t = screenCenterPos.subtract(eyePos).dot(screenNormal) / denom;
        if (t <= 0.0) return null;

        Vec3 hit = eyePos.add(playerView.scale(t));

        // 5) local screen coordinates of hit relative to center
        Vec3 local = hit.subtract(screenCenterPos);
        double x = local.dot(normalRight);
        double y = local.dot(normalUp);

        // 6) bounds check (screen centered at screenCenterPos)
        if (Math.abs(x) <= (sideWidth / 2f) && Math.abs(y) <= (sideHeight / 2f)) {
            // distance = t (distance from eye to hit along ray)
            return new TVSpectatorView(player, new Vec2((float) x / sideWidth, (float) y / sideHeight), t);
        }
        return null;
    }

    private boolean angerEntitiesBeingLookedAt(ViewFinderBlockEntity camera, List<TVSpectatorView> views) {
        if (views.isEmpty()) return false;
        BlockPos blockPos = camera.getBlockPos();
        Level level = camera.getLevel();
        Vec3 lensCenter = Vec3.atCenterOf(blockPos);
        double rangeSq = (double) ENDERMEN_TO_CAMERA_DIST_SQ * ENDERMEN_TO_CAMERA_DIST_SQ;

        AABB aabb = new AABB(blockPos).inflate(ENDERMEN_TO_CAMERA_DIST_SQ);
        //TODO: tag
        List<EnderMan> enderMen = level.getEntitiesOfClass(EnderMan.class, aabb, em ->
                em.distanceToSqr(lensCenter.x, lensCenter.y, lensCenter.z) < rangeSq
        );
        if (enderMen.isEmpty()) return false;
        boolean anyAnger = false;

        List<EndermanLookResult> lookResults = computeEndermanLookedAt(camera, views, enderMen);
        for (var r : lookResults) {
            if (EndermanFreezeWhenLookedAtThroughTVGoal.anger(r.enderman(), r.player(), camera, myTv)) {
                anyAnger = true;
            }
        }
        return anyAnger;
    }

    private List<EndermanLookResult> computeEndermanLookedAt(ViewFinderBlockEntity vf,
                                                             List<TVSpectatorView> views, List<EnderMan> enderMen) {
        List<EndermanLookResult> lookResults = new ArrayList<>();
        Vec3 lensFacing = Vec3.directionFromRotation(vf.getPitch(), vf.getYaw()).normalize();
        Vec3 lensCenter = Vec3.atCenterOf(vf.getBlockPos());

        // Prepare fake player once
        Player fakePlayer = FakePlayerManager.get(VIEW_FINDER_PLAYER, vf.getLevel());
        float eyeH = fakePlayer.getEyeHeight();


        // For each view result: map local (x,y) -> destination world point, orient fake player, notify endermen
        for (TVSpectatorView vr : views) {
            // local offsets on source screen (meters)
            float localX = -vr.localHit().x; //flip since tv faces the other way
            float localY = vr.localHit().y;

            // If your hitPos is *normalized* in [-0.5..0.5], convert here:
            // localX *= screenSideLength; localY *= screenSideLength;

            // Map local offset onto the destination screen:
            Vec3 t = lensCenter.add(lensFacing.scale(-ViewFinderBlockEntity.NEAR_PLANE));
            // Place fake player's eye at the destination screen center height (adjust if you want different origin)
            fakePlayer.setPos(t.x, t.y - eyeH, t.z);

            // Compute look vector from fake eye to mapped hit
            Vec3 look = pixelRayDir(vf, localX, localY);

            //flip look since its inverted
            float yRot = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) + 90);
            double horiz = Math.sqrt(look.x * look.x + look.z * look.z);
            float xRot = (float) (-Math.toDegrees(Math.atan2(look.y, horiz)));

            fakePlayer.setYRot(yRot + vf.getYaw());
            fakePlayer.setYHeadRot(yRot + vf.getYaw());
            fakePlayer.setXRot(xRot + vf.getPitch());

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


    private static Vec3 pixelRayDir(ViewFinderBlockEntity vf, float px, float py) {
        // 1) NDC coords in [-1, 1]
        float fovRad = vf.getFOV() * Mth.DEG_TO_RAD;
        float ndcX = (2.0f * px);
        float ndcY = (2.0f * py); // flip Y if needed by your convention

        // 2) camera-space ray direction (z = -1 for right-handed camera looking down -Z)
        float aspect = 1;
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

    public boolean isInvalid() {
        return myTv.isRemoved();
    }
}
