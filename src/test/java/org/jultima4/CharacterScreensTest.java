package org.jultima4;

import org.junit.jupiter.api.Test;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CharacterScreensTest {
    @Test void titleRequiresStartingACharacterBeforeChoices() {
        CharacterScreens screens = new CharacterScreens(new Random(17)); screens.title();
        assertEquals(CharacterScreens.Stage.TITLE, screens.stage());
        assertThrows(IllegalStateException.class, screens::creation);
        assertThrows(IllegalStateException.class, () -> screens.choose(0));
        assertThrows(IllegalStateException.class, screens::confirm);
        screens.start();
        assertEquals(CharacterScreens.Stage.CREATION, screens.stage());
        assertEquals(0, screens.creation().choices());
    }

    @Test void sevenChoicesSelectAClassButRequireConfirmationBeforePlaying() throws Exception {
        CharacterScreens screens = new CharacterScreens(new Random(17)); screens.title(); screens.start();
        for (int i = 0; i < 6; i++) { screens.choose(i % 2); assertEquals(CharacterScreens.Stage.CREATION, screens.stage()); }
        screens.choose(0);
        assertEquals(CharacterScreens.Stage.SELECTION, screens.stage());
        assertTrue(screens.creation().complete());
        Virtue winner = screens.creation().winner();
        assertNotNull(Avatar.from(screens.creation()).weapon());
        assertThrows(IllegalStateException.class, () -> screens.choose(0));
        screens.confirm();
        assertEquals(CharacterScreens.Stage.PLAYING, screens.stage());
        assertEquals(winner, screens.creation().winner());
        assertThrows(IllegalStateException.class, screens::confirm);
    }

    @Test void restartDiscardsPreviousSelectionsAndTitleDiscardsTheCharacter() {
        CharacterScreens screens = new CharacterScreens(new Random(17)); screens.title(); screens.start();
        for (int i = 0; i < 7; i++) screens.choose(0);
        CharacterCreation previous = screens.creation();
        screens.start();
        assertNotSame(previous, screens.creation());
        assertEquals(15, screens.creation().strength());
        assertEquals(0, screens.creation().choices());
        screens.title();
        assertEquals(CharacterScreens.Stage.TITLE, screens.stage());
        assertThrows(IllegalStateException.class, screens::creation);
        screens.start();
        assertEquals(CharacterScreens.Stage.CREATION, screens.stage());
    }

    @Test void invalidChoiceDoesNotAdvanceTheCreationScreen() {
        CharacterScreens screens = new CharacterScreens(new Random(17)); screens.title(); screens.start();
        assertThrows(IllegalArgumentException.class, () -> screens.choose(2));
        assertEquals(0, screens.creation().choices());
        assertEquals(CharacterScreens.Stage.CREATION, screens.stage());
    }
}
