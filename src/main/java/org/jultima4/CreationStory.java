package org.jultima4;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Original DOS text; question order matches NEWGAME's ascending virtue-card order. */
final class CreationStory {
  enum Phase {
    INTRODUCTION,
    QUESTIONS,
    CONCLUSION,
    READY
  }

  record Question(Virtue first, Virtue second, String text, int firstOption) {
    int option(int answer) {
      if (answer < 0 || answer > 1) throw new IllegalArgumentException("Choose A or B");
      return answer == 0 ? firstOption : 1 - firstOption;
    }
  }

  private final List<String> questions;
  private final List<String> introduction;
  private final List<String> conclusion;
  private Phase phase = Phase.INTRODUCTION;
  private int page;

  private CreationStory(
      List<String> questions, List<String> introduction, List<String> conclusion) {
    this.questions = questions;
    this.introduction = introduction;
    this.conclusion = conclusion;
  }

  static CreationStory load() throws IOException {
    try {
      return decode(DosGameData.read("TITLE.EXE"));
    } catch (IllegalArgumentException ex) {
      throw new IOException("Invalid character-creation text", ex);
    }
  }

  static CreationStory decode(byte[] executable) {
    // DOS offsets matched to NEWGAME texts $00-$1B, $1D-$34 and $41-$42.
    return new CreationStory(
        strings(executable, 17444, 23487, 28),
        strings(executable, 23487, 27769, 24),
        strings(executable, 27996, 28389, 2));
  }

  private static List<String> strings(byte[] bytes, int start, int end, int count) {
    if (bytes.length < end) throw new IllegalArgumentException("Truncated creation text");
    List<String> result = new ArrayList<>();
    int position = start;
    for (int i = 0; i < count; i++) {
      int terminator = position;
      while (terminator < end && bytes[terminator] != 0) {
        int value = Byte.toUnsignedInt(bytes[terminator++]);
        if (value != 10 && (value < 32 || value > 126))
          throw new IllegalArgumentException("Invalid creation text character");
      }
      if (terminator == end || terminator == position)
        throw new IllegalArgumentException("Missing creation text terminator");
      result.add(
          new String(bytes, position, terminator - position, StandardCharsets.US_ASCII).trim());
      position = terminator + 1;
    }
    if (position != end) throw new IllegalArgumentException("Unexpected creation text size");
    return List.copyOf(result);
  }

  void start() {
    phase = Phase.INTRODUCTION;
    page = 0;
  }

  Phase phase() {
    return phase;
  }

  int page() {
    return page;
  }

  String text() {
    return switch (phase) {
      case INTRODUCTION -> introduction.get(page);
      case CONCLUSION -> conclusion.get(page);
      default -> throw new IllegalStateException("No narrative page");
    };
  }

  void advance() {
    switch (phase) {
      case INTRODUCTION -> {
        if (++page == introduction.size()) {
          phase = Phase.QUESTIONS;
          page = 0;
        }
      }
      case CONCLUSION -> {
        if (++page == conclusion.size()) {
          phase = Phase.READY;
          page = 0;
        }
      }
      default -> throw new IllegalStateException("Not reading narrative");
    }
  }

  void choiceFinished(boolean complete) {
    if (phase != Phase.QUESTIONS) throw new IllegalStateException("Not choosing cards");
    if (complete) {
      phase = Phase.CONCLUSION;
      page = 0;
    }
  }

  Question question(CharacterCreation creation) {
    if (phase != Phase.QUESTIONS) throw new IllegalStateException("Not choosing cards");
    Virtue left = creation.option(0), right = creation.option(1);
    int firstOption = left.ordinal() < right.ordinal() ? 0 : 1;
    Virtue first = firstOption == 0 ? left : right;
    Virtue second = firstOption == 0 ? right : left;
    int index =
        first.ordinal() * (15 - first.ordinal()) / 2 + second.ordinal() - first.ordinal() - 1;
    String placement =
        creation.choices() == 0
            ? "The gypsy places the first two cards"
            : creation.choices() == 6
                ? "The gypsy places the last two cards"
                : "The gypsy places two more of the cards";
    return new Question(
        first,
        second,
        placement
            + " upon the table. They are the cards of "
            + displayName(first)
            + " and "
            + displayName(second)
            + ".\n\n"
            + questions.get(index),
        firstOption);
  }

  static String displayName(Virtue virtue) {
    String name = virtue.name().toLowerCase(java.util.Locale.ROOT);
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }

  String picture() {
    if (phase == Phase.CONCLUSION) return "GYPSY";
    if (phase != Phase.INTRODUCTION) return "ABACUS";
    if (page == 6) return "PORTAL";
    if (page >= 22) return "ABACUS";
    if (page == 21) return "GYPSY";
    if (page == 20) return "WAGON";
    if (page >= 18) return "INSIDE";
    if (page >= 16) return "OUTSIDE";
    if (page == 15) return "OUTSIDE";
    return "TREE";
  }
}
