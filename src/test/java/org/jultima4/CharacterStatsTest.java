package org.jultima4;

import static org.assertj.core.api.Assertions.assertThat;

import com.badlogic.gdx.Input;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class CharacterStatsTest {
  @Test
  void handleKey_whenZPressedOutsideDialogue_thenOpenStats() {
    CharacterStats stats = new CharacterStats();

    boolean consumed = stats.handleKey(Input.Keys.Z, false);

    assertThat(List.of(consumed, stats.isOpen())).containsExactly(true, true);
  }

  @Test
  void handleKey_whenZPressedInDialogue_thenPreserveTextInput() {
    CharacterStats stats = new CharacterStats();

    boolean consumed = stats.handleKey(Input.Keys.Z, true);

    assertThat(List.of(consumed, stats.isOpen())).containsExactly(false, false);
  }

  @Test
  void handleKey_whenZPressedAgain_thenCloseStats() {
    CharacterStats stats = new CharacterStats();
    stats.handleKey(Input.Keys.Z, false);

    boolean consumed = stats.handleKey(Input.Keys.Z, false);

    assertThat(List.of(consumed, stats.isOpen())).containsExactly(true, false);
  }

  @Test
  void handleKey_whenEscapePressedInStats_thenCloseWithoutExitingQuest() {
    CharacterStats stats = new CharacterStats();
    stats.handleKey(Input.Keys.Z, false);

    boolean consumed = stats.handleKey(Input.Keys.ESCAPE, false);

    assertThat(List.of(consumed, stats.isOpen())).containsExactly(true, false);
  }

  @Test
  void handleKey_whenEnterPressedInStats_thenCloseWithoutTakingTurn() {
    CharacterStats stats = new CharacterStats();
    stats.handleKey(Input.Keys.Z, false);

    boolean consumed = stats.handleKey(Input.Keys.ENTER, false);

    assertThat(List.of(consumed, stats.isOpen())).containsExactly(true, false);
  }

  @Test
  void handleKey_whenGameplayKeysPressedInStats_thenConsumeAndRemainOpen() {
    CharacterStats stats = new CharacterStats();
    stats.handleKey(Input.Keys.Z, false);

    List<Boolean> consumed =
        List.of(
            stats.handleKey(Input.Keys.UP, false),
            stats.handleKey(Input.Keys.SPACE, false),
            stats.handleKey(Input.Keys.F, false),
            stats.handleKey(Input.Keys.F5, false),
            stats.handleKey(Input.Keys.F9, false),
            stats.handleKey(Input.Keys.R, false));

    assertModalInput(consumed, stats.isOpen());
  }

  @Test
  void handleKey_whenStatsClosedAndMovementPressed_thenLeaveGameplayInputAvailable() {
    CharacterStats stats = new CharacterStats();

    boolean consumed = stats.handleKey(Input.Keys.UP, false);

    assertThat(List.of(consumed, stats.isOpen())).containsExactly(false, false);
  }

  @Test
  void lines_whenCharacterHasCurrentCombatStats_thenShowAttributesEquipmentAndVirtues()
      throws Exception {
    CharacterCreation character = character();
    Avatar avatar = Avatar.from(character);
    avatar.hurt(12);
    avatar.awardExperience(42);
    CharacterStats stats = new CharacterStats();

    List<String> lines = stats.lines(character, avatar);

    assertCharacterLines(lines, character, avatar);
  }

  @Test
  void lines_whenAvatarChangesAfterFirstView_thenShowUpdatedHealthExperienceAndWeapon()
      throws Exception {
    CharacterCreation character = character();
    Avatar avatar = Avatar.from(character);
    CharacterStats stats = new CharacterStats();
    stats.lines(character, avatar);
    avatar.hurt(25);
    avatar.awardExperience(73);
    avatar.loseThrownDagger();

    List<String> lines = stats.lines(character, avatar);

    assertThat(lines)
        .contains(
            "Health: " + avatar.health() + " / " + avatar.maximumHealth(),
            "Experience: " + avatar.experience(),
            "Weapon: Hands");
  }

  @Test
  void lines_whenAvatarDefeated_thenDisplayZeroHealth() throws Exception {
    CharacterCreation character = character();
    Avatar avatar = Avatar.from(character);
    avatar.hurt(avatar.maximumHealth());

    List<String> lines = new CharacterStats().lines(character, avatar);

    assertThat(lines).contains("Health: 0 / " + avatar.maximumHealth());
  }

  @Test
  void lines_whenViewedRepeatedly_thenPreserveCharacterAndAvatarState() throws Exception {
    CharacterCreation character = character();
    Avatar avatar = Avatar.from(character);
    CharacterCreation.State expectedCharacter = character.snapshot();
    Avatar.State expectedAvatar = avatar.snapshot();
    CharacterStats stats = new CharacterStats();

    stats.lines(character, avatar);
    stats.lines(character, avatar);

    assertUnchanged(character.snapshot(), avatar.snapshot(), expectedCharacter, expectedAvatar);
  }

  private CharacterCreation character() {
    CharacterCreation character = new CharacterCreation(new Random(0));
    while (!character.complete()) character.choose(0);
    return character;
  }

  private void assertModalInput(List<Boolean> consumed, boolean open) {
    assertThat(consumed).containsOnly(true).hasSize(6);
    assertThat(open).isTrue();
  }

  private void assertCharacterLines(
      List<String> lines, CharacterCreation character, Avatar avatar) {
    assertThat(lines)
        .hasSize(16)
        .startsWith(
            "Class: " + character.winner().characterClass,
            "Strength: " + character.strength(),
            "Dexterity: " + character.dexterity(),
            "Intelligence: " + character.intelligence(),
            "Health: " + avatar.health() + " / " + avatar.maximumHealth(),
            "Experience: " + avatar.experience(),
            "Weapon: " + avatar.weapon().displayName,
            "Armor defense: " + avatar.defense());
    for (Virtue virtue : Virtue.values()) {
      assertThat(lines).contains(virtue.name() + ": " + character.score(virtue));
    }
  }

  private void assertUnchanged(
      CharacterCreation.State character,
      Avatar.State avatar,
      CharacterCreation.State expectedCharacter,
      Avatar.State expectedAvatar) {
    assertThat(character).usingRecursiveComparison().isEqualTo(expectedCharacter);
    assertThat(avatar).usingRecursiveComparison().isEqualTo(expectedAvatar);
  }
}
