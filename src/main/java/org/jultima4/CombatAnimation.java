package org.jultima4;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Replays a completed turn without changing combat state or consuming random rolls. */
public final class CombatAnimation {
    public record Sprite(int tile, float x, float y, float alpha) { }
    private final Map<Integer, Integer> actors = new LinkedHashMap<>();
    private List<Battle.Event> events = List.of();
    private int index;
    private float elapsed;

    public void capture(Battle battle) {
        clear();
        actors.put(battle.y() * 11 + battle.x(), 31);
        for (Battle.Enemy enemy : battle.enemies()) actors.put(enemy.y() * 11 + enemy.x(), enemy.type().tile);
    }

    public void play(List<Battle.Event> turn) { events = List.copyOf(turn); index = 0; elapsed = 0; }
    public void clear() { actors.clear(); events = List.of(); index = 0; elapsed = 0; }
    public boolean playing() { return index < events.size(); }
    public Battle.Event current() { return playing() ? events.get(index) : null; }
    public float progress() { return playing() ? elapsed / duration(current().action()) : 1; }

    public void update(float seconds) {
        if (!Float.isFinite(seconds) || seconds < 0) throw new IllegalArgumentException("Invalid animation delta");
        elapsed += seconds;
        while (playing() && elapsed >= duration(current().action())) {
            elapsed -= duration(current().action());
            apply(current());
            index++;
        }
        if (!playing()) elapsed = 0;
    }

    private static float duration(Battle.Action action) {
        return switch (action) {
            case MOVE -> 0.14f;
            case ATTACK -> 0.20f;
            case HIT, MISS -> 0.16f;
            case DEATH, FLEE -> 0.24f;
        };
    }

    private void apply(Battle.Event event) {
        int source = event.fromY() * 11 + event.fromX();
        switch (event.action()) {
            case MOVE -> {
                Integer actor = actors.remove(source);
                if (actor != null) actors.put(event.toY() * 11 + event.toX(), actor);
            }
            case DEATH, FLEE -> actors.remove(source);
            default -> { }
        }
    }

    public List<Sprite> sprites() {
        List<Sprite> result = new ArrayList<>();
        Battle.Event event = current();
        for (var actor : actors.entrySet()) {
            float x = actor.getKey() % 11, y = actor.getKey() / 11, alpha = 1;
            if (event != null && x == event.fromX() && y == event.fromY()) {
                if (event.action() == Battle.Action.MOVE || event.action() == Battle.Action.FLEE) {
                    x += (event.toX() - event.fromX()) * progress();
                    y += (event.toY() - event.fromY()) * progress();
                }
                if (event.action() == Battle.Action.DEATH || event.action() == Battle.Action.FLEE) alpha = 1 - progress();
                if (event.action() == Battle.Action.ATTACK && Math.abs(event.toX() - event.fromX()) +
                    Math.abs(event.toY() - event.fromY()) == 1) {
                    float lunge = (float) Math.sin(progress() * Math.PI) * 0.2f;
                    x += (event.toX() - event.fromX()) * lunge;
                    y += (event.toY() - event.fromY()) * lunge;
                }
            }
            result.add(new Sprite(actor.getValue(), x, y, alpha));
        }
        return List.copyOf(result);
    }
}
