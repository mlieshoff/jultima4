package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CombatMapTest {
  @Test
  void conLayoutUsesSeparateSpawnArraysAndUnsignedTerrain() {
    byte[] data = new byte[192];
    data[0] = 3;
    data[16] = 2;
    data[32] = 5;
    data[40] = 7;
    data[64 + 2 * 11 + 3] = (byte) 200;
    CombatMap map = CombatMap.decode("test", data);
    assertEquals(new CombatMap.Position(3, 2), map.monsterStarts().getFirst());
    assertEquals(new CombatMap.Position(5, 7), map.avatarStart());
    assertEquals(200, map.tile(3, 2));
    assertEquals(126, map.tile(-1, 0));
    data[64 + 2 * 11 + 3] = 4;
    byte[] copy = map.terrain();
    Arrays.fill(copy, (byte) 4);
    assertEquals(200, map.tile(3, 2));
  }

  @Test
  void malformedLengthsAndCoordinatesAreRejected() {
    assertThrows(IllegalArgumentException.class, () -> CombatMap.decode("bad", new byte[191]));
    byte[] data = new byte[192];
    data[16] = 11;
    assertThrows(IllegalArgumentException.class, () -> CombatMap.decode("bad", data));
  }

  @Test
  void terrainSelectionMatchesTheDecompiledCombatTable() {
    assertEquals("MARSH", CombatMap.forTerrain(3));
    assertEquals("BRUSH", CombatMap.forTerrain(5));
    assertEquals("FOREST", CombatMap.forTerrain(6));
    assertEquals("HILL", CombatMap.forTerrain(7));
    assertEquals("DUNGEON", CombatMap.forTerrain(22));
    assertEquals("BRICK", CombatMap.forTerrain(62));
    for (int tile : new int[] {23, 25, 26, 63}) assertEquals("BRIDGE", CombatMap.forTerrain(tile));
    assertEquals("GRASS", CombatMap.forTerrain(4));
  }

  @Test
  void everySuppliedLandBattlefieldSupportsTwoReachableMonsters() throws Exception {
    var maps = CombatMap.load();
    assertEquals(8, maps.size());
    for (CombatMap map : maps.values()) {
      Avatar avatar = new Avatar(200, 15, 15, Avatar.Weapon.STAFF, 128, 0);
      Battle battle = new Battle(map, avatar, MonsterType.ORC, 2, new Random(42));
      assertEquals(2, battle.enemies().size(), map.name());
      assertTrue(DosMap.walkable(map.tile(battle.x(), battle.y())), map.name());
    }
  }
}
