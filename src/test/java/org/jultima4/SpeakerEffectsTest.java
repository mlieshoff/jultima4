package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SpeakerEffectsTest {
  @ParameterizedTest
  @EnumSource(SpeakerEffects.Effect.class)
  void pcm_whenEffectRequested_thenProduceDeterministicBoundedAudibleMonoSamples(
      SpeakerEffects.Effect effect) {
    byte[] samples = SpeakerEffects.pcm(effect);
    byte[] repeated = SpeakerEffects.pcm(effect);

    assertPcm(samples, repeated);
  }

  private static void assertPcm(byte[] samples, byte[] repeated) {
    assertThat(samples).isEqualTo(repeated);
    assertThat(samples.length % 2).isZero();
    assertThat(samples.length).isBetween(2, SpeakerEffects.SAMPLE_RATE * 2);
    boolean audible = false;
    for (int i = 0; i < samples.length; i += 2) {
      int sample = (short) ((samples[i] & 255) | samples[i + 1] << 8);
      assertThat(sample).isBetween(-6000, 6000);
      audible |= sample != 0;
    }
    assertThat(audible).isTrue();
    assertThat(
            List.of(
                samples[0], samples[1], samples[samples.length - 2], samples[samples.length - 1]))
        .containsOnly((byte) 0);
  }
}
