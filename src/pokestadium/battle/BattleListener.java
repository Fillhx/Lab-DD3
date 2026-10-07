package pokestadium.battle;

/**
 * Eventos que emite {@link Battle}. La UI se actualiza únicamente a partir de ellos,
 * lo que desacopla las reglas del combate de la interfaz gráfica.
 */
public interface BattleListener {

    /** Se emite al iniciar el combate, indicando quién ataca primero. */
    default void onBattleStarted(String first, String second) { }

    /**
     * Un ataque fue resuelto.
     *
     * @param modifier multiplicador por efectividad de tipo (1.3, 1.0 ó 0.7)
     */
    void onTurn(String attacker, String defender, int damage, boolean critical, double modifier);

    void onHpChanged(String pokemon, int hpActual);

    void onBattleEnded(String winner);
}
