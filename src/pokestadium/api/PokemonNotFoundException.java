package pokestadium.api;

/**
 * Se lanza cuando PokeAPI responde 404 para el nombre/id solicitado.
 */
public class PokemonNotFoundException extends Exception {
    public PokemonNotFoundException(String query) {
        super("Pokémon no encontrado: \"" + query + "\"");
    }
}
