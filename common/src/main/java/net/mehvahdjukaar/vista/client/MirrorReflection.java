package net.mehvahdjukaar.vista.client;

import net.minecraft.world.phys.Vec3;

/**
 * Reflects a viewer eye across a planar mirror. We only need the reflected eye position now —
 * the camera orientation in the reflection render is always perpendicular to the mirror, and
 * the off-axis projection matrix in {@code MirrorRenderManager} bends the frustum to fit the
 * mirror's frame from that reflected vantage point.
 */
public record MirrorReflection(Vec3 reflectedEye, double signedDistance) {

    public static MirrorReflection compute(Vec3 planePoint, Vec3 planeNormal, Vec3 eyePos) {
        Vec3 d = eyePos.subtract(planePoint);
        double signedDistance = d.dot(planeNormal);
        Vec3 reflectedEye = eyePos.subtract(planeNormal.scale(2 * signedDistance));
        return new MirrorReflection(reflectedEye, signedDistance);
    }

    public boolean viewerInFront() {
        return signedDistance > 0;
    }
}
