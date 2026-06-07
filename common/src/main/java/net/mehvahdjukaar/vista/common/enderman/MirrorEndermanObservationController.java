package net.mehvahdjukaar.vista.common.enderman;

import com.mojang.authlib.GameProfile;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class MirrorEndermanObservationController extends AbstractEndermanObservationController {

    private static final float PLAYERS_TO_MIRROR_DIST = 20;
    private static final float ENDERMEN_TO_MIRROR_DIST = 20;
    private static final GameProfile MIRROR_PLAYER = new GameProfile(
            UUID.fromString("33242C44-27d9-1f22-3d27-99D2C45d1379"),
            "[MIRROR_ENDERMAN_PLAYER]");

    private final MirrorBlockEntity myMirror;

    public MirrorEndermanObservationController(MirrorBlockEntity mirror) {
        this.myMirror = mirror;
    }

    @Override
    protected Level level() {
        return myMirror.getLevel();
    }

    @Override
    protected GameProfile fakePlayerProfile() {
        return MIRROR_PLAYER;
    }

    @Override
    protected float playersScreenDist() {
        return PLAYERS_TO_MIRROR_DIST;
    }

    @Override
    protected float endermenSearchDist() {
        return ENDERMEN_TO_MIRROR_DIST;
    }

    @Override
    public boolean isInvalid() {
        return myMirror.isRemoved();
    }

    @Override
    protected TickContext openTick() {
        ScreenInfo sb = computeScreenBasis();
        return new TickContext(sb, myMirror.getBlockPos(), (fp, hit) -> orientAtReflection(sb, fp, hit));
    }

    private ScreenInfo computeScreenBasis() {
        Direction facing = myMirror.getBlockState().getValue(MirrorBlock.FACING);
        Vec2i connected = myMirror.getConnectedCount();
        // Master tile sits at the bottom-right corner of the grid (see MirrorBlockEntityRenderer);
        // the grid extends along facing.getCounterClockWise() and +Y from the master.
        Vec3 normal = Vec3.atLowerCornerOf(facing.getNormal());
        Vec3 right = Vec3.atLowerCornerOf(facing.getCounterClockWise().getNormal());
        Vec3 up = new Vec3(0, 1, 0);
        float w = connected.x();
        float h = connected.y();
        Vec3 center = Vec3.atCenterOf(myMirror.getBlockPos())
                .add(normal.scale(0.5))
                .add(right.scale((w - 1) * 0.5))
                .add(up.scale((h - 1) * 0.5));
        return new ScreenInfo(center, normal, right, up, w, h);
    }

    private static boolean orientAtReflection(ScreenInfo sb, Player fakePlayer, ScreenSpectatorView hit) {
        // 1) reconstruct world-space hit point on the mirror surface
        double localX = hit.localHit().x * sb.width();
        double localY = hit.localHit().y * sb.height();
        Vec3 hitWorld = sb.center().add(sb.right().scale(localX)).add(sb.up().scale(localY));

        // 2) reflect the player's view across the mirror normal
        Vec3 playerView = hit.player().getViewVector(1.0F).normalize();
        double dotLN = playerView.dot(sb.normal());
        Vec3 reflected = playerView.subtract(sb.normal().scale(2 * dotLN)).normalize();

        // 3) place fake player slightly in front of the mirror along the reflected ray so its
        // bounding box doesn't intersect the mirror block itself
        float offset = 0.8f;
        Vec3 fpPos = hitWorld.add(reflected.scale(offset));
        float eyeH = fakePlayer.getEyeHeight();
        fakePlayer.setPos(fpPos.x, fpPos.y - eyeH, fpPos.z);

        // 4) orient fake player along the reflected dir using MC yaw/pitch conventions
        float yRot = (float) Math.toDegrees(Math.atan2(-reflected.x, reflected.z));
        double horiz = Math.sqrt(reflected.x * reflected.x + reflected.z * reflected.z);
        float xRot = (float) -Math.toDegrees(Math.atan2(reflected.y, horiz));
        fakePlayer.setYRot(yRot);
        fakePlayer.setYHeadRot(yRot);
        fakePlayer.setXRot(xRot);
        return true;
    }
}
