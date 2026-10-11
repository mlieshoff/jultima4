package org.jultima4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Read-only replay of BOOT's $6E73 map, $6F42 actors and $7170 command stream. */
final class TitleScene {
  static final int WIDTH = 19, HEIGHT = 5;
  static final List<String> CREDITS =
      List.of("Lord British", "and", "Origin Systems, Inc.", "presents");
  static final List<String> MENU =
      List.of("B - Begin new quest", "J - Journey onward", "R - Return to the view");
  private static final int ACTOR_OFFSET = 16584;
  private static final int MAP_OFFSET = 30339;
  private static final int SCRIPT_OFFSET = 30434;
  private static final int SCRIPT_END = 30982;
  private static final double TICK_SECONDS = 1.0 / 15;

  private record Actor(int tile, int x, int y) {}

  private record Frame(int ticks, List<Integer> tiles) {}

  private final List<Frame> frames;
  private final int ticks;

  private TitleScene(List<Frame> frames) {
    this.frames = List.copyOf(frames);
    ticks = frames.stream().mapToInt(Frame::ticks).sum();
  }

  static TitleScene load() throws IOException {
    try {
      return decode(DosGameData.read("TITLE.EXE"));
    } catch (IllegalArgumentException ex) {
      throw new IOException("Invalid title demonstration: " + ex.getMessage(), ex);
    }
  }

  static TitleScene decode(byte[] executable) {
    if (executable.length < SCRIPT_END) {
      throw new IllegalArgumentException("Missing title demonstration data");
    }
    byte[] terrain = Arrays.copyOfRange(executable, MAP_OFFSET, SCRIPT_OFFSET);
    Actor[] actors = new Actor[15];
    List<Frame> frames = new ArrayList<>();
    int position = SCRIPT_OFFSET;
    boolean ended = false;
    while (position < SCRIPT_END) {
      int command = Byte.toUnsignedInt(executable[position++]);
      if (command == 0xff) {
        ended = true;
        break;
      }
      int operation = command >>> 4, actor = command & 15;
      if (operation == 8) {
        // BOOT decrements the delay before checking: even $80 displays one tick.
        List<Integer> tiles = new ArrayList<>();
        for (byte tile : terrain) tiles.add(Byte.toUnsignedInt(tile));
        for (Actor sprite : actors) {
          if (sprite != null) tiles.set(sprite.y() * WIDTH + sprite.x(), sprite.tile());
        }
        frames.add(new Frame(actor * 2 + 1, List.copyOf(tiles)));
      } else if (actor >= actors.length) {
        throw new IllegalArgumentException("Invalid demonstration actor");
      } else if (operation == 7) {
        actors[actor] = null;
      } else if (operation < HEIGHT && position < SCRIPT_END) {
        int encoded = Byte.toUnsignedInt(executable[position++]);
        int x = encoded & 31;
        int tile = Byte.toUnsignedInt(executable[ACTOR_OFFSET + actor]) + (encoded >>> 5);
        if (x >= WIDTH || tile > 255) {
          throw new IllegalArgumentException("Invalid demonstration sprite");
        }
        actors[actor] = new Actor(tile, x, operation);
      } else {
        throw new IllegalArgumentException("Invalid demonstration command");
      }
    }
    if (!ended || frames.isEmpty()) {
      throw new IllegalArgumentException("Incomplete title demonstration");
    }
    return new TitleScene(frames);
  }

  List<Integer> tiles(double seconds) {
    if (!Double.isFinite(seconds) || seconds < 0) {
      throw new IllegalArgumentException("Invalid demonstration time");
    }
    double remaining = seconds % durationSeconds();
    for (Frame frame : frames) {
      double duration = frame.ticks() * TICK_SECONDS;
      if (remaining < duration) return frame.tiles();
      remaining -= duration;
    }
    return frames.getLast().tiles();
  }

  double durationSeconds() {
    return ticks * TICK_SECONDS;
  }
}
