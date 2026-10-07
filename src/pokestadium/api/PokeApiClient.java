package pokestadium.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pokestadium.model.Pokemon;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Cliente de PokeAPI: hace las peticiones HTTP y convierte el JSON en objetos {@link Pokemon}.
 * Sus métodos son bloqueantes, por eso la UI siempre los invoca desde un hilo en segundo plano.
 */
public class PokeApiClient {
    private static final String BASE_URL = "https://pokeapi.co/api/v2/pokemon/";
    /** Cantidad de Pokémon de la National Dex disponibles en PokeAPI. */
    private static final int MAX_POKEMON_ID = 1025;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** Busca un Pokémon por nombre o id (no distingue mayúsculas). */
    public Pokemon fetchPokemon(String nameOrId) throws PokemonNotFoundException, IOException, InterruptedException {
        String query = nameOrId.trim().toLowerCase();
        if (query.isEmpty()) {
            throw new PokemonNotFoundException(nameOrId);
        }
        URI uri = URI.create(BASE_URL + URLEncoder.encode(query, StandardCharsets.UTF_8));
        HttpResponse<String> response = http.send(request(uri), HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 404) {
            throw new PokemonNotFoundException(nameOrId);
        }
        if (response.statusCode() != 200) {
            throw new IOException("PokeAPI respondió con código HTTP " + response.statusCode());
        }
        try {
            return parsePokemon(new JSONObject(response.body()));
        } catch (JSONException e) {
            throw new IOException("Respuesta JSON inválida de PokeAPI", e);
        }
    }

    /** Busca un Pokémon aleatorio entre 1 y {@value #MAX_POKEMON_ID}. */
    public Pokemon fetchRandomPokemon() throws PokemonNotFoundException, IOException, InterruptedException {
        int id = ThreadLocalRandom.current().nextInt(1, MAX_POKEMON_ID + 1);
        return fetchPokemon(String.valueOf(id));
    }

    /** Descarga el sprite; devuelve null si el Pokémon no tiene sprite o la imagen no es válida. */
    public BufferedImage fetchSprite(String spriteUrl) throws IOException, InterruptedException {
        if (spriteUrl == null || spriteUrl.isBlank()) {
            return null;
        }
        HttpResponse<byte[]> response = http.send(request(URI.create(spriteUrl)),
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            return null;
        }
        return ImageIO.read(new ByteArrayInputStream(response.body()));
    }

    private HttpRequest request(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    /** Extrae del JSON de /pokemon/{name} solo los campos que usa la app. */
    private Pokemon parsePokemon(JSONObject json) {
        int id = json.getInt("id");
        String name = json.getString("name");

        // "types" viene ordenado por "slot": el slot 1 es el tipo principal.
        List<String> types = new ArrayList<>();
        JSONArray typesJson = json.getJSONArray("types");
        for (int i = 0; i < typesJson.length(); i++) {
            types.add(typesJson.getJSONObject(i).getJSONObject("type").getString("name"));
        }

        int hp = 0, attack = 0, defense = 0, speed = 0;
        JSONArray stats = json.getJSONArray("stats");
        for (int i = 0; i < stats.length(); i++) {
            JSONObject stat = stats.getJSONObject(i);
            int value = stat.getInt("base_stat");
            switch (stat.getJSONObject("stat").getString("name")) {
                case "hp" -> hp = value;
                case "attack" -> attack = value;
                case "defense" -> defense = value;
                case "speed" -> speed = value;
                default -> { /* special-attack / special-defense no se usan */ }
            }
        }

        // front_default puede ser null en algunas formas alternativas.
        String sprite = json.getJSONObject("sprites").optString("front_default", null);

        return new Pokemon(id, name, types, hp, attack, defense, speed, sprite);
    }
}
