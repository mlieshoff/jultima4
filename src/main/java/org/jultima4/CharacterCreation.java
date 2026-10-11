package org.jultima4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Seven elimination choices; chosen virtues add five points and table-based stats. */
public final class CharacterCreation {
  private static final int[] STARTING_X = {231, 83, 35, 59, 158, 105, 23, 186};
  private static final int[] STARTING_Y = {136, 105, 221, 44, 21, 183, 129, 171};

  private final List<Virtue> remaining = new ArrayList<>(List.of(Virtue.values()));
  private final int[] scores = {50, 50, 50, 50, 50, 50, 50, 50};
  private int strength = 15, dexterity = 15, intelligence = 15;

  public CharacterCreation(Random random) {
    Collections.shuffle(remaining, random);
  }

  record State(Virtue winner, List<Integer> scores) {
    State {
      scores = List.copyOf(scores);
    }
  }

  State snapshot() {
    return new State(winner(), java.util.Arrays.stream(scores).boxed().toList());
  }

  static CharacterCreation restore(State state) {
    if (state.winner() == null || state.scores().size() != Virtue.values().length) {
      throw new IllegalArgumentException("Invalid saved character");
    }
    CharacterCreation creation = new CharacterCreation(new Random(0));
    creation.remaining.clear();
    creation.remaining.add(state.winner());
    int choices = 0;
    for (Virtue virtue : Virtue.values()) {
      int score = state.scores().get(virtue.ordinal());
      if (score < 50 || score > 85 || (score - 50) % 5 != 0) {
        throw new IllegalArgumentException("Invalid virtue score");
      }
      int wins = (score - 50) / 5;
      choices += wins;
      creation.scores[virtue.ordinal()] = score;
      creation.strength += wins * virtue.strength;
      creation.dexterity += wins * virtue.dexterity;
      creation.intelligence += wins * virtue.intelligence;
    }
    if (choices != 7 || creation.score(state.winner()) == 50) {
      throw new IllegalArgumentException("Saved character must have seven completed choices");
    }
    return creation;
  }

  public Virtue option(int index) {
    if (complete()) {
      throw new IllegalStateException("Character creation is complete");
    }
    if (index < 0 || index > 1) {
      throw new IllegalArgumentException("Choose 0 or 1");
    }
    return remaining.get(index);
  }

  public void choose(int index) {
    Virtue chosen = option(index);
    remaining.remove(1 - index);
    // Requeue the winner so all untouched cards are presented before it returns.
    remaining.remove(chosen);
    remaining.add(chosen);
    strength += chosen.strength;
    dexterity += chosen.dexterity;
    intelligence += chosen.intelligence;
    scores[chosen.ordinal()] += 5;
  }

  public boolean complete() {
    return remaining.size() == 1;
  }

  public int choices() {
    return 8 - remaining.size();
  }

  public Virtue winner() {
    if (!complete()) {
      throw new IllegalStateException("Choose all seven pairs first");
    }
    return remaining.getFirst();
  }

  /** Starting coordinates from NEWGAME $67F9/$6801, verified as land in WORLD.MAP. */
  public int startingX() {
    return STARTING_X[winner().ordinal()];
  }

  public int startingY() {
    return STARTING_Y[winner().ordinal()];
  }

  public int strength() {
    return strength;
  }

  public int dexterity() {
    return dexterity;
  }

  public int intelligence() {
    return intelligence;
  }

  public int score(Virtue virtue) {
    return scores[virtue.ordinal()];
  }
}
