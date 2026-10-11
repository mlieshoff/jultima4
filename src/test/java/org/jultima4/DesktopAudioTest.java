package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class DesktopAudioTest {
  @ParameterizedTest
  @EnumSource(GameAudio.Track.class)
  void load_whenSuppliedTrackIsPackaged_thenDecodePlayableMidiNotes(GameAudio.Track track)
      throws Exception {
    Sequence song = DesktopAudio.load(track);

    assertPlayableSong(song);
  }

  private static void assertPlayableSong(Sequence song) {
    assertThat(song.getTickLength()).isPositive();
    assertThat(song.getMicrosecondLength()).isPositive();
    int notes = 0;
    for (var track : song.getTracks()) {
      for (int i = 0; i < track.size(); i++) {
        var message = track.get(i).getMessage();
        if (message instanceof ShortMessage note
            && note.getCommand() == ShortMessage.NOTE_ON
            && note.getData2() > 0) notes++;
      }
    }
    assertThat(notes).isPositive();
  }

  @org.junit.jupiter.api.Test
  void close_whenAudioHardwareUnavailable_thenReportBothFailuresAndKeepPlaybackSafe() {
    java.util.List<String> warnings = new java.util.ArrayList<>();
    DesktopAudio.Devices devices =
        new DesktopAudio.Devices() {
          public javax.sound.sampled.Clip clip()
              throws javax.sound.sampled.LineUnavailableException {
            throw new javax.sound.sampled.LineUnavailableException("No PCM device");
          }

          public javax.sound.midi.Sequencer sequencer()
              throws javax.sound.midi.MidiUnavailableException {
            throw new javax.sound.midi.MidiUnavailableException("No MIDI device");
          }

          public javax.sound.midi.Synthesizer synthesizer() {
            throw new AssertionError("Should not open synthesizer");
          }
        };
    DesktopAudio audio = new DesktopAudio(warnings::add, devices);
    audio.effect(SpeakerEffects.Effect.HIT);
    audio.music(GameAudio.Track.TITLE);
    audio.stop();
    audio.close();
    audio.close();
    audio.effect(SpeakerEffects.Effect.HIT);
    audio.music(GameAudio.Track.COMBAT);

    assertThat(warnings)
        .containsExactly(
            "Sound effects unavailable: check the desktop audio output. No PCM device",
            "MIDI music unavailable: check installed MIDI support and audio output. No MIDI device");
  }
}
