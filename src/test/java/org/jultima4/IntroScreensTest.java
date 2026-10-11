package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class IntroScreensTest {
  @Test
  void automaticSequenceShowsSignatureThenOriginThenTitleWithoutStartingACharacter() {
    CharacterScreens screens = new CharacterScreens(new Random(17));
    assertEquals(CharacterScreens.Stage.SIGNATURE, screens.stage());
    screens.update(3.99);
    assertEquals(CharacterScreens.Stage.SIGNATURE, screens.stage());
    screens.update(0.02);
    assertEquals(CharacterScreens.Stage.ORIGIN, screens.stage());
    assertEquals(0.01, screens.elapsed(), 0.0001);
    screens.update(2);
    assertEquals(CharacterScreens.Stage.TITLE, screens.stage());
    screens.update(100);
    assertEquals(CharacterScreens.Stage.TITLE, screens.stage());
    assertThrows(IllegalStateException.class, screens::creation);
  }

  @Test
  void skipsReplaysAndLargeDeltasAreSafe() {
    CharacterScreens screens = new CharacterScreens(new Random(17));
    screens.advanceIntro();
    assertEquals(CharacterScreens.Stage.ORIGIN, screens.stage());
    screens.advanceIntro();
    assertEquals(CharacterScreens.Stage.TITLE, screens.stage());
    assertThrows(IllegalStateException.class, screens::advanceIntro);
    screens.intro();
    assertEquals(0, screens.elapsed());
    screens.update(20);
    assertEquals(CharacterScreens.Stage.TITLE, screens.stage());
    screens.intro();
    screens.title();
    assertEquals(CharacterScreens.Stage.TITLE, screens.stage());
    assertThrows(IllegalArgumentException.class, () -> screens.update(-1));
    assertThrows(IllegalArgumentException.class, () -> screens.update(Double.NaN));
  }

  @Test
  void originalSignatureHasOrderedBoundedImmutableStrokes() throws Exception {
    LordBritishSignature signature = LordBritishSignature.load();
    assertEquals(266, signature.points().size());
    assertTrue(
        signature.points().stream()
            .allMatch(p -> p.x() >= 0 && p.x() <= 129 && p.y() >= 0 && p.y() <= 16));
    assertThrows(UnsupportedOperationException.class, () -> signature.points().clear());
    assertThrows(IllegalArgumentException.class, () -> LordBritishSignature.decode(new byte[0]));
    assertThrows(IllegalArgumentException.class, () -> LordBritishSignature.decode(new byte[533]));
    byte[] invalid = new byte[533];
    invalid[0] = 1;
    invalid[1] = 1;
    assertThrows(IllegalArgumentException.class, () -> LordBritishSignature.decode(invalid));
    byte[] unterminated = new byte[533];
    for (int i = 0; i < 532; i += 2) {
      unterminated[i] = 0x4d;
      unterminated[i + 1] = (byte) 0xbf;
    }
    unterminated[532] = 1;
    assertThrows(IllegalArgumentException.class, () -> LordBritishSignature.decode(unterminated));
  }
}
