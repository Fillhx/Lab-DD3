package pokestadium.ui;

import pokestadium.api.PokeApiClient;
import pokestadium.battle.Battle;
import pokestadium.battle.BattleListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Ventana principal: dos {@link PokemonPanel}, botón "Fight!" y log de batalla.
 * Durante el combate la UI solo cambia en respuesta a los eventos de {@link BattleListener}.
 */
public class BattleFrame extends JFrame {
    private static final long TURN_DELAY_MS = 800;

    private final PokeApiClient client = new PokeApiClient();
    private final PokemonPanel player1;
    private final PokemonPanel player2;
    private final JButton fightButton = new JButton("Fight!");
    private final JLabel resultLabel = new JLabel("VS", SwingConstants.CENTER);
    private final JTextArea logArea = new JTextArea(12, 60);
    private boolean battleRunning;

    public BattleFrame() {
        super("Pokémon Stadium Lite");
        player1 = new PokemonPanel("Jugador 1", client, this::updateFightButton);
        player2 = new PokemonPanel("Jugador 2", client, this::updateFightButton);
        buildUi();
        updateFightButton();
        fightButton.addActionListener(e -> startBattle());
    }

    private void buildUi() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        resultLabel.setFont(resultLabel.getFont().deriveFont(Font.BOLD, 28f));
        fightButton.setFont(fightButton.getFont().deriveFont(Font.BOLD, 18f));
        fightButton.setPreferredSize(new Dimension(130, 45));

        JPanel center = new JPanel(new GridLayout(2, 1, 0, 10));
        center.add(resultLabel);
        JPanel buttonHolder = new JPanel(new GridBagLayout());
        buttonHolder.add(fightButton);
        center.add(buttonHolder);
        center.setPreferredSize(new Dimension(220, 0));

        JPanel arena = new JPanel(new BorderLayout(10, 0));
        arena.add(player1, BorderLayout.WEST);
        arena.add(center, BorderLayout.CENTER);
        arena.add(player2, BorderLayout.EAST);

        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        JScrollPane logScroll = new JScrollPane(logArea);
        logScroll.setBorder(BorderFactory.createTitledBorder("Log de batalla"));

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(arena, BorderLayout.CENTER);
        root.add(logScroll, BorderLayout.SOUTH);
        setContentPane(root);

        pack();
        setMinimumSize(getSize());
        setLocationRelativeTo(null);
    }

    /** "Fight!" solo se habilita con ambos Pokémon cargados y sin combate en curso. */
    private void updateFightButton() {
        fightButton.setEnabled(!battleRunning && player1.isReady() && player2.isReady());
    }

    private void startBattle() {
        Battle battle = new Battle(player1.getPokemon(), player2.getPokemon());

        // Relaciona el nombre que usa la batalla en sus eventos con el panel a actualizar.
        Map<String, PokemonPanel> panels = new HashMap<>();
        panels.put(battle.nameOf(player1.getPokemon()), player1);
        panels.put(battle.nameOf(player2.getPokemon()), player2);
        battle.addListener(new UiBattleListener(panels));

        setBattleRunning(true);
        logArea.setText("");
        resultLabel.setText("VS");

        // El combate (con sus pausas entre turnos) corre fuera del EDT.
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                battle.run(TURN_DELAY_MS);
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (ExecutionException e) {
                    log("Error durante el combate: " + e.getCause());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                setBattleRunning(false);
            }
        }.execute();
    }

    private void setBattleRunning(boolean running) {
        battleRunning = running;
        player1.setControlsEnabled(!running);
        player2.setControlsEnabled(!running);
        updateFightButton();
    }

    private void log(String line) {
        logArea.append(line + "\n");
        logArea.setCaretPosition(logArea.getDocument().getLength());
    }

    /**
     * Traduce los eventos del combate a cambios de UI. Los eventos llegan desde el hilo
     * del combate, así que cada actualización se reenvía al EDT con invokeLater.
     */
    private class UiBattleListener implements BattleListener {
        private final Map<String, PokemonPanel> panels;

        UiBattleListener(Map<String, PokemonPanel> panels) {
            this.panels = panels;
        }

        @Override
        public void onBattleStarted(String first, String second) {
            SwingUtilities.invokeLater(() ->
                    log("¡Comienza el combate! " + first + " vs " + second + ". " + first + " ataca primero."));
        }

        @Override
        public void onTurn(String attacker, String defender, int damage, boolean critical, double modifier) {
            StringBuilder sb = new StringBuilder()
                    .append(attacker).append(" ataca a ").append(defender)
                    .append(" y causa ").append(damage).append(" de daño");
            if (critical) {
                sb.append(" ¡GOLPE CRÍTICO!");
            }
            if (modifier > 1.0) {
                sb.append(" ¡Es súper efectivo! (x").append(modifier).append(")");
            } else if (modifier < 1.0) {
                sb.append(" No es muy efectivo... (x").append(modifier).append(")");
            }
            String line = sb.append('.').toString();
            SwingUtilities.invokeLater(() -> log(line));
        }

        @Override
        public void onHpChanged(String pokemon, int hpActual) {
            SwingUtilities.invokeLater(() -> {
                panels.get(pokemon).updateHp(hpActual);
                log("   → HP de " + pokemon + ": " + hpActual);
            });
        }

        @Override
        public void onBattleEnded(String winner) {
            SwingUtilities.invokeLater(() -> {
                log("★ ¡" + winner + " gana el combate! ★");
                resultLabel.setText("<html><center>¡" + winner + "<br>gana!</center></html>");
            });
        }
    }
}
