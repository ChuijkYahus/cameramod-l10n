package net.mehvahdjukaar.vista.client;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public final class LOD {

    // ---- squared distance tiers (tweak as you like) ----
    public static final double VERY_NEAR_DIST = sq(16);
    public static final double NEAR_DIST = sq(32);
    public static final double NEAR_MED_DIST = sq(48);
    public static final double MEDIUM_DIST = sq(64);
    public static final double FAR_DIST = sq(96);

    private final Vec3 camPos;
    private final Vec3 camDir;
    private final Vec3 objCenter;
    private final double distSq;       // computed once

    public static LOD at(BlockEntity be) {
        return LOD.at(be.getBlockPos());
    }

    public static LOD at(BlockPos objPos) {
        Minecraft mc = Minecraft.getInstance();
        return LOD.at(mc.gameRenderer.getMainCamera(), objPos.getCenter());
    }

    public static LOD at(Camera camera, BlockPos objPos) {
        return LOD.at(camera, objPos.getCenter());
    }

    public static LOD at(Camera camera, Vec3 objCenter) {
        return new LOD(camera, objCenter);
    }

    private LOD(Camera camera, Vec3 objCenter) {
        this.camPos = camera.getPosition();
        this.camDir = new Vec3(camera.getLookVector()).normalize();
        this.objCenter = objCenter;
        this.distSq = isScoping() ? 1 : camPos.distanceToSqr(objCenter);
    }

    private static boolean isScoping() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        return p != null && mc.options.getCameraType().isFirstPerson() && p.isScoping();
    }


    // ---- distance helpers (use precomputed distSq) ----
    public boolean isVeryNear() {
        return distSq <= VERY_NEAR_DIST;
    }

    public boolean isNear() {
        return distSq <= NEAR_DIST;
    }

    public boolean isNearMed() {
        return distSq <= NEAR_MED_DIST;
    }

    public boolean isMedium() {
        return distSq <= MEDIUM_DIST;
    }

    public boolean isFar() {
        return distSq <= FAR_DIST;
    }

    /**
     * Generic max range check (if you want custom per-object limits).
     */
    public boolean within(double maxDistSq) {
        return distSq <= maxDistSq;
    }

    // ---- plane facing (optional offset) ----

    /**
     * Returns true if the plane (with normal) faces the camera.
     */
    public boolean isPlaneCulled(Vec3 normalVec) {
        return isPlaneCulled(normalVec, null, 0.0f);
    }

    public boolean isPlaneCulled(Direction facing, float offset, float cosTolerance) {
        Vector3f normal = facing.step();
        return isPlaneCulled(new Vec3(normal), new Vec3(normal.mul(offset)), cosTolerance);
    }

    /**
     * @param planeNormal  unit-length normal
     * @param offset       optional offset from object center (null for none)
     * @param cosTolerance require normal·toCam > cosTolerance (0 = any front-facing)
     */
    public boolean isPlaneCulled(Vec3 planeNormal, @Nullable Vec3 offset, float cosTolerance) {
        // Plane point (object center + optional offset)
        Vec3 planePoint = (offset == null) ? this.objCenter : this.objCenter.add(offset);

        // Vector from camera to plane
        Vec3 camToPlane = planePoint.subtract(this.camPos);

        // If plane is behind the camera (i.e., in the opposite direction of where the camera looks)
        double dirDot = camToPlane.normalize().dot(this.camDir);
        if (dirDot <= 0.0) {
            // Plane is behind camera — not visible
            return true;
        }

        // Check if plane normal faces the camera
        double facing = planeNormal.normalize().dot(camToPlane.normalize());

        // Visible only if plane faces toward camera and is within tolerance
        return facing >= -cosTolerance; // negative because normal points *outward* from the visible face
    }


    public static double sq(double v) {
        return v * v;
    }

}
