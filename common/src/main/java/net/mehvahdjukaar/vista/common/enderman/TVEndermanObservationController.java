package net.mehvahdjukaar.vista.common.enderman;

import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.moonlight.api.platform.PlatHelper;
import net.mehvahdjukaar.moonlight.api.util.math.EntityAngles;
import net.mehvahdjukaar.moonlight.api.util.math.MthUtils;
import net.mehvahdjukaar.vista.client.renderer.ViewFinderBlockEntityRenderer;
import net.mehvahdjukaar.vista.common.broadcast.BroadcastManager;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVBlockEntity;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import net.mehvahdjukaar.vista.configs.ClientConfigs;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class TVEndermanObservationController extends AbstractEndermanObservationController {

    private static final float PLAYERS_TO_TV_DIST = 20;
    private static final float ENDERMEN_TO_CAMERA_DIST = 20;
    private static final GameProfile VIEW_FINDER_PLAYER = new GameProfile(
            UUID.fromString("33242C44-27d9-1f22-3d27-99D2C45d1378"),
            "[VIEW_FINDER_ENDERMAN_PLAYER]");

    private final UUID broadcastUUID;
    private final TVBlockEntity myTv;

    public TVEndermanObservationController(@NotNull UUID broadcastUUID, TVBlockEntity tv) {
        this.broadcastUUID = broadcastUUID;
        this.myTv = tv;
    }

    @Override
    protected Level level() {
        return myTv.getLevel();
    }

    @Override
    protected GameProfile fakePlayerProfile() {
        return VIEW_FINDER_PLAYER;
    }

    @Override
    protected float playersScreenDist() {
        return PLAYERS_TO_TV_DIST;
    }

    @Override
    protected float endermenSearchDist() {
        return ENDERMEN_TO_CAMERA_DIST;
    }

    @Override
    public boolean isInvalid() {
        return myTv.isRemoved();
    }

    @Nullable
    @Override
    protected TickContext openTick() {
        Level level = level();
        BroadcastManager manager = BroadcastManager.getInstance(level);
        if (!(manager.getBroadcast(broadcastUUID, level.isClientSide) instanceof ViewFinderBlockEntity vf)) {
            return null;
        }
        // Endermen are searched around the *remote* ViewFinder, not the TV.
        return new TickContext(computeScreenBasis(), vf.getBlockPos(),
                (fp, hit) -> orientAtViewFinder(vf, fp, hit));
    }

    private ScreenInfo computeScreenBasis() {
        Direction facing = myTv.getBlockState().getValue(TVBlock.FACING);
        float screenW = myTv.getScreenPixelWidth() / 16f;
        float screenH = myTv.getScreenPixelHeight() / 16f;
        Vec2 relativeCenter = myTv.getScreenBlockCenter();
        Vec3 center = myTv.getBlockPos().getCenter()
                .add(MthUtils.rotateVec3(new Vec3(relativeCenter.x, relativeCenter.y, 0.5), facing.getOpposite()));
        Vec3 normal = new Vec3(facing.step()).normalize();
        Vec3 up = new Vec3(0, 1, 0);
        Vec3 right = normal.cross(up);
        return new ScreenInfo(center, normal, right, up, screenW, screenH);
    }

    private static boolean orientAtViewFinder(ViewFinderBlockEntity vf, Player fakePlayer, ScreenSpectatorView hit) {
        Vec3 lensFacing = new Vec3(vf.getGlobalFacing(1));
        Vec3 lensCenter = Vec3.atCenterOf(vf.getBlockPos());
        float eyeH = fakePlayer.getEyeHeight();

        if (PlatHelper.getPhysicalSide().isClient() && ClientConfigs.rendersDebug()) {
            ViewFinderBlockEntityRenderer.debugLastPlayer = new WeakReference<>(fakePlayer);
        }

        float localX = -hit.localHit().x; // flip since tv faces the other way
        float localY = hit.localHit().y;

        Vec3 t = lensCenter.add(lensFacing.scale(-ViewFinderBlockEntity.NEAR_PLANE));
        fakePlayer.setPos(t.x, t.y - eyeH, t.z);

        Vec3 look = pixelRayDir(vf, localX, localY);
        float yRot = (float) (Math.toDegrees(Math.atan2(look.z, look.x)) + 90);
        double horiz = Math.sqrt(look.x * look.x + look.z * look.z);
        float xRot = (float) (-Math.toDegrees(Math.atan2(look.y, horiz)));

        EntityAngles ea = EntityAngles.fromQuaternion(vf.getWorldOrientation(1));
        float yaw = ea.yaw();
        float pitch = ea.pitch();
        fakePlayer.setYRot(yRot + yaw);
        fakePlayer.setYHeadRot(yRot + yaw);
        fakePlayer.setXRot(xRot + pitch);

        // move forward to skip our own BB since it cant be empty due to particles
        float offset = 0.8f;
        Vec3 forward = lensFacing.normalize();
        fakePlayer.setPos(
                fakePlayer.getX() + forward.x * offset,
                fakePlayer.getY() + forward.y * offset,
                fakePlayer.getZ() + forward.z * offset
        );
        return true;
    }

    private static Vec3 pixelRayDir(ViewFinderBlockEntity vf, float px, float py) {
        float fovRad = vf.getFOV() * Mth.DEG_TO_RAD;
        float ndcX = (2.0f * px);
        float ndcY = (2.0f * py);
        float aspect = 1;
        float tanHalfFov = (float) Math.tan(fovRad * 0.5f);
        float camX = ndcX * aspect * tanHalfFov;
        float camY = ndcY * tanHalfFov;
        Vector3f dirCam = new Vector3f(camX, camY, -1.0f).normalize();
        return new Vec3(dirCam.x, dirCam.y, dirCam.z);
    }
}
