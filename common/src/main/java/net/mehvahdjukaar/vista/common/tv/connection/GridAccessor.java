package net.mehvahdjukaar.vista.common.tv.connection;


import net.mehvahdjukaar.moonlight.api.util.math.Rect2D;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.common.tv.TVType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface GridAccessor {
    @NotNull
    GridTile getAt(Vec2i pos);

    void setAt(Vec2i pos, @Nullable TVType type, boolean setPower);

    default void setAt(Vec2i pos, @Nullable TVType type) {
        this.setAt(pos, type, false);
    }

    //Here set block entity?
    default void setSquare(Rect2D square) {
        int left = square.left();
        int bottom = square.y();
        int top = square.y() + square.height() - 1;
        int right = square.right();

        boolean hasStrongPowerAnywhere = false;
        var iter = square.iteratePoints();
        while (iter.hasNext()) {
            Vec2i p = iter.next();
            GridTile t = this.getAt(p);
            if (t.powerState().isStrong()) {
                hasStrongPowerAnywhere = true;
                break;
            }
        }


        for (int y = bottom; y <= top; y++) {
            for (int x = left; x <= right; x++) {
                boolean edgeUp = (y == top);
                boolean edgeDown = (y == bottom);
                boolean edgeLeft = (x == left);
                boolean edgeRight = (x == right);

                TVType conn = TVType.fromConnections(!edgeUp, !edgeDown, !edgeLeft, !edgeRight);
                // GridAccess is (left, top) → pass (col, row)
                this.setAt(new Vec2i(x, y), conn, hasStrongPowerAnywhere);
            }
        }
    }
}

