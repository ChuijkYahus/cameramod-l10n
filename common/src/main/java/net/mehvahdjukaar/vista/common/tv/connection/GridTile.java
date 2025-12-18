package net.mehvahdjukaar.vista.common.tv.connection;

import net.mehvahdjukaar.vista.common.tv.PowerState;
import net.mehvahdjukaar.vista.common.tv.TVBlock;
import net.mehvahdjukaar.vista.common.tv.TVType;
import org.jetbrains.annotations.Nullable;

public record GridTile(@Nullable TVType type, boolean hasBe, PowerState powerState) {

    public static final GridTile EMPTY = new GridTile(null, false, PowerState.OFF);

    public static GridTile of(TVType type, PowerState power) {
        return new GridTile(type, false, power);
    }

    public static GridTile ofBe(TVType type, PowerState power) {
        return new GridTile(type, true, power);
    }
}
