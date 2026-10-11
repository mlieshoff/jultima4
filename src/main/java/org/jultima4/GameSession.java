package org.jultima4;

import java.util.EnumSet;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.io.IOException;

/** Exploration state; town data and graphics remain immutable. */
public final class GameSession {
    private final DosGameData data;
    private final Map<String, Map<Integer, Integer>> changedTiles = new HashMap<>();
    private final EnumSet<Virtue> shrines = EnumSet.noneOf(Virtue.class);
    private int x, y, worldX, worldY, turns;
    private String townKey;
    private String locationName = "Britannia";
    private String message = "Explore Britannia. Stand on a town and press E to enter.";
    private NpcDialogue conversation;
    private boolean awaitingAnswer;
    private final Avatar avatar;
    private final Map<String, CombatMap> combatMaps;
    private final OverworldMonsters monsters;
    private final Random random;
    private Battle battle;
    private OverworldMonsters.Encounter currentEncounter;

    public GameSession(DosGameData data, int x, int y) {
        this(data, x, y, new Avatar(200, 15, 15, Avatar.Weapon.STAFF, 128, 0), loadCombatMaps(), new Random());
    }

    public static GameSession start(DosGameData data, CharacterCreation creation) throws IOException {
        return new GameSession(data, creation.startingX(), creation.startingY(), Avatar.from(creation), CombatMap.load(), new Random());
    }

    public GameSession(DosGameData data, int x, int y, Avatar avatar, Map<String, CombatMap> combatMaps, Random random) {
        this.data = data;
        this.avatar = avatar;
        this.combatMaps = Map.copyOf(combatMaps);
        this.random = random;
        this.monsters = new OverworldMonsters(random);
        this.x = Math.floorMod(x, 256);
        this.y = Math.floorMod(y, 256);
        if (!DosMap.walkable(data.world().tile(this.x, this.y))) {
            throw new IllegalArgumentException("Starting position is not walkable");
        }
    }

    public void move(int dx, int dy) {
        direction(dx, dy);
        if (battle != null) { battle.move(dx, dy); turns++; return; }
        exploring();
        if (conversation != null) throw new IllegalStateException("Finish the conversation before moving");
        turns++;
        int nextX = x + dx, nextY = y + dy;
        if (inTown() && (nextX < 0 || nextY < 0 || nextX >= 32 || nextY >= 32)) {
            leave();
            return;
        }
        if (!inTown()) {
            nextX = Math.floorMod(nextX, 256);
            nextY = Math.floorMod(nextY, 256);
        }
        OverworldMonsters.Encounter encounter = inTown() ? null : monsters.at(nextX, nextY);
        if (encounter != null) { beginCombat(encounter); return; }
        if (!DosMap.walkable(groundTile(nextX, nextY)) || npcAt(nextX, nextY) != null) {
            message = "Blocked!";
            advanceMonsters();
            return;
        }
        x = nextX;
        y = nextY;
        message = locationName;
        advanceMonsters();
    }

    public void enter() {
        exploring();
        turns++;
        if (inTown()) { message = "Already inside a town. Walk off its edge to leave."; return; }
        for (DosGameData.Location location : DosGameData.LOCATIONS) {
            if (x == location.x() && y == location.y()) {
                worldX = x;
                worldY = y;
                townKey = location.file();
                locationName = location.name();
                x = location.entryX();
                y = location.entryY();
                message = "Enter " + locationName + ". T then a direction to talk.";
                return;
            }
        }
        message = groundTile(x, y) == 9 ? "Dungeon gameplay is not implemented yet." :
                  groundTile(x, y) == 30 ? "Stand at the shrine and press M to contemplate." : "No town entrance here.";
        advanceMonsters();
    }

    public void leave() {
        exploring();
        if (!inTown()) { message = "Already in Britannia."; return; }
        x = worldX;
        y = worldY;
        townKey = null;
        locationName = "Britannia";
        endConversation();
        message = "Return to Britannia.";
    }

    public void climb() {
        exploring();
        turns++;
        if (("LCB_1".equals(townKey) || "LCB_2".equals(townKey)) && y == 3 && (x == 3 || x == 27)) {
            townKey = townKey.equals("LCB_1") ? "LCB_2" : "LCB_1";
            locationName = townKey.equals("LCB_2") ? "Castle Britannia - upper floor" : "Castle Britannia";
            message = locationName;
        } else message = "No usable castle stairs here.";
        advanceMonsters();
    }

    public void openDoor(int dx, int dy) {
        exploring();
        direction(dx, dy);
        turns++;
        int tile = groundTile(x + dx, y + dy);
        if (inTown() && tile == 59) {
            changedTiles.computeIfAbsent(townKey, key -> new HashMap<>()).put((y + dy) * 32 + x + dx, 22);
            message = "Door opened.";
        } else message = tile == 58 ? "Locked!" : "No door in that direction.";
        advanceMonsters();
    }

    public void talk(int dx, int dy) {
        exploring();
        direction(dx, dy);
        turns++;
        DosTown.Npc npc = npcAt(x + dx, y + dy);
        if (npc == null) { message = "No one in that direction."; advanceMonsters(); return; }
        if (npc.dialogue() == null) { message = "This person cannot converse yet."; return; }
        conversation = npc.dialogue();
        awaitingAnswer = false;
        message = conversation.name() + "\n" + conversation.description() + "\nAsk NAME, JOB, HEAL or a keyword. BYE ends the conversation.";
    }

    public void ask(String text) {
        if (conversation == null) throw new IllegalStateException("No active conversation");
        String input = text.strip();
        if (input.equalsIgnoreCase("bye")) { endConversation(); return; }
        if (awaitingAnswer) {
            if (!input.equalsIgnoreCase("yes") && !input.equalsIgnoreCase("no")) {
                message = "Answer YES or NO. " + conversation.question();
                return;
            }
            message = conversation.answer(input.equalsIgnoreCase("yes"));
            awaitingAnswer = false;
        } else {
            int topic = conversation.topic(input);
            message = conversation.response(topic);
            if (topic > 0 && topic == conversation.questionTrigger() && !conversation.question().isBlank()) {
                awaitingAnswer = true;
                message += "\n" + conversation.question() + "\nAnswer YES or NO.";
            }
        }
    }

    public void endConversation() {
        conversation = null;
        awaitingAnswer = false;
        message = "Fare thee well.";
    }

    public void meditate() {
        exploring();
        turns++;
        if (inTown() || groundTile(x, y) != 30) { message = "No shrine here."; advanceMonsters(); return; }
        int[][] coordinates = {{233,66},{128,92},{36,229},{73,11},{205,45},{81,207},{231,216}};
        Virtue[] surfaceVirtues = {Virtue.HONESTY, Virtue.COMPASSION, Virtue.VALOR, Virtue.JUSTICE,
            Virtue.SACRIFICE, Virtue.HONOR, Virtue.HUMILITY};
        // Spirituality is reached through the original moongate system (not implemented).
        for (int i = 0; i < coordinates.length; i++) {
            if (x == coordinates[i][0] && y == coordinates[i][1]) {
                shrines.add(surfaceVirtues[i]);
                message = "Contemplate " + surfaceVirtues[i] + ". Full shrine rituals are not implemented yet.";
                advanceMonsters();
                return;
            }
        }
        message = "Contemplate at the shrine. Full shrine rituals are not implemented yet.";
        advanceMonsters();
    }

    public void pass() {
        if (battle != null) { battle.pass(); turns++; return; }
        exploring();
        turns++;
        message = "Pass";
        advanceMonsters();
    }

    public void attack(int dx, int dy) {
        direction(dx, dy);
        if (battle != null) { battle.attack(dx, dy); turns++; return; }
        exploring();
        if (talking()) throw new IllegalStateException("Finish the conversation before attacking");
        turns++;
        OverworldMonsters.Encounter encounter = inTown() ? null : monsters.at(x + dx, y + dy);
        if (encounter != null) beginCombat(encounter);
        else { message = "No monster in that direction."; advanceMonsters(); }
    }

    private void advanceMonsters() {
        if (inTown() || talking() || battle != null) return;
        OverworldMonsters.Encounter encounter = monsters.advance(data.world(), x, y);
        if (encounter != null) beginCombat(encounter);
    }

    private void beginCombat(OverworldMonsters.Encounter encounter) {
        String name = CombatMap.forTerrain(groundTile(x, y));
        CombatMap map = combatMaps.get(name);
        if (map == null) throw new IllegalStateException("Missing battlefield: " + name);
        battle = new Battle(map, avatar, encounter.type(), encounter.count(), random);
        currentEncounter = encounter;
    }

    public void finishCombat() {
        if (battle == null || battle.outcome() == Battle.Outcome.ACTIVE || battle.outcome() == Battle.Outcome.DEFEAT) {
            throw new IllegalStateException("Combat cannot be dismissed");
        }
        if (battle.outcome() == Battle.Outcome.VICTORY) {
            monsters.remove(currentEncounter);
            message = "Victory! Return to Britannia.";
        } else {
            monsters.escaped(currentEncounter, battle.enemies().size());
            message = "Thou hast escaped. Move away from the surviving monsters.";
        }
        battle = null;
        currentEncounter = null;
    }

    private void exploring() {
        if (battle != null || avatar.defeated()) throw new IllegalStateException("Exploration is unavailable during combat or defeat");
    }

    private static Map<String, CombatMap> loadCombatMaps() {
        try { return CombatMap.load(); }
        catch (IOException ex) { throw new IllegalStateException("Cannot load combat maps", ex); }
    }

    public Avatar avatar() { return avatar; }
    public Battle battle() { return battle; }
    public List<OverworldMonsters.Encounter> monsters() { return monsters.encounters(); }
    public boolean inTown() { return townKey != null; }
    public boolean talking() { return conversation != null; }
    public String npcName() { return conversation == null ? "" : conversation.name(); }
    public int x() { return x; }
    public int y() { return y; }
    public int turns() { return turns; }
    public int visitedCount() { return shrines.size(); }
    public String locationName() { return locationName; }
    public String message() { return message; }

    /** Current sight only; monsters and NPCs do not replace terrain in occlusion checks. */
    public boolean visible(int targetX, int targetY) {
        if (inTown() && (targetX < 0 || targetY < 0 || targetX >= 32 || targetY >= 32)) return false;
        int dx = inTown() ? targetX - x : Math.floorMod(targetX - x + 128, 256) - 128;
        int dy = inTown() ? targetY - y : Math.floorMod(targetY - y + 128, 256) - 128;
        if (Math.abs(dx) > 7 || Math.abs(dy) > 7) return false;
        return LineOfSight.visible(this::groundTile, x, y, x + dx, y + dy);
    }

    public int tile(int x, int y) {
        if (!inTown()) {
            OverworldMonsters.Encounter encounter = monsters.at(x, y);
            if (encounter != null) return encounter.type().tile;
        }
        DosTown.Npc npc = npcAt(x, y);
        return npc == null ? groundTile(x, y) : npc.tile();
    }

    private int groundTile(int x, int y) {
        if (!inTown()) return data.world().tile(x, y);
        if (x < 0 || y < 0 || x > 31 || y > 31) return 126;
        return changedTiles.getOrDefault(townKey, Map.of()).getOrDefault(y * 32 + x, data.towns().get(townKey).map().tile(x, y));
    }

    private DosTown.Npc npcAt(int x, int y) {
        if (!inTown()) return null;
        for (DosTown.Npc npc : data.towns().get(townKey).npcs()) {
            if (npc.x() == x && npc.y() == y) return npc;
        }
        return null;
    }

    private static void direction(int dx, int dy) {
        if (Math.abs(dx) + Math.abs(dy) != 1) throw new IllegalArgumentException("Choose one cardinal direction");
    }
}
