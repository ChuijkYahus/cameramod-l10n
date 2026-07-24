package net.mehvahdjukaar.vista.integration.computer_craft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.mehvahdjukaar.moonlight.api.util.math.EntityAngles;
import net.mehvahdjukaar.vista.common.view_finder.ViewFinderBlockEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

import java.util.Objects;

public class ViewFinderPeripheral implements IPeripheral {
    private final ViewFinderBlockEntity tile;

    public ViewFinderPeripheral(ViewFinderBlockEntity tile) {
        this.tile = tile;
    }
    @LuaFunction
    public float getYaw() {
        Quaternionf orientation = tile.getLocalOrientation(1);
        EntityAngles angles = EntityAngles.fromQuaternion(orientation);
        return angles.yaw();
    }

    @LuaFunction
    public void setYaw(double value) {
        Quaternionf orientation = tile.getLocalOrientation(1);
        EntityAngles angles = EntityAngles.fromQuaternion(orientation);
        angles = EntityAngles.of(angles.pitch(), (float) value); //both deg

        tile.setLocalOrientation(angles.toQuaternion());
        tile.syncToClients();
    }

    @LuaFunction
    public float getPitch() {
        Quaternionf orientation = tile.getLocalOrientation(1);
        EntityAngles angles = EntityAngles.fromQuaternion(orientation);
        return angles.pitch(); //deg
    }

    @LuaFunction
    public void setPitch(double value) {
        Quaternionf orientation = tile.getLocalOrientation(1);
        EntityAngles angles = EntityAngles.fromQuaternion(orientation);
        angles = EntityAngles.of((float) value, angles.yaw()); //both deg

        tile.setLocalOrientation(angles.toQuaternion());

        tile.syncToClients();
    }

    @LuaFunction
    public void setZoom(int zoom) {
        byte power = (byte) Math.clamp(zoom, 1, ViewFinderBlockEntity.MAX_ZOOM);
        this.tile.setZoomLevel(power);
        this.tile.syncToClients();
    }

    @LuaFunction
    public int getZoom() {
        return this.tile.getZoomLevel();
    }

    @LuaFunction
    public void setLocked(boolean locked) {
        this.tile.setLocked(locked);
    }

    @LuaFunction
    public boolean isLocked() {
        return tile.isLocked();
    }

    @Override
    public String getType() {
        return "view_finder";
    }

    @Override
    public boolean equals(@Nullable IPeripheral obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (ViewFinderPeripheral) obj;
        return this.tile == that.tile;
    }

    @Override
    public int hashCode() {
        return Objects.hash(tile);
    }

    @Override
    public String toString() {
        return "ViewFinderPeripheral[" +
                "tile=" + tile + ']';
    }
}