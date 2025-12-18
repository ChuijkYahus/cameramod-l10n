package net.mehvahdjukaar.vista.common.tv;

import net.minecraft.util.StringRepresentable;

import java.util.Locale;

public enum PowerState implements StringRepresentable {
    OFF,
    DIRECT,
    INDIRECT;

    public static PowerState direct(boolean powered) {
        return powered ? DIRECT : OFF;
    }

    public static PowerState indirect(PowerState existing, boolean indirectPower) {
        if(existing == DIRECT) return DIRECT;
        else return indirectPower ? INDIRECT : OFF;
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }

    public boolean isOn() {
        return this != OFF;
    }

    public boolean isStrong() {
        return this == DIRECT;
    }
}