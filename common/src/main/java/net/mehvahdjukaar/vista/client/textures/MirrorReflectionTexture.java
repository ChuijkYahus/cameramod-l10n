package net.mehvahdjukaar.vista.client.textures;

import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.client.MirrorReflection;
import net.mehvahdjukaar.vista.client.renderer.MirrorBlockEntityRenderer;
import net.mehvahdjukaar.vista.client.renderer.SceneCameraSetup;
import net.mehvahdjukaar.vista.client.renderer.VistaLevelRenderer;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlock;
import net.mehvahdjukaar.vista.common.mirror.MirrorBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.UUID;

/**
 * Texture backing a mirror's reflection. The BE renderer stamps the current frame's mirror and
 * camera-eye position on the texture via {@link #setPending}; the end-of-frame texture refresh
 * consumes both and draws the off-axis frustum render via {@link #renderReflection}.
 *
 * <p>Eye position is captured at BE-render time (not at refresh time) so the camera state matches
 * the frame this update was requested for.
 *
 * <p>The reflection is rendered with an <b>off-axis frustum</b>: the reflected camera is placed at
 * the viewer's mirror image, oriented to look perpendicularly into the mirror, and the projection
 * matrix is {@code glFrustum(l, r, b, t, near, far)} where {@code near} is the distance from the
 * reflected eye to the mirror plane and {@code (l, r, b, t)} are the mirror's frame corners
 * projected onto the near plane. This is what makes coplanar mirrors look different (each one
 * sees the reflected scene through *its own* frame), and it's what makes the reflection's
 * parallax line up with the mirror surface when you move.
 */
public class MirrorReflectionTexture extends PerspectiveTexture {

    // Minimum near-plane distance — only kicks in if the viewer's eye is essentially
    // touching the mirror surface, where depth precision would collapse.
    private static final float MIN_NEAR = 0.05f;
    private static final float FAR = 1000f;

    @Nullable
    private MirrorBlockEntity pendingMirror;
    @Nullable
    private Vec3 pendingEye;

    public MirrorReflectionTexture(ResourceLocation resourceLocation, int width, int height, UUID id) {
        super(resourceLocation, width, height, id);
    }

    public void setPending(MirrorBlockEntity mirror, Vec3 eye) {
        this.pendingMirror = mirror;
        this.pendingEye = eye;
    }

    @Override
    protected void refresh() {
        MirrorBlockEntity mirror = pendingMirror;
        Vec3 eye = pendingEye;
        pendingMirror = null;
        pendingEye = null;
        if (mirror == null || eye == null) return;
        renderReflection(mirror, eye);
    }

    public void renderReflection(MirrorBlockEntity mirror, Vec3 eye) {
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
        VistaLevelRenderer.render(this, mirror, setup, 0f, false, projection, bfsStart);

        // Blit the freshly-rendered write target onto the read target so the next frame's quad
        // samples the new image (matches RenderableDynamicTexture.redraw()'s tail).
        swapBackToFront();
    }
}
