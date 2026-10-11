package org.jultima4;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

class DosFormatsTest {
    @Test void worldUsesRowMajorChunksAndWrapsAtBothEdges() {
        byte[] bytes = new byte[65536];
        bytes[31 * 32 + 31] = 4;
        bytes[1024] = 10;
        bytes[8 * 1024] = 11;
        bytes[65535] = (byte) 200;
        DosMap map = DosMap.world(bytes);
        assertEquals(4, map.tile(31, 31));
        assertEquals(10, map.tile(32, 0));
        assertEquals(11, map.tile(0, 32));
        assertEquals(200, map.tile(-1, -1));
        assertEquals(map.tile(0, 0), map.tile(256, 256));
        bytes[1024] = 9;
        assertEquals(10, map.tile(32, 0));
        assertThrows(IllegalArgumentException.class, () -> DosMap.world(new byte[65535]));
    }

    @Test void townIsLinearAndDoesNotWrap() {
        byte[] bytes = new byte[1024];
        bytes[32 + 2] = (byte) 127;
        DosMap map = DosMap.town(bytes);
        assertEquals(127, map.tile(2, 1));
        assertEquals(126, map.tile(-1, 0));
        assertEquals(126, map.tile(0, 32));
        assertFalse(DosMap.walkable(127));
        assertTrue(DosMap.walkable(62));
        assertFalse(DosMap.walkable(1));
        assertFalse(DosMap.walkable(8));
    }

    @Test void egaDecodesHighNibbleFirstAndKeepsTileRowsSeparate() {
        byte[] bytes = new byte[32768];
        bytes[0] = (byte) 0x1e;
        bytes[128 + 8] = (byte) 0x6f;
        EgaTiles tiles = new EgaTiles(bytes);
        assertEquals(0x0000aaff, tiles.rgba(0, 0, 0));
        assertEquals(0xffff55ff, tiles.rgba(0, 1, 0));
        assertEquals(0xaa5500ff, tiles.rgba(1, 0, 1));
        assertEquals(0xffffffff, tiles.rgba(1, 1, 1));
        assertEquals(0x000000ff, tiles.rgba(1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> tiles.rgba(256, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new EgaTiles(new byte[128]));
    }

    @Test void truncatedAndUnterminatedDialogueRecordsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> NpcDialogue.decode(new byte[288]));
        byte[] bytes = new byte[4608];
        Arrays.fill(bytes, 3, 288, (byte) 'X');
        assertThrows(IllegalArgumentException.class, () -> NpcDialogue.decode(bytes));
        assertThrows(IllegalArgumentException.class, () -> DosTown.decode(new byte[1279], new byte[4608]));
    }

    @Test void suppliedDataLoadsWithKnownLandmarksAndIolosConversation() throws Exception {
        DosGameData data = DosGameData.load();
        assertEquals(17, data.towns().size());
        assertEquals(10, data.world().tile(82, 106));
        assertEquals(14, data.world().tile(86, 107));
        assertEquals(30, data.world().tile(233, 66));
        for (DosGameData.Location location : DosGameData.LOCATIONS) {
            DosTown town = data.towns().get(location.file());
            assertTrue(DosMap.walkable(town.map().tile(location.entryX(), location.entryY())), location.name());
        }
        DosTown.Npc iolo = data.towns().get("BRITAIN").npcs().stream()
            .filter(npc -> npc.dialogue() != null && npc.dialogue().name().equals("Iolo")).findFirst().orElseThrow();
        assertEquals(28, iolo.x());
        assertEquals(29, iolo.y());
        assertEquals("I play for\nthe people.", iolo.dialogue().response(3));
        assertTrue(iolo.dialogue().response(5).contains("lute"));
        assertEquals(4, iolo.dialogue().topic("  HEALTH  "));
        assertEquals(-1, iolo.dialogue().topic("unknown"));
    }
}
