package org.jultima4;

import java.io.IOException;
import java.util.Random;

/** Solo Avatar's persistent combat stats and starting equipment. */
public final class Avatar {
    public enum Weapon {
        HANDS("Hands", 1, 8), STAFF("Staff", 1, 16), DAGGER("Dagger", 10, 24),
        SLING("Sling", 10, 32), MACE("Mace", 1, 40), AXE("Axe", 1, 48), SWORD("Sword", 1, 64);
        public final String displayName;
        public final int range, damage;
        Weapon(String name, int range, int damage) { displayName = name; this.range = range; this.damage = damage; }
    }

    private final int maximumHealth, strength, dexterity, defense;
    private int health, experience;
    private Weapon weapon;

    public Avatar(int maximumHealth, int strength, int dexterity, Weapon weapon, int defense, int experience) {
        if (maximumHealth <= 0 || strength < 0 || dexterity < 0 || defense < 0 || defense > 255 || experience < 0) {
            throw new IllegalArgumentException("Invalid Avatar combat stats");
        }
        this.maximumHealth = maximumHealth;
        this.health = maximumHealth;
        this.strength = strength;
        this.dexterity = dexterity;
        this.weapon = java.util.Objects.requireNonNull(weapon);
        this.defense = defense;
        this.experience = Math.min(9999, experience);
    }

    public static Avatar from(CharacterCreation creation) throws IOException {
        return fromTemplate(creation, DosGameData.read("PARTY.NEW"));
    }

    static Avatar fromTemplate(CharacterCreation creation, byte[] data) {
        if (data.length != 502) throw new IllegalArgumentException("PARTY.NEW must contain 502 bytes");
        int record = 8 + creation.winner().ordinal() * 39;
        int weapon = word(data, record + 16), armor = word(data, record + 18);
        if (weapon >= Weapon.values().length || armor > 3 || data[record + 37] != creation.winner().ordinal()) {
            throw new IllegalArgumentException("Unsupported starting equipment or class in PARTY.NEW");
        }
        int[] defenses = {96, 128, 144, 160};
        return new Avatar(word(data, record + 2), creation.strength(), creation.dexterity(),
            Weapon.values()[weapon], defenses[armor], word(data, record + 4));
    }

    private static int word(byte[] data, int offset) {
        return Byte.toUnsignedInt(data[offset]) | Byte.toUnsignedInt(data[offset + 1]) << 8;
    }

    public int health() { return health; }
    public int maximumHealth() { return maximumHealth; }
    public int experience() { return experience; }
    public int defense() { return defense; }
    public int attackBonus() { return dexterity >= 40 ? 255 : dexterity; }
    public Weapon weapon() { return weapon; }
    public boolean defeated() { return health == 0; }

    int damage(Random random) { return random.nextInt(Math.min(255, weapon.damage + strength)); }
    void loseThrownDagger() { weapon = Weapon.HANDS; }
    void hurt(int damage) {
        if (damage < 0) throw new IllegalArgumentException("Negative damage");
        health = Math.max(0, health - damage);
    }
    void awardExperience(int amount) {
        if (amount < 0) throw new IllegalArgumentException("Negative experience");
        experience = (int) Math.min(9999L, (long) experience + amount);
    }
}
