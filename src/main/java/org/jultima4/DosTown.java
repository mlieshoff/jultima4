package org.jultima4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public record DosTown(DosMap map, List<Npc> npcs) {
    public DosTown {
        npcs = List.copyOf(npcs);
    }

    public record Npc(int tile, int x, int y, NpcDialogue dialogue) {
    }

    public static DosTown decode(byte[] ult, byte[] tlk) {
        if (ult.length != 1280) {
            throw new IllegalArgumentException("ULT must contain 1280 bytes");
        }
        List<NpcDialogue> dialogues = NpcDialogue.decode(tlk);
        List<Npc> npcs = new ArrayList<>();
        for (int i = 0; i < 32; i++) {
            int tile = Byte.toUnsignedInt(ult[0x400 + i]);
            if (tile == 0) {
                continue;
            }
            int x = Byte.toUnsignedInt(ult[0x420 + i]);
            int y = Byte.toUnsignedInt(ult[0x440 + i]);
            if (x > 31 || y > 31) {
                throw new IllegalArgumentException("Invalid NPC position");
            }
            int conversation = Byte.toUnsignedInt(ult[0x4e0 + i]);
            // Zero and special indexes are handled by original hard-coded vendor logic.
            NpcDialogue dialogue = conversation >= 1 && conversation <= 16 ? dialogues.get(conversation - 1) : null;
            npcs.add(new Npc(tile, x, y, dialogue));
        }
        return new DosTown(DosMap.town(Arrays.copyOf(ult, 1024)), npcs);
    }
}
