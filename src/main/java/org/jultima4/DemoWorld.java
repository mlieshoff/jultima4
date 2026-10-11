package org.jultima4;

import java.util.EnumSet;

/** Hand-authored demonstration island, not the original Britannia map. */
public final class DemoWorld {
    public static final int SIZE = 32;
    private static final int[][] SHRINES = {{5, 5}, {16, 5}, {26, 5}, {5, 16}, {26, 16}, {5, 26}, {16, 26}, {26, 26}};
    private final EnumSet<Virtue> visited = EnumSet.noneOf(Virtue.class);
    private int x = 16, y = 16, turns;
    private String message = "Seek the eight shrines. Press M beside one to meditate.";

    public int tile(int x, int y) {
        x = Math.floorMod(x, SIZE);
        y = Math.floorMod(y, SIZE);
        if (x < 2 || y < 2 || x > 29 || y > 29) {
            return 0;
        }
        for (int[] shrine : SHRINES) if (x == shrine[0] && y == shrine[1]) {
            return 0x1d;
        }
        if (x == 11 && y >= 8 && y <= 23 && y != 16) {
            return 0x0d;
        }
        return (x * 7 + y * 3) % 13 == 0 ? 6 : 4;
    }

    public void move(int dx, int dy) {
        if (Math.abs(dx) + Math.abs(dy) != 1) {
            throw new IllegalArgumentException("Move one cardinal step");
        }
        int nextX = Math.floorMod(x + dx, SIZE), nextY = Math.floorMod(y + dy, SIZE);
        turns++;
        if (tile(nextX, nextY) == 0 || tile(nextX, nextY) == 0x0d) {
            message = "Blocked!";
        } else {
            x = nextX;
            y = nextY;
            message = "Explore the demonstration island.";
        }
    }

    public void pass() {
        turns++;
        message = "Pass";
    }

    public void meditate() {
        turns++;
        for (int i = 0; i < SHRINES.length; i++) {
            if (Math.abs(x - SHRINES[i][0]) + Math.abs(y - SHRINES[i][1]) <= 1) {
                Virtue virtue = Virtue.values()[i];
                boolean added = visited.add(virtue);
                message = complete() ? "All eight shrines visited! Demo quest complete." :
                added ? "Meditation on " + virtue + " complete." : "This shrine has already been visited.";
                return;
            }
        }
        message = "No shrine nearby.";
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
    }

    public int turns() {
        return turns;
    }

    public int visitedCount() {
        return visited.size();
    }

    public boolean complete() {
        return visited.size() == 8;
    }

    public String message() {
        return message;
    }
}
