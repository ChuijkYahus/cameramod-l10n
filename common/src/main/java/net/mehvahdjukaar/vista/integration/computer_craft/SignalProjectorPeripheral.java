package net.mehvahdjukaar.vista.integration.computer_craft;

import dan200.computercraft.api.lua.LuaFunction;
import dan200.computercraft.api.peripheral.IPeripheral;
import net.mehvahdjukaar.vista.common.projector.SignalProjectorBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class SignalProjectorPeripheral implements IPeripheral {
    private final SignalProjectorBlockEntity tile;

    public SignalProjectorPeripheral(SignalProjectorBlockEntity tile) {
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
    public void setUrl(String url) {
        tile.setUrl(url);
    }


    @Override
    public boolean equals(@Nullable IPeripheral obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (SignalProjectorPeripheral) obj;
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
