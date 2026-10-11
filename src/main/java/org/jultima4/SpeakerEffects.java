package org.jultima4;

import java.util.Random;

/** Mono PCM recreations of the speaker patterns in SUBS $1962-$1A7E and NEWGAME $6838. */
final class SpeakerEffects {
  static final int SAMPLE_RATE = 22050;

  enum Effect {
    STEP,
    BLOCKED,
    ERROR,
    ATTACK,
    HIT,
    MISS,
    DEATH,
    CHOICE,
    PORTAL
  }

  private SpeakerEffects() {}

  static byte[] pcm(Effect effect) {
    double duration =
        switch (effect) {
          case STEP -> 0.025;
          case BLOCKED, ERROR, CHOICE -> 0.10;
          case ATTACK, HIT, MISS -> 0.13;
          case DEATH -> 0.28;
          case PORTAL -> 0.6;
        };
    byte[] pcm = new byte[(int) (SAMPLE_RATE * duration) * 2];
    Random noise = new Random(0x1962 + effect.ordinal());
    double phase = 0;
    double frequency = 800;
    for (int i = 0; i < pcm.length / 2; i++) {
      double progress = (double) i / (pcm.length / 2 - 1);
      // Random delay loops yield the original click/noise character; use a separate fixed RNG.
      if (i % 32 == 0) frequency = 350 + noise.nextInt(1700);
      double hz =
          switch (effect) {
            case STEP, HIT -> frequency;
            case BLOCKED -> 420;
            case ERROR -> progress < 0.5 ? 660 : 330;
            case ATTACK -> 400 + 1400 * progress;
            case MISS -> 1800 - 1300 * progress;
            case DEATH -> 800 - 650 * progress;
            case CHOICE -> 880;
            case PORTAL -> 220 + 1600 * Math.sin(Math.PI * progress);
          };
      phase += hz / SAMPLE_RATE;
      double envelope = Math.min(1, i / 64.0) * Math.min(1, (pcm.length / 2 - 1 - i) / 128.0);
      short sample = (short) ((phase % 1 < 0.5 ? 1 : -1) * 6000 * envelope);
      pcm[i * 2] = (byte) sample;
      pcm[i * 2 + 1] = (byte) (sample >>> 8);
    }
    return pcm;
  }
}
