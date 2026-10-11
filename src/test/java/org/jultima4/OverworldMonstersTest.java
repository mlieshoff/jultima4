package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class OverworldMonstersTest {
  private DosMap map(int tile) {
    byte[] terrain = new byte[65536];
    Arrays.fill(terrain, (byte) tile);
    return DosMap.world(terrain);
  }

  @Test
  void landMonstersSpawnPursueAndWrapAcrossTheWorldEdge() {
    OverworldMonsters monsters = new OverworldMonsters(new Random(17));
    DosMap world = map(4);
    for (int i = 0; i < 11; i++) assertNull(monsters.advance(world, 0, 0));
    assertTrue(monsters.encounters().isEmpty());
    monsters.advance(world, 0, 0);
    assertEquals(1, monsters.encounters().size());
    OverworldMonsters.Encounter encounter = monsters.encounters().getFirst();
    assertSame(encounter, monsters.at(encounter.x() - 256, encounter.y() + 256));
    assertTrue(encounter.count() >= 1 && encounter.count() <= 2);
    assertThrows(UnsupportedOperationException.class, () -> monsters.encounters().clear());
    OverworldMonsters.Encounter attacking = null;
    for (int i = 0; i < 30 && attacking == null; i++) attacking = monsters.advance(world, 0, 0);
    assertNotNull(attacking);
    monsters.remove(attacking);
    assertNull(monsters.at(attacking.x(), attacking.y()));
  }

  @Test
  void monstersCannotSpawnOnWater() {
    OverworldMonsters monsters = new OverworldMonsters(new Random(17));
    for (int i = 0; i < 120; i++) assertNull(monsters.advance(map(0), 50, 50));
    assertTrue(monsters.encounters().isEmpty());
  }

  @Test
  void distantMonstersDespawn() {
    OverworldMonsters monsters = new OverworldMonsters(new Random(17));
    DosMap world = map(4);
    for (int i = 0; i < 12; i++) monsters.advance(world, 50, 50);
    assertFalse(monsters.encounters().isEmpty());
    monsters.advance(world, 150, 150);
    assertTrue(monsters.encounters().isEmpty());
  }
}
