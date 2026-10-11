package org.jultima4;

import java.util.List;
import java.util.Objects;

/** Audio decisions independent of sound hardware and game randomness. */
final class GameAudio implements AutoCloseable {
  enum Track {
    TITLE("rule-brittania.mid"),
    FANFARE("fanfare-of-the-lord-british.mid"),
    SHRINES("shrines.mid"),
    SHOPPING("shopping.mid"),
    WANDERER("wanderer.mid"),
    TOWNS("towns.mid"),
    CASTLES("castles.mid"),
    COMBAT("combat.mid");

    final String fileName;

    Track(String fileName) {
      this.fileName = fileName;
    }
  }

  interface Output extends AutoCloseable {
    void effect(SpeakerEffects.Effect effect);

    void music(Track track);

    void stop();

    @Override
    void close();
  }

  private final Output output;
  private Track track;
  private boolean muted, paused, closed;

  GameAudio(Output output) {
    this.output = Objects.requireNonNull(output);
  }

  static Track context(CharacterScreens.Stage stage, GameSession world) {
    if (stage != CharacterScreens.Stage.PLAYING)
      return switch (stage) {
        case SIGNATURE, ORIGIN -> Track.FANFARE;
        case CREATION, SELECTION -> Track.SHRINES;
        default -> Track.TITLE;
      };
    Objects.requireNonNull(world, "Playing requires a session");
    if (world.battle() != null) return Track.COMBAT;
    if (!world.inTown())
      return world.tile(world.x(), world.y()) == 30 ? Track.SHRINES : Track.WANDERER;
    return world.locationName().startsWith("Castle") ? Track.CASTLES : Track.TOWNS;
  }

  static Track context(CharacterScreens.Stage stage, CreationStory story, GameSession world) {
    if (stage == CharacterScreens.Stage.CREATION
        && story.phase() == CreationStory.Phase.INTRODUCTION) {
      if (story.page() < 15) return Track.WANDERER;
      if (story.page() <= 20) return Track.SHOPPING;
    }
    return context(stage, world);
  }

  void music(Track next) {
    Objects.requireNonNull(next);
    if (closed || next == track) return;
    track = next;
    if (active()) output.music(track);
  }

  void effect(SpeakerEffects.Effect effect) {
    Objects.requireNonNull(effect);
    if (active()) output.effect(effect);
  }

  void combat(List<Battle.Event> events) {
    for (Battle.Event event : events) {
      effect(
          switch (event.action()) {
            case MOVE -> SpeakerEffects.Effect.STEP;
            case ATTACK -> SpeakerEffects.Effect.ATTACK;
            case HIT -> SpeakerEffects.Effect.HIT;
            case MISS, FLEE -> SpeakerEffects.Effect.MISS;
            case DEATH -> SpeakerEffects.Effect.DEATH;
          });
    }
  }

  boolean muted() {
    return muted;
  }

  void toggleMute() {
    if (closed) return;
    muted = !muted;
    resumeOrStop();
  }

  void pause() {
    if (!closed && !paused) {
      paused = true;
      output.stop();
    }
  }

  void resume() {
    if (!closed && paused) {
      paused = false;
      resumeOrStop();
    }
  }

  private boolean active() {
    return !muted && !paused && !closed;
  }

  private void resumeOrStop() {
    if (active()) {
      if (track != null) output.music(track);
    } else output.stop();
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      output.close();
    }
  }
}
