package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EgaPictureTest {
  private byte[] codes(int... codes) {
    byte[] data = new byte[(codes.length * 12 + 7) / 8];
    for (int i = 0; i < codes.length; i++)
      for (int bit = 0; bit < 12; bit++) {
        if ((codes[i] & 1 << (11 - bit)) != 0) {
          int position = i * 12 + bit;
          data[position / 8] |= (byte) (1 << (7 - position % 8));
        }
      }
    return data;
  }

  @Test
  void decodesLiteralAndHashedDictionaryCodesIncludingTheNewWordCase() {
    assertArrayEquals(new byte[] {65, 66, 65, 66}, EgaPicture.decompress(codes(65, 66, 1121), 4));
    assertArrayEquals(new byte[] {0, 0, 0}, EgaPicture.decompress(codes(0, 509), 3));
    assertArrayEquals(new byte[] {65, 65, 65}, EgaPicture.decompress(codes(65, 1105), 3));
  }

  @Test
  void rejectsTruncatedInvalidAndOversizedStreams() {
    assertThrows(IllegalArgumentException.class, () -> EgaPicture.decompress(new byte[0], 1));
    assertThrows(IllegalArgumentException.class, () -> EgaPicture.decompress(codes(300), 1));
    assertThrows(IllegalArgumentException.class, () -> EgaPicture.decompress(codes(65, 300), 3));
    assertThrows(IllegalArgumentException.class, () -> EgaPicture.decompress(codes(65), 2));
    assertThrows(IllegalArgumentException.class, () -> EgaPicture.decompress(codes(65, 66), 1));
  }

  @Test
  void originalPicturesDecodeFullyWithValidCoordinates() throws Exception {
    assertEquals(0x5555ffff, EgaPicture.load("TITLE").rgba(0, 110));
    assertEquals(0xaa0000ff, EgaPicture.load("GYPSY").rgba(0, 0));
    assertEquals(0x00aa00ff, EgaPicture.load("WAGON").rgba(0, 0));
    for (String name : new String[] {"TITLE", "GYPSY", "WAGON"}) {
      EgaPicture image = EgaPicture.load(name);
      assertEquals(255, image.rgba(0, 0) & 255);
      assertEquals(255, image.rgba(319, 199) & 255);
      assertThrows(IllegalArgumentException.class, () -> image.rgba(320, 0));
    }
  }
}
