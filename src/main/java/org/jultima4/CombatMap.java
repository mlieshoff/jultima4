package org.jultima4;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** DOS .CON battlefield: spawn coordinates followed by an 11x11 tile matrix. */
public record CombatMap(String name, byte[] terrain, List<Position> monsterStarts, Position avatarStart) {
    public static final int SIZE = 11;
    public record Position(int x, int y) {
        public Position {
            if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) {
                throw new IllegalArgumentException("Invalid combat position");
            }
        }
    }

    public CombatMap {
        if (terrain.length != SIZE * SIZE) {
            throw new IllegalArgumentException("Combat map must contain 121 tiles");
        }
        terrain = terrain.clone();
        monsterStarts = List.copyOf(monsterStarts);
        if (monsterStarts.size() != 16) {
            throw new IllegalArgumentException("Combat map must contain sixteen monster starts");
        }
    }

    @Override public byte[] terrain() {
        return terrain.clone();
    }

    public static CombatMap decode(String name, byte[] data) {
        if (data.length != 192) {
            throw new IllegalArgumentException("CON must contain 192 bytes");
        }
        List<Position> starts = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            starts.add(new Position(Byte.toUnsignedInt(data[i]), Byte.toUnsignedInt(data[16 + i])));
        }
        return new CombatMap(name, Arrays.copyOfRange(data, 64, 185), starts,
            new Position(Byte.toUnsignedInt(data[32]), Byte.toUnsignedInt(data[40])));
    }

    public int tile(int x, int y) {
        if (x < 0 || y < 0 || x >= SIZE || y >= SIZE) {
            return 126;
        }
        return Byte.toUnsignedInt(terrain[y * SIZE + x]);
    }

    public static String forTerrain(int tile) {
        return switch (tile) {
            case 3 -> "MARSH";
            case 5 -> "BRUSH";
            case 6 -> "FOREST";
            case 7 -> "HILL";
            case 22 -> "DUNGEON";
            case 62 -> "BRICK";
            case 23, 25, 26, 63 -> "BRIDGE";
            default -> "GRASS";
        };
    }

    public static Map<String, CombatMap> load() throws IOException {
        Map<String, CombatMap> result = new LinkedHashMap<>();
        for (String name : List.of("GRASS", "MARSH", "BRUSH", "FOREST", "HILL", "DUNGEON", "BRICK", "BRIDGE")) {
            try {
                result.put(name, decode(name, DosGameData.read(name + ".CON")));
            } catch (IllegalArgumentException ex) {
                throw new IOException("Invalid combat map: " + name, ex);
            }
        }
        return Map.copyOf(result);
    }
}
