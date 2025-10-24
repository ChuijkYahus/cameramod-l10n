package net.mehvahdjukaar.vista.common;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

public final class TvHelper {

    // ---------- External API (bottom-left origin) ----------

    /**
     * Access cells relative to the clicked position.
     * Coordinate system: bottom-left origin at the clicked cell.
     * - (0, 0) is the clicked cell.
     * - X axis = left (columns) grows to the right.
     * - Y axis = bottom (rows) grows upward.
     */
    public interface GridAccess {
        /**
         * Get the cell at (left, bottom). Return null if absent/non-materialized.
         * Null is treated as OFF by the algorithm.
         */
        @Nullable
        TvConnection get(int left, int bottom);

        void set(int left, int bottom, @Nullable TvConnection state);
    }

    // ---------- Core algorithm ----------

    public static void updateConnections(GridAccess g) {
        Square sq = findBiggestSquare(g);
        if (sq != null) {
            // Use your existing setter/painting logic here:
            setStates(g, sq);
        }
    }

    /**
     * Find the largest ON square (all non-null cells) that CONTAINS (0,0),
     * using bottom-left square coordinates.
     * Returns (bottom, left, size), or null if (0,0) is OFF (null).
     */
    @Nullable
    public static Square findBiggestSquare(GridAccess g) {
        // If clicked cell is OFF => no square
        if (g.get(0, 0) == null) return null;

        Square best = new Square(0, 0, 1); // start from the 1x1 at (0,0)
        Set<Square> frontier = new HashSet<>();
        Set<Square> visited = new HashSet<>();
        frontier.add(best);
        visited.add(best);

        while (true) {
            Set<Square> next = new HashSet<>();
            for (Square sq : frontier) {
                int b = sq.bottom, l = sq.left, s = sq.size;
                // Try to grow to size s+1 in four ways that still contain (0,0)
                propose(g, visited, next, b, l - 1, s + 1, ExpandKind.UL); // up + left
                propose(g, visited, next, b, l, s + 1, ExpandKind.UR); // up + right
                propose(g, visited, next, b - 1, l - 1, s + 1, ExpandKind.DL); // down + left
                propose(g, visited, next, b - 1, l, s + 1, ExpandKind.DR); // down + right
            }
            if (next.isEmpty()) break;
            for (Square s : next) if (s.size > best.size) best = s;
            frontier = next;
        }

        return best;
    }

    // ---------- Helpers ----------

    private enum ExpandKind {UL, UR, DL, DR}

    private static void propose(
            GridAccess g, Set<Square> visited, Set<Square> next,
            int b, int l, int s, ExpandKind kind
    ) {
        // Must contain (0,0). With bottom-left origin:
        // y in [b, b+s-1], x in [l, l+s-1]
        if (!(b <= 0 && 0 < b + s && l <= 0 && 0 < l + s)) return;

        Square key = new Square(b, l, s);
        if (visited.contains(key)) return;

        if (newBorderIsOn(g, b, l, s, kind)) {
            visited.add(key);
            next.add(key);
        }
    }

    /**
     * Check only the newly added border cells for expansion to
     * (bottom=b, left=l, size=s). Any null cell is considered OFF => fail.
     * <p>
     * Bounds (bottom-left origin):
     * bottom = b
     * top    = b + s - 1
     * left   = l
     * right  = l + s - 1
     */
    private static boolean newBorderIsOn(GridAccess g, int b, int l, int s, ExpandKind kind) {
        int bottom = b, top = b + s - 1, left = l, right = l + s - 1;

        switch (kind) {
            case UL -> {
                // New top row inclusive; new left column excluding (top,left)
                for (int x = left; x <= right; x++) if (g.get(x, top) == null) return false;
                for (int y = bottom; y < top; y++) if (g.get(left, y) == null) return false;
                return true;
            }
            case UR -> {
                // New top row excluding (top,right); new right column inclusive
                for (int x = left; x < right; x++) if (g.get(x, top) == null) return false;
                for (int y = bottom; y <= top; y++) if (g.get(right, y) == null) return false;
                return true;
            }
            case DL -> {
                // New bottom row excluding (bottom,left); new left column inclusive
                for (int x = left + 1; x <= right; x++) if (g.get(x, bottom) == null) return false;
                for (int y = bottom; y <= top; y++) if (g.get(left, y) == null) return false;
                return true;
            }
            case DR -> {
                // New bottom row excluding (bottom,right); new right column inclusive
                for (int x = left; x < right; x++) if (g.get(x, bottom) == null) return false;
                for (int y = bottom; y <= top; y++) if (g.get(right, y) == null) return false;
                return true;
            }
        }
        return false;
    }

    /**
     * A square described by its bottom-left corner (bottom, left) and side length.
     * Coordinates are relative to the clicked cell at (0,0) with bottom-left origin.
     */
    public record Square(int bottom, int left, int size) {
    }

    /**
     * Compute the largest ON square containing (0,0) and assign directional states
     * (UP, DOWN, LEFT, RIGHT, CENTER, and corner variants) to each cell inside it.
     * Does nothing if no valid square is found (i.e., origin is null/OFF).
     */
    public static void setStates(GridAccess grid, Square square) {
        int left = square.left;
        int bottom = square.bottom;
        int top = square.bottom + square.size - 1;
        int right = left + square.size - 1;
//TODO: redstone
        for (int y = bottom; y <= top; y++) {
            for (int x = left; x <= right; x++) {
                boolean edgeUp = (y == top);
                boolean edgeDown = (y == bottom);
                boolean edgeLeft = (x == left);
                boolean edgeRight = (x == right);

                TvConnection conn = TvConnection.get(!edgeUp, !edgeDown, !edgeLeft, !edgeRight);
                // GridAccess is (left, top) → pass (col, row)
                grid.set(x, y, conn);
            }
        }
    }

}
