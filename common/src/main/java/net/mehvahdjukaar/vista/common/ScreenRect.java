package net.mehvahdjukaar.vista.common;

import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public record ScreenRect(Vec3 center, Vec3 normal, float width, float height) {

    public static final Vec3 UP = new Vec3(0, 1, 0);

    public Vec3 right() {
        return UP.cross(normal);
    }

    //-0.5 to 0.5, +x is right when facing it
    @Nullable
    public Vec2 projectToLocal(Vec3 worldPoint) {
        Vec3 local = worldPoint.subtract(center);
        double x = local.dot(right());
        double y = local.dot(UP);
        if (Math.abs(x) > width / 2f || Math.abs(y) > height / 2f) return null;
        return new Vec2((float) (x / width), (float) (y / height));
    }
}
