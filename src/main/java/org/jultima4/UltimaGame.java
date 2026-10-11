package org.jultima4;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.io.IOException;
import java.util.Random;

public final class UltimaGame extends ApplicationAdapter {
    private final FitViewport viewport = new FitViewport(1000, 720);
    private final TextureRegion[] tiles = new TextureRegion[256];
    private final StringBuilder question = new StringBuilder();
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture sheet;
    private DosGameData data;
    private CharacterCreation creation;
    private GameSession world;
    private String directionAction;
    private char directionCharacter;

    @Override public void create() {
        try {
            data = DosGameData.load();
        } catch (IOException | IllegalArgumentException ex) {
            throw new IllegalStateException("Cannot load the supplied DOS game data", ex);
        }
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.2f);
        Pixmap pixels = new Pixmap(256, 256, Pixmap.Format.RGBA8888);
        try {
            for (int tile = 0; tile < 256; tile++) {
                for (int y = 0; y < 16; y++) {
                    for (int x = 0; x < 16; x++) {
                        pixels.drawPixel(tile % 16 * 16 + x, tile / 16 * 16 + y, data.shapes().rgba(tile, x, y));
                    }
                }
            }
            sheet = new Texture(pixels);
        } finally {
            pixels.dispose();
        }
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (int i = 0; i < tiles.length; i++) tiles[i] = new TextureRegion(sheet, i % 16 * 16, i / 16 * 16, 16, 16);
        restart();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int key) { return handleKey(key); }
            @Override public boolean keyTyped(char character) {
                if (directionCharacter != 0) {
                    char suppressed = directionCharacter;
                    directionCharacter = 0;
                    if (Character.toLowerCase(character) == suppressed) return true;
                }
                if (world != null && world.talking() && character >= 32 && character <= 126 && question.length() < 40) {
                    question.append(character);
                    return true;
                }
                return false;
            }
        });
    }

    private boolean handleKey(int key) {
        if (world != null && world.talking()) {
            switch (key) {
                case Input.Keys.ESCAPE -> { world.endConversation(); question.setLength(0); }
                case Input.Keys.ENTER -> { world.ask(question.toString()); question.setLength(0); }
                case Input.Keys.BACKSPACE -> { if (!question.isEmpty()) question.setLength(question.length() - 1); }
                default -> { return false; }
            }
            return true;
        }
        if (key == Input.Keys.ESCAPE) {
            if (directionAction != null) directionAction = null;
            else Gdx.app.exit();
            return true;
        }
        if (key == Input.Keys.R) { restart(); return true; }
        if (!creation.complete()) {
            if (key != Input.Keys.A && key != Input.Keys.B) return false;
            creation.choose(key == Input.Keys.A ? 0 : 1);
            if (creation.complete()) world = new GameSession(data, creation.startingX(), creation.startingY());
            return true;
        }
        int dx = 0, dy = 0;
        switch (key) {
            case Input.Keys.UP, Input.Keys.W -> dy = -1;
            case Input.Keys.DOWN, Input.Keys.S -> dy = 1;
            case Input.Keys.LEFT, Input.Keys.A -> dx = -1;
            case Input.Keys.RIGHT, Input.Keys.D -> dx = 1;
            default -> { }
        }
        if (dx != 0 || dy != 0) {
            if ("Talk".equals(directionAction)) {
                world.talk(dx, dy);
                if (world.talking()) {
                    directionCharacter = switch (key) {
                        case Input.Keys.W -> 'w';
                        case Input.Keys.S -> 's';
                        case Input.Keys.A -> 'a';
                        case Input.Keys.D -> 'd';
                        default -> 0;
                    };
                }
            }
            else if ("Open".equals(directionAction)) world.openDoor(dx, dy);
            else world.move(dx, dy);
            directionAction = null;
            return true;
        }
        if (directionAction != null) return false;
        switch (key) {
            case Input.Keys.E -> world.enter();
            case Input.Keys.X -> world.leave();
            case Input.Keys.K -> world.climb();
            case Input.Keys.T -> directionAction = "Talk";
            case Input.Keys.O -> directionAction = "Open";
            case Input.Keys.SPACE -> world.pass();
            case Input.Keys.M -> world.meditate();
            default -> { return false; }
        }
        return true;
    }

    private void restart() {
        creation = new CharacterCreation(new Random());
        world = null;
        directionAction = null;
        directionCharacter = 0;
        question.setLength(0);
    }

    @Override public void render() {
        ScreenUtils.clear(0.025f, 0.035f, 0.055f, 1);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.setColor(Color.GOLD);
        text("ULTIMA IV / Quest of the Avatar", 24, 690);
        font.setColor(Color.WHITE);
        if (!creation.complete()) {
            text("Character creation - choice " + (creation.choices() + 1) + " of 7", 60, 580);
            text("Choose the virtue you value more:", 60, 520);
            text("A: " + creation.option(0), 60, 460);
            text("B: " + creation.option(1), 60, 410);
            text("STR " + creation.strength() + "   DEX " + creation.dexterity() + "   INT " + creation.intelligence(), 60, 330);
            text("Seven choices determine thy class and starting place in Britannia.", 60, 250);
            text("Simplified prompts; stat tables follow the decompiled NEWGAME.", 60, 210);
            text("A/B choose | R restart | Esc quit", 24, 30);
        } else {
            for (int row = 0; row < 15; row++) {
                for (int col = 0; col < 15; col++) {
                    int tile = world.tile(world.x() + col - 7, world.y() + row - 7);
                    batch.draw(tiles[tile], 24 + col * 38, 620 - row * 38, 38, 38);
                }
            }
            batch.draw(tiles[31], 24 + 7 * 38, 620 - 7 * 38, 38, 38);
            text(world.locationName(), 24, 665);
            text("Avatar: " + creation.winner().characterClass, 620, 630);
            text("STR " + creation.strength() + " DEX " + creation.dexterity() + " INT " + creation.intelligence(), 620, 595);
            text("Position: " + world.x() + ", " + world.y(), 620, 560);
            text("Turns: " + world.turns(), 620, 525);
            text("Surface shrines visited: " + world.visitedCount() + "/7", 620, 490);
            font.setColor(Color.GOLD);
            text(world.talking() ? "Talking to " + world.npcName() : "Chronicle", 620, 445);
            font.setColor(Color.WHITE);
            font.draw(batch, world.message(), 620, 415, 355, -1, true);
            if (world.talking()) {
                text("> " + question + "_", 24, 60);
                text("Type a topic and Enter | BYE or Esc ends the conversation", 24, 30);
            } else if (directionAction != null) {
                text(directionAction + ": choose a direction (arrows or WASD). Esc cancels.", 24, 60);
                text("Arrows/WASD choose direction", 24, 30);
            } else {
                text("Arrows/WASD move | E enter | X leave | T talk | O open door", 24, 60);
                text("K castle stairs | M contemplate | Space pass | R restart | Esc quit", 24, 30);
            }
        }
        batch.end();
    }

    private void text(String text, float x, float y) { font.draw(batch, text, x, y); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() {
        if (batch != null) batch.dispose();
        if (font != null) font.dispose();
        if (sheet != null) sheet.dispose();
    }
}
