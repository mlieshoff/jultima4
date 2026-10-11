package org.jultima4;

import com.badlogic.gdx.Input;
import java.util.ArrayList;
import java.util.List;

/** Modal character information; reading statistics never advances the quest. */
final class CharacterStats {
  private boolean open;

  boolean isOpen() {
    return open;
  }

  boolean handleKey(int key, boolean talking) {
    if (open) {
      if (key == Input.Keys.Z || key == Input.Keys.ENTER || key == Input.Keys.ESCAPE) close();
      return true;
    }
    if (key == Input.Keys.Z && !talking) {
      open = true;
      return true;
    }
    return false;
  }

  void close() {
    open = false;
  }

  List<String> lines(CharacterCreation character, Avatar avatar) {
    List<String> lines = new ArrayList<>();
    lines.add("Class: " + character.winner().characterClass);
    lines.add("Strength: " + character.strength());
    lines.add("Dexterity: " + character.dexterity());
    lines.add("Intelligence: " + character.intelligence());
    lines.add("Health: " + avatar.health() + " / " + avatar.maximumHealth());
    lines.add("Experience: " + avatar.experience());
    lines.add("Weapon: " + avatar.weapon().displayName);
    lines.add("Armor defense: " + avatar.defense());
    for (Virtue virtue : Virtue.values()) {
      lines.add(virtue.name() + ": " + character.score(virtue));
    }
    return List.copyOf(lines);
  }
}
