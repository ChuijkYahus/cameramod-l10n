package net.mehvahdjukaar.vista.integration.computer_craft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.mehvahdjukaar.vista.common.wave_gate.WaveGateBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class WaveGatePeripheral implements IPeripheral {
    private final WaveGateBlockEntity tile;

    public WaveGatePeripheral(WaveGateBlockEntity tile) {
        this.tile = tile;
    }

    @Override
    public String getType() {
        return "wave_gate";
    }

    @LuaFunction
    public String getUrl() {
        return tile.getUrl();
    }

    @LuaFunction
    public void setUrl(String url) {
        tile.setUrl(url);
        tile.getLevel().sendBlockUpdated(tile.getBlockPos(),tile.getBlockState(),tile.getBlockState(),3);
    }


    @Override
    public boolean equals(@Nullable IPeripheral obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (WaveGatePeripheral) obj;
        return Objects.equals(this.tile, that.tile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tile);
    }

    @Override
    public String toString() {
        return "WaveGatePeripheral[" +
                "tile=" + tile + ']';

    }
}
