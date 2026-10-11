package org.jultima4;

import java.util.Random;

/** Title, virtue choices, and confirmation of the class selected by those choices. */
public final class CharacterScreens {
  public enum Stage {
    SIGNATURE,
    ORIGIN,
    TITLE,
    CREATION,
    SELECTION,
    PLAYING
  }

  private final Random random;
  private Stage stage = Stage.SIGNATURE;
  private double elapsed;
  private CharacterCreation creation;

  public CharacterScreens(Random random) {
    this.random = random;
  }

  public Stage stage() {
    return stage;
  }

  public double elapsed() {
    return elapsed;
  }

  public void intro() {
    creation = null;
    stage = Stage.SIGNATURE;
    elapsed = 0;
  }

  public void advanceIntro() {
    if (stage != Stage.SIGNATURE && stage != Stage.ORIGIN) {
      throw new IllegalStateException("Not showing introduction");
    }
    stage = stage == Stage.SIGNATURE ? Stage.ORIGIN : Stage.TITLE;
    elapsed = 0;
  }

  public void update(double seconds) {
    if (!Double.isFinite(seconds) || seconds < 0) {
      throw new IllegalArgumentException("Invalid screen time");
    }
    if (stage != Stage.SIGNATURE && stage != Stage.ORIGIN) {
      return;
    }
    elapsed += seconds;
    while (stage == Stage.SIGNATURE || stage == Stage.ORIGIN) {
      double duration = stage == Stage.SIGNATURE ? 4 : 2;
      if (elapsed < duration) {
        break;
      }
      double remainder = elapsed - duration;
      advanceIntro();
      elapsed = remainder;
    }
  }

  public CharacterCreation creation() {
    if (creation == null) {
      throw new IllegalStateException("No character has been started");
    }
    return creation;
  }

  public void start() {
    creation = new CharacterCreation(random);
    stage = Stage.CREATION;
    elapsed = 0;
  }

  public void choose(int option) {
    if (stage != Stage.CREATION) {
      throw new IllegalStateException("Not choosing virtues");
    }
    creation.choose(option);
    if (creation.complete()) {
      stage = Stage.SELECTION;
    }
  }

  public void confirm() {
    if (stage != Stage.SELECTION) {
      throw new IllegalStateException("No selected character to confirm");
    }
    stage = Stage.PLAYING;
  }

  public void title() {
    creation = null;
    stage = Stage.TITLE;
    elapsed = 0;
  }
}
