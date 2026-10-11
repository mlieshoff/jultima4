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
import java.nio.file.Path;
import java.util.Objects;
import java.util.Random;

public final class UltimaGame extends ApplicationAdapter {
  private final FitViewport viewport = new FitViewport(1000, 720);
  private final TextureRegion[] tiles = new TextureRegion[256];
  private final StringBuilder question = new StringBuilder();
  private SpriteBatch batch;
  private BitmapFont font;
  private Texture sheet;
  private Texture scrollingSheet;
  private final TextureRegion[] scrollingTiles = new TextureRegion[128];
  private final CombatAnimation combatAnimation = new CombatAnimation();
  private double animationSeconds;
  private DosGameData data;
  private CharacterScreens screens;
  private Avatar selectedAvatar;
  private LordBritishSignature signature;
  private Texture penPixel;
  private final Texture[] frontPictures = new Texture[3];
  private GameSession world;
  private String directionAction;
  private char directionCharacter;
  private String saveMessage;
  private final Path savePath;

  public UltimaGame() {
    this(SaveGame.DEFAULT_PATH);
  }

  UltimaGame(Path savePath) {
    this.savePath = Objects.requireNonNull(savePath);
  }

  @Override
  public void create() {
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
            pixels.drawPixel(
                tile % 16 * 16 + x, tile / 16 * 16 + y, data.shapes().rgba(tile, x, y));
          }
        }
      }
      sheet = new Texture(pixels);
    } finally {
      pixels.dispose();
    }
    sheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    for (int i = 0; i < tiles.length; i++) {
      tiles[i] = new TextureRegion(sheet, i % 16 * 16, i / 16 * 16, 16, 16);
    }
    createScrollingTiles();
    createFrontPictures();
    restart();
    screens.intro();
    Gdx.input.setInputProcessor(
        new InputAdapter() {
          @Override
          public boolean keyDown(int key) {
            return handleKey(key);
          }

          @Override
          public boolean keyTyped(char character) {
            if (directionCharacter != 0) {
              char suppressed = directionCharacter;
              directionCharacter = 0;
              if (Character.toLowerCase(character) == suppressed) {
                return true;
              }
            }
            if (world != null
                && world.talking()
                && character >= 32
                && character <= 126
                && question.length() < 40) {
              question.append(character);
              return true;
            }
            return false;
          }
        });
  }

  private boolean handleKey(int key) {
    if (screens.stage() != CharacterScreens.Stage.PLAYING) {
      return handleFrontKey(key);
    }
    if (key == Input.Keys.F5 || key == Input.Keys.F9) {
      if (world.talking() || world.battle() != null || combatAnimation.playing()) {
        saveMessage = "Finish combat or the conversation before saving or loading.";
      } else if (key == Input.Keys.F5) {
        saveQuest();
      } else {
        loadQuest();
      }
      return true;
    }
    saveMessage = null;
    if (world != null && world.talking()) {
      switch (key) {
        case Input.Keys.ESCAPE -> {
          world.endConversation();
          question.setLength(0);
        }
        case Input.Keys.ENTER -> {
          world.ask(question.toString());
          question.setLength(0);
        }
        case Input.Keys.BACKSPACE -> {
          if (!question.isEmpty()) {
            question.setLength(question.length() - 1);
          }
        }
        default -> {
          return false;
        }
      }
      return true;
    }
    if (key == Input.Keys.ESCAPE) {
      if (directionAction != null) {
        directionAction = null;
      } else {
        Gdx.app.exit();
      }
      return true;
    }
    if (key == Input.Keys.R) {
      restart();
      return true;
    }
    if (combatAnimation.playing()) {
      return true;
    }
    if (world != null && world.battle() != null) {
      return handleCombatKey(key);
    }
    int dx = horizontalStep(key);
    int dy = verticalStep(key);
    if (dx != 0 || dy != 0) {
      if ("Talk".equals(directionAction)) {
        world.talk(dx, dy);
        if (world.talking()) {
          directionCharacter =
              switch (key) {
                case Input.Keys.W -> 'w';
                case Input.Keys.S -> 's';
                case Input.Keys.A -> 'a';
                case Input.Keys.D -> 'd';
                default -> 0;
              };
        }
      } else if ("Open".equals(directionAction)) {
        world.openDoor(dx, dy);
      } else if ("Attack".equals(directionAction)) {
        world.attack(dx, dy);
      } else {
        world.move(dx, dy);
      }
      directionAction = null;
      return true;
    }
    if (directionAction != null) {
      return false;
    }
    switch (key) {
      case Input.Keys.E -> world.enter();
      case Input.Keys.X -> world.leave();
      case Input.Keys.K -> world.climb();
      case Input.Keys.T -> directionAction = "Talk";
      case Input.Keys.F -> directionAction = "Attack";
      case Input.Keys.O -> directionAction = "Open";
      case Input.Keys.SPACE -> world.pass();
      case Input.Keys.M -> world.meditate();
      default -> {
        return false;
      }
    }
    return true;
  }

  private void saveQuest() {
    try {
      SaveGame.save(savePath, screens.creation(), world);
      saveMessage = "Quest saved. F9 restores this save.";
      directionAction = null;
    } catch (IOException | IllegalArgumentException | IllegalStateException ex) {
      saveMessage = "Cannot save quest: " + ex.getMessage();
    }
  }

  private void loadQuest() {
    try {
      SaveGame.Loaded loaded = SaveGame.load(savePath, data);
      screens.resume(loaded.character());
      world = loaded.session();
      selectedAvatar = world.avatar();
      directionAction = null;
      directionCharacter = 0;
      question.setLength(0);
      combatAnimation.clear();
      saveMessage = "Quest restored.";
    } catch (IOException ex) {
      saveMessage = "Cannot load quest: " + ex.getMessage();
    }
  }

  private static int horizontalStep(int key) {
    return switch (key) {
      case Input.Keys.LEFT, Input.Keys.A -> -1;
      case Input.Keys.RIGHT, Input.Keys.D -> 1;
      default -> 0;
    };
  }

  private static int verticalStep(int key) {
    return switch (key) {
      case Input.Keys.UP, Input.Keys.W -> -1;
      case Input.Keys.DOWN, Input.Keys.S -> 1;
      default -> 0;
    };
  }

  private boolean handleFrontKey(int key) {
    switch (screens.stage()) {
      case SIGNATURE, ORIGIN -> {
        if (key == Input.Keys.ESCAPE) {
          screens.title();
        } else if (key == Input.Keys.ENTER || key == Input.Keys.SPACE) {
          screens.advanceIntro();
        } else {
          return false;
        }
      }
      case TITLE -> {
        if (key == Input.Keys.ESCAPE) {
          Gdx.app.exit();
        } else if (key == Input.Keys.N || key == Input.Keys.ENTER) {
          screens.start();
        } else if (key == Input.Keys.L || key == Input.Keys.F9) {
          loadQuest();
        } else if (key == Input.Keys.I) {
          screens.intro();
        } else {
          return false;
        }
      }
      case CREATION -> {
        if (key == Input.Keys.ESCAPE) {
          screens.title();
        } else if (key == Input.Keys.R) {
          restart();
        } else if (key == Input.Keys.A || key == Input.Keys.B) {
          screens.choose(key == Input.Keys.A ? 0 : 1);
          if (screens.stage() == CharacterScreens.Stage.SELECTION) {
            try {
              selectedAvatar = Avatar.from(screens.creation());
            } catch (IOException ex) {
              throw new IllegalStateException("Cannot show starting character", ex);
            }
          }
        } else {
          return false;
        }
      }
      case SELECTION -> {
        if (key == Input.Keys.ESCAPE) {
          screens.title();
        } else if (key == Input.Keys.R || key == Input.Keys.BACKSPACE) {
          restart();
        } else if (key == Input.Keys.ENTER) {
          try {
            world = GameSession.start(data, screens.creation());
          } catch (IOException ex) {
            throw new IllegalStateException("Cannot initialize character", ex);
          }
          screens.confirm();
        } else {
          return false;
        }
      }
      default -> {
        return false;
      }
    }
    return true;
  }

  private void createFrontPictures() {
    try {
      signature = LordBritishSignature.load();
    } catch (IOException ex) {
      throw new IllegalStateException("Cannot load Lord British signature", ex);
    }
    Pixmap pen = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    try {
      pen.setColor(Color.WHITE);
      pen.fill();
      penPixel = new Texture(pen);
    } finally {
      pen.dispose();
    }
    String[] names = {"TITLE", "GYPSY", "WAGON"};
    for (int i = 0; i < names.length; i++) {
      EgaPicture picture;
      try {
        picture = EgaPicture.load(names[i]);
      } catch (IOException ex) {
        throw new IllegalStateException("Cannot load " + names[i] + " background", ex);
      }
      Pixmap pixels = new Pixmap(EgaPicture.WIDTH, EgaPicture.HEIGHT, Pixmap.Format.RGBA8888);
      try {
        for (int y = 0; y < EgaPicture.HEIGHT; y++) {
          for (int x = 0; x < EgaPicture.WIDTH; x++) {
            pixels.drawPixel(x, y, picture.rgba(x, y));
          }
        }
        frontPictures[i] = new Texture(pixels);
        frontPictures[i].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
      } finally {
        pixels.dispose();
      }
    }
  }

  private void renderFront() {
    CharacterScreens.Stage stage = screens.stage();
    if (stage == CharacterScreens.Stage.SIGNATURE || stage == CharacterScreens.Stage.ORIGIN) {
      renderIntro(stage);
      return;
    }
    int background =
        stage == CharacterScreens.Stage.TITLE
            ? 0
            : stage == CharacterScreens.Stage.CREATION ? 1 : 2;
    batch.setColor(Color.WHITE);
    batch.draw(frontPictures[background], 20, 100, 960, 600);
    font.setColor(Color.GOLD);
    switch (stage) {
      case TITLE -> renderTitle();
      case CREATION -> renderCharacterChoices();
      case SELECTION -> renderSelectedCharacter();
      default -> throw new IllegalStateException("Unexpected front screen: " + stage);
    }
  }

  private void renderTitle() {
    font.draw(batch, "Begin a new quest", 44, 350, 912, 1, false);
    font.setColor(Color.WHITE);
    font.draw(batch, "N / Enter - Create a character", 44, 295, 912, 1, false);
    font.draw(batch, "Esc - Quit", 44, 240, 912, 1, false);
    text("I replay intro | N / Enter new quest | L continue | Esc quit", 24, 50);
    if (saveMessage != null) font.draw(batch, saveMessage, 24, 100, 950, -1, true);
  }

  private void renderCharacterChoices() {
    CharacterCreation creation = screens.creation();
    text("The Gypsy asks / Choice " + (creation.choices() + 1) + " of 7", 50, 235);
    font.setColor(Color.WHITE);
    text("A: " + creation.option(0), 50, 195);
    text("B: " + creation.option(1), 510, 195);
    text(
        "STR "
            + creation.strength()
            + "   DEX "
            + creation.dexterity()
            + "   INT "
            + creation.intelligence(),
        50,
        155);
    text("Which virtue guides thy heart?", 510, 155);
    text("A/B choose | R start over | Esc title", 24, 50);
  }

  private void renderSelectedCharacter() {
    CharacterCreation creation = screens.creation();
    Avatar avatar = selectedAvatar;
    text("Thy path: " + creation.winner().characterClass + " / " + creation.winner(), 50, 235);
    font.setColor(Color.WHITE);
    text(
        "STR "
            + creation.strength()
            + "   DEX "
            + creation.dexterity()
            + "   INT "
            + creation.intelligence(),
        50,
        195);
    text("HP " + avatar.maximumHealth() + "   XP " + avatar.experience(), 50, 155);
    text("Weapon: " + avatar.weapon().displayName, 510, 195);
    text("Britannia: " + creation.startingX() + ", " + creation.startingY(), 510, 155);
    drawTile(31, 880, 158, 64);
    text("Enter begin quest | R / Backspace choose again | Esc title", 24, 50);
  }

  private void renderIntro(CharacterScreens.Stage stage) {
    batch.setColor(Color.WHITE);
    font.setColor(Color.WHITE);
    if (stage == CharacterScreens.Stage.SIGNATURE) {
      int count = (int) (signature.points().size() * Math.min(1, screens.elapsed() / 3));
      batch.setColor(Color.CYAN);
      for (int i = 0; i < count; i++) {
        LordBritishSignature.Point point = signature.points().get(i);
        batch.draw(penPixel, 238 + point.x() * 4, 390 - point.y() * 4, 8, 4);
      }
      batch.setColor(Color.WHITE);
    } else {
      int rows = Math.min(9, (int) (screens.elapsed() / 0.8 * 9));
      if (rows > 0) {
        TextureRegion lettering = new TextureRegion(frontPictures[0], 86, 21 + 9 - rows, 148, rows);
        batch.draw(lettering, 204, 360, 592, rows * 4);
      }
      font.draw(batch, "presents", 0, 310, 1000, 1, false);
    }
    text("Enter / Space next | Esc title", 24, 50);
  }

  private boolean handleCombatKey(int key) {
    if (world.battle().outcome() != Battle.Outcome.ACTIVE) {
      if (key == Input.Keys.ENTER && world.battle().outcome() != Battle.Outcome.DEFEAT) {
        world.finishCombat();
        directionAction = null;
        return true;
      }
      return false;
    }
    int dx = horizontalStep(key);
    int dy = verticalStep(key);
    if (dx != 0 || dy != 0) {
      combatAnimation.capture(world.battle());
      if ("Attack".equals(directionAction)) {
        world.attack(dx, dy);
      } else {
        world.move(dx, dy);
      }
      combatAnimation.play(world.battle().events());
      directionAction = null;
      return true;
    }
    if (directionAction != null) {
      return false;
    }
    if (key == Input.Keys.F) {
      directionAction = "Attack";
      return true;
    }
    if (key == Input.Keys.SPACE) {
      combatAnimation.capture(world.battle());
      world.pass();
      combatAnimation.play(world.battle().events());
      return true;
    }
    return false;
  }

  private void restart() {
    saveMessage = null;
    combatAnimation.clear();
    animationSeconds = 0;
    if (screens == null) {
      screens = new CharacterScreens(new Random());
    }
    screens.start();
    world = null;
    selectedAvatar = null;
    directionAction = null;
    directionCharacter = 0;
    question.setLength(0);
  }

  @Override
  public void render() {
    float delta = Math.max(0, Gdx.graphics.getDeltaTime());
    animationSeconds += delta;
    screens.update(delta);
    combatAnimation.update(delta);
    ScreenUtils.clear(0.025f, 0.035f, 0.055f, 1);
    viewport.apply();
    batch.setProjectionMatrix(viewport.getCamera().combined);
    batch.begin();
    if (screens.stage() != CharacterScreens.Stage.PLAYING) {
      renderFront();
      batch.end();
      return;
    }
    font.setColor(Color.GOLD);
    text("ULTIMA IV / Quest of the Avatar", 24, 690);
    font.setColor(Color.WHITE);
    if (world.battle() != null) {
      renderCombat();
    } else {
      for (int row = 0; row < 15; row++) {
        for (int col = 0; col < 15; col++) {
          int mapX = world.x() + col - 7, mapY = world.y() + row - 7;
          int tile = world.visible(mapX, mapY) ? world.tile(mapX, mapY) : 126;
          drawTile(tile, 24 + col * 38, 620 - row * 38, 38);
        }
      }
      drawTile(31, 24 + 7 * 38, 620 - 7 * 38, 38);
      text(world.locationName(), 24, 665);
      text("Avatar: " + screens.creation().winner().characterClass, 620, 630);
      text(
          "STR "
              + screens.creation().strength()
              + " DEX "
              + screens.creation().dexterity()
              + " INT "
              + screens.creation().intelligence(),
          620,
          595);
      text(
          "HP "
              + world.avatar().health()
              + "/"
              + world.avatar().maximumHealth()
              + "  XP "
              + world.avatar().experience(),
          620,
          560);
      text("Weapon: " + world.avatar().weapon().displayName, 620, 530);
      text("Position: " + world.x() + ", " + world.y(), 620, 500);
      text("Turns: " + world.turns(), 620, 470);
      text("Surface shrines visited: " + world.visitedCount() + "/7", 620, 440);
      font.setColor(Color.GOLD);
      text(world.talking() ? "Talking to " + world.npcName() : "Chronicle", 620, 395);
      font.setColor(Color.WHITE);
      font.draw(
          batch, saveMessage == null ? world.message() : saveMessage, 620, 365, 355, -1, true);
      if (world.talking()) {
        text("> " + question + "_", 24, 60);
        text("Type a topic and Enter | BYE or Esc ends the conversation", 24, 30);
      } else if (directionAction != null) {
        text(directionAction + ": choose a direction (arrows or WASD). Esc cancels.", 24, 60);
        text("Arrows/WASD choose direction", 24, 30);
      } else {
        text("Move: arrows/WASD | F attack | E enter | X leave | T talk | O open", 24, 60);
        text(
            "K stairs | M contemplate | Space pass | F5 save | F9 load | R restart | Esc quit",
            24,
            30);
      }
    }
    batch.end();
  }

  private void renderCombat() {
    Battle battle = world.battle();
    int observerX = battle.x(), observerY = battle.y();
    if (combatAnimation.playing()) {
      for (CombatAnimation.Sprite sprite : combatAnimation.sprites()) {
        if (sprite.tile() == 31) {
          observerX = Math.max(0, Math.min(10, Math.round(sprite.x())));
          observerY = Math.max(0, Math.min(10, Math.round(sprite.y())));
          break;
        }
      }
    }
    text("COMBAT / " + battle.mapName(), 24, 665);
    for (int row = 0; row < 11; row++) {
      for (int col = 0; col < 11; col++) {
        int tile =
            battle.visibleFrom(observerX, observerY, col, row)
                ? (combatAnimation.playing() ? battle.groundTile(col, row) : battle.tile(col, row))
                : 126;
        drawTile(tile, 44 + col * 48, 590 - row * 48, 48);
      }
    }
    if (combatAnimation.playing()) {
      for (CombatAnimation.Sprite sprite : combatAnimation.sprites()) {
        if (sprite.tile() != 31
            && !battle.visibleFrom(
                observerX, observerY, Math.round(sprite.x()), Math.round(sprite.y()))) {
          continue;
        }
        batch.setColor(1, 1, 1, sprite.alpha());
        drawTile(sprite.tile(), 44 + sprite.x() * 48, 590 - sprite.y() * 48, 48);
      }
      batch.setColor(Color.WHITE);
      drawCombatEffect(observerX, observerY);
      // Moving sprites straddle tiles; cover their portions outside the visible area.
      for (int row = 0; row < 11; row++) {
        for (int col = 0; col < 11; col++) {
          if (!battle.visibleFrom(observerX, observerY, col, row)) {
            drawTile(126, 44 + col * 48, 590 - row * 48, 48);
          }
        }
      }
    } else if (battle.outcome() != Battle.Outcome.FLED) {
      drawTile(
          world.avatar().defeated() ? 56 : 31, 44 + battle.x() * 48, 590 - battle.y() * 48, 48);
    }
    text("Avatar: " + screens.creation().winner().characterClass, 620, 630);
    text("HP " + world.avatar().health() + "/" + world.avatar().maximumHealth(), 620, 595);
    text("XP " + world.avatar().experience(), 620, 560);
    text("Weapon: " + world.avatar().weapon().displayName, 620, 525);
    text("Round: " + battle.rounds(), 620, 490);
    int visibleEnemy = 0;
    for (Battle.Enemy enemy : battle.enemies()) {
      if (!combatAnimation.playing()
          && battle.visibleFrom(observerX, observerY, enemy.x(), enemy.y())) {
        text(enemy.type().displayName + " HP " + enemy.health(), 620, 445 - visibleEnemy++ * 30);
      }
    }
    font.setColor(Color.GOLD);
    text("Chronicle", 620, 365);
    font.setColor(Color.WHITE);
    font.draw(batch, saveMessage == null ? battle.message() : saveMessage, 620, 335, 355, -1, true);
    if (combatAnimation.playing()) {
      text("Turn in motion... | R restart | Esc quit", 24, 60);
    } else if (battle.outcome() == Battle.Outcome.DEFEAT) {
      text("Thou art defeated. R starts a new character | Esc quit", 24, 60);
    } else if (battle.outcome() != Battle.Outcome.ACTIVE) {
      text("Enter returns to Britannia | R restart | Esc quit", 24, 60);
    } else if (directionAction != null) {
      text("Attack: choose a direction (arrows/WASD) | Esc cancels", 24, 60);
    } else {
      text("Arrows/WASD move | F then direction attack | Space pass", 24, 60);
      text("Walk off the battlefield to flee | R restart | Esc quit", 24, 30);
    }
  }

  private void createScrollingTiles() {
    Pixmap pixels = new Pixmap(256, 128, Pixmap.Format.RGBA8888);
    try {
      for (int tile = 0; tile < 256; tile++) {
        int slot = TileAnimation.scrollSlot(tile);
        if (slot < 0) {
          continue;
        }
        for (int offset = 0; offset < 16; offset++) {
          for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
              pixels.drawPixel(
                  offset * 16 + x, slot * 16 + y, data.shapes().rgba(tile, x, (y + offset) % 16));
            }
          }
        }
      }
      scrollingSheet = new Texture(pixels);
    } finally {
      pixels.dispose();
    }
    scrollingSheet.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
    for (int i = 0; i < scrollingTiles.length; i++) {
      scrollingTiles[i] = new TextureRegion(scrollingSheet, i % 16 * 16, i / 16 * 16, 16, 16);
    }
  }

  private void drawTile(int tile, float x, float y, float size) {
    if (tile == 31) {
      tile = 32 + screens.creation().winner().ordinal() * 2;
    }
    int slot = TileAnimation.scrollSlot(tile);
    TextureRegion region =
        slot < 0
            ? tiles[TileAnimation.frame(tile, animationSeconds)]
            : scrollingTiles[slot * 16 + TileAnimation.scrollOffset(animationSeconds)];
    batch.draw(region, x, y, size, size);
  }

  private void drawCombatEffect(int observerX, int observerY) {
    Battle.Event event = combatAnimation.current();
    float progress = combatAnimation.progress();
    if (event.action() == Battle.Action.HIT || event.action() == Battle.Action.MISS) {
      if (world.battle().visibleFrom(observerX, observerY, event.toX(), event.toY())
          && (int) (progress * 6) % 2 == 0) {
        drawTile(
            event.action() == Battle.Action.HIT ? 79 : 77,
            44 + event.toX() * 48,
            590 - event.toY() * 48,
            48);
      }
    } else if (event.action() == Battle.Action.ATTACK
        && Math.abs(event.toX() - event.fromX()) + Math.abs(event.toY() - event.fromY()) > 1) {
      float x = event.fromX() + (event.toX() - event.fromX()) * progress;
      float y = event.fromY() + (event.toY() - event.fromY()) * progress;
      if (world.battle().visibleFrom(observerX, observerY, Math.round(x), Math.round(y))) {
        drawTile(78, 44 + x * 48 + 12, 590 - y * 48 + 12, 24);
      }
    }
  }

  private void text(String text, float x, float y) {
    font.draw(batch, text, x, y);
  }

  @Override
  public void resize(int width, int height) {
    viewport.update(width, height, true);
  }

  @Override
  public void dispose() {
    if (batch != null) {
      batch.dispose();
    }
    if (font != null) {
      font.dispose();
    }
    if (sheet != null) {
      sheet.dispose();
    }
    if (scrollingSheet != null) {
      scrollingSheet.dispose();
    }
    if (penPixel != null) {
      penPixel.dispose();
    }
    for (Texture picture : frontPictures) {
      if (picture != null) {
        picture.dispose();
      }
    }
  }
}
