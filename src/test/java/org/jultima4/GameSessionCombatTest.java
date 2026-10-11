package org.jultima4;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class GameSessionCombatTest {
    private static class MaximumRoll extends Random {
        @Override public int nextInt(int bound) { return bound - 1; }
    }

    private GameSession encounter(int health) throws Exception {
        byte[] terrain = new byte[65536];
        Arrays.fill(terrain, (byte) 4);
        DosGameData data = new DosGameData(DosMap.world(terrain), new EgaTiles(new byte[32768]), Map.of());
        GameSession session = new GameSession(data, 50, 50,
            new Avatar(health, 200, 20, Avatar.Weapon.SWORD, 128, 0), CombatMap.load(), new MaximumRoll());
        for (int i = 0; i < 60 && session.battle() == null; i++) session.pass();
        assertNotNull(session.battle(), "Land monsters must spawn and initiate combat");
        return session;
    }

    @Test void victoryRemovesEncounterAndPreservesHealthExperienceAndWorldPosition() throws Exception {
        GameSession session = encounter(1000);
        Battle battle = session.battle();
        int encountersBefore = session.monsters().size();
        OverworldMonsters.Encounter fought = session.monsters().stream().filter(monster ->
            Math.abs(monster.x() - session.x()) + Math.abs(monster.y() - session.y()) <= 1).findFirst().orElseThrow();
        int experience = battle.enemies().stream().mapToInt(enemy -> enemy.type().experience).sum();
        assertThrows(IllegalStateException.class, session::finishCombat);
        assertThrows(IllegalStateException.class, session::enter);
        for (int i = 0; i < 30 && session.avatar().health() == 1000; i++) session.pass();
        for (int i = 0; i < 60 && battle.outcome() == Battle.Outcome.ACTIVE; i++) {
            Battle.Enemy adjacent = battle.enemies().stream().filter(enemy ->
                Math.abs(enemy.x() - battle.x()) + Math.abs(enemy.y() - battle.y()) == 1).findFirst().orElse(null);
            if (adjacent == null) session.pass();
            else session.attack(adjacent.x() - battle.x(), adjacent.y() - battle.y());
        }
        assertEquals(Battle.Outcome.VICTORY, battle.outcome());
        assertEquals(experience, session.avatar().experience());
        int remainingHealth = session.avatar().health();
        assertTrue(remainingHealth < 1000);
        session.finishCombat();
        assertNull(session.battle());
        assertEquals(encountersBefore - 1, session.monsters().size());
        assertFalse(session.monsters().contains(fought));
        assertEquals(remainingHealth, session.avatar().health());
        assertEquals(50, session.x());
        assertEquals(50, session.y());
        session.move(1, 0);
        assertEquals(51, session.x());
    }

    @Test void fleeingKeepsSurvivorsAndGivesTimeToMoveAway() throws Exception {
        GameSession session = encounter(1000);
        int survivors = session.battle().enemies().size();
        for (int i = 0; i < 12 && session.battle().outcome() == Battle.Outcome.ACTIVE; i++) session.move(0, 1);
        assertEquals(Battle.Outcome.FLED, session.battle().outcome());
        session.finishCombat();
        assertEquals(survivors, session.monsters().getFirst().count());
        assertEquals(0, session.avatar().experience());
        for (int i = 0; i < 6; i++) session.move(0, 1);
        assertNull(session.battle());
        assertEquals(56, session.y());
    }

    @Test void defeatPreventsReturningToExploration() throws Exception {
        GameSession session = encounter(1);
        for (int i = 0; i < 60 && session.battle().outcome() == Battle.Outcome.ACTIVE; i++) session.pass();
        assertEquals(Battle.Outcome.DEFEAT, session.battle().outcome());
        assertEquals(0, session.avatar().health());
        assertThrows(IllegalStateException.class, session::finishCombat);
        assertThrows(IllegalStateException.class, session::enter);
        assertThrows(IllegalStateException.class, () -> session.move(1, 0));
    }
}
