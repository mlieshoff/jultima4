# jultima4

A Java 21 / Maven / LibGDX desktop version of Ultima IV, using the supplied
MS-DOS game data in `COMPILED` and the Apple II listings in `DECOMPILED`.

## Build and play

Requires JDK 21, Maven 3.9+, and a desktop with OpenGL support.

```sh
mvn verify
mvn exec:java
```

On macOS, run on the first JVM thread:

```sh
mvn dependency:copy-dependencies
java -XstartOnFirstThread -cp 'target/classes:target/dependency/*' org.jultima4.DesktopLauncher
```

Choose A or B for seven virtue pairs. Your final virtue determines your class
and starting position in Britannia. Prompts still use virtue names rather than
the original dilemmas.

| Control | Action |
| --- | --- |
| Arrows / WASD | Move one tile |
| E | Enter the town or castle beneath the Avatar |
| X | Leave the current town; walking off its edge also leaves |
| T, then a direction | Talk to the adjacent NPC |
| Type a topic, Enter | Ask NAME, JOB, HEAL, or a keyword from the conversation |
| YES / NO, Enter | Answer an NPC question |
| BYE, Enter / Escape | End a conversation |
| O, then a direction | Open an unlocked door |
| K | Use Castle Britannia stairs at (3,3) or (27,3) |
| M | Contemplate at a surface shrine; records a visit only |
| F, then a direction | Attack a monster; use the equipped weapon in combat |
| Enter after victory / fleeing | Return to Britannia |
| Space | Pass a turn |
| R | Restart character creation (discards the session) |
| Escape | Cancel a direction prompt, or quit when exploring |

A Bard begins near Britain at (83,105); Britain is at (82,106). Stand on the
town and press E. Castle Britannia is nearby at (86,107). Other classes start
near their respective towns, matching the decompiled starting-coordinate table.

## Implemented data and behavior

* Actual 256x256 Britannia map from `WORLD.MAP`, including world-edge wrapping.
* All 256 EGA tiles decoded directly from `SHAPES.EGA`, with nearest-neighbor
  scaling.
* All sixteen settlements plus the upper floor of Castle Britannia, using
  `.ULT` terrain and original NPC placement.
* Standard `.TLK` conversations: descriptions, names, jobs, health, both custom
  keywords, triggered questions, and yes/no responses. Topic matching uses the
  first four characters, without case sensitivity.
* On-foot terrain collision, NPC collision, unlocked doors, town entry/exit,
  castle stairs, passing turns, and visits to the seven surface shrines.
* Visible Orc, Skeleton, and Headless land encounters that pursue the Avatar.
* Solo turn-based combat on eight original `.CON` battlefields: cardinal melee
  and ranged attacks, terrain blocking, enemy movement, damage, kills, experience,
  victory, fleeing by walking off the battlefield, and defeat.
* Class starting health, experience, weapon, and armor from `PARTY.NEW`; selected
  strength and dexterity affect attacks. Health and experience persist between battles.
* Seven virtue selections with the documented stat increments and +5 virtue
  points per choice.

Maven packages the world map, EGA shapes, town maps, dialogue, combat maps, and
new-party template as classpath resources. The supplied `COMPILED` folder is retained in Git as requested.
The DOS executables, drivers, documents, dungeon data, and saves remain
in the source folder; they are not executed or altered by the Java game.
Existing save files are not loaded; `PARTY.NEW` supplies only initial character
values. The old `DemoWorld` model and its tests are retained, but the desktop game now uses `GameSession` and the DOS data.

## Remaining work

This is a playable exploration, dialogue, and solo combat increment. NPCs stay
at their original starting positions. Shops and special conversations (including
Lord British and Hawkwind), recruiting, party combat, dungeon gameplay, spellcasting,
equipment changes, healing/level advancement, inventory, food, terrain delays/damage,
vehicles, moongates, full shrine rituals,
virtue changes from conversations, save/load, sound, and the endgame are not
implemented. Without vehicles and moongates, some areas cannot yet be reached
on foot. Shrine contemplation does not grant partial Avatarhood.

Land encounters use a custom initial cadence: one group of one or two monsters
spawns every twelve outdoor turns on reachable land six to eight steps away,
with at most eight groups. They move every second turn and disappear more than
twenty tiles away. Towns are safe. Fleeing grants six outdoor turns to move away;
survivors remain, with fresh health when encountered again. R restarts after defeat.
Ranged daggers are lost when thrown at a distant target. Monster special abilities,
fleeing AI, treasure drops, and the rest of the original bestiary remain unfinished.

## Evidence and validation

* `DECOMPILED/DISK_01/NEWGAME/full_listing.md`: seven choices at $6707, starting
  coordinates at $67F9/$6801, base stats at $6809, stat increments at
  $680C/$6814/$681C, virtue scores at $6824 and +5 per choice at $66C9.
* [XU4 file-format notes](https://github.com/xu4-engine/u4/blob/master/doc/FileFormats.md)
  describe the EGA pixels, ULT NPC arrays, and TLK records. Actual file sizes and
  known landmarks are checked by tests against the supplied binaries.
* [XU4 map loader](https://github.com/xu4-engine/u4/blob/master/src/maploader.cpp)
  and [world-map reader](https://github.com/jtauber/ultima4/blob/master/world_map.py)
  establish row-major chunk order. This corrects the contradictory chunk-order
  prose in the format notes.
* [XU4 map metadata](https://github.com/xu4-engine/u4/blob/master/module/Ultima-IV/maps.b)
  identifies settlement coordinates, entry positions, and castle stairs;
  [tile metadata](https://github.com/xu4-engine/u4/blob/master/module/Ultima-IV/config.b)
  informs the implemented on-foot collision subset. No XU4 implementation code
  is included.

* `DECOMPILED/DISK_01/ULT4/01_ULT4_6FB0.md` and
  `DECOMPILED/DISK_02/CON_/full_listing.md` inform terrain battlefield selection
  and combat map layout. `COMPILED/History.txt` describes directional combat.
* [XU4 combat metadata](https://github.com/xu4-engine/u4/blob/master/module/Ultima-IV/config.b),
  [combat rules](https://github.com/xu4-engine/u4/blob/master/src/combat.cpp),
  [party attacks](https://github.com/xu4-engine/u4/blob/master/src/party.cpp), and
  [creature attributes](https://github.com/xu4-engine/u4/blob/master/src/creature.cpp)
  inform the selected monster health/experience, weapon ranges/damage, hit checks,
  and damage rolls. The Java implementation is new and covers the subset above.

JUnit tests cover encounter spawning/pursuit, combat map decoding, all class
starting equipment, attacks and obstructions, enemy routing, turn order, experience,
victory/fleeing/defeat transitions, persistent health, chunk order and boundaries,
EGA nibble/color decoding,
malformed records, actual resource loading and landmarks, class starts,
town transitions, doors/NPC blocking, castle stairs, and dialogue questions.
