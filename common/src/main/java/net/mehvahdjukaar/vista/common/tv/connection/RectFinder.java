package net.mehvahdjukaar.vista.common.tv.connection;

import net.mehvahdjukaar.moonlight.api.util.math.Direction2D;
import net.mehvahdjukaar.moonlight.api.util.math.Rect2D;
import net.mehvahdjukaar.moonlight.api.util.math.Vec2i;
import net.mehvahdjukaar.vista.common.tv.TVType;

import java.util.*;

public final class RectFinder {

    public static Rect2D findMaxRect(
            GridAccessor grid,
            Vec2i from,
            boolean squareOnly
    ) {
        return findMaxRects(grid, from, squareOnly)
                .stream()
                .max(Comparator.comparingInt(Rect2D::getArea))
                .orElseThrow();
    }

    private static Set<Rect2D> findMaxRects(
            GridAccessor grid,
            Vec2i from,
            boolean squareOnly
    ) {

        Rect2D start = new Rect2D(from.x(), from.y(), 1, 1);

        Set<Rect2D> visited = new HashSet<>();
        Set<Rect2D> maximalRects = new HashSet<>();
        Deque<Rect2D> stack = new ArrayDeque<>();

        stack.push(start);
        maximalRects.add(start);

        while (!stack.isEmpty()) {
            Rect2D r = stack.pop();
            if (!visited.add(r)) continue;

            for (Direction2D d : Direction2D.values()) {

                if (!couldBeConnectedToward(grid, r, d, d)) continue;
                Rect2D expandedRect = r.expandToward(d);

                if (couldBeConnectedToward(grid, expandedRect, d, d.getOpposite())) {
                    stack.push(expandedRect);
                }
            }

            // maximal = no further expansion possible
            if (!squareOnly || r.isSquare()) {
                maximalRects.add(r);
            }
        }

        return maximalRects;
    }

    private static boolean couldBeConnectedToward(GridAccessor grid, Rect2D currentRect, Direction2D d, Direction2D d2) {
        var edgeLocs = currentRect.iterateEdge(d);

        while (edgeLocs.hasNext()) {
            Vec2i p = edgeLocs.next();
            TVType t = grid.getAt(p);
            if (t == null) return false;

            if (t.hasEdge(d2)) {
                return false;
            }
        }
        return true;
    }


    public static Rect2D findMaxExpandedRect(GridAccessor grid, Vec2i from, boolean squareOnly) {
        return findMaxExpandedRects(grid, from, squareOnly)
                .stream()
                .max(Comparator.comparingInt(Rect2D::getArea))
                .orElse(new Rect2D(0, 0, 1, 1));
    }

    private static Set<Rect2D> findMaxExpandedRects(
            GridAccessor grid,
            Vec2i from,
            boolean squareOnly
    ) {
        Rect2D start = new Rect2D(from.x(), from.y(), 1, 1);

        Set<ExpandState> visited = new HashSet<>();
        Set<Rect2D> maximalRects = new HashSet<>();
        Deque<ExpandState> stack = new ArrayDeque<>();

        stack.push(new ExpandState(start, null));

        while (!stack.isEmpty()) {
            ExpandState s = stack.pop();
            if (!visited.add(s)) continue;

            for (Direction2D d : Direction2D.values()) {
                for (ExpandState next : expand(grid, s, d)) {
                    stack.push(next);
                }
            }

            //validate solution
            if ((!squareOnly || s.selection.isSquare()) && s.selection.contains(s.touchedRect)) {
                maximalRects.add(s.selection);
            }
        }

        return maximalRects;
    }

    private static List<ExpandState> expand(
            GridAccessor grid,
            ExpandState state,
            Direction2D d
    ) {
        Rect2D nextRect = state.selection.expandToward(d);
        List<ExpandState> results = new ArrayList<>();

        Rect2D touched = state.touchedRect;

        var edge = nextRect.iterateEdge(d);

        while (edge.hasNext()) {
            Vec2i p = edge.next();
            TVType t = grid.getAt(p);

            if (t == null) {
                return List.of(); // no expansion possible
            }

            if (t != TVType.SINGLE) {

                // If we already chose a selection, it must match
                if (touched != null) {
                    if (!touched.contains(p)) {
                        return List.of(); // cannot expand this way
                    }
                } else {
                    //bug here, wont work if it has multiple owners
                    touched = findMaxRect(grid, p, false);
                }
            }
        }

        // No new selection touched OR it matches existing
        results.add(new ExpandState(nextRect, touched));
        return results;
    }

    record ExpandState(Rect2D selection, Rect2D touchedRect) {
    }
}
