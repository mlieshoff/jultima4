package org.jultima4;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Loads only data resources; DOS executables and supplied saves are never run or modified. */
public record DosGameData(DosMap world, EgaTiles shapes, Map<String, DosTown> towns) {
  public record Location(String file, String name, int x, int y, int entryX, int entryY) {}

  // Portal coordinates verified against WORLD.MAP and XU4's Ultima-IV/maps.b.
  public static final List<Location> LOCATIONS =
      List.of(
          new Location("LCB_1", "Castle Britannia", 86, 107, 15, 30),
          new Location("LYCAEUM", "Lycaeum", 218, 107, 15, 30),
          new Location("EMPATH", "Empath Abbey", 28, 50, 15, 30),
          new Location("SERPENT", "Serpent's Hold", 146, 241, 15, 30),
          new Location("MOONGLOW", "Moonglow", 232, 135, 1, 15),
          new Location("BRITAIN", "Britain", 82, 106, 1, 15),
          new Location("JHELOM", "Jhelom", 36, 222, 1, 15),
          new Location("YEW", "Yew", 58, 43, 1, 15),
          new Location("MINOC", "Minoc", 159, 20, 1, 15),
          new Location("TRINSIC", "Trinsic", 106, 184, 1, 15),
          new Location("SKARA", "Skara Brae", 22, 128, 1, 15),
          new Location("MAGINCIA", "Magincia", 187, 169, 1, 15),
          new Location("PAWS", "Paws", 98, 145, 1, 15),
          new Location("DEN", "Buccaneer's Den", 136, 158, 1, 15),
          new Location("VESPER", "Vesper", 201, 59, 1, 15),
          new Location("COVE", "Cove", 136, 90, 1, 15));

  public DosGameData {
    towns = Map.copyOf(towns);
  }

  public static DosGameData load() throws IOException {
    Map<String, DosTown> towns = new LinkedHashMap<>();
    for (Location location : LOCATIONS) {
      String tlk = location.file().equals("LCB_1") ? "LCB" : location.file();
      towns.put(location.file(), town(location.file(), tlk));
    }
    towns.put("LCB_2", town("LCB_2", "LCB"));
    return new DosGameData(
        DosMap.world(read("WORLD.MAP")), new EgaTiles(read("SHAPES.EGA")), towns);
  }

  private static DosTown town(String name, String tlk) throws IOException {
    try {
      return DosTown.decode(read(name + ".ULT"), read(tlk + ".TLK"));
    } catch (IllegalArgumentException ex) {
      throw new IOException("Invalid town data: " + name, ex);
    }
  }

  static byte[] read(String name) throws IOException {
    try (InputStream stream = DosGameData.class.getResourceAsStream("/dos/" + name)) {
      if (stream == null) {
        throw new IOException("Missing DOS data resource: " + name);
      }
      return stream.readAllBytes();
    }
  }
}
