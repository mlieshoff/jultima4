package org.jultima4;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class VisibilityIntegrationTest {
    private byte[] grass(int size) { byte[] tiles = new byte[size]; Arrays.fill(tiles, (byte) 4); return tiles; }
    private DosGameData data(byte[] world, Map<String, DosTown> towns) {
        return new DosGameData(DosMap.world(world), new EgaTiles(new byte[32768]), towns);
    }
    private void worldTile(byte[] tiles, int x, int y, int tile) {
        tiles[(y / 32 * 8 + x / 32) * 1024 + y % 32 * 32 + x % 32] = (byte) tile;
    }

    @Test void worldSightWrapsUsingTheShortPathAndStaysInsideTheViewport() {
        byte[] world = grass(65536);
        worldTile(world, 255, 0, 8);
        GameSession session = new GameSession(data(world, Map.of()), 0, 0);
        assertTrue(session.visible(255, 0));
        assertTrue(session.visible(-1, 0));
        assertFalse(session.visible(254, 0));
        assertFalse(session.visible(-2, 0));
        assertTrue(session.visible(0, 255));
        assertTrue(session.visible(7, 7));
        assertFalse(session.visible(8, 0));
        assertFalse(session.visible(128, 0));
        session.move(0, -1);
        assertTrue(session.visible(0, 0));
    }

    @Test void townDoorsRevealTerrainAndNpcsImmediatelyAndTownEdgesDoNotWrap() {
        byte[] town = grass(1024);
        for (int y = 0; y < 32; y++) town[y * 32 + 2] = 57;
        town[15 * 32 + 2] = 59;
        DosTown city = new DosTown(DosMap.town(town), List.of(new DosTown.Npc(34, 3, 15, null)));
        GameSession session = new GameSession(data(grass(65536), Map.of("BRITAIN", city)), 82, 106);
        session.enter();
        assertTrue(session.visible(2, 15));
        assertFalse(session.visible(3, 15));
        assertFalse(session.visible(4, 15));
        assertFalse(session.visible(-1, 15));
        session.openDoor(1, 0);
        assertTrue(session.visible(3, 15));
        assertTrue(session.visible(4, 15), "NPCs do not block sight");
        session.leave(); session.enter();
        assertTrue(session.visible(4, 15), "Opened doors retain their visibility state");
    }

    private Battle battle(byte[] terrain, int enemyY) {
        List<CombatMap.Position> starts = new ArrayList<>(); starts.add(new CombatMap.Position(5, enemyY));
        for (int i = 1; i < 16; i++) starts.add(new CombatMap.Position(i % 11, i / 11));
        Avatar hero = new Avatar(200, 200, 40, Avatar.Weapon.SLING, 128, 0);
        Random high = new Random() { @Override public int nextInt(int bound) { return bound - 1; } };
        return new Battle(new CombatMap("test", terrain, starts, new CombatMap.Position(5, 9)), hero, MonsterType.SKELETON, 1, high);
    }

    @Test void forestHidesDistantCombatantsAndBlocksRangedAttacksAndTheirAnimationPath() {
        byte[] terrain = grass(121); terrain[6 * 11 + 5] = 6;
        Battle battle = battle(terrain, 3);
        assertTrue(battle.visible(5, 6));
        assertFalse(battle.visible(5, 3));
        assertFalse(battle.visible(-1, 9));
        int health = battle.enemies().getFirst().health();
        battle.attack(0, -1);
        assertEquals(Battle.Outcome.ACTIVE, battle.outcome());
        assertEquals(health, battle.enemies().getFirst().health());
        assertEquals(6, battle.events().getFirst().toY());
        assertTrue(battle.message().contains("blocked"));
        assertTrue(battle.visibleFrom(5, 5, 5, 3), "Changing observer position changes visibility");
    }

    @Test void enemiesOnTheBlockingTileAreVisibleAndCanBeHit() {
        byte[] terrain = grass(121); terrain[6 * 11 + 5] = 6;
        Battle battle = battle(terrain, 6);
        assertTrue(battle.visible(5, 6));
        battle.attack(0, -1);
        assertEquals(Battle.Outcome.VICTORY, battle.outcome());
    }
}
