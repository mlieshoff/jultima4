# jultima4

A first playable Java 21 / Maven / LibGDX desktop demonstration based on the
Apple II Ultima IV listings in `DECOMPILED`.

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

Choose A or B for seven virtue pairs to create an Avatar. Then explore the
island using arrows or WASD. Stand on or beside each shrine and press M to
meditate. Visit all eight to finish the demo quest. Space passes a turn, R
starts a new character, and Escape exits. Restart discards the current session;
there is no persistence yet.

Shrines are at (5,5), (16,5), (26,5), (5,16), (26,16), (5,26), (16,26),
and (26,26). A mountain barrier has a passage at y=16. Water and mountains
block movement. These terrain rules and shrine objectives are demonstration
rules, not a claim of an exact original-game port.

## Source evidence and scope

* `DECOMPILED/DISK_01/NEWGAME/full_listing.md`: seven choices at $6707,
  class names near $67F9, base stats at $6809, stat increments at $680C/$6814/
  $681C, virtue scores at $6824 and +5 per selection at $66C9.
* `DECOMPILED/assets/game/01_SHP.png`: original supplied tile sheet, packaged
  as a Maven resource without changing the source asset.
* `DECOMPILED/DISK_01/ULT4/full_listing.md`: pass command at $4145.

Virtue prompts are simplified to selecting names, rather than reproducing the
28 original dilemmas. Card pairing uses a shuffled elimination queue. The
hand-authored 32x32 demonstration island is **not Britannia**. Town and dungeon
map files are listed as not yet covered in DISK_03 and DISK_04; no original
world map or map binaries are supplied. Original starting coordinates are
marked tentative in the notes and are not used for this demo.

Combat, NPC conversations, towns, dungeons, spells, inventory, food, vehicles,
save/load, and the original endgame are not implemented. The eight-shrine
exploration goal is new demo content; it does not implement original shrine
or virtue advancement mechanics.

The plain Java character and world models are independent of OpenGL so their
rules can be tested without a window. The desktop adapter handles input,
viewport scaling, and rendering. LibGDX's LWJGL3 desktop backend is used
([Maven integration documentation](https://libgdx.com/wiki/articles/maven-integration)).
