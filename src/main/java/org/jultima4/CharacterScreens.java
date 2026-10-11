package org.jultima4;

import java.util.Random;

/** Title, virtue choices, and confirmation of the class selected by those choices. */
public final class CharacterScreens {
    public enum Stage { TITLE, CREATION, SELECTION, PLAYING }
    private final Random random;
    private Stage stage = Stage.TITLE;
    private CharacterCreation creation;

    public CharacterScreens(Random random) { this.random = random; }
    public Stage stage() { return stage; }
    public CharacterCreation creation() {
        if (creation == null) throw new IllegalStateException("No character has been started");
        return creation;
    }
    public void start() { creation = new CharacterCreation(random); stage = Stage.CREATION; }
    public void choose(int option) {
        if (stage != Stage.CREATION) throw new IllegalStateException("Not choosing virtues");
        creation.choose(option);
        if (creation.complete()) stage = Stage.SELECTION;
    }
    public void confirm() {
        if (stage != Stage.SELECTION) throw new IllegalStateException("No selected character to confirm");
        stage = Stage.PLAYING;
    }
    public void title() { creation = null; stage = Stage.TITLE; }
}
