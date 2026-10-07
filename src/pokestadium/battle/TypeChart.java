package pokestadium.battle;

import java.util.Map;

/**
 * Tabla de efectividad simplificada (solo primer tipo):
 * Agua > Fuego, Fuego > Planta, Planta > Agua.
 */
public final class TypeChart {
    public static final double SUPER_EFFECTIVE = 1.3;
    public static final double NOT_VERY_EFFECTIVE = 0.7;
    public static final double NEUTRAL = 1.0;

    /** atacante -> tipo al que le gana. */
    private static final Map<String, String> BEATS = Map.of(
            "water", "fire",
            "fire", "grass",
            "grass", "water"
    );

    private TypeChart() { }

    public static double modifier(String attackerType, String defenderType) {
        if (defenderType.equals(BEATS.get(attackerType))) {
            return SUPER_EFFECTIVE;
        }
        if (attackerType.equals(BEATS.get(defenderType))) {
            return NOT_VERY_EFFECTIVE;
        }
        return NEUTRAL;
    }
}
