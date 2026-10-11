package org.jultima4;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Random;
import org.junit.jupiter.api.Test;

class AvatarTest {
  @Test
  void classStartsUseActualTemplateHealthWeaponsAndArmour() throws Exception {
    int[] health = {200, 300, 300, 200, 200, 300, 200, 100};
    Avatar.Weapon[] weapons = {
      Avatar.Weapon.STAFF,
      Avatar.Weapon.SLING,
      Avatar.Weapon.AXE,
      Avatar.Weapon.DAGGER,
      Avatar.Weapon.MACE,
      Avatar.Weapon.SWORD,
      Avatar.Weapon.SWORD,
      Avatar.Weapon.STAFF
    };
    int[] defenses = {128, 128, 144, 128, 144, 160, 144, 128};
    for (Virtue virtue : Virtue.values()) {
      CharacterCreation creation = new CharacterCreation(new Random(42));
      while (!creation.complete()) creation.choose(creation.option(1) == virtue ? 1 : 0);
      assertEquals(virtue, creation.winner());
      Avatar avatar = Avatar.from(creation);
      assertEquals(health[virtue.ordinal()], avatar.health());
      assertEquals(avatar.health(), avatar.maximumHealth());
      assertEquals(weapons[virtue.ordinal()], avatar.weapon());
      assertEquals(defenses[virtue.ordinal()], avatar.defense());
      assertEquals(creation.dexterity() >= 40 ? 255 : creation.dexterity(), avatar.attackBonus());
    }
  }

  @Test
  void damageAndExperienceAreBoundedAndInvalidValuesAreRejected() {
    Avatar avatar = new Avatar(100, 20, 20, Avatar.Weapon.SWORD, 128, 9998);
    avatar.hurt(120);
    assertTrue(avatar.defeated());
    assertEquals(0, avatar.health());
    avatar.awardExperience(10);
    assertEquals(9999, avatar.experience());
    assertThrows(IllegalArgumentException.class, () -> avatar.hurt(-1));
    assertThrows(IllegalArgumentException.class, () -> avatar.awardExperience(-1));
    assertThrows(
        IllegalArgumentException.class, () -> new Avatar(0, 15, 15, Avatar.Weapon.STAFF, 128, 0));
  }

  @Test
  void invalidTemplatesDoNotCreateCharacters() {
    CharacterCreation creation = new CharacterCreation(new Random(42));
    for (int i = 0; i < 7; i++) creation.choose(0);
    assertThrows(
        IllegalArgumentException.class, () -> Avatar.fromTemplate(creation, new byte[501]));
    assertThrows(
        IllegalArgumentException.class, () -> Avatar.fromTemplate(creation, new byte[502]));
  }
}
