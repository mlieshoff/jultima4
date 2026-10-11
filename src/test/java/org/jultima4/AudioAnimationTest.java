package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class AudioAnimationTest {
  private static final Battle.Event ATTACK = new Battle.Event(Battle.Action.ATTACK, 31, 5, 5, 5, 4);
  private static final Battle.Event HIT = new Battle.Event(Battle.Action.HIT, 31, 5, 5, 5, 4);
  private static final Battle.Event DEATH = new Battle.Event(Battle.Action.DEATH, 80, 5, 4, 5, 4);

  @Test
  void advance_whenTurnBegins_thenEmitFirstEventOnce() {
    CombatAnimation animation = new CombatAnimation();
    animation.play(List.of(ATTACK, HIT));
    List<Battle.Event> first = animation.advance(0);
    List<Battle.Event> next = animation.advance(0.05f);

    assertThat(List.of(first, next)).containsExactly(List.of(ATTACK), List.of());
  }

  @Test
  void advance_whenFrameCrossesSeveralEvents_thenEmitEachInOrder() {
    CombatAnimation animation = new CombatAnimation();
    animation.play(List.of(ATTACK, HIT, DEATH));

    List<Battle.Event> actual = animation.advance(1);

    assertThat(actual).containsExactly(ATTACK, HIT, DEATH);
  }

  @Test
  void advance_whenReplayingIdenticalTurn_thenEmitNewTurnAgain() {
    CombatAnimation animation = new CombatAnimation();
    animation.play(List.of(ATTACK, HIT));
    animation.advance(1);
    animation.play(List.of(ATTACK, HIT));

    assertThat(animation.advance(0)).containsExactly(ATTACK);
  }

  @Test
  void advance_whenAnimationCleared_thenDiscardPendingCues() {
    CombatAnimation animation = new CombatAnimation();
    animation.play(List.of(ATTACK));
    animation.clear();

    assertThat(animation.advance(1)).isEmpty();
  }

  @Test
  void advance_whenDeltaInvalid_thenRejectBeforeConsumingCue() {
    CombatAnimation animation = new CombatAnimation();
    animation.play(List.of(ATTACK));

    assertThatThrownBy(() -> animation.advance(Float.NaN))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void advance_whenEventBoundaryReached_thenCueNextEventExactlyOnce() {
    CombatAnimation animation = new CombatAnimation();
    animation.play(List.of(ATTACK, HIT));
    animation.advance(0);
    List<Battle.Event> boundary = animation.advance(0.20f);
    List<Battle.Event> next = animation.advance(0);

    assertThat(List.of(boundary, next)).containsExactly(List.of(HIT), List.of());
  }
}
