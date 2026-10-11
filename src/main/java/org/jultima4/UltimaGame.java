package org.jultima4;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import java.util.Random;

public final class UltimaGame extends ApplicationAdapter {
    private final FitViewport viewport = new FitViewport(1000, 720);
    private final TextureRegion[] tiles = new TextureRegion[32];
    private SpriteBatch batch;
    private BitmapFont font;
    private Texture sheet;
    private CharacterCreation creation;
    private DemoWorld world;

    @Override public void create() {
        batch = new SpriteBatch();
        font = new BitmapFont();
        font.getData().setScale(1.25f);
        sheet = new Texture(Gdx.files.internal("tiles/01_SHP.png"));
        sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        for (int i = 0; i < tiles.length; i++) {
            tiles[i] = new TextureRegion(sheet, (i % 16) * 32, (i / 16) * 64, Math.min(32, sheet.getWidth() - (i % 16) * 32), 32);
        }
        restart();
        Gdx.input.setInputProcessor(new InputAdapter() {
            @Override public boolean keyDown(int key) {
                if (key == Input.Keys.ESCAPE) { Gdx.app.exit(); return true; }
                if (key == Input.Keys.R) { restart(); return true; }
                if (!creation.complete()) {
                    if (key == Input.Keys.A || key == Input.Keys.B) creation.choose(key == Input.Keys.A ? 0 : 1);
                } else {
                    switch (key) {
                        case Input.Keys.UP, Input.Keys.W -> world.move(0, -1);
                        case Input.Keys.DOWN, Input.Keys.S -> world.move(0, 1);
                        case Input.Keys.LEFT, Input.Keys.A -> world.move(-1, 0);
                        case Input.Keys.RIGHT, Input.Keys.D -> world.move(1, 0);
                        case Input.Keys.SPACE -> world.pass();
                        case Input.Keys.M -> world.meditate();
                        default -> { return false; }
                    }
                }
                return true;
            }
        });
    }

    private void restart() {
        creation = new CharacterCreation(new Random());
        world = new DemoWorld();
    }

    @Override public void render() {
        ScreenUtils.clear(0.025f, 0.035f, 0.055f, 1);
        viewport.apply();
        batch.setProjectionMatrix(viewport.getCamera().combined);
        batch.begin();
        font.setColor(Color.GOLD);
        text("ULTIMA IV  /  Quest of the Avatar", 24, 690);
        font.setColor(Color.WHITE);
        if (!creation.complete()) {
            text("Character creation — choice " + (creation.choices() + 1) + " of 7", 60, 580);
            text("Choose the virtue you value more:", 60, 520);
            text("A: " + creation.option(0), 60, 460);
            text("B: " + creation.option(1), 60, 410);
            text("STR " + creation.strength() + "   DEX " + creation.dexterity() + "   INT " + creation.intelligence(), 60, 330);
            text("Simplified virtue prompts; stat tables follow the decompiled NEWGAME.", 60, 240);
        } else {
            for (int row = 0; row < 15; row++) {
                for (int col = 0; col < 15; col++) {
                    int tile = world.tile(world.x() + col - 7, world.y() + row - 7);
                    batch.draw(tiles[tile], 24 + col * 38, 620 - row * 38, 38, 38);
                }
            }
            batch.draw(tiles[0x1f], 24 + 7 * 38, 620 - 7 * 38, 38, 38);
            font.setColor(Color.GOLD);
            text("@", 24 + 7 * 38 + 10, 620 - 7 * 38 + 26);
            font.setColor(Color.WHITE);
            text("Avatar: " + creation.winner().characterClass, 620, 630);
            text("STR " + creation.strength() + "  DEX " + creation.dexterity() + "  INT " + creation.intelligence(), 620, 590);
            text("Position: " + world.x() + ", " + world.y(), 620, 550);
            text("Turns: " + world.turns(), 620, 510);
            text("Shrines: " + world.visitedCount() + " / 8", 620, 470);
            for (int i = 0; i < 8; i++) text(Virtue.values()[i] + ": " + creation.score(Virtue.values()[i]), 620, 420 - i * 28);
            text(world.message(), 24, 60);
        }
        text("Arrows/WASD move | Space pass | M meditate | R restart | Esc quit", 24, 30);
        text("Demonstration island — original world data unavailable", 24, 665);
        batch.end();
    }

    private void text(String text, float x, float y) { font.draw(batch, text, x, y); }
    @Override public void resize(int width, int height) { viewport.update(width, height, true); }
    @Override public void dispose() {
        batch.dispose();
        font.dispose();
        sheet.dispose();
    }
}
