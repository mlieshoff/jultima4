package org.jultima4;

/** Decodes DOS SHAPES.EGA: 256 tiles, 16x16 pixels, high nibble first. */
public final class EgaTiles {
    private static final int[] PALETTE = {
        0x000000ff, 0x0000aaff, 0x00aa00ff, 0x00aaaaff,
        0xaa0000ff, 0xaa00aaff, 0xaa5500ff, 0xaaaaaaff,
        0x555555ff, 0x5555ffff, 0x55ff55ff, 0x55ffffff,
        0xff5555ff, 0xff55ffff, 0xffff55ff, 0xffffffff
    };
    private final byte[] data;

    public EgaTiles(byte[] data) {
        if (data.length != 32768) throw new IllegalArgumentException("SHAPES.EGA must contain 32768 bytes");
        this.data = data.clone();
    }

    static int palette(int color) { return PALETTE[color]; }

    public int rgba(int tile, int x, int y) {
        if (tile < 0 || tile > 255 || x < 0 || x > 15 || y < 0 || y > 15) {
            throw new IllegalArgumentException("Invalid tile or pixel coordinate");
        }
        int packed = Byte.toUnsignedInt(data[tile * 128 + y * 8 + x / 2]);
        int color = x % 2 == 0 ? packed >>> 4 : packed & 15;
        return palette(color);
    }
}
