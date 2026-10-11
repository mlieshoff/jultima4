package org.jultima4;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DemoWorldTest {
    @Test void waterAndMountainsBlockMovementAndPassConsumesATurn() {
        DemoWorld world = new DemoWorld();
        for (int i = 0; i < 5; i++) world.move(-1, 0);
        world.move(0, -1);
        assertEquals(11, world.x());
        assertEquals(16, world.y());
        assertEquals("Blocked!", world.message());
        world.move(-1, 0);
        for (int i = 0; i < 8; i++) world.move(-1, 0);
        world.move(-1, 0);
        assertEquals(2, world.x());
        assertEquals("Blocked!", world.message());
        int turns = world.turns();
        world.pass();
        assertEquals(turns + 1, world.turns());
    }

    @Test void allEightShrinesAreReachableAndRepeatedMeditationDoesNotCountTwice() {
        DemoWorld world = new DemoWorld();
        int[][] shrines = {{5,5},{16,5},{26,5},{5,16},{26,16},{5,26},{16,26},{26,26}};
        for (int i = 0; i < shrines.length; i++) {
            // Return through the east-west corridor, then use the clear shrine columns.
            while (world.y() != 16) world.move(0, Integer.signum(16 - world.y()));
            while (world.x() != shrines[i][0]) world.move(Integer.signum(shrines[i][0] - world.x()), 0);
            while (world.y() != shrines[i][1]) world.move(0, Integer.signum(shrines[i][1] - world.y()));
            world.meditate();
            assertEquals(i + 1, world.visitedCount());
            world.meditate();
            assertEquals(i + 1, world.visitedCount());
        }
        assertTrue(world.complete());
        assertTrue(world.message().contains("quest complete"));
    }

    @Test void meditationRequiresANearbyShrineAndInvalidMovementIsRejected() {
        DemoWorld world = new DemoWorld();
        world.meditate();
        assertEquals(0, world.visitedCount());
        assertEquals("No shrine nearby.", world.message());
        assertThrows(IllegalArgumentException.class, () -> world.move(1, 1));
        assertEquals(1, world.turns());
        assertEquals(world.tile(0, 0), world.tile(32, 32));
    }
}
