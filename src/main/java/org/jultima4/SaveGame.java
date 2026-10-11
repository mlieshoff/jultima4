package org.jultima4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.zip.CRC32;

/** Application save format, independent of the supplied DOS save files. */
final class SaveGame {
  static final Path DEFAULT_PATH = Path.of("saves", "ultima4.sav");
  private static final int MAGIC = 0x4a553453;
  private static final int VERSION = 1;
  private static final int MAX_BYTES = 1024 * 1024;

  record Loaded(CharacterCreation character, GameSession session) {}

  private SaveGame() {}

  static void save(Path path, CharacterCreation character, GameSession session) throws IOException {
    CharacterCreation.State creation = character.snapshot();
    GameSession.State state = session.snapshot();
    if (state.avatar().strength() != character.strength()
        || state.avatar().dexterity() != character.dexterity()) {
      throw new IllegalArgumentException("Character and Avatar stats do not match");
    }
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (DataOutputStream output = new DataOutputStream(bytes)) {
      output.writeInt(MAGIC);
      output.writeInt(VERSION);
      output.writeUTF(creation.winner().name());
      for (int score : creation.scores()) output.writeInt(score);
      writeSession(output, state);
      CRC32 checksum = new CRC32();
      checksum.update(bytes.toByteArray());
      output.writeLong(checksum.getValue());
    }
    Path destination = path.toAbsolutePath();
    Files.createDirectories(destination.getParent());
    Path temporary = Files.createTempFile(destination.getParent(), ".ultima4-", ".tmp");
    try {
      Files.write(temporary, bytes.toByteArray());
      // Fail instead of risking the existing slot if the filesystem cannot replace atomically.
      Files.move(
          temporary,
          destination,
          StandardCopyOption.ATOMIC_MOVE,
          StandardCopyOption.REPLACE_EXISTING);
    } finally {
      Files.deleteIfExists(temporary);
    }
  }

  static Loaded load(Path path, DosGameData data) throws IOException {
    byte[] bytes;
    try (InputStream input = Files.newInputStream(path)) {
      bytes = input.readNBytes(MAX_BYTES + 1);
    }
    if (bytes.length < 16 || bytes.length > MAX_BYTES) {
      throw new IOException("Invalid save file size");
    }
    int payloadSize = bytes.length - Long.BYTES;
    CRC32 checksum = new CRC32();
    checksum.update(bytes, 0, payloadSize);
    try (DataInputStream trailer =
        new DataInputStream(new ByteArrayInputStream(bytes, payloadSize, Long.BYTES))) {
      if (trailer.readLong() != checksum.getValue()) {
        throw new IOException("Save file is damaged (checksum mismatch)");
      }
    }
    try (DataInputStream input =
        new DataInputStream(new ByteArrayInputStream(bytes, 0, payloadSize))) {
      if (input.readInt() != MAGIC) throw new IOException("Not an Ultima IV desktop save");
      if (input.readInt() != VERSION) throw new IOException("Unsupported save version");
      Virtue winner = Virtue.valueOf(input.readUTF());
      List<Integer> scores = new ArrayList<>();
      for (int i = 0; i < Virtue.values().length; i++) scores.add(input.readInt());
      CharacterCreation character =
          CharacterCreation.restore(new CharacterCreation.State(winner, scores));
      GameSession.State state = readSession(input);
      if (input.available() != 0) throw new IOException("Unexpected data in save file");
      if (state.avatar().strength() != character.strength()
          || state.avatar().dexterity() != character.dexterity()) {
        throw new IllegalArgumentException("Saved character and Avatar stats do not match");
      }
      return new Loaded(character, GameSession.restore(data, state));
    } catch (IllegalArgumentException | NullPointerException ex) {
      throw new IOException("Invalid save data: " + ex.getMessage(), ex);
    }
  }

  private static void writeSession(DataOutputStream output, GameSession.State state)
      throws IOException {
    output.writeInt(state.x());
    output.writeInt(state.y());
    output.writeInt(state.worldX());
    output.writeInt(state.worldY());
    output.writeUTF(state.townKey() == null ? "" : state.townKey());
    output.writeInt(state.turns());
    Avatar.State avatar = state.avatar();
    output.writeInt(avatar.maximumHealth());
    output.writeInt(avatar.health());
    output.writeInt(avatar.strength());
    output.writeInt(avatar.dexterity());
    output.writeUTF(avatar.weapon().name());
    output.writeInt(avatar.defense());
    output.writeInt(avatar.experience());
    output.writeInt(state.changedTiles().size());
    for (var town : state.changedTiles().entrySet()) {
      output.writeUTF(town.getKey());
      output.writeInt(town.getValue().size());
      for (var tile : town.getValue().entrySet()) {
        output.writeInt(tile.getKey());
        output.writeInt(tile.getValue());
      }
    }
    output.writeInt(state.shrines().size());
    for (Virtue shrine : state.shrines()) output.writeUTF(shrine.name());
    output.writeInt(state.monsters().turns());
    output.writeInt(state.monsters().escapeGrace());
    output.writeInt(state.monsters().encounters().size());
    for (OverworldMonsters.Encounter encounter : state.monsters().encounters()) {
      output.writeUTF(encounter.type().name());
      output.writeInt(encounter.count());
      output.writeInt(encounter.x());
      output.writeInt(encounter.y());
    }
  }

  private static GameSession.State readSession(DataInputStream input) throws IOException {
    int x = input.readInt(),
        y = input.readInt(),
        worldX = input.readInt(),
        worldY = input.readInt();
    String town = input.readUTF();
    int turns = input.readInt();
    Avatar.State avatar =
        new Avatar.State(
            input.readInt(),
            input.readInt(),
            input.readInt(),
            input.readInt(),
            Avatar.Weapon.valueOf(input.readUTF()),
            input.readInt(),
            input.readInt());
    Map<String, Map<Integer, Integer>> changedTiles = new HashMap<>();
    int towns = count(input, 17);
    for (int i = 0; i < towns; i++) {
      String key = input.readUTF();
      Map<Integer, Integer> tiles = new HashMap<>();
      int tileCount = count(input, 1024);
      for (int j = 0; j < tileCount; j++) {
        if (tiles.put(input.readInt(), input.readInt()) != null) {
          throw new IOException("Duplicate saved door");
        }
      }
      if (changedTiles.put(key, tiles) != null) throw new IOException("Duplicate saved town");
    }
    var shrines = new HashSet<Virtue>();
    int shrineCount = count(input, 7);
    for (int i = 0; i < shrineCount; i++) {
      if (!shrines.add(Virtue.valueOf(input.readUTF())))
        throw new IOException("Duplicate saved shrine");
    }
    int monsterTurns = input.readInt(), escapeGrace = input.readInt();
    List<OverworldMonsters.Encounter> encounters = new ArrayList<>();
    int monsterCount = count(input, 8);
    for (int i = 0; i < monsterCount; i++) {
      encounters.add(
          new OverworldMonsters.Encounter(
              MonsterType.valueOf(input.readUTF()),
              input.readInt(),
              input.readInt(),
              input.readInt()));
    }
    return new GameSession.State(
        x,
        y,
        worldX,
        worldY,
        town.isEmpty() ? null : town,
        turns,
        avatar,
        changedTiles,
        shrines,
        new OverworldMonsters.State(monsterTurns, escapeGrace, encounters));
  }

  private static int count(DataInputStream input, int maximum) throws IOException {
    int count = input.readInt();
    if (count < 0 || count > maximum) throw new IOException("Invalid save collection size");
    return count;
  }
}
