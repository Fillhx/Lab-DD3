package pokestadium.model;

import java.util.List;

/**
 * Modelo de un Pokémon: datos básicos obtenidos de PokeAPI más su HP actual en combate.
 */
public class Pokemon {
    private final int id;
    private final String name;
    private final List<String> types;
    private final int maxHp;
    private final int attack;
    private final int defense;
    private final int speed;
    private final String spriteUrl;
    private int currentHp;

    public Pokemon(int id, String name, List<String> types, int maxHp, int attack,
                   int defense, int speed, String spriteUrl) {
        this.id = id;
        this.name = name;
        this.types = List.copyOf(types);
        this.maxHp = maxHp;
        this.attack = attack;
        this.defense = defense;
        this.speed = speed;
        this.spriteUrl = spriteUrl;
        this.currentHp = maxHp;
    }

    /** Resta daño al HP actual sin permitir valores negativos. */
    public void receiveDamage(int damage) {
        currentHp = Math.max(0, currentHp - Math.max(0, damage));
    }

    /** Restaura el HP al máximo (antes de cada combate). */
    public void restoreHp() {
        currentHp = maxHp;
    }

    public boolean isFainted() {
        return currentHp == 0;
    }

    /** Primer tipo del Pokémon (el único que se usa para la efectividad). */
    public String getPrimaryType() {
        return types.isEmpty() ? "" : types.get(0);
    }

    public int getId() { return id; }
    public String getName() { return name; }
    public List<String> getTypes() { return types; }
    public int getMaxHp() { return maxHp; }
    public int getAttack() { return attack; }
    public int getDefense() { return defense; }
    public int getSpeed() { return speed; }
    public String getSpriteUrl() { return spriteUrl; }
    public int getCurrentHp() { return currentHp; }

    @Override
    public String toString() {
        return name + " (HP " + currentHp + "/" + maxHp + ")";
    }
}
