package net.mehvahdjukaar.vista.client.renderer;

import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.MirrorReflection;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexture;
import net.mehvahdjukaar.vista.client.textures.LiveFeedTexturesManager;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Mirrors can't render their reflection inside {@code BlockEntityRenderer.render}: the level
 * render that runs inside {@link VistaLevelRenderer#render} flushes the outer
 * {@code MultiBufferSource} mid-batch, which corrupts every other block-entity batch for the rest
 * of the frame. We queue the mirror during the main render and flush pending reflections in
 * {@code onRenderTickEnd}, after the frame has been drawn — each frame samples the texture
 * rendered at the end of the previous frame.
 *
 * <p>The reflection itself is rendered with an <b>off-axis frustum</b>: the reflected camera is
 * placed at the viewer's mirror image, oriented to look perpendicularly into the mirror, and the
 * projection matrix is {@code glFrustum(l, r, b, t, near, far)} where {@code near} is the
 * distance from the reflected eye to the mirror plane and {@code (l, r, b, t)} are the mirror's
 * frame corners projected onto the near plane. This is what makes coplanar mirrors look
 * different (each one sees the reflected scene through *its own* frame), and it's what makes the
 * reflection's parallax line up with the mirror surface when you move.
 */
public class MirrorRenderManager {

    // Minimum near-plane distance — only kicks in if the viewer's eye is essentially
    // touching the mirror surface, where depth precision would collapse.
    private static final float MIN_NEAR = 0.05f;
    private static final float FAR = 1000f;

    private static final Map<UUID, Pending> PENDING = new HashMap<>();

    private record Pending(MirrorBlockEntity mirror, Vec2i screenSize) {}

    public static void requestUpdate(MirrorBlockEntity mirror, Vec2i screenSize) {
        PENDING.put(mirror.getId(), new Pending(mirror, screenSize));
    }

    public static void processPending() {
        if (PENDING.isEmpty()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            PENDING.clear();
            return;
        }
        Camera mainCamera = mc.gameRenderer.mainCamera;
        if (mainCamera == null) {
            PENDING.clear();
            return;
        }
        Vec3 eye = mainCamera.getPosition();
        for (Pending p : PENDING.values()) {
            renderOne(p.mirror(), p.screenSize(), eye);
        }
        PENDING.clear();
    }

    public static void clear() {
        PENDING.clear();
    }

    private static void renderOne(MirrorBlockEntity mirror, Vec2i screenSize, Vec3 eye) {
        if (mirror.isRemoved()) return;
        Level level = mirror.getLevel();
        if (level == null || level != Minecraft.getInstance().level) return;

        Direction dir = mirror.getBlockState().getValue(MirrorBlock.FACING);
        BlockPos pos = mirror.getBlockPos();
        Vec3 normal = Vec3.atLowerCornerOf(dir.getNormal());
        Vec3 planePoint = Vec3.atCenterOf(pos).add(normal.scale(0.5));

        MirrorReflection reflection = MirrorReflection.compute(planePoint, normal, eye);
        if (!reflection.viewerInFront()) return;

        // Build the mirror's local right axis in world space. For a horizontal mirror the up axis
        // is world +Y, and right = forward × up where forward = mirror normal (the reflected
        // camera looks INTO the mirror, i.e. in the normal direction since the reflected eye sits
        // on the −normal side).
        Vec3 worldUp = new Vec3(0, 1, 0);
        Vec3 camRight = normal.cross(worldUp).normalize();

        // Mirror surface corners in world space (1×1 block face).
        Vec3 halfRight = camRight.scale(0.5);
        Vec3 halfUp = worldUp.scale(0.5);
        Vec3 bottomLeft  = planePoint.subtract(halfRight).subtract(halfUp);
        Vec3 bottomRight = planePoint.add(halfRight).subtract(halfUp);
        Vec3 topLeft     = planePoint.subtract(halfRight).add(halfUp);

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

        LiveFeedTexture text = LiveFeedTexturesManager.getMirrorTexture(mirror.getId(), screenSize);
        if (text == null) return;

        final float camYaw = dir.toYRot();
        SceneCameraSetup setup = (camera, pt) ->
                MirrorBlockEntityRenderer.setupMirrorCamera(camera, level, reflection.reflectedEye(), camYaw);

        // fov is ignored because we pass a custom projection; flipWinding stays off (planar
        // reflection via a moved camera preserves winding — flipping would draw cube interiors).
        VistaLevelRenderer.render(text, mirror, setup, 0f, false, projection);

        // Blit the freshly-rendered write target onto the read target so the next frame's quad
        // samples the new image (matches RenderableDynamicTexture.redraw()'s tail).
        text.swapBackToFront();
    }
}
