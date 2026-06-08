package net.mehvahdjukaar.vista.common.connection;

import net.mehvahdjukaar.vista.common.tv.PowerState;
import org.jetbrains.annotations.Nullable;

public record GridTile(@Nullable ConnectionType type, boolean hasBe, PowerState powerState) {

    public static final GridTile EMPTY = new GridTile(null, false, PowerState.OFF);

    public static GridTile of(ConnectionType type, PowerState power) {
        return new GridTile(type, false, power);
    }

    public static GridTile ofBe(ConnectionType type, PowerState power) {
        return new GridTile(type, true, power);
    }
}
