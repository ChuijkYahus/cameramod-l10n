package net.mehvahdjukaar.vista.common;


import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum TvConnection implements StringRepresentable {
    NONE(0),
    CENTER(F.U | F.D | F.L | F.R),
    TOP(F.D | F.L | F.R),
    BOTTOM(F.U | F.L | F.R),
    LEFT(F.U | F.D | F.R),
    RIGHT(F.U | F.D | F.L),
    TOP_LEFT(F.D | F.R),
    TOP_RIGHT(F.D | F.L),
    BOTTOM_LEFT(F.U | F.R),
    BOTTOM_RIGHT(F.U | F.L);

    private final int mask;

    TvConnection(int mask) {
        this.mask = mask;
        F.LUT[mask] = this;
    }

    private static class F {
        private static final int U = 1, D = 2, L = 4, R = 8;
        private static final TvConnection[] LUT = new TvConnection[16];
    }

    public static TvConnection get(boolean up, boolean down, boolean left, boolean right) {
        int mask = (up ? F.U : 0) | (down ? F.D : 0) | (left ? F.L : 0) | (right ? F.R : 0);
        TvConnection c = F.LUT[mask];
        if (c != null) return c;
        throw new IllegalArgumentException("Invalid pattern for square tiling (mask=" + mask + ")");
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isConnected(TvConnection other) {
        if (other == null) return false;
        return (mask & other.mask) != 0;
    }
}

