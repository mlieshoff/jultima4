package org.jultima4;

/** Tables from NEWGAME $680C, $6814 and $681C. */
public enum Virtue {
  HONESTY("Mage", 0, 0, 3),
  COMPASSION("Bard", 0, 3, 0),
  VALOR("Fighter", 3, 0, 0),
  JUSTICE("Druid", 0, 1, 1),
  SACRIFICE("Tinker", 1, 1, 0),
  HONOR("Paladin", 1, 0, 1),
  SPIRITUALITY("Ranger", 1, 1, 1),
  HUMILITY("Shepherd", 0, 0, 0);

  public final String characterClass;
  public final int strength, dexterity, intelligence;

  Virtue(String characterClass, int strength, int dexterity, int intelligence) {
    this.characterClass = characterClass;
    this.strength = strength;
    this.dexterity = dexterity;
    this.intelligence = intelligence;
  }
}
