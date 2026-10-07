package pokestadium;

import pokestadium.ui.BattleFrame;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de Pokémon Stadium Lite.
 */
public class Main {
    public static void main(String[] args) {
        // Toda la construcción de la UI ocurre en el Event Dispatch Thread (EDT).
        SwingUtilities.invokeLater(() -> new BattleFrame().setVisible(true));
    }
}
