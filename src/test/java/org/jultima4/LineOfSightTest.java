package org.jultima4;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.Random;
import java.util.function.IntBinaryOperator;
import static org.junit.jupiter.api.Assertions.*;

class LineOfSightTest {
    @Test void opaqueTerrainIsSeparateFromMovementCollision() {
        for (int tile : new int[] {6, 8, 57, 58, 59, 73, 127}) assertTrue(LineOfSight.opaque(tile));
        for (int tile : new int[] {0, 4, 5, 7, 22, 32, 48, 55, 68, 69, 192}) assertFalse(LineOfSight.opaque(tile));
        assertTrue(DosMap.walkable(6));
        assertFalse(DosMap.walkable(0));
    }

    @Test void blockerIsVisibleButTilesBehindItAreHiddenInEveryDirection() {
        for (int dx = -1; dx <= 1; dx++) for (int dy = -1; dy <= 1; dy++) {
            if (dx == 0 && dy == 0) continue;
            int blockX = dx * 2, blockY = dy * 2;
            IntBinaryOperator terrain = (x, y) -> x == blockX && y == blockY ? 57 : 4;
            assertTrue(LineOfSight.visible(terrain, 0, 0, blockX, blockY));
            assertFalse(LineOfSight.visible(terrain, 0, 0, dx * 4, dy * 4));
            assertTrue(LineOfSight.visible(terrain, 0, 0, 0, 0));
        }
    }

    @Test void closedDiagonalCornersBlockButASingleCornerAllowsSight() {
        IntBinaryOperator sealed = (x, y) -> (x == 1 && y == 0 || x == 0 && y == 1) ? 57 : 4;
        assertFalse(LineOfSight.visible(sealed, 0, 0, 1, 1));
        assertFalse(LineOfSight.visible(sealed, 1, 1, 0, 0));
        IntBinaryOperator open = (x, y) -> x == 1 && y == 0 ? 57 : 4;
        assertTrue(LineOfSight.visible(open, 0, 0, 1, 1));
        assertTrue(LineOfSight.visible(open, 1, 1, 0, 0));
    }

    @Test void raysAreSymmetricForClearEndpointsAndDoNotMissShallowOrSteepBlockers() {
        byte[] tiles = new byte[121]; Arrays.fill(tiles, (byte) 4);
        Random random = new Random(17);
        for (int i = 0; i < 20; i++) tiles[random.nextInt(121)] = 57;
        IntBinaryOperator terrain = (x, y) -> tiles[y * 11 + x];
        for (int a = 0; a < 121; a++) for (int b = 0; b < 121; b++) {
            if (tiles[a] == 57 || tiles[b] == 57) continue;
            assertEquals(LineOfSight.visible(terrain, a % 11, a / 11, b % 11, b / 11),
                LineOfSight.visible(terrain, b % 11, b / 11, a % 11, a / 11));
        }
        IntBinaryOperator shallow = (x, y) -> x == 2 && y == 1 ? 8 : 4;
        assertFalse(LineOfSight.visible(shallow, 0, 0, 6, 2));
        IntBinaryOperator steep = (x, y) -> x == 1 && y == 2 ? 8 : 4;
        assertFalse(LineOfSight.visible(steep, 0, 0, 2, 6));
    }

    @Test void observerStandingInForestCanSeeOutWhileNextForestBlocksTheView() {
        IntBinaryOperator terrain = (x, y) -> (x == 0 || x == 2) && y == 0 ? 6 : 4;
        assertTrue(LineOfSight.visible(terrain, 0, 0, 1, 0));
        assertTrue(LineOfSight.visible(terrain, 0, 0, 2, 0));
        assertFalse(LineOfSight.visible(terrain, 0, 0, 3, 0));
    }
}
