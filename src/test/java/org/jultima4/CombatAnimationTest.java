package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CombatAnimationTest {
  private static class Roll extends Random {
    private final boolean high;

    Roll(boolean high) {
      this.high = high;
    }

    @Override
    public int nextInt(int bound) {
      return high ? bound - 1 : 0;
    }
  }

  private Battle battle(Avatar hero, int enemyY, Random random) {
    byte[] terrain = new byte[121];
    Arrays.fill(terrain, (byte) 4);
    List<CombatMap.Position> starts = new ArrayList<>();
    starts.add(new CombatMap.Position(5, enemyY));
    for (int i = 1; i < 16; i++) starts.add(new CombatMap.Position(i % 11, i / 11));
    return new Battle(
        new CombatMap("test", terrain, starts, new CombatMap.Position(5, 5)),
        hero,
        MonsterType.ORC,
        1,
        random);
  }

  private Avatar hero(int hp) {
    return new Avatar(hp, 200, 20, Avatar.Weapon.SWORD, 128, 0);
  }

  @Test
  void killingStrikePlaysAttackHitAndDeathBeforeRemovingTheSprite() {
    Avatar hero = hero(200);
    Battle battle = battle(hero, 4, new Roll(true));
    CombatAnimation animation = new CombatAnimation();
    animation.capture(battle);
    battle.attack(0, -1);
    assertEquals(
        List.of(Battle.Action.ATTACK, Battle.Action.HIT, Battle.Action.DEATH),
        battle.events().stream().map(Battle.Event::action).toList());
    assertThrows(UnsupportedOperationException.class, () -> battle.events().clear());
    animation.play(battle.events());
    assertEquals(2, animation.sprites().size());
    animation.update(0.10f);
    assertTrue(
        animation.sprites().stream()
                .filter(sprite -> sprite.tile() == 31)
                .findFirst()
                .orElseThrow()
                .y()
            < 5);
    animation.update(0.30f);
    assertEquals(Battle.Action.DEATH, animation.current().action());
    assertTrue(
        animation.sprites().stream()
                .filter(sprite -> sprite.tile() == 192)
                .findFirst()
                .orElseThrow()
                .alpha()
            < 1);
    animation.update(1);
    assertFalse(animation.playing());
    assertEquals(1, animation.sprites().size());
    assertEquals(6, hero.experience());
    assertEquals(200, hero.health());
  }

  @Test
  void movesInterpolateInTurnOrderAndCarryRemainderAcrossEvents() {
    Battle battle = battle(hero(200), 2, new Roll(false));
    CombatAnimation animation = new CombatAnimation();
    animation.capture(battle);
    battle.move(1, 0);
    assertEquals(
        List.of(Battle.Action.MOVE, Battle.Action.MOVE),
        battle.events().stream().map(Battle.Event::action).toList());
    animation.play(battle.events());
    animation.update(0.07f);
    var avatar =
        animation.sprites().stream()
            .filter(sprite -> sprite.tile() == 31)
            .findFirst()
            .orElseThrow();
    assertEquals(5.5f, avatar.x(), 0.001f);
    assertEquals(5, avatar.y());
    animation.update(0.10f);
    avatar =
        animation.sprites().stream()
            .filter(sprite -> sprite.tile() == 31)
            .findFirst()
            .orElseThrow();
    assertEquals(6, avatar.x());
    assertEquals(0.03f / 0.14f, animation.progress(), 0.001f);
    animation.update(1);
    var enemy =
        animation.sprites().stream()
            .filter(sprite -> sprite.tile() == 192)
            .findFirst()
            .orElseThrow();
    assertEquals(battle.enemies().getFirst().x(), enemy.x());
    assertEquals(battle.enemies().getFirst().y(), enemy.y());
  }

  @Test
  void missesAndRetaliationAreReportedWithoutParsingMessages() {
    Battle battle = battle(hero(200), 4, new Roll(false));
    battle.attack(0, -1);
    assertEquals(
        List.of(Battle.Action.ATTACK, Battle.Action.MISS, Battle.Action.ATTACK, Battle.Action.MISS),
        battle.events().stream().map(Battle.Event::action).toList());
    assertEquals(5, battle.events().get(1).toX());
    assertEquals(4, battle.events().get(1).toY());
    assertEquals(5, battle.events().get(3).toY());
    battle.pass();
    assertEquals(2, battle.events().size(), "Only the latest turn is retained");
  }

  @Test
  void enemyKillingStrikeFadesTheAvatarAndResetCancelsPlayback() {
    Battle battle = battle(hero(1), 4, new Roll(true));
    CombatAnimation animation = new CombatAnimation();
    animation.capture(battle);
    battle.pass();
    assertEquals(Battle.Action.DEATH, battle.events().getLast().action());
    assertEquals(31, battle.events().getLast().tile());
    animation.play(battle.events());
    animation.update(2);
    assertFalse(animation.sprites().stream().anyMatch(sprite -> sprite.tile() == 31));
    animation.clear();
    assertFalse(animation.playing());
    assertTrue(animation.sprites().isEmpty());
    assertThrows(IllegalArgumentException.class, () -> animation.update(-1));
    assertThrows(IllegalArgumentException.class, () -> animation.update(Float.NaN));
  }

  @Test
  void fleeingSlidesAndFadesTheAvatarOffTheBoardWithoutEnemyActions() {
    Battle battle = battle(hero(200), 2, new Roll(false));
    for (int i = 0; i < 5; i++) battle.move(0, 1);
    CombatAnimation animation = new CombatAnimation();
    animation.capture(battle);
    battle.move(0, 1);
    assertEquals(1, battle.events().size());
    assertEquals(Battle.Action.FLEE, battle.events().getFirst().action());
    animation.play(battle.events());
    animation.update(0.12f);
    var avatar =
        animation.sprites().stream()
            .filter(sprite -> sprite.tile() == 31)
            .findFirst()
            .orElseThrow();
    assertEquals(10.5f, avatar.y(), 0.001f);
    assertEquals(0.5f, avatar.alpha(), 0.001f);
    animation.update(1);
    assertFalse(animation.sprites().stream().anyMatch(sprite -> sprite.tile() == 31));
  }

  @Test
  void emptyAttacksAnimateOnlyToTheWorldEdge() {
    Avatar hero = new Avatar(200, 20, 20, Avatar.Weapon.SLING, 128, 0);
    Battle battle = battle(hero, 2, new Roll(false));
    battle.attack(1, 0);
    assertEquals(Battle.Action.ATTACK, battle.events().getFirst().action());
    assertEquals(10, battle.events().getFirst().toX());
    assertEquals(Battle.Action.MISS, battle.events().get(1).action());
    assertEquals(5, battle.events().get(1).toY());
  }

  @Test
  void presentationDoesNotAdvanceTheModelOrConsumeRandomness() {
    Avatar animatedHero = hero(200), plainHero = hero(200);
    Battle animated = battle(animatedHero, 2, new Random(17));
    Battle plain = battle(plainHero, 2, new Random(17));
    CombatAnimation animation = new CombatAnimation();
    for (int i = 0; i < 6; i++) {
      animation.capture(animated);
      animated.pass();
      plain.pass();
      animation.play(animated.events());
      for (int frame = 0; frame < 100; frame++) {
        animation.update(0.016f);
        animation.sprites();
      }
      assertEquals(plain.enemies(), animated.enemies());
      assertEquals(plainHero.health(), animatedHero.health());
      assertEquals(plain.rounds(), animated.rounds());
    }
  }
}
