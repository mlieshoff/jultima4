package org.jultima4;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Visible land encounters; spawning cadence is a playable subset of the original rules. */
public final class OverworldMonsters {
  public record Encounter(MonsterType type, int count, int x, int y) {}

  private static final int[][] DIRECTIONS = {{0, -1}, {1, 0}, {0, 1}, {-1, 0}};
  private final Random random;
  private final List<Encounter> encounters = new ArrayList<>();
  private int turns, escapeGrace;

  public OverworldMonsters(Random random) {
    this.random = random;
  }

  public List<Encounter> encounters() {
    return List.copyOf(encounters);
  }

  record State(int turns, int escapeGrace, List<Encounter> encounters) {
    State {
      encounters = List.copyOf(encounters);
    }
  }

  State snapshot() {
    return new State(turns, escapeGrace, encounters);
  }

  void restore(State state, DosMap map) {
    if (state.turns() < 0
        || state.escapeGrace() < 0
        || state.escapeGrace() > 6
        || state.encounters().size() > 8) {
      throw new IllegalArgumentException("Invalid saved monster state");
    }
    var positions = new HashSet<Integer>();
    for (Encounter encounter : state.encounters()) {
      if (encounter.type() == null
          || encounter.count() < 1
          || encounter.count() > 2
          || encounter.x() < 0
          || encounter.x() > 255
          || encounter.y() < 0
          || encounter.y() > 255
          || !land(map.tile(encounter.x(), encounter.y()))
          || !positions.add(encounter.y() * 256 + encounter.x())) {
        throw new IllegalArgumentException("Invalid saved monster encounter");
      }
    }
    turns = state.turns();
    escapeGrace = state.escapeGrace();
    encounters.clear();
    encounters.addAll(state.encounters());
  }

  public Encounter at(int x, int y) {
    x = Math.floorMod(x, 256);
    y = Math.floorMod(y, 256);
    for (Encounter encounter : encounters) {
      if (encounter.x() == x && encounter.y() == y) {
        return encounter;
      }
    }
    return null;
  }

  public Encounter advance(DosMap map, int avatarX, int avatarY) {
    turns++;
    encounters.removeIf(enemy -> distance(enemy.x(), avatarX) + distance(enemy.y(), avatarY) > 20);
    if (escapeGrace > 0) {
      escapeGrace--;
      return null;
    }
    if (turns % 12 == 0 && encounters.size() < 8) {
      spawn(map, avatarX, avatarY);
    }
    for (int i = 0; i < encounters.size(); i++) {
      Encounter encounter = encounters.get(i);
      if (distance(encounter.x(), avatarX) + distance(encounter.y(), avatarY) <= 1) {
        return encounter;
      }
      if (turns % 2 == 0) {
        int step = stepToward(map, encounter.x(), encounter.y(), avatarX, avatarY);
        if (step >= 0) {
          encounter = new Encounter(encounter.type(), encounter.count(), step % 256, step / 256);
          encounters.set(i, encounter);
          if (distance(encounter.x(), avatarX) + distance(encounter.y(), avatarY) <= 1) {
            return encounter;
          }
        }
      }
    }
    return null;
  }

  private void spawn(DosMap map, int avatarX, int avatarY) {
    List<Integer> candidates = new ArrayList<>();
    Map<Integer, Integer> distances = new HashMap<>();
    int start = avatarY * 256 + avatarX;
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    distances.put(start, 0);
    queue.add(start);
    while (!queue.isEmpty()) {
      int current = queue.remove(), distance = distances.get(current);
      if (distance >= 6 && at(current % 256, current / 256) == null) {
        candidates.add(current);
      }
      if (distance == 8) {
        continue;
      }
      for (int[] direction : DIRECTIONS) {
        int nx = Math.floorMod(current % 256 + direction[0], 256);
        int ny = Math.floorMod(current / 256 + direction[1], 256);
        int next = ny * 256 + nx;
        if (distances.containsKey(next) || !land(map.tile(nx, ny)) || at(nx, ny) != null) {
          continue;
        }
        distances.put(next, distance + 1);
        queue.add(next);
      }
    }
    if (!candidates.isEmpty()) {
      int position = candidates.get(random.nextInt(candidates.size()));
      MonsterType type = MonsterType.values()[random.nextInt(MonsterType.values().length)];
      encounters.add(new Encounter(type, random.nextInt(2) + 1, position % 256, position / 256));
    }
  }

  private int stepToward(DosMap map, int fromX, int fromY, int targetX, int targetY) {
    int start = fromY * 256 + fromX, goal = targetY * 256 + targetX;
    Map<Integer, Integer> previous = new HashMap<>();
    Map<Integer, Integer> distances = new HashMap<>();
    ArrayDeque<Integer> queue = new ArrayDeque<>();
    queue.add(start);
    previous.put(start, start);
    distances.put(start, 0);
    while (!queue.isEmpty()) {
      int current = queue.remove();
      if (current == goal) {
        while (previous.get(current) != start && current != start) {
          current = previous.get(current);
        }
        return current == start ? -1 : current;
      }
      if (distances.get(current) >= 10) {
        continue;
      }
      for (int[] direction : DIRECTIONS) {
        int nx = Math.floorMod(current % 256 + direction[0], 256);
        int ny = Math.floorMod(current / 256 + direction[1], 256);
        int next = ny * 256 + nx;
        if (previous.containsKey(next)
            || (next != goal && (!land(map.tile(nx, ny)) || at(nx, ny) != null))) {
          continue;
        }
        previous.put(next, current);
        distances.put(next, distances.get(current) + 1);
        queue.add(next);
      }
    }
    return -1;
  }

  public void remove(Encounter encounter) {
    encounters.remove(encounter);
  }

  public void escaped(Encounter encounter, int survivors) {
    int index = encounters.indexOf(encounter);
    if (index >= 0) {
      if (survivors == 0) {
        encounters.remove(index);
      } else {
        encounters.set(
            index, new Encounter(encounter.type(), survivors, encounter.x(), encounter.y()));
      }
    }
    escapeGrace = 6;
  }

  private static boolean land(int tile) {
    return (tile >= 3 && tile <= 7)
        || tile == 22
        || tile == 23
        || tile == 25
        || tile == 26
        || tile == 62
        || tile == 63;
  }

  private static int distance(int a, int b) {
    int delta = Math.abs(a - b);
    return Math.min(delta, 256 - delta);
  }
}
