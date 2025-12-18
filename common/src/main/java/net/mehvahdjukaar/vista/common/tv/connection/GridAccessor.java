package net.mehvahdjukaar.vista.common.tv.connection;


import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.common.tv.TVType;
import org.jetbrains.annotations.Nullable;

public interface GridAccessor {
    @Nullable
    TVType getAt(Vec2i pos);

    void setAt(Vec2i pos, @Nullable TVType type);
}

