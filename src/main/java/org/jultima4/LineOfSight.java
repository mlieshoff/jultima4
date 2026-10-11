package org.jultima4;

import java.util.function.IntBinaryOperator;

/** Center-to-center grid rays. Blockers remain visible; two solid corners seal a diagonal. */
public final class LineOfSight {
    private LineOfSight() { }

    public static boolean opaque(int tile) {
        return switch (tile) {
            case 6, 8, 57, 58, 59, 73, 127 -> true;
            default -> false;
        };
    }

    public static boolean visible(IntBinaryOperator terrain, int fromX, int fromY, int toX, int toY) {
        int dx = Math.abs(toX - fromX), dy = Math.abs(toY - fromY);
        int stepX = Integer.compare(toX, fromX), stepY = Integer.compare(toY, fromY);
        int x = fromX, y = fromY, crossedX = 0, crossedY = 0;
        while (x != toX || y != toY) {
            long nextX = (1L + 2L * crossedX) * dy;
            long nextY = (1L + 2L * crossedY) * dx;
            if (nextX == nextY) {
                if (opaque(terrain.applyAsInt(x + stepX, y)) && opaque(terrain.applyAsInt(x, y + stepY))) return false;
                x += stepX; y += stepY; crossedX++; crossedY++;
            } else if (nextX < nextY) {
                x += stepX; crossedX++;
            } else {
                y += stepY; crossedY++;
            }
            if (x == toX && y == toY) return true;
            if (opaque(terrain.applyAsInt(x, y))) return false;
        }
        return true;
    }
}
