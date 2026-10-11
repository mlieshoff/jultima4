package org.jultima4;

import java.util.Random;

/** Initial land-monster subset, using original tile IDs, base HP and experience. */
public enum MonsterType {
  ORC("Orc", 192, 80, 6),
  SKELETON("Skeleton", 196, 48, 4),
  HEADLESS("Headless", 212, 64, 5);
  public final String displayName;
  public final int tile, baseHealth, experience;

  MonsterType(String name, int tile, int health, int experience) {
    displayName = name;
    this.tile = tile;
    baseHealth = health;
    this.experience = experience;
  }

  int initialHealth(Random random) {
    return Math.max(24, random.nextInt(baseHealth) | baseHealth / 2);
  }

  int damage(Random random) {
    int roll = random.nextInt(baseHealth / 4);
    return (roll >> 4) * 10 + roll % 10;
  }
}
