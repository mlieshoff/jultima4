package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class CharacterCreationTest {
  @Test
  void sevenChoicesApplyDocumentedStatsAndVirtueScores() {
    CharacterCreation creation = new CharacterCreation(new Random(42));
    int strength = 15, dexterity = 15, intelligence = 15;
    int[] scores = {50, 50, 50, 50, 50, 50, 50, 50};
    Virtue last = null;
    for (int i = 0; i < 7; i++) {
      last = creation.option(i % 2);
      strength += last.strength;
      dexterity += last.dexterity;
      intelligence += last.intelligence;
      scores[last.ordinal()] += 5;
      creation.choose(i % 2);
    }
    assertTrue(creation.complete());
    assertEquals(7, creation.choices());
    assertEquals(last, creation.winner());
    int[] startX = {231, 83, 35, 59, 158, 105, 23, 186};
    int[] startY = {136, 105, 221, 44, 21, 183, 129, 171};
    assertEquals(startX[last.ordinal()], creation.startingX());
    assertEquals(startY[last.ordinal()], creation.startingY());
    assertEquals(strength, creation.strength());
    assertEquals(dexterity, creation.dexterity());
    assertEquals(intelligence, creation.intelligence());
    for (Virtue virtue : Virtue.values())
      assertEquals(scores[virtue.ordinal()], creation.score(virtue));
    assertThrows(IllegalStateException.class, () -> creation.choose(0));
  }

  @Test
  void invalidChoicesDoNotChangeState() {
    CharacterCreation creation = new CharacterCreation(new Random(42));
    assertThrows(IllegalArgumentException.class, () -> creation.choose(2));
    assertThrows(IllegalStateException.class, creation::winner);
    assertThrows(IllegalStateException.class, creation::startingX);
    assertEquals(0, creation.choices());
    assertEquals(15, creation.strength());
  }

  @Test
  void publishedStatTablesArePreserved() {
    assertEquals(3, Virtue.HONESTY.intelligence);
    assertEquals(3, Virtue.COMPASSION.dexterity);
    assertEquals(3, Virtue.VALOR.strength);
    assertEquals(
        0, Virtue.HUMILITY.strength + Virtue.HUMILITY.dexterity + Virtue.HUMILITY.intelligence);
    assertEquals("Paladin", Virtue.HONOR.characterClass);
  }
}
