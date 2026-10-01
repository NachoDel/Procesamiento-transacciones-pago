package policy;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Random;

/**
 * [RANDOM-POLICY]
 *
 * Política que resuelve un conflicto seleccionando aleatoriamente
 * una transición entre las candidatas disponibles.
 *
 * [REPRODUCIBILITY]
 * Permite utilizar una seed fija para repetir exactamente
 * una secuencia pseudoaleatoria durante tests o experimentos.
 *
 * [STATE-INDEPENDENCE]
 * Esta clase no conoce ni modifica la Red de Petri.
 * Solamente trabaja con la colección de candidatos recibida.
 */
public final class RandomPolicy implements Policy {

    /**
     * [RANDOM-GENERATOR]
     *
     * Generador pseudoaleatorio utilizado para seleccionar
     * una posición dentro de la lista de candidatos.
     */
    private final Random random;

    /**
     * [NON-DETERMINISTIC-CONSTRUCTOR]
     *
     * Construye una política con una seed elegida automáticamente.
     *
     * Es apropiada para ejecuciones donde no se necesita reproducir
     * exactamente la misma secuencia de decisiones.
     */
    public RandomPolicy() {
        this.random = new Random();
    }

    /**
     * [REPRODUCIBLE-CONSTRUCTOR]
     *
     * Construye una política pseudoaleatoria con una seed conocida.
     *
     * Dos instancias creadas con la misma seed producirán las mismas
     * elecciones siempre que reciban, en cada llamada y en el mismo
     * orden, las mismas listas ordenadas de candidatos.
     *
     * La seed hace reproducible la decisión de la Policy, pero no
     * garantiza por sí sola que toda una ejecución concurrente del
     * sistema sea determinista.
     *
     * @param seed seed del generador pseudoaleatorio
     */
    public RandomPolicy(long seed) {
        this.random = new Random(seed);
    }

    /**
     * [POLICY-SELECTION]
     *
     * Selecciona uniformemente una posición de la lista.
     *
     * La política no modifica la colección recibida.
     */
    @Override
    public OptionalInt select(List<Integer> candidates) {

        // [INPUT-VALIDATION]
        Objects.requireNonNull(
                candidates,
                "Candidates cannot be null"
        );

        /*
         * [NO-CANDIDATES]
         *
         * La ausencia de alternativas es una situación válida.
         * OptionalInt permite representarla sin null ni valores
         * especiales como -1.
         */
        if (candidates.isEmpty()) {
            return OptionalInt.empty();
        }

        /*
         * [RANDOM-SELECTION]
         *
         * nextInt(size) produce un índice entre:
         *
         * 0 <= index < candidates.size()
         */
        int selectedIndex =
                random.nextInt(candidates.size());

        return OptionalInt.of(
                candidates.get(selectedIndex)
        );
    }
}


// ~~~~~~~~~~~ INFO ~~~~~~~~~~~ \\
/**
 * En esta politica, todas las transiciones candidatas 
 * tienen la MISMA PROBABILIDAD de ser elegidas.
 */

/**
 * la Policy no garantiza que haya exactamente 33,33 % de cada flujo al final de una corrida. 
 * La disponibilidad de recursos y la temporalidad también afectan qué candidatas existen en 
 * cada momento. Más adelante los experimentos analizarán la distribución real
 */

/** 
 * Por mas que podamos poner una seed fija, no podemos garantizar que la secuencia de decisiones sea la misma en cada corrida.
 * Ni aunque pongamos el mismo marcado inicial. Porque si bien podemos usar la misma seed, la secuencia de candidatos puede variar en cada corrida.
 * Por mera disponibilidad de los hilos (recordemos que un hilo representa un camino posible)
 */
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ \\