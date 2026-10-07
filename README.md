# Pokémon Stadium Lite

Laboratorio de **Desarrollo de Software III**. Es una aplicación de escritorio en **Java Swing** que simula un combate por turnos entre dos Pokémon obtenidos en vivo desde [PokeAPI](https://pokeapi.co/).

## Participantes

| Nombre | Código |
|---|---|
| Gustavo Restrepo | 2380618-3743 |
| Santiago Velasquez Bedoya | 2380378-3743 |

## ¿Qué hace?

- Permite cargar dos Pokémon por nombre/número (**Load**) o de forma aleatoria (**Random**).
- Muestra el sprite, nombre, tipos, stats (HP, Attack, Defense, Speed) y una barra de HP de cada uno.
- Con **Fight!** inicia el combate: ataca primero el de mayor Speed y cada turno queda registrado en el log.
- Al final se anuncia el ganador.

## Requisitos

- JDK 11 o superior.
- Conexión a internet.
- Librería `org.json` (incluida en `lib/json-20250517.jar`).

## Ejecución

**IntelliJ IDEA:** abrir el proyecto y ejecutar la clase `pokestadium.Main`.

**Línea de comandos:**

```bash
javac -d out/app -cp lib/json-20250517.jar $(find src -name '*.java')
java -cp out/app:lib/json-20250517.jar pokestadium.Main
```

(En Windows usar `;` en lugar de `:`).

## Diseño

El código se organiza en cuatro paquetes:

| Paquete | Contenido |
|---|---|
| `model` | `Pokemon`: stats, tipos, sprite y HP actual. |
| `api` | `PokeApiClient`: consulta PokeAPI con `HttpClient` y parsea el JSON. |
| `battle` | `Battle`, `TypeChart`, `BattleListener`: reglas del combate. |
| `ui` | `BattleFrame`, `PokemonPanel`: la interfaz Swing. |

La lógica del combate no depende de la interfaz. Avisa de cada evento por medio de `BattleListener` y la ventana se actualiza a partir de esos eventos. Las peticiones a la API y el combate se ejecutan en segundo plano con `SwingWorker`, así la interfaz no se congela.

**Fórmula de daño:** `max(1, round((10 · ATK / DEF + 2) · aleatorio(0.85–1.0) · crítico · efectividad))`, con 10 % de probabilidad de crítico (×1.5) y efectividad simple por tipo (×1.3 / ×0.7).
