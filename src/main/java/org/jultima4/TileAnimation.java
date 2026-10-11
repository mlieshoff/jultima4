package org.jultima4;

/** Presentation-only timing for the original EGA frame groups and scrolling tiles. */
public final class TileAnimation {
    private TileAnimation() {
    }

    public static int frame(int tile, double seconds) {
        validate(tile, seconds);
        int base, count;
        if (tile >= 32 && tile <= 47 || tile >= 80 && tile <= 95 || tile >= 136 && tile <= 143) {
            base = tile / 2 * 2;
            count = 2;
        } else if (tile >= 144) {
            base = tile / 4 * 4;
            count = 4;
        } else {
            return tile;
        }
        double interval = tile == 80 || tile == 81 ? 0.5 : 0.2;
        return base + (int) ((seconds / interval + tile - base) % count);
    }

    public static int scrollSlot(int tile) {
        if (tile >= 0 && tile <= 2) {
            return tile;
        }
        if (tile >= 68 && tile <= 71) {
            return tile - 65;
        }
        return tile == 76 ? 7 : -1;
    }

    public static int scrollOffset(double seconds) {
        if (!Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Invalid animation time");
        }
        return (int) (seconds / 0.1 % 16);
    }

    private static void validate(int tile, double seconds) {
        if (tile < 0 || tile > 255 || !Double.isFinite(seconds) || seconds < 0) {
            throw new IllegalArgumentException("Invalid tile or animation time");
        }
    }
}
