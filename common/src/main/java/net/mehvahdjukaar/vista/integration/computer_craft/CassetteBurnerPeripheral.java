package net.mehvahdjukaar.vista.integration.computer_craft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class CassetteBurnerPeripheral implements IPeripheral {
    private final CassetteBurnerBlockEntity tile;

    public CassetteBurnerPeripheral(CassetteBurnerBlockEntity tile) {
        this.tile = tile;
    }

    @Override
    public String getType() {
        return "cassette_burner";
    }

    @LuaFunction
    public String getUrl() {
        return tile.getUrl();
    }

    @LuaFunction
    public boolean hasCassette() {
        return !tile.isEmpty();
    }

    @LuaFunction
    public boolean burnCassette(String url) {
        return tile.setUrl(url);
    }


    @Override
    public boolean equals(@Nullable IPeripheral obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (CassetteBurnerPeripheral) obj;
        return Objects.equals(this.tile, that.tile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tile);
    }

    @Override
    public String toString() {
        return "CassetteBurnerPeripheral[" +
                "tile=" + tile + ']';

    }
}
