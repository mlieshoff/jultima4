package org.jultima4;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/** One Avatar action followed by one action per surviving enemy. */
public final class Battle {
    public enum Outcome { ACTIVE, VICTORY, FLED, DEFEAT }
    public enum Action { MOVE, ATTACK, HIT, MISS, DEATH, FLEE }
    public record Event(Action action, int tile, int fromX, int fromY, int toX, int toY) { }
    private final List<Event> events = new ArrayList<>();
    public record Enemy(MonsterType type, int x, int y, int health) { }
    private static final int[][] DIRECTIONS = {{0,-1},{1,0},{0,1},{-1,0}};
    private final CombatMap map;
    private final Avatar avatar;
    private final Random random;
    private final List<Enemy> enemies = new ArrayList<>();
    private int x, y, rounds;
    private Outcome outcome = Outcome.ACTIVE;
    private String message;

    public Battle(CombatMap map, Avatar avatar, MonsterType type, int count, Random random) {
        if (count < 1 || count > 2 || avatar.defeated()) throw new IllegalArgumentException("Invalid encounter");
        this.map = map;
        this.avatar = avatar;
        this.random = random;
        x = map.avatarStart().x();
        y = map.avatarStart().y();
        if (!DosMap.walkable(map.tile(x, y))) throw new IllegalArgumentException("Blocked Avatar start");
        for (CombatMap.Position position : map.monsterStarts()) {
            if ((position.x() == x && position.y() == y) || enemyAt(position.x(), position.y()) >= 0 ||
                !DosMap.walkable(map.tile(position.x(), position.y())) ||
                nextStep(position.x(), position.y()) < 0) continue;
            enemies.add(new Enemy(type, position.x(), position.y(), type.initialHealth(random)));
            if (enemies.size() == count) break;
        }
        if (enemies.size() != count) throw new IllegalArgumentException("Not enough reachable monster starts");
        message = "Combat! " + count + " " + type.displayName + (count == 1 ? "" : "s") + ". F then a direction to attack.";
    }

    public void move(int dx, int dy) {
        active();
        direction(dx, dy);
        events.clear();
        int nextX = x + dx, nextY = y + dy;
        if (outside(nextX, nextY)) {
            events.add(new Event(Action.FLEE, 31, x, y, nextX, nextY));
            outcome = Outcome.FLED;
            rounds++;
            message = "Thou hast fled the battlefield.";
            return;
        }
        if (!DosMap.walkable(map.tile(nextX, nextY)) || enemyAt(nextX, nextY) >= 0) message = "Blocked!";
        else {
            events.add(new Event(Action.MOVE, 31, x, y, nextX, nextY));
            x = nextX; y = nextY; message = "Moved.";
        }
        enemyTurn();
    }

    public void attack(int dx, int dy) {
        active();
        direction(dx, dy);
        events.clear();
        message = "No enemy in that direction.";
        int endX = x, endY = y;
        boolean foundEnemy = false;
        boolean thrownDagger = false;
        for (int distance = 1; distance <= avatar.weapon().range; distance++) {
            int targetX = x + dx * distance, targetY = y + dy * distance;
            if (outside(targetX, targetY)) break;
            endX = targetX; endY = targetY;
            int index = enemyAt(targetX, targetY);
            if (index >= 0) {
                foundEnemy = true;
                events.add(new Event(Action.ATTACK, 31, x, y, targetX, targetY));
                Enemy enemy = enemies.get(index);
                thrownDagger = distance > 1 && avatar.weapon() == Avatar.Weapon.DAGGER;
                if (random.nextInt(256) + avatar.attackBonus() > 128) {
                    events.add(new Event(Action.HIT, enemy.type().tile, x, y, targetX, targetY));
                    int damage = avatar.damage(random);
                    int health = Math.max(0, enemy.health() - damage);
                    message = "Hit " + enemy.type().displayName + " for " + damage + ".";
                    if (health == 0) {
                        events.add(new Event(Action.DEATH, enemy.type().tile, targetX, targetY, targetX, targetY));
                        enemies.remove(index);
                        avatar.awardExperience(enemy.type().experience);
                        message += " Killed! +" + enemy.type().experience + " XP.";
                    } else enemies.set(index, new Enemy(enemy.type(), enemy.x(), enemy.y(), health));
                } else {
                    events.add(new Event(Action.MISS, enemy.type().tile, x, y, targetX, targetY));
                    message = "Missed " + enemy.type().displayName + "!";
                }
                break;
            }
            if (!DosMap.walkable(map.tile(targetX, targetY))) { message = "Attack blocked by terrain."; break; }
        }
        if (!foundEnemy) {
            events.add(new Event(Action.ATTACK, 31, x, y, endX, endY));
            events.add(new Event(Action.MISS, 31, x, y, endX, endY));
        }
        if (thrownDagger) { avatar.loseThrownDagger(); message += " Dagger thrown; hands readied."; }
        if (enemies.isEmpty()) {
            rounds++;
            outcome = Outcome.VICTORY;
            message += "\nVictory! Press Enter to return to Britannia.";
        } else enemyTurn();
    }

    public void pass() { active(); events.clear(); message = "Pass"; enemyTurn(); }

    private void enemyTurn() {
        rounds++;
        StringBuilder log = new StringBuilder(message);
        for (int i = 0; i < enemies.size(); i++) {
            Enemy enemy = enemies.get(i);
            if (Math.abs(enemy.x() - x) + Math.abs(enemy.y() - y) == 1) {
                events.add(new Event(Action.ATTACK, enemy.type().tile, enemy.x(), enemy.y(), x, y));
                if (random.nextInt(256) > avatar.defense()) {
                    events.add(new Event(Action.HIT, 31, enemy.x(), enemy.y(), x, y));
                    int damage = enemy.type().damage(random);
                    avatar.hurt(damage);
                    log.append("\n").append(enemy.type().displayName).append(" hits for ").append(damage).append('.');
                } else {
                    events.add(new Event(Action.MISS, 31, enemy.x(), enemy.y(), x, y));
                    log.append("\n").append(enemy.type().displayName).append(" misses.");
                }
                if (avatar.defeated()) {
                    events.add(new Event(Action.DEATH, 31, x, y, x, y));
                    outcome = Outcome.DEFEAT;
                    log.append("\nThou art defeated. R starts a new character.");
                    break;
                }
            } else {
                int step = nextStep(enemy.x(), enemy.y());
                if (step >= 0) {
                    events.add(new Event(Action.MOVE, enemy.type().tile, enemy.x(), enemy.y(), step % 11, step / 11));
                    enemies.set(i, new Enemy(enemy.type(), step % 11, step / 11, enemy.health()));
                }
            }
        }
        message = log.toString();
    }

    /** Breadth-first routing prevents enemies getting stuck behind rocks or each other. */
    private int nextStep(int fromX, int fromY) {
        int[] previous = new int[121];
        Arrays.fill(previous, -1);
        int start = fromY * 11 + fromX, goal = y * 11 + x;
        ArrayDeque<Integer> queue = new ArrayDeque<>();
        previous[start] = start;
        queue.add(start);
        while (!queue.isEmpty()) {
            int current = queue.remove();
            if (current == goal) {
                while (previous[current] != start && current != start) current = previous[current];
                return current == start ? -1 : current;
            }
            for (int[] direction : DIRECTIONS) {
                int nx = current % 11 + direction[0], ny = current / 11 + direction[1];
                if (outside(nx, ny) || !DosMap.walkable(map.tile(nx, ny))) continue;
                int next = ny * 11 + nx;
                if (previous[next] >= 0 || (next != goal && enemyAt(nx, ny) >= 0)) continue;
                previous[next] = current;
                queue.add(next);
            }
        }
        return -1;
    }

    private int enemyAt(int x, int y) {
        for (int i = 0; i < enemies.size(); i++) if (enemies.get(i).x() == x && enemies.get(i).y() == y) return i;
        return -1;
    }
    private static boolean outside(int x, int y) { return x < 0 || y < 0 || x >= 11 || y >= 11; }
    private void active() { if (outcome != Outcome.ACTIVE) throw new IllegalStateException("Battle has finished"); }
    private static void direction(int dx, int dy) {
        if (Math.abs(dx) + Math.abs(dy) != 1) throw new IllegalArgumentException("Choose a cardinal direction");
    }
    public int tile(int x, int y) {
        int enemy = enemyAt(x, y);
        return enemy >= 0 ? enemies.get(enemy).type().tile : map.tile(x, y);
    }
    public int groundTile(int x, int y) { return map.tile(x, y); }
    public List<Event> events() { return List.copyOf(events); }
    public int x() { return x; }
    public int y() { return y; }
    public int rounds() { return rounds; }
    public Outcome outcome() { return outcome; }
    public String message() { return message; }
    public String mapName() { return map.name(); }
    public List<Enemy> enemies() { return List.copyOf(enemies); }
}
