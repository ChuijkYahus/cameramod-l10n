package net.mehvahdjukaar.vista.client.renderer;

import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.MirrorReflection;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.client.textures.MirrorReflectionTexture;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mirror reflection rendering: builds an off-axis frustum from the reflected eye through the
 * mirror's frame, then drives {@link VistaLevelRenderer#render} into the mirror's
 * {@link MirrorReflectionTexture}. Two dispatch modes are supported, switchable via
 * {@code ClientConfigs.MIRROR_UPDATE_MODE}:
 *
 * <ul>
 *   <li><b>TEXTURE_REFRESH</b> — the BE renderer stashes the mirror + eye on the texture and
 *       marks it for refresh; the existing live-feed texture refresh dispatch invokes
 *       {@link #renderMirror} at end of frame.</li>
 *   <li><b>RENDER_TICK_END</b> — the BE renderer calls {@link #requestUpdate}, which pushes the
 *       request into a pending queue; {@link #processPending} flushes the queue from the
 *       {@code onRenderTickEnd} hook.</li>
 * </ul>
 *
 * <p>In both modes the camera eye is captured at BE-render time (see {@link #captureEye}) and
 * carried through to the actual render — the mirror frame and the source view stay coherent.
 *
 * <p>The reflection itself is rendered with an <b>off-axis frustum</b>: the reflected camera is
 * placed at the viewer's mirror image, oriented to look perpendicularly into the mirror, and the
 * projection matrix is {@code glFrustum(l, r, b, t, near, far)} where {@code near} is the
 * distance from the reflected eye to the mirror plane and {@code (l, r, b, t)} are the mirror's
 * frame corners projected onto the near plane. This is what makes coplanar mirrors look
 * different (each one sees the reflected scene through *its own* frame), and it's what makes the
 * reflection's parallax line up with the mirror surface when you move.
 */
public class MirrorTextureRenderer {

    // Minimum near-plane distance — only kicks in if the viewer's eye is essentially
    // touching the mirror surface, where depth precision would collapse.
    private static final float MIN_NEAR = 0.05f;
    private static final float FAR = 1000f;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Pending(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye) {}

    /**
     * Queue a render for the RENDER_TICK_END mode. The BE renderer calls this from
     * {@code render(...)}; {@link #processPending} flushes the queue at the end of the frame.
     */
    public static void requestUpdate(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye) {
        PENDING.put(mirror.getId(), new Pending(mirror, screenSize, eye));
    }

    public static void processPending() {
        if (PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING.clear();
            return;
        }
        // Snapshot + clear before iterating: each renderOne triggers a nested level render
        // that walks block entities, and any other visible mirror's BE renderer will call
        // requestUpdate(...) again — mutating PENDING mid-iteration would CME. Anything
        // re-queued during iteration lands in a fresh PENDING for next frame.
        List<Pending> snapshot = new ArrayList<>(PENDING.values());
        PENDING.clear();
        for (Pending p : snapshot) {
            MirrorReflectionTexture text = LiveFeedTexturesManager.getMirrorTexture(p.mirror.getId(), p.screenSize);
            if (text != null) renderMirror(text, p.mirror, p.eye);
        }
    }

    public static void clear() {
        PENDING.clear();
    }

    public static void renderMirror(MirrorReflectionTexture text, MirrorBlockEntity mirror, Vec3 eye) {
        if (mirror.isRemoved()) return;
        Level level = mirror.getLevel();
        Minecraft mc = Minecraft.getInstance();
        if (level == null || level != mc.level) return;

        Direction dir = mirror.getBlockState().getValue(MirrorBlock.FACING);
        BlockPos pos = mirror.getBlockPos();
        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());

        // Build the mirror's local right axis in world space.
        // right = normal × worldUp  (points screen-right from the viewer's perspective)
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 camRight = normal.cross(worldUp).normalize();

        // For connected mirrors the master is at the bottom-left of the group.
        // The group centre is offset right by (W-1)/2 and up by (H-1)/2 from the master centre.
        Vec2i connection = mirror.getConnectedCount();
        double halfW = connection.x() * 0.5;
        double halfH = connection.y() * 0.5;

        Vec3 masterCenter = Vec3.atCenterOf(pos).add(normal.scale(0.5));
        Vec3 groupCenter = masterCenter
                .add(camRight.scale((connection.x() - 1) * 0.5))
                .add(worldUp.scale((connection.y() - 1) * 0.5));

        // Use groupCenter as the plane-point for the reflection so the reflected-eye
        // distance is computed relative to the actual surface midpoint.
        MirrorReflection reflection = MirrorReflection.compute(groupCenter, normal, eye);
        if (!reflection.viewerInFront()) return;

        // Mirror surface corners in world space (WxH block face, anchored at group centre).
        Vec3 halfRight = camRight.scale(halfW);
        Vec3 halfUp    = worldUp.scale(halfH);
        Vec3 bottomLeft  = groupCenter.subtract(halfRight).subtract(halfUp);
        Vec3 bottomRight = groupCenter.add(halfRight).subtract(halfUp);
        Vec3 topLeft     = groupCenter.subtract(halfRight).add(halfUp);

        // Off-axis frustum bounds, computed in the reflected camera's eye space.
        // All four corners lie on the mirror plane, so they share the same depth from the eye
        // (= signedDistance, the perpendicular distance from the reflected eye to the plane).
        // Setting near = depth makes the projection's near plane coincide with the mirror plane,
        // so anything between the reflected camera and the mirror (the wall it's mounted on,
        // the viewer's legs jutting through the plane, etc.) is z-clipped without any oblique
        // matrix trickery.
        double depth = reflection.signedDistance();
        float near = Math.max(MIN_NEAR, (float) depth);

        Vec3 vbl = bottomLeft.subtract(reflection.reflectedEye());
        Vec3 vbr = bottomRight.subtract(reflection.reflectedEye());
        Vec3 vtl = topLeft.subtract(reflection.reflectedEye());

        float scale = near / (float) depth;
        float l = (float) vbl.dot(camRight) * scale;
        float r = (float) vbr.dot(camRight) * scale;
        float b = (float) vbl.dot(worldUp)  * scale;
        float t = (float) vtl.dot(worldUp)  * scale;

        Matrix4f projection = new Matrix4f().frustum(l, r, b, t, near, FAR);

        final float camYaw = dir.toYRot();
        SceneCameraSetup setup = (camera, pt) ->
                MirrorBlockEntityRenderer.setupMirrorCamera(camera, level, reflection.reflectedEye(), camYaw);

        // BFS start override: a point one block in front of the mirror's center. That chunk is
        // guaranteed visible (the player can see the mirror's front face, so the chunk between
        // them is on-screen) and has its mesh built. Without this, mirrors mounted on walls
        // would have BFS seeded inside the wall block, stalling smart culling.
        Vec3 bfsStart = groupCenter.add(normal.scale(1.0));

        // fov is ignored because we pass a custom projection; planar reflection via a moved
        // camera preserves winding so we leave back-face culling alone.
        VistaLevelRenderer.render(text, mirror, setup, 0f, false, projection, bfsStart);

        // Blit the freshly-rendered write target onto the read target so the next frame's quad
        // samples the new image (matches RenderableDynamicTexture.redraw()'s tail).
        text.swapBackToFront();
    }

    /**
     * Captures the current main-camera eye position, with head-bob compensation applied. Called
     * from the BE renderer at the moment the mirror is about to be drawn — bake the eye into the
     * texture's pending state so the actual reflection render (which fires later in the frame)
     * uses the same vantage point the source view used.
     *
     * <p>The main view applies a small sinusoidal head-bob to its modelview when the player walks.
     * Our reflection's framebuffer is rendered from a fixed (un-bobbed) eye, but the BE quad it
     * gets displayed on bobs along with the rest of the world via the main view's modelview —
     * the result is the reflection's far edges visibly drifting against the source. The fix is
     * to shift the reflected eye position by the bob's geometric world equivalent, so the
     * off-axis frustum re-aims through the mirror frame at the bobbed POV and parallax stays
     * locked. We do NOT apply bob to the reflection's modelview (that double-bobs).
     *
     * <p>Only the translation component is applied; bob's tiny X/Z rotations are dropped — they
     * would require off-axis-frustum tilts that aren't worth the complexity for the magnitude.
     */
    public static Vec3 captureEye(Minecraft mc, Camera mainCamera) {
        Vec3 eye = mainCamera.getPosition();
        if (!mc.options.bobView().get()) return eye;
        if (!(mc.cameraEntity instanceof Player player)) return eye;

        float partialTick = mc.getTimer().getGameTimeDeltaTicks();
        float f = player.walkDist - player.walkDistO;
        float g = -(player.walkDist + f * partialTick);
        float h = Mth.lerp(partialTick, player.oBob, player.bob);
        if (h == 0f) return eye;

        // Same translation vanilla GameRenderer.bobView applies — in eye-rotated coords.
        float bobX = Mth.sin(g * (float) Math.PI) * h * 0.5F;
        float bobY = -Math.abs(Mth.cos(g * (float) Math.PI) * h);

        // Convert eye-rotated bob to world: apply eye-to-world rotation = camera.rotation().
        // The bob shifts the world by this much in modelview, which from the camera's POV is
        // equivalent to the camera moving by the OPPOSITE in world coords.
        Vector3f worldBob = new Vector3f(bobX, bobY, 0f);
        mainCamera.rotation().transform(worldBob);

        return eye.subtract(worldBob.x, worldBob.y, worldBob.z);
    }
}
