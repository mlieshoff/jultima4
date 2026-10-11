package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class GameAudioTest {
  private static final class RecordingOutput implements GameAudio.Output {
    final List<String> calls = new ArrayList<>();

    public void effect(SpeakerEffects.Effect effect) {
      calls.add("effect:" + effect);
    }

    public void music(GameAudio.Track track) {
      calls.add("music:" + track);
    }

    public void stop() {
      calls.add("stop");
    }

    public void close() {
      calls.add("close");
    }
  }

  @Test
  void music_whenRepeatedFramesKeepSameContext_thenDoNotRestartTrack() {
    RecordingOutput output = new RecordingOutput();
    GameAudio audio = new GameAudio(output);
    audio.music(GameAudio.Track.TITLE);
    audio.music(GameAudio.Track.TITLE);
    audio.music(GameAudio.Track.SHRINES);

    assertThat(output.calls).containsExactly("music:TITLE", "music:SHRINES");
  }

  @Test
  void toggleMute_whenMusicAndEffectsAreMuted_thenSuppressPlaybackUntilUnmuted() {
    RecordingOutput output = new RecordingOutput();
    GameAudio audio = new GameAudio(output);
    audio.music(GameAudio.Track.TITLE);
    audio.toggleMute();
    audio.effect(SpeakerEffects.Effect.HIT);
    audio.music(GameAudio.Track.COMBAT);
    audio.toggleMute();
    audio.effect(SpeakerEffects.Effect.HIT);

    assertThat(output.calls).containsExactly("music:TITLE", "stop", "music:COMBAT", "effect:HIT");
  }

  @Test
  void resume_whenPausedAndContextChanged_thenResumeLatestMusicWithoutEffects() {
    RecordingOutput output = new RecordingOutput();
    GameAudio audio = new GameAudio(output);
    audio.music(GameAudio.Track.TITLE);
    audio.pause();
    audio.pause();
    audio.music(GameAudio.Track.SHRINES);
    audio.effect(SpeakerEffects.Effect.CHOICE);
    audio.resume();
    audio.resume();

    assertThat(output.calls).containsExactly("music:TITLE", "stop", "music:SHRINES");
  }

  @Test
  void resume_whenStillMuted_thenRemainSilent() {
    RecordingOutput output = new RecordingOutput();
    GameAudio audio = new GameAudio(output);
    audio.music(GameAudio.Track.TITLE);
    audio.toggleMute();
    audio.pause();
    audio.resume();

    assertThat(output.calls).containsExactly("music:TITLE", "stop", "stop", "stop");
  }

  @Test
  void toggleMute_whenUnmutingWhilePaused_thenWaitForResume() {
    RecordingOutput output = new RecordingOutput();
    GameAudio audio = new GameAudio(output);
    audio.music(GameAudio.Track.TITLE);
    audio.pause();
    audio.toggleMute();
    audio.toggleMute();
    audio.resume();

    assertThat(output.calls).containsExactly("music:TITLE", "stop", "stop", "stop", "music:TITLE");
  }

  @Test
  void close_whenCalledRepeatedly_thenReleaseOutputOnceAndIgnoreLaterPlayback() {
    RecordingOutput output = new RecordingOutput();
    GameAudio audio = new GameAudio(output);
    audio.close();
    audio.close();
    audio.effect(SpeakerEffects.Effect.HIT);
    audio.music(GameAudio.Track.COMBAT);
    audio.toggleMute();
    audio.pause();
    audio.resume();

    assertThat(output.calls).containsExactly("close");
  }

  @ParameterizedTest
  @EnumSource(Battle.Action.class)
  void combat_whenActionStarts_thenPlayCorrespondingEffect(Battle.Action action) {
    RecordingOutput output = new RecordingOutput();
    GameAudio audio = new GameAudio(output);
    var effect =
        switch (action) {
          case MOVE -> SpeakerEffects.Effect.STEP;
          case ATTACK -> SpeakerEffects.Effect.ATTACK;
          case HIT -> SpeakerEffects.Effect.HIT;
          case MISS, FLEE -> SpeakerEffects.Effect.MISS;
          case DEATH -> SpeakerEffects.Effect.DEATH;
        };
    audio.combat(List.of(new Battle.Event(action, 31, 5, 5, 5, 4)));

    assertThat(output.calls).containsExactly("effect:" + effect);
  }

  @ParameterizedTest
  @EnumSource(
      value = CharacterScreens.Stage.class,
      names = {"PLAYING"},
      mode = EnumSource.Mode.EXCLUDE)
  void context_whenFrontScreenShown_thenChooseTitleOrGypsyMusic(CharacterScreens.Stage stage) {
    var expected =
        switch (stage) {
          case CREATION, SELECTION -> GameAudio.Track.SHRINES;
          case SIGNATURE, ORIGIN -> GameAudio.Track.FANFARE;
          default -> GameAudio.Track.TITLE;
        };

    assertThat(GameAudio.context(stage, null)).isEqualTo(expected);
  }

  @Test
  void context_whenExploringBritannia_thenChooseWandererMusic() throws Exception {
    GameSession world = new GameSession(DosGameData.load(), 82, 106);

    assertThat(GameAudio.context(CharacterScreens.Stage.PLAYING, world))
        .isEqualTo(GameAudio.Track.WANDERER);
  }

  @Test
  void context_whenInsideTown_thenChooseTownMusic() throws Exception {
    GameSession world = new GameSession(DosGameData.load(), 82, 106);
    world.enter();

    assertThat(GameAudio.context(CharacterScreens.Stage.PLAYING, world))
        .isEqualTo(GameAudio.Track.TOWNS);
  }

  @ParameterizedTest
  @org.junit.jupiter.params.provider.CsvSource({
    "0,WANDERER",
    "14,WANDERER",
    "15,SHOPPING",
    "20,SHOPPING",
    "21,SHRINES",
    "23,SHRINES"
  })
  void context_whenCreationNarrativeChangesScene_thenSelectMatchingMusic(
      int page, GameAudio.Track expected) throws Exception {
    CreationStory story = CreationStory.load();
    for (int i = 0; i < page; i++) story.advance();

    assertThat(GameAudio.context(CharacterScreens.Stage.CREATION, story, null)).isEqualTo(expected);
  }

  @Test
  void context_whenCastingCards_thenSelectShrineMusic() throws Exception {
    CreationStory story = CreationStory.load();
    for (int i = 0; i < 24; i++) story.advance();

    assertThat(GameAudio.context(CharacterScreens.Stage.CREATION, story, null))
        .isEqualTo(GameAudio.Track.SHRINES);
  }

  @Test
  void context_whenInsideCastle_thenSelectCastleMusic() throws Exception {
    var location =
        DosGameData.LOCATIONS.stream()
            .filter(place -> place.file().equals("LCB_1"))
            .findFirst()
            .orElseThrow();
    GameSession world = new GameSession(DosGameData.load(), location.x(), location.y());
    world.enter();

    assertThat(GameAudio.context(CharacterScreens.Stage.PLAYING, world))
        .isEqualTo(GameAudio.Track.CASTLES);
  }

  @Test
  void context_whenStandingOnShrine_thenSelectShrineMusic() throws Exception {
    GameSession world = new GameSession(DosGameData.load(), 233, 66);

    assertThat(GameAudio.context(CharacterScreens.Stage.PLAYING, world))
        .isEqualTo(GameAudio.Track.SHRINES);
  }

  @Test
  void context_whenEncounterStarts_thenCombatOverridesOutdoorMusic() throws Exception {
    byte[] terrain = new byte[65536];
    java.util.Arrays.fill(terrain, (byte) 4);
    DosGameData data =
        new DosGameData(DosMap.world(terrain), new EgaTiles(new byte[32768]), java.util.Map.of());
    java.util.Random random =
        new java.util.Random(0) {
          @Override
          public int nextInt(int bound) {
            return bound - 1;
          }
        };
    GameSession world =
        new GameSession(
            data,
            50,
            50,
            new Avatar(1000, 200, 20, Avatar.Weapon.SWORD, 128, 0),
            CombatMap.load(),
            random);
    for (int i = 0; i < 60 && world.battle() == null; i++) world.pass();

    assertThat(GameAudio.context(CharacterScreens.Stage.PLAYING, world))
        .isEqualTo(GameAudio.Track.COMBAT);
  }
}
