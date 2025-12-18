package net.mehvahdjukaar.vista.common.tv.connection;

import net.mehvahdjukaar.moonlight.api.util.math.Rect2D;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.common.tv.TVType;

public class TVConnectionHelper {
    public static void updateConnections(GridAccessor gridAccess, boolean placed) {

        Rect2D newRec = RectFinder.findMaxExpandedRect(gridAccess, Vec2i.ZERO, true);

        setStates(gridAccess, newRec);
    }

    public static void setStates(GridAccessor grid, Rect2D square) {
        int left = square.left();
        int bottom = square.y();
        int top = square.y() + square.height() - 1;
        int right = square.right();
//TODO: redstone
        for (int y = bottom; y <= top; y++) {
            for (int x = left; x <= right; x++) {
                boolean edgeUp = (y == top);
                boolean edgeDown = (y == bottom);
                boolean edgeLeft = (x == left);
                boolean edgeRight = (x == right);

                TVType conn = TVType.fromConnections(!edgeUp, !edgeDown, !edgeLeft, !edgeRight);
                // GridAccess is (left, top) → pass (col, row)
                grid.setAt(new Vec2i(x, y), conn);
            }
        }
    }
}
