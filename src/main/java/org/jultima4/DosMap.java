package org.jultima4;

/** Immutable DOS map; WORLD.MAP stores rows of 32x32 chunks, towns are linear. */
public final class DosMap {
  private final byte[] tiles;
  private final int size;
  private final boolean chunked;

  private DosMap(byte[] data, int size, boolean chunked) {
    if (data.length != size * size) {
      throw new IllegalArgumentException("Invalid map length");
    }
    tiles = data.clone();
    this.size = size;
    this.chunked = chunked;
  }

  public static DosMap world(byte[] data) {
    return new DosMap(data, 256, true);
  }

  public static DosMap town(byte[] data) {
    return new DosMap(data, 32, false);
  }

  public int size() {
    return size;
  }

  public int tile(int x, int y) {
    if (chunked) {
      x = Math.floorMod(x, size);
      y = Math.floorMod(y, size);
      return Byte.toUnsignedInt(tiles[(y / 32 * 8 + x / 32) * 1024 + y % 32 * 32 + x % 32]);
    }
    if (x < 0 || y < 0 || x >= size || y >= size) {
      return 126;
    }
    return Byte.toUnsignedInt(tiles[y * size + x]);
  }

  /** On-foot subset; terrain delays and damaging fields are not simulated yet. */
  public static boolean walkable(int tile) {
    return switch (tile) {
      case 3,
          4,
          5,
          6,
          7,
          9,
          10,
          11,
          12,
          14,
          22,
          23,
          25,
          26,
          27,
          28,
          29,
          30,
          60,
          62,
          63,
          67,
          68,
          70,
          71,
          73,
          74,
          76 ->
          true;
      default -> false;
    };
  }
}
