package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TitleSceneTest {
  @Test
  void load_whenOriginalExecutableSupplied_thenShowVerifiedTerrain() throws Exception {
    TitleScene scene = TitleScene.load();

    List<Integer> tiles = scene.tiles(0);

    assertThat(tiles)
        .hasSize(95)
        .startsWith(6, 6, 6, 4, 4, 4, 1, 1, 0, 0, 0, 0, 1, 4, 4, 13, 14, 15, 4);
  }

  @Test
  void tiles_whenOriginalScriptMovesShip_thenReplayCommandedSpriteFrames() throws Exception {
    TitleScene scene = TitleScene.load();

    List<Integer> frames =
        List.of(
            scene.tiles(1.14).get(2 * 19 + 17),
            scene.tiles(1.21).get(2 * 19 + 17),
            scene.tiles(1.28).get(2 * 19 + 17));

    assertThat(frames).containsExactly(64, 65, 66);
  }

  @Test
  void tiles_whenFullOriginalSequenceElapses_thenLoopBackToTerrain() throws Exception {
    TitleScene scene = TitleScene.load();
    List<Integer> beginning = scene.tiles(0);

    List<Integer> repeated = scene.tiles(scene.durationSeconds());

    assertThat(repeated).containsExactlyElementsOf(beginning);
  }

  @Test
  void tiles_whenTwoLoopsAndSceneOffsetElapse_thenReplaySameFrame() throws Exception {
    TitleScene scene = TitleScene.load();
    List<Integer> expected = scene.tiles(10);

    List<Integer> repeated = scene.tiles(scene.durationSeconds() * 2 + 10);

    assertThat(repeated).containsExactlyElementsOf(expected);
  }

  @Test
  void tiles_whenSpriteRemoved_thenRestoreUnderlyingTerrain() throws Exception {
    TitleScene scene = TitleScene.decode(program(0x27, 0x21, 0x80, 0x77, 0x80, 0xff));

    List<Integer> tiles = List.of(scene.tiles(0).get(39), scene.tiles(0.07).get(39));

    assertThat(tiles).containsExactly(37, 4);
  }

  @Test
  void tiles_whenActorMoves_thenRestorePreviousCell() throws Exception {
    TitleScene scene = TitleScene.decode(program(0x27, 0x01, 0x80, 0x27, 0x02, 0x80, 0xff));

    List<Integer> tiles = List.of(scene.tiles(0.07).get(39), scene.tiles(0.07).get(40));

    assertThat(tiles).containsExactly(4, 36);
  }

  @Test
  void tiles_whenActorOverlapsAnother_thenUseOriginalActorOrder() throws Exception {
    TitleScene scene = TitleScene.decode(program(0x20, 0x01, 0x27, 0x01, 0x80, 0xff));

    int tile = scene.tiles(0).get(39);

    assertThat(tile).isEqualTo(36);
  }

  @Test
  void tiles_whenTimeInvalid_thenRejectInsteadOfIndexingFrames() throws Exception {
    TitleScene scene = TitleScene.load();

    assertInvalidTimes(scene);
  }

  @Test
  void decode_whenExecutableTruncated_thenRejectMissingData() {
    assertThatThrownBy(() -> TitleScene.decode(new byte[30981]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Missing");
  }

  @Test
  void decode_whenCommandInvalid_thenRejectScript() throws Exception {
    byte[] executable = program(0x90, 0xff);

    assertThatThrownBy(() -> TitleScene.decode(executable))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decode_whenSpriteOutsideScene_thenRejectCoordinates() throws Exception {
    byte[] executable = program(0x27, 0x1f, 0x80, 0xff);

    assertThatThrownBy(() -> TitleScene.decode(executable))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("sprite");
  }

  @Test
  void decode_whenActorIndexInvalid_thenRejectSprite() throws Exception {
    byte[] executable = program(0x2f, 0x01, 0x80, 0xff);

    assertThatThrownBy(() -> TitleScene.decode(executable))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("actor");
  }

  @Test
  void decode_whenScriptHasNoFrames_thenRejectIncompleteDemonstration() throws Exception {
    byte[] executable = program(0xff);

    assertThatThrownBy(() -> TitleScene.decode(executable))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Incomplete");
  }

  @Test
  void decode_whenTerminatorMissing_thenRejectIncompleteDemonstration() throws Exception {
    byte[] executable = program(0x80);
    Arrays.fill(executable, 30435, 30982, (byte) 0x80);

    assertThatThrownBy(() -> TitleScene.decode(executable))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Incomplete");
  }

  @Test
  void tiles_whenReadingFrame_thenKeepImmutableSnapshot() throws Exception {
    List<Integer> tiles = TitleScene.load().tiles(0);

    assertThatThrownBy(() -> tiles.set(0, 99)).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void load_whenTitleDisplayed_thenProvideOrderedRequestedCreditsAndMenu() {
    assertPresentation(TitleScene.CREDITS, TitleScene.MENU);
  }

  private byte[] program(int... commands) throws Exception {
    byte[] executable = DosGameData.read("TITLE.EXE");
    Arrays.fill(executable, 30339, 30434, (byte) 4);
    for (int i = 0; i < commands.length; i++) executable[30434 + i] = (byte) commands[i];
    return executable;
  }

  private void assertInvalidTimes(TitleScene scene) {
    assertThatThrownBy(() -> scene.tiles(-1)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> scene.tiles(Double.NaN)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> scene.tiles(Double.POSITIVE_INFINITY))
        .isInstanceOf(IllegalArgumentException.class);
  }

  private void assertPresentation(List<String> credits, List<String> menu) {
    assertThat(credits).containsExactly("Lord British", "and", "Origin Systems, Inc.", "presents");
    assertThat(menu)
        .containsExactly("B - Begin new quest", "J - Journey onward", "R - Return to the view");
  }
}
