package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TileAnimationTest {
  @Test
  void monsterFramesStayInsideTheirOriginalFourTileGroupsAndLoop() {
    for (int base : new int[] {192, 196, 212}) {
      assertEquals(base, TileAnimation.frame(base, 0));
      assertEquals(base + 1, TileAnimation.frame(base, 0.21));
      assertEquals(base + 2, TileAnimation.frame(base, 0.41));
      assertEquals(base + 3, TileAnimation.frame(base, 0.61));
      assertEquals(base, TileAnimation.frame(base, 0.81));
      assertEquals(base, TileAnimation.frame(base + 3, 0.21));
    }
  }

  @Test
  void characterAndNpcPairsAnimateButDirectionalAndStaticTilesDoNot() {
    for (int base : new int[] {32, 46, 82, 94, 136, 142}) {
      assertEquals(base + 1, TileAnimation.frame(base, 0.21));
      assertEquals(base, TileAnimation.frame(base, 0.41));
    }
    assertEquals(80, TileAnimation.frame(80, 0.21));
    assertEquals(81, TileAnimation.frame(80, 0.51));
    for (int tile : new int[] {4, 8, 16, 17, 18, 19, 20, 21, 31, 56, 77, 79}) {
      assertEquals(tile, TileAnimation.frame(tile, 2.21));
    }
  }

  @Test
  void scrollingWaterFieldsAndLavaHaveSeparateSlotsAndLoopWithinSixteenPixels() {
    int[] tiles = {0, 1, 2, 68, 69, 70, 71, 76};
    for (int i = 0; i < tiles.length; i++) assertEquals(i, TileAnimation.scrollSlot(tiles[i]));
    assertEquals(-1, TileAnimation.scrollSlot(4));
    assertEquals(0, TileAnimation.scrollOffset(0));
    assertEquals(15, TileAnimation.scrollOffset(1.51));
    assertEquals(0, TileAnimation.scrollOffset(1.61));
    assertThrows(IllegalArgumentException.class, () -> TileAnimation.frame(256, 0));
    assertThrows(IllegalArgumentException.class, () -> TileAnimation.frame(4, -1));
    assertThrows(IllegalArgumentException.class, () -> TileAnimation.scrollOffset(Double.NaN));
  }
}
