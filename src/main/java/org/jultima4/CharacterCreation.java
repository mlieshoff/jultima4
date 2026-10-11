package org.jultima4;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Seven elimination choices; chosen virtues add five points and table-based stats. */
public final class CharacterCreation {
    private final List<Virtue> remaining = new ArrayList<>(List.of(Virtue.values()));
    private final int[] scores = {50, 50, 50, 50, 50, 50, 50, 50};
    private int strength = 15, dexterity = 15, intelligence = 15;

    public CharacterCreation(Random random) {
        Collections.shuffle(remaining, random);
    }

    public Virtue option(int index) {
        if (complete()) throw new IllegalStateException("Character creation is complete");
        if (index < 0 || index > 1) throw new IllegalArgumentException("Choose 0 or 1");
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

    public boolean complete() { return remaining.size() == 1; }
    public int choices() { return 8 - remaining.size(); }
    public Virtue winner() {
        if (!complete()) throw new IllegalStateException("Choose all seven pairs first");
        return remaining.getFirst();
    }
    /** Starting coordinates from NEWGAME $67F9/$6801, verified as land in WORLD.MAP. */
    public int startingX() { return new int[]{231, 83, 35, 59, 158, 105, 23, 186}[winner().ordinal()]; }
    public int startingY() { return new int[]{136, 105, 221, 44, 21, 183, 129, 171}[winner().ordinal()]; }
    public int strength() { return strength; }
    public int dexterity() { return dexterity; }
    public int intelligence() { return intelligence; }
    public int score(Virtue virtue) { return scores[virtue.ordinal()]; }
}
