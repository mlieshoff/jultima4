package org.jultima4;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Synthesizer;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.LineUnavailableException;

/** Java's desktop PCM/MIDI devices; no external audio library or runtime downloads. */
final class DesktopAudio implements GameAudio.Output {
  private final Map<SpeakerEffects.Effect, Clip> clips = new EnumMap<>(SpeakerEffects.Effect.class);
  private final Map<GameAudio.Track, Sequence> songs = new EnumMap<>(GameAudio.Track.class);
  private final Consumer<String> warning;
  private Sequencer sequencer;
  private Synthesizer synthesizer;
  private boolean closed;

  interface Devices {
    Clip clip() throws LineUnavailableException;

    Sequencer sequencer() throws MidiUnavailableException;

    Synthesizer synthesizer() throws MidiUnavailableException;
  }

  DesktopAudio(Consumer<String> warning) {
    this(
        warning,
        new Devices() {
          public Clip clip() throws LineUnavailableException {
            return AudioSystem.getClip();
          }

          public Sequencer sequencer() throws MidiUnavailableException {
            return MidiSystem.getSequencer(false);
          }

          public Synthesizer synthesizer() throws MidiUnavailableException {
            return MidiSystem.getSynthesizer();
          }
        });
  }

  DesktopAudio(Consumer<String> warning, Devices devices) {
    this.warning = warning;
    try {
      AudioFormat format = new AudioFormat(SpeakerEffects.SAMPLE_RATE, 16, 1, true, false);
      for (SpeakerEffects.Effect effect : SpeakerEffects.Effect.values()) {
        Clip clip = devices.clip();
        clips.put(effect, clip);
        byte[] pcm = SpeakerEffects.pcm(effect);
        clip.open(format, pcm, 0, pcm.length);
      }
    } catch (LineUnavailableException | IllegalArgumentException | SecurityException ex) {
      closeClips();
      warning.accept(
          "Sound effects unavailable: check the desktop audio output. " + ex.getMessage());
    }
    try {
      for (GameAudio.Track track : GameAudio.Track.values()) {
        songs.put(track, load(track));
      }
      sequencer = devices.sequencer();
      if (sequencer == null) throw new MidiUnavailableException("No MIDI sequencer");
      synthesizer = devices.synthesizer();
      if (synthesizer == null) throw new MidiUnavailableException("No MIDI synthesizer");
      synthesizer.open();
      sequencer.open();
      sequencer.getTransmitter().setReceiver(synthesizer.getReceiver());
    } catch (IOException
        | InvalidMidiDataException
        | MidiUnavailableException
        | SecurityException ex) {
      closeMidi();
      warning.accept(
          "MIDI music unavailable: check installed MIDI support and audio output. "
              + ex.getMessage());
    }
  }

  static Sequence load(GameAudio.Track track) throws IOException, InvalidMidiDataException {
    String resource = "/music/" + track.fileName;
    try (InputStream input = DesktopAudio.class.getResourceAsStream(resource)) {
      if (input == null) throw new IOException("Missing MIDI resource " + resource);
      return MidiSystem.getSequence(new BufferedInputStream(input));
    }
  }

  @Override
  public void effect(SpeakerEffects.Effect effect) {
    if (closed) return;
    Clip clip = clips.get(effect);
    if (clip != null) {
      clip.stop();
      clip.setFramePosition(0);
      clip.start();
    }
  }

  @Override
  public void music(GameAudio.Track track) {
    if (closed || sequencer == null) return;
    try {
      sequencer.stop();
      silenceMidi();
      sequencer.setSequence(songs.get(track));
      sequencer.setTickPosition(0);
      sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
      sequencer.start();
    } catch (InvalidMidiDataException ex) {
      closeMidi();
      warning.accept("Cannot play MIDI track " + track + ": " + ex.getMessage());
    }
  }

  @Override
  public void stop() {
    if (closed) return;
    for (Clip clip : clips.values()) clip.stop();
    if (sequencer != null) sequencer.stop();
    silenceMidi();
  }

  private void silenceMidi() {
    if (synthesizer != null && synthesizer.isOpen()) {
      for (var channel : synthesizer.getChannels()) {
        if (channel != null) channel.allSoundOff();
      }
    }
  }

  private void closeClips() {
    for (Clip clip : clips.values()) clip.close();
    clips.clear();
  }

  private void closeMidi() {
    if (sequencer != null) {
      sequencer.close();
      sequencer = null;
    }
    if (synthesizer != null) {
      synthesizer.close();
      synthesizer = null;
    }
    songs.clear();
  }

  @Override
  public void close() {
    if (!closed) {
      stop();
      closeClips();
      closeMidi();
      closed = true;
    }
  }
}
