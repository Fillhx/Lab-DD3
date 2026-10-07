package pokestadium.battle;

import pokestadium.model.Pokemon;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Reglas del combate por turnos. No conoce nada de Swing: comunica todo
 * a través de {@link BattleListener}.
 *
 * <p>Fórmula de daño elegida (inspirada en la de los juegos, pero simplificada):
 * <pre>
 *   base   = 10 * ATK / DEF + 2
 *   random = uniforme en [0.85, 1.00]
 *   daño   = max(1, round(base * random * crítico * efectividad))
 * </pre>
 * Usar la razón ATK/DEF (en vez de una resta) evita daños negativos o nulos cuando la
 * defensa es alta, y produce combates de ~3 a 10 turnos con los stats base de PokeAPI.
 * Crítico: 10% de probabilidad, x1.5. Efectividad: ver {@link TypeChart}.
 */
public class Battle {
    private static final double CRITICAL_CHANCE = 0.10;
    private static final double CRITICAL_MULTIPLIER = 1.5;

    private final Pokemon first;
    private final Pokemon second;
    private final String firstName;
    private final String secondName;
    private final Random random;
    private final List<BattleListener> listeners = new CopyOnWriteArrayList<>();

    public Battle(Pokemon p1, Pokemon p2) {
        this(p1, p2, new Random());
    }

    /** Constructor con Random inyectable (útil para pruebas). */
    public Battle(Pokemon p1, Pokemon p2, Random random) {
        this.random = random;
        // Orden de turnos: mayor Speed primero; empate -> aleatorio.
        boolean p1First = p1.getSpeed() > p2.getSpeed()
                || (p1.getSpeed() == p2.getSpeed() && random.nextBoolean());
        this.first = p1First ? p1 : p2;
        this.second = p1First ? p2 : p1;

        // Si ambos son el mismo Pokémon, se distinguen por jugador en los eventos.
        boolean sameName = p1.getName().equals(p2.getName());
        String n1 = sameName ? p1.getName() + " (J1)" : p1.getName();
        String n2 = sameName ? p2.getName() + " (J2)" : p2.getName();
        this.firstName = p1First ? n1 : n2;
        this.secondName = p1First ? n2 : n1;
    }

    public void addListener(BattleListener listener) {
        listeners.add(listener);
    }

    /** Nombre con el que este combate identifica al Pokémon en los eventos. */
    public String nameOf(Pokemon p) {
        return p == first ? firstName : secondName;
    }

    /**
     * Ejecuta el combate completo. Es bloqueante (duerme entre turnos para que se vea la
     * animación), así que debe llamarse desde un hilo que no sea el EDT.
     *
     * @param turnDelayMs pausa entre ataques en milisegundos
     */
    public void run(long turnDelayMs) throws InterruptedException {
        first.restoreHp();
        second.restoreHp();
        listeners.forEach(l -> l.onBattleStarted(firstName, secondName));
        listeners.forEach(l -> l.onHpChanged(firstName, first.getCurrentHp()));
        listeners.forEach(l -> l.onHpChanged(secondName, second.getCurrentHp()));

        Pokemon attacker = first;
        Pokemon defender = second;
        while (true) {
            Thread.sleep(turnDelayMs);
            attack(attacker, defender);
            if (defender.isFainted()) {
                String winner = nameOf(attacker);
                listeners.forEach(l -> l.onBattleEnded(winner));
                return;
            }
            Pokemon tmp = attacker;
            attacker = defender;
            defender = tmp;
        }
    }

    /** Resuelve un ataque y notifica los eventos correspondientes. */
    private void attack(Pokemon attacker, Pokemon defender) {
        boolean critical = random.nextDouble() < CRITICAL_CHANCE;
        double modifier = TypeChart.modifier(attacker.getPrimaryType(), defender.getPrimaryType());
        int damage = calculateDamage(attacker, defender, critical, modifier);

        defender.receiveDamage(damage);

        String atkName = nameOf(attacker);
        String defName = nameOf(defender);
        int hp = defender.getCurrentHp();
        listeners.forEach(l -> l.onTurn(atkName, defName, damage, critical, modifier));
        listeners.forEach(l -> l.onHpChanged(defName, hp));
    }

    int calculateDamage(Pokemon attacker, Pokemon defender, boolean critical, double modifier) {
        double base = 10.0 * attacker.getAttack() / Math.max(1, defender.getDefense()) + 2;
        double variance = 0.85 + random.nextDouble() * 0.15;
        double damage = base * variance * (critical ? CRITICAL_MULTIPLIER : 1.0) * modifier;
        return Math.max(1, (int) Math.round(damage));
    }
}
