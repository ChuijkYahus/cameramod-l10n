package net.mehvahdjukaar.vista.common.tv;


import net.mehvahdjukaar.moonlight.api.util.math.Direction2D;
import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TVType implements StringRepresentable {
    SINGLE(0),
    CENTER(C.U | C.D | C.L | C.R),
    TOP(C.D | C.L | C.R),
    BOTTOM(C.U | C.L | C.R),
    LEFT(C.U | C.D | C.R),
    RIGHT(C.U | C.D | C.L),
    TOP_LEFT(C.D | C.R),
    TOP_RIGHT(C.D | C.L),
    BOTTOM_LEFT(C.U | C.R),
    BOTTOM_RIGHT(C.U | C.L);

    private final int mask;

    TVType(int mask) {
        this.mask = mask;
        C.LUT[mask] = this;
    }

    //just due to class loading
    private static class C {
        private static final int U = 1, D = 2, L = 4, R = 8;
        private static final TVType[] LUT = new TVType[16];
    }

    public static TVType fromConnections(boolean up, boolean down, boolean left, boolean right) {
        int mask = (up ? C.U : 0) | (down ? C.D : 0) | (left ? C.L : 0) | (right ? C.R : 0);
        TVType c = C.LUT[mask];
        if (c != null) return c;
        throw new IllegalArgumentException("Invalid pattern for square tiling (mask=" + mask + ")");
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isConnected(TVType other) {
        if (other == null) return false;
        return (mask & other.mask) != 0;
    }

    public boolean isConnected(Direction2D dir){
        return !hasEdge(dir);
    }

    public boolean hasEdge(Direction2D dir){
        return (switch (dir) {
            case UP -> C.U;
            case DOWN -> C.D;
            case LEFT -> C.L;
            case RIGHT -> C.R;
        } & mask) == 0;
    }

}

