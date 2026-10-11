package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GameSessionTest {
  private DosGameData fixture() {
    byte[] world = new byte[65536];
    Arrays.fill(world, (byte) 4);
    world[(106 / 32 * 8 + 82 / 32) * 1024 + 106 % 32 * 32 + 82 % 32] = 10;
    byte[] town = new byte[1024];
    Arrays.fill(town, (byte) 4);
    town[15 * 32 + 2] = 59;
    town[14 * 32 + 1] = 58;
    NpcDialogue dialogue =
        new NpcDialogue(
            3,
            List.of(
                "Tester",
                "He",
                "A bard",
                "I play music.",
                "Quite well.",
                "Music brings joy.",
                "Keep learning.",
                "Dost thou sing?",
                "Good!",
                "Try it!",
                "musi",
                "lear"));
    DosTown city = new DosTown(DosMap.town(town), List.of(new DosTown.Npc(34, 3, 15, dialogue)));
    return new DosGameData(
        DosMap.world(world),
        new EgaTiles(new byte[32768]),
        Map.of("BRITAIN", city, "LCB_1", city, "LCB_2", city));
  }

  @Test
  void enteringAndLeavingRestoreTheWorldPosition() {
    GameSession session = new GameSession(fixture(), 82, 106);
    session.enter();
    assertTrue(session.inTown());
    assertEquals("Britain", session.locationName());
    assertEquals(1, session.x());
    assertEquals(15, session.y());
    session.move(-1, 0);
    session.move(-1, 0);
    assertFalse(session.inTown());
    assertEquals(82, session.x());
    assertEquals(106, session.y());
    assertEquals(3, session.turns());
  }

  @Test
  void doorsAndNpcsBlockMovementAndOpenedDoorsPersist() {
    GameSession session = new GameSession(fixture(), 82, 106);
    session.enter();
    session.move(1, 0);
    assertEquals(1, session.x());
    session.openDoor(0, -1);
    assertEquals("Locked!", session.message());
    session.openDoor(1, 0);
    session.move(1, 0);
    assertEquals(2, session.x());
    session.move(1, 0);
    assertEquals(2, session.x());
    assertEquals("Blocked!", session.message());
    assertEquals(34, session.tile(3, 15));
    session.leave();
    session.enter();
    assertEquals(22, session.tile(2, 15));
  }

  @Test
  void dialogueSupportsTopicsQuestionsYesNoAndGoodbye() {
    GameSession session = new GameSession(fixture(), 82, 106);
    session.enter();
    session.openDoor(1, 0);
    session.move(1, 0);
    session.talk(1, 0);
    assertTrue(session.talking());
    assertEquals("Tester", session.npcName());
    session.ask("job");
    assertTrue(session.message().contains("Dost thou sing?"));
    session.ask("maybe");
    assertTrue(session.message().contains("YES or NO"));
    session.ask("YES");
    assertEquals("Good!", session.message());
    session.ask("music");
    assertEquals("Music brings joy.", session.message());
    session.ask("nonsense");
    assertTrue(session.message().contains("cannot help"));
    assertThrows(IllegalStateException.class, () -> session.move(0, -1));
    session.ask("BYE");
    assertFalse(session.talking());
    assertThrows(IllegalStateException.class, () -> session.ask("job"));
  }

  @Test
  void surfaceShrineVisitsRequireAShrineAndDoNotCountTwice() throws Exception {
    DosGameData data = DosGameData.load();
    GameSession session = new GameSession(data, 233, 66);
    session.meditate();
    assertEquals(1, session.visitedCount());
    assertTrue(session.message().contains("HONESTY"));
    session.meditate();
    assertEquals(1, session.visitedCount());
    GameSession onLand = new GameSession(data, 83, 105);
    onLand.meditate();
    assertEquals(0, onLand.visitedCount());
    assertEquals("No shrine here.", onLand.message());
  }

  @Test
  void worldMovementWrapsAndInvalidDirectionsDoNotConsumeTurns() {
    GameSession session = new GameSession(fixture(), 0, 0);
    session.move(-1, 0);
    assertEquals(255, session.x());
    session.move(0, -1);
    assertEquals(255, session.y());
    assertThrows(IllegalArgumentException.class, () -> session.move(1, 1));
    assertEquals(2, session.turns());
    session.pass();
    assertEquals(3, session.turns());
  }

  @Test
  void castleStairsSwitchBothFloorsWithoutLosingWorldPosition() {
    GameSession session = new GameSession(fixture(), 86, 107);
    session.enter();
    for (int i = 0; i < 12; i++) session.move(1, 0);
    for (int i = 0; i < 27; i++) session.move(0, -1);
    assertEquals(27, session.x());
    assertEquals(3, session.y());
    session.climb();
    assertTrue(session.locationName().contains("upper floor"));
    session.climb();
    assertEquals("Castle Britannia", session.locationName());
    session.leave();
    assertEquals(86, session.x());
    assertEquals(107, session.y());
  }

  @Test
  void allClassStartsMatchTheSuppliedWorldAndCastleEntryRestoresPosition() throws Exception {
    DosGameData data = DosGameData.load();
    int[][] starts = {
      {231, 136}, {83, 105}, {35, 221}, {59, 44}, {158, 21}, {105, 183}, {23, 129}, {186, 171}
    };
    for (int[] start : starts) assertDoesNotThrow(() -> new GameSession(data, start[0], start[1]));
    GameSession session = new GameSession(data, 86, 107);
    session.enter();
    assertEquals("Castle Britannia", session.locationName());
    session.leave();
    assertEquals(86, session.x());
    assertEquals(107, session.y());
  }
}
