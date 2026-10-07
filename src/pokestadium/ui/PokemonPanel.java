package pokestadium.ui;

import pokestadium.api.PokeApiClient;
import pokestadium.api.PokemonNotFoundException;
import pokestadium.model.Pokemon;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Panel de un jugador: selección (Load / Random) y vista del Pokémon cargado.
 * Las peticiones a PokeAPI se hacen con {@link SwingWorker} para no bloquear el EDT.
 */
public class PokemonPanel extends JPanel {
    private static final int SPRITE_SIZE = 192;
    private static final Color HP_GREEN = new Color(0x3CB043);
    private static final Color HP_YELLOW = new Color(0xF4C430);
    private static final Color HP_RED = new Color(0xD0312D);

    private final PokeApiClient client;
    private final Runnable onStateChanged;

    private final JTextField nameField = new JTextField(12);
    private final JButton loadButton = new JButton("Load");
    private final JButton randomButton = new JButton("Random");
    private final JLabel spriteLabel = new JLabel("?", SwingConstants.CENTER);
    private final JLabel nameLabel = new JLabel("—", SwingConstants.CENTER);
    private final JLabel typesLabel = new JLabel(" ", SwingConstants.CENTER);
    private final JLabel hpStat = new JLabel();
    private final JLabel attackStat = new JLabel();
    private final JLabel defenseStat = new JLabel();
    private final JLabel speedStat = new JLabel();
    private final JProgressBar hpBar = new JProgressBar();
    private final JLabel statusLabel = new JLabel(" ", SwingConstants.CENTER);

    private Pokemon pokemon;
    private boolean loading;

    /**
     * @param onStateChanged se invoca (en el EDT) cuando cambia el Pokémon o el estado de carga
     */
    public PokemonPanel(String title, PokeApiClient client, Runnable onStateChanged) {
        this.client = client;
        this.onStateChanged = onStateChanged;
        buildUi(title);
        showPokemon(null);

        // ActionListeners de los botones (Enter en el campo de texto equivale a "Load").
        loadButton.addActionListener(e -> loadByName());
        nameField.addActionListener(e -> loadByName());
        randomButton.addActionListener(e -> load(client::fetchRandomPokemon));
    }

    private void buildUi(String title) {
        setLayout(new BorderLayout(5, 5));
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(title),
                BorderFactory.createEmptyBorder(5, 5, 5, 5)));

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        controls.add(nameField);
        controls.add(loadButton);
        controls.add(randomButton);
        add(controls, BorderLayout.NORTH);

        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        spriteLabel.setPreferredSize(new Dimension(SPRITE_SIZE, SPRITE_SIZE));
        spriteLabel.setMinimumSize(spriteLabel.getPreferredSize());
        spriteLabel.setMaximumSize(spriteLabel.getPreferredSize());
        spriteLabel.setFont(spriteLabel.getFont().deriveFont(Font.BOLD, 48f));
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 20f));
        typesLabel.setFont(typesLabel.getFont().deriveFont(Font.ITALIC));

        hpBar.setStringPainted(true);
        hpBar.setMaximumSize(new Dimension(SPRITE_SIZE + 60, 22));

        JPanel stats = new JPanel(new GridLayout(2, 2, 10, 2));
        stats.add(hpStat);
        stats.add(attackStat);
        stats.add(defenseStat);
        stats.add(speedStat);
        stats.setMaximumSize(new Dimension(SPRITE_SIZE + 60, 40));

        statusLabel.setForeground(HP_RED);

        for (Component c : new Component[]{spriteLabel, nameLabel, typesLabel, hpBar, stats, statusLabel}) {
            ((javax.swing.JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
            info.add(c);
            info.add(Box.createVerticalStrut(4));
        }
        add(info, BorderLayout.CENTER);
    }

    private void loadByName() {
        String name = nameField.getText().trim();
        if (name.isEmpty()) {
            showError("Escribe un nombre o número de Pokémon.");
            return;
        }
        load(() -> client.fetchPokemon(name));
    }

    /** Resultado de la carga en segundo plano: datos + sprite ya descargado. */
    private record Loaded(Pokemon pokemon, Image sprite) { }

    /** Ejecuta la petición en un hilo de fondo y actualiza la vista al terminar. */
    private void load(Callable<Pokemon> fetch) {
        setLoading(true);
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setText("Cargando...");

        new SwingWorker<Loaded, Void>() {
            @Override
            protected Loaded doInBackground() throws Exception {
                Pokemon p = fetch.call();
                Image sprite = null;
                try {
                    BufferedImage img = client.fetchSprite(p.getSpriteUrl());
                    if (img != null) {
                        // SCALE_FAST mantiene el aspecto pixel-art del sprite.
                        sprite = img.getScaledInstance(SPRITE_SIZE, SPRITE_SIZE, Image.SCALE_FAST);
                    }
                } catch (IOException e) {
                    // Sin sprite el Pokémon sigue siendo válido para combatir.
                }
                return new Loaded(p, sprite);
            }

            @Override
            protected void done() {
                try {
                    Loaded result = get();
                    showPokemon(result.pokemon());
                    spriteLabel.setText(result.sprite() == null ? "Sin imagen" : null);
                    spriteLabel.setIcon(result.sprite() == null ? null : new ImageIcon(result.sprite()));
                    nameField.setText(result.pokemon().getName());
                    statusLabel.setText(" ");
                } catch (ExecutionException e) {
                    showError(describeError(e.getCause()));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    showError("Carga interrumpida.");
                } finally {
                    setLoading(false);
                }
            }
        }.execute();
    }

    private static String describeError(Throwable cause) {
        if (cause instanceof PokemonNotFoundException) {
            return cause.getMessage();
        }
        if (cause instanceof IOException) {
            return "Error de red: " + (cause.getMessage() != null ? cause.getMessage() : "sin conexión");
        }
        return "Error inesperado: " + cause;
    }

    private void showError(String message) {
        statusLabel.setForeground(HP_RED);
        statusLabel.setText(message);
    }

    /** Pinta los datos del Pokémon (o el estado vacío si es null). */
    private void showPokemon(Pokemon p) {
        this.pokemon = p;
        if (p == null) {
            nameLabel.setText("—");
            typesLabel.setText(" ");
            spriteLabel.setIcon(null);
            spriteLabel.setText("?");
            hpStat.setText("HP: -");
            attackStat.setText("Attack: -");
            defenseStat.setText("Defense: -");
            speedStat.setText("Speed: -");
            hpBar.setMaximum(1);
            hpBar.setValue(0);
            hpBar.setString("HP");
            return;
        }
        nameLabel.setText("#" + p.getId() + " " + capitalize(p.getName()));
        typesLabel.setText(p.getTypes().stream().map(PokemonPanel::capitalize)
                .collect(Collectors.joining(" / ")));
        hpStat.setText("HP: " + p.getMaxHp());
        attackStat.setText("Attack: " + p.getAttack());
        defenseStat.setText("Defense: " + p.getDefense());
        speedStat.setText("Speed: " + p.getSpeed());
        hpBar.setMaximum(p.getMaxHp());
        updateHp(p.getMaxHp());
    }

    /** Actualiza la barra de HP. La invoca la ventana al recibir onHpChanged. */
    public void updateHp(int hp) {
        int max = hpBar.getMaximum();
        hpBar.setValue(hp);
        hpBar.setString(hp + " / " + max);
        double ratio = (double) hp / max;
        hpBar.setForeground(ratio > 0.5 ? HP_GREEN : ratio > 0.2 ? HP_YELLOW : HP_RED);
    }

    private void setLoading(boolean loading) {
        this.loading = loading;
        setControlsEnabled(!loading);
        onStateChanged.run();
    }

    /** Habilita/deshabilita la selección (se bloquea durante cargas y combates). */
    public void setControlsEnabled(boolean enabled) {
        nameField.setEnabled(enabled);
        loadButton.setEnabled(enabled);
        randomButton.setEnabled(enabled);
    }

    /** true si hay un Pokémon cargado correctamente y no hay una carga en curso. */
    public boolean isReady() {
        return pokemon != null && !loading;
    }

    public Pokemon getPokemon() {
        return pokemon;
    }

    private static String capitalize(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
