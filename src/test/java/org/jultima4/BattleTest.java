package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class BattleTest {
  private static class MaximumRoll extends Random {
    @Override
    public int nextInt(int bound) {
      return bound - 1;
    }
  }

  private static class MinimumRoll extends Random {
    @Override
    public int nextInt(int bound) {
      return 0;
    }
  }

  private CombatMap arena(byte[] terrain, int avatarX, int avatarY, int enemyX, int enemyY) {
    List<CombatMap.Position> positions = new ArrayList<>();
    positions.add(new CombatMap.Position(enemyX, enemyY));
    for (int i = 1; i < 16; i++) positions.add(new CombatMap.Position(i % 11, i / 11));
    return new CombatMap("test", terrain, positions, new CombatMap.Position(avatarX, avatarY));
  }

  private byte[] grass() {
    byte[] tiles = new byte[121];
    Arrays.fill(tiles, (byte) 4);
    return tiles;
  }

  private Avatar hero(int hp, int strength, Avatar.Weapon weapon) {
    return new Avatar(hp, strength, 20, weapon, 128, 0);
  }

  @Test
  void lethalMeleeAttackAwardsXpOnceAndDoesNotAllowRetaliation() {
    Avatar hero = hero(200, 200, Avatar.Weapon.SWORD);
    Battle battle =
        new Battle(arena(grass(), 5, 5, 5, 4), hero, MonsterType.ORC, 1, new MaximumRoll());
    battle.attack(0, -1);
    assertEquals(Battle.Outcome.VICTORY, battle.outcome());
    assertEquals(6, hero.experience());
    assertEquals(200, hero.health());
    assertTrue(battle.enemies().isEmpty());
    assertThrows(IllegalStateException.class, () -> battle.attack(0, -1));
    assertEquals(6, hero.experience());
  }

  @Test
  void missStillConsumesATurnAndSurvivingEnemyCanAct() {
    Avatar hero = hero(200, 15, Avatar.Weapon.STAFF);
    Battle battle =
        new Battle(arena(grass(), 5, 5, 5, 4), hero, MonsterType.ORC, 1, new MinimumRoll());
    int health = battle.enemies().getFirst().health();
    battle.attack(0, -1);
    assertEquals(1, battle.rounds());
    assertEquals(health, battle.enemies().getFirst().health());
    assertTrue(battle.message().contains("Missed"));
    assertTrue(battle.message().contains("misses"));
    assertEquals(0, hero.experience());
  }

  @Test
  void enemiesAdvanceOnlyOncePerActionAndCanDefeatTheAvatar() {
    Avatar hero = hero(1, 15, Avatar.Weapon.STAFF);
    Battle battle =
        new Battle(arena(grass(), 5, 5, 5, 3), hero, MonsterType.ORC, 1, new MaximumRoll());
    battle.pass();
    assertEquals(4, battle.enemies().getFirst().y());
    assertEquals(1, hero.health());
    battle.pass();
    assertTrue(hero.defeated());
    assertEquals(Battle.Outcome.DEFEAT, battle.outcome());
    assertThrows(IllegalStateException.class, battle::pass);
  }

  @Test
  void blockedMovesConsumeATurnButInvalidDirectionsDoNot() {
    byte[] terrain = grass();
    terrain[5 * 11 + 6] = 55;
    Battle battle =
        new Battle(
            arena(terrain, 5, 5, 5, 2),
            hero(200, 15, Avatar.Weapon.STAFF),
            MonsterType.ORC,
            1,
            new MinimumRoll());
    assertThrows(IllegalArgumentException.class, () -> battle.move(1, 1));
    assertEquals(0, battle.rounds());
    battle.move(1, 0);
    assertEquals(5, battle.x());
    assertEquals(1, battle.rounds());
    assertEquals(3, battle.enemies().getFirst().y());
  }

  @Test
  void rangedAttacksReachAlignedEnemiesButCannotCrossSolidTerrain() {
    Avatar hero = hero(200, 100, Avatar.Weapon.SLING);
    Battle open =
        new Battle(arena(grass(), 5, 9, 5, 3), hero, MonsterType.SKELETON, 1, new MaximumRoll());
    open.attack(0, -1);
    assertEquals(Battle.Outcome.VICTORY, open.outcome());
    byte[] terrain = grass();
    terrain[6 * 11 + 5] = 55;
    Avatar other = hero(200, 100, Avatar.Weapon.SLING);
    Battle blocked =
        new Battle(arena(terrain, 5, 9, 5, 3), other, MonsterType.SKELETON, 1, new MaximumRoll());
    blocked.attack(0, -1);
    assertEquals(Battle.Outcome.ACTIVE, blocked.outcome());
    assertTrue(blocked.message().contains("blocked"));
    assertEquals(0, other.experience());
  }

  @Test
  void meleeCannotHitDistantEnemiesAndThrownDaggersAreLost() {
    Avatar melee = hero(200, 100, Avatar.Weapon.SWORD);
    Battle distant =
        new Battle(arena(grass(), 5, 9, 5, 3), melee, MonsterType.SKELETON, 1, new MaximumRoll());
    distant.attack(0, -1);
    assertEquals(0, melee.experience());
    assertEquals(1, distant.enemies().size());
    Avatar dagger = hero(200, 100, Avatar.Weapon.DAGGER);
    Battle thrown =
        new Battle(arena(grass(), 5, 9, 5, 3), dagger, MonsterType.SKELETON, 1, new MaximumRoll());
    thrown.attack(0, -1);
    assertEquals(Avatar.Weapon.HANDS, dagger.weapon());
    assertEquals(Battle.Outcome.VICTORY, thrown.outcome());
  }

  @Test
  void walkingOffAnEdgeFleesWithoutAnotherEnemyTurn() {
    Avatar hero = hero(200, 15, Avatar.Weapon.STAFF);
    Battle battle =
        new Battle(arena(grass(), 5, 10, 5, 3), hero, MonsterType.ORC, 1, new MaximumRoll());
    battle.move(0, 1);
    assertEquals(Battle.Outcome.FLED, battle.outcome());
    assertEquals(3, battle.enemies().getFirst().y());
    assertEquals(200, hero.health());
    assertEquals(0, hero.experience());
    assertThrows(IllegalStateException.class, () -> battle.move(0, 1));
  }

  @Test
  void enemiesRouteAroundBarriersAndNeverShareATile() {
    byte[] terrain = grass();
    for (int x = 1; x < 10; x++) terrain[4 * 11 + x] = 55;
    Battle battle =
        new Battle(
            arena(terrain, 5, 7, 5, 2),
            hero(200, 15, Avatar.Weapon.STAFF),
            MonsterType.ORC,
            2,
            new MinimumRoll());
    for (int i = 0; i < 25; i++) {
      battle.pass();
      var first = battle.enemies().get(0);
      var second = battle.enemies().get(1);
      assertFalse(first.x() == second.x() && first.y() == second.y());
      assertTrue(DosMap.walkable(Byte.toUnsignedInt(terrain[first.y() * 11 + first.x()])));
    }
    assertTrue(
        battle.enemies().stream()
            .anyMatch(
                enemy -> Math.abs(enemy.x() - battle.x()) + Math.abs(enemy.y() - battle.y()) == 1));
  }
}
