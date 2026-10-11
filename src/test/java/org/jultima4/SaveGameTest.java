package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.CRC32;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SaveGameTest {
  private Path directory;
  private Path slot;
  private CharacterCreation character;
  private DosGameData data;

  @BeforeEach
  void setUp() throws Exception {
    Files.createDirectories(Path.of("target"));
    directory = Files.createTempDirectory(Path.of("target"), "save-test-");
    slot = directory.resolve("quest.sav");
    character = new CharacterCreation(new Random(0));
    while (!character.complete()) character.choose(0);
    byte[] world = new byte[65536];
    Arrays.fill(world, (byte) 4);
    byte[] town = new byte[1024];
    Arrays.fill(town, (byte) 4);
    town[15 * 32 + 2] = 59;
    NpcDialogue dialogue =
        new NpcDialogue(
            0,
            List.of(
                "Tester", "He", "A bard", "Music", "Well", "Joy", "Learn", "Sing?", "Good", "Try",
                "musi", "lear"));
    DosTown city = new DosTown(DosMap.town(town), List.of(new DosTown.Npc(34, 3, 15, dialogue)));
    data =
        new DosGameData(
            DosMap.world(world),
            new EgaTiles(new byte[32768]),
            Map.of("BRITAIN", city, "LCB_1", city, "LCB_2", city));
  }

  @AfterEach
  void tearDown() throws IOException {
    if (directory != null) {
      try (var paths = Files.walk(directory)) {
        for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.delete(path);
      }
    }
  }

  private GameSession session(int x, int y) throws IOException {
    return new GameSession(data, x, y, Avatar.from(character), CombatMap.load(), new Random(0));
  }

  @Test
  void load_whenWorldQuestSaved_thenRestoreStatsEquipmentAndMonsters() throws Exception {
    GameSession original = session(50, 50);
    original.avatar().hurt(27);
    original.avatar().awardExperience(99);
    original.avatar().loseThrownDagger();
    for (int i = 0; i < 12; i++) original.pass();
    GameSession.State expected = original.snapshot();
    SaveGame.save(slot, character, original);

    GameSession restored = SaveGame.load(slot, data).session();

    assertThat(restored.snapshot()).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void load_whenCharacterSaved_thenRestoreClassScoresAndAttributes() throws Exception {
    SaveGame.save(slot, character, session(50, 50));

    CharacterCreation restored = SaveGame.load(slot, data).character();

    assertThat(restored).usingRecursiveComparison().isEqualTo(character);
  }

  @Test
  void load_whenTownDoorsOpened_thenRestoreDoorsAndTownPosition() throws Exception {
    GameSession original = session(82, 106);
    original.enter();
    original.openDoor(1, 0);
    original.move(1, 0);
    GameSession.State expected = original.snapshot();
    SaveGame.save(slot, character, original);

    GameSession restored = SaveGame.load(slot, data).session();

    assertThat(restored.snapshot()).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void load_whenTownSaved_thenLeavingReturnsToWorldEntrance() throws Exception {
    GameSession original = session(82, 106);
    original.enter();
    SaveGame.save(slot, character, original);
    GameSession restored = SaveGame.load(slot, data).session();

    restored.leave();

    assertThat(List.of(restored.x(), restored.y(), restored.inTown()))
        .containsExactly(82, 106, false);
  }

  @Test
  void load_whenCastleUpperFloorSaved_thenRestoreFloorAndExit() throws Exception {
    GameSession original = session(86, 107);
    original.enter();
    for (int i = 0; i < 12; i++) original.move(1, 0);
    for (int i = 0; i < 27; i++) original.move(0, -1);
    original.climb();
    SaveGame.save(slot, character, original);

    GameSession restored = SaveGame.load(slot, data).session();

    assertThat(
            List.of(
                restored.locationName(),
                restored.x(),
                restored.y(),
                restored.snapshot().worldX(),
                restored.snapshot().worldY()))
        .containsExactly("Castle Britannia - upper floor", 27, 3, 86, 107);
  }

  @Test
  void load_whenShrineVisited_thenRestoreProgress() throws Exception {
    DosGameData supplied = DosGameData.load();
    GameSession original =
        new GameSession(supplied, 233, 66, Avatar.from(character), CombatMap.load(), new Random(0));
    original.meditate();
    SaveGame.save(slot, character, original);

    GameSession restored = SaveGame.load(slot, supplied).session();

    assertThat(restored.snapshot().shrines()).containsExactly(Virtue.HONESTY);
  }

  @Test
  void save_whenSlotExists_thenReplaceWithLatestProgress() throws Exception {
    GameSession original = session(50, 50);
    SaveGame.save(slot, character, original);
    original.move(1, 0);
    SaveGame.save(slot, character, original);

    GameSession restored = SaveGame.load(slot, data).session();

    assertThat(List.of(restored.x(), restored.y(), restored.turns())).containsExactly(51, 50, 1);
  }

  @Test
  void load_whenSlotMissing_thenReportIOException() {
    assertThatThrownBy(() -> SaveGame.load(slot, data)).isInstanceOf(IOException.class);
  }

  @Test
  void load_whenChecksumDamaged_thenRejectSave() throws Exception {
    SaveGame.save(slot, character, session(50, 50));
    byte[] bytes = Files.readAllBytes(slot);
    bytes[20] ^= 1;
    Files.write(slot, bytes);

    assertThatThrownBy(() -> SaveGame.load(slot, data))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("checksum");
  }

  @Test
  void load_whenVersionUnsupported_thenReportVersionError() throws Exception {
    SaveGame.save(slot, character, session(50, 50));
    byte[] bytes = Files.readAllBytes(slot);
    ByteBuffer.wrap(bytes).putInt(4, 99);
    writeWithChecksum(bytes);

    assertThatThrownBy(() -> SaveGame.load(slot, data))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("version");
  }

  @Test
  void load_whenMagicInvalid_thenRejectOtherFileFormat() throws Exception {
    SaveGame.save(slot, character, session(50, 50));
    byte[] bytes = Files.readAllBytes(slot);
    ByteBuffer.wrap(bytes).putInt(0, 0);
    writeWithChecksum(bytes);

    assertThatThrownBy(() -> SaveGame.load(slot, data))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Not an Ultima");
  }

  @Test
  void load_whenFileTruncated_thenRejectSave() throws Exception {
    SaveGame.save(slot, character, session(50, 50));
    byte[] bytes = Files.readAllBytes(slot);
    Files.write(slot, Arrays.copyOf(bytes, bytes.length / 2));

    assertThatThrownBy(() -> SaveGame.load(slot, data)).isInstanceOf(IOException.class);
  }

  @Test
  void load_whenFileOversized_thenRejectSave() throws Exception {
    Files.write(slot, new byte[1024 * 1024 + 1]);

    assertThatThrownBy(() -> SaveGame.load(slot, data))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("size");
  }

  @Test
  void load_whenCoordinatesInvalidWithValidChecksum_thenRejectState() throws Exception {
    SaveGame.save(slot, character, session(50, 50));
    byte[] bytes = Files.readAllBytes(slot);
    int xOffset = 8 + 2 + character.winner().name().length() + 8 * Integer.BYTES;
    ByteBuffer.wrap(bytes).putInt(xOffset, -1);
    writeWithChecksum(bytes);

    assertThatThrownBy(() -> SaveGame.load(slot, data))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("exploration");
  }

  @Test
  void load_whenPositionOccupiedByNpc_thenRejectSave() throws Exception {
    GameSession original = session(82, 106);
    original.enter();
    SaveGame.save(slot, character, original);
    byte[] bytes = Files.readAllBytes(slot);
    int xOffset = 8 + 2 + character.winner().name().length() + 8 * Integer.BYTES;
    ByteBuffer.wrap(bytes).putInt(xOffset, 3);
    writeWithChecksum(bytes);

    assertThatThrownBy(() -> SaveGame.load(slot, data))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("blocked");
  }

  @Test
  void load_whenPositionOccupiedByMonster_thenRejectSave() throws Exception {
    GameSession original = session(50, 50);
    for (int i = 0; i < 12; i++) original.pass();
    var encounter = original.monsters().getFirst();
    SaveGame.save(slot, character, original);
    byte[] bytes = Files.readAllBytes(slot);
    int xOffset = 8 + 2 + character.winner().name().length() + 8 * Integer.BYTES;
    ByteBuffer.wrap(bytes).putInt(xOffset, encounter.x()).putInt(xOffset + 4, encounter.y());
    writeWithChecksum(bytes);

    assertThatThrownBy(() -> SaveGame.load(slot, data))
        .isInstanceOf(IOException.class)
        .hasMessageContaining("blocked");
  }

  @Test
  void save_whenConversationActive_thenPreservePreviousSave() throws Exception {
    GameSession original = session(82, 106);
    original.enter();
    original.openDoor(1, 0);
    original.move(1, 0);
    SaveGame.save(slot, character, original);
    byte[] previous = Files.readAllBytes(slot);
    original.talk(1, 0);

    Throwable error = catchThrowable(() -> SaveGame.save(slot, character, original));
    byte[] after = Files.readAllBytes(slot);

    assertRejectedSave(error, previous, after);
  }

  @Test
  void save_whenCombatActive_thenRejectPartialBattleSnapshot() throws Exception {
    Random maximum =
        new Random() {
          @Override
          public int nextInt(int bound) {
            return bound - 1;
          }
        };
    GameSession original =
        new GameSession(data, 50, 50, Avatar.from(character), CombatMap.load(), maximum);
    for (int i = 0; i < 60 && original.battle() == null; i++) original.pass();

    assertThatThrownBy(() -> SaveGame.save(slot, character, original))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("combat");
  }

  @Test
  void save_whenDestinationCannotBeReplaced_thenPreserveExistingDataAndCleanTemporaryFile()
      throws Exception {
    Files.createDirectory(slot);
    Path existing = slot.resolve("keep.txt");
    Files.writeString(existing, "keep");
    GameSession original = session(50, 50);

    Throwable error = catchThrowable(() -> SaveGame.save(slot, character, original));
    String preserved = Files.readString(existing);
    List<String> remaining;
    try (var files = Files.list(directory)) {
      remaining = files.map(path -> path.getFileName().toString()).toList();
    }

    assertFailedReplacement(error, preserved, remaining);
  }

  @Test
  void resume_whenCharacterRestored_thenEnterPlayingWithSavedClass() throws Exception {
    SaveGame.save(slot, character, session(50, 50));
    CharacterCreation restored = SaveGame.load(slot, data).character();
    CharacterScreens screens = new CharacterScreens(new Random(0));

    screens.resume(restored);

    assertResumedScreen(screens, character);
  }

  private void writeWithChecksum(byte[] bytes) throws IOException {
    CRC32 checksum = new CRC32();
    checksum.update(bytes, 0, bytes.length - Long.BYTES);
    ByteBuffer.wrap(bytes).putLong(bytes.length - Long.BYTES, checksum.getValue());
    Files.write(slot, bytes);
  }

  private void assertRejectedSave(Throwable error, byte[] previous, byte[] after) {
    assertThat(error).isInstanceOf(IllegalStateException.class);
    assertThat(after).containsExactly(previous);
  }

  private void assertFailedReplacement(Throwable error, String preserved, List<String> remaining) {
    assertThat(error).isInstanceOf(IOException.class);
    assertThat(preserved).isEqualTo("keep");
    assertThat(remaining).containsExactly("quest.sav");
  }

  private void assertResumedScreen(CharacterScreens screens, CharacterCreation expected) {
    assertThat(screens.stage()).isEqualTo(CharacterScreens.Stage.PLAYING);
    assertThat(screens.creation()).usingRecursiveComparison().isEqualTo(expected);
  }
}
