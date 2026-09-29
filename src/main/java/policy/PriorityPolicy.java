package policy;

import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * [PRIORITY-POLICY]
 *
 * Política que prioriza una transición determinada cuando
 * dicha transición forma parte de los candidatos disponibles.
 *
 * [HIGH-RISK-FLOW]
 * En el TP, la transición prioritaria representa la entrada
 * al flujo de pago de alto riesgo.
 *
 * [NETWORK-INDEPENDENCE]
 * La transición prioritaria se recibe por configuración.
 * La clase no contiene referencias hardcodeadas como T4.
 *
 * [FALLBACK]
 * Cuando la transición prioritaria no está disponible,
 * la selección se delega a otra Policy.
 *
 * Esto permite, por ejemplo:
 *
 * PriorityPolicy
 *      ↓
 * RandomPolicy
 *
 * obteniendo prioridad estricta para alto riesgo y selección
 * aleatoria entre los demás flujos.
 */
public final class PriorityPolicy implements Policy {

    /**
     * [PRIORITY-CONFIGURATION]
     *
     * Índice de la transición que representa el flujo
     * que debe recibir prioridad.
     */
    private final int priorityTransition;

    /**
     * [FALLBACK-POLICY]
     *
     * Política utilizada cuando la transición prioritaria
     * no forma parte de los candidatos actuales.
     */
    private final Policy fallbackPolicy;

    public PriorityPolicy(
            int priorityTransition,
            Policy fallbackPolicy
    ) {

        if (priorityTransition < 0) {
            throw new IllegalArgumentException(
                    "Priority transition cannot be negative"
            );
        }

        this.priorityTransition =
                priorityTransition;

        this.fallbackPolicy =
                Objects.requireNonNull(
                        fallbackPolicy,
                        "Fallback policy cannot be null"
                );
    }

    /**
     * [POLICY-SELECTION]
     *
     * Si la transición prioritaria está disponible,
     * se selecciona inmediatamente.
     *
     * En caso contrario, la decisión se delega
     * a la política de fallback.
     */
    @Override
    public OptionalInt select(List<Integer> candidates) {

        // [INPUT-VALIDATION]
        Objects.requireNonNull(
                candidates,
                "Candidates cannot be null"
        );

        // [NO-CANDIDATES]
        if (candidates.isEmpty()) {
            return OptionalInt.empty();
        }

        /*
         * [STRICT-PRIORITY]
         *
         * La transición prioritaria gana siempre que
         * forme parte del conflicto actual.
         */
        if (candidates.contains(priorityTransition)) {
            return OptionalInt.of(
                    priorityTransition
            );
        }

        /*
         * [FALLBACK]
         *
         * La transición prioritaria no puede ejecutarse.
         * Delegamos la selección entre las restantes.
         *
         * Se entrega una copia inmutable para evitar
         * modificaciones accidentales de candidatos.
         */
        return fallbackPolicy.select(
                List.copyOf(candidates)
        );
    }
}

/**
 * Dentro tiene otra policy para dar la prioridad a
 * TX. Si TX no esta disponible, delega la decision 
 * a otra policy (es decir es aleatorio).
 * 
 * TX ya que nos permite decidir QUE TRANSICION PRIORIZAR
 * 
 * Si la TX esta constantemente habilitada, PRODUCE INANICION
 * Ya que se va a dar prioridad de disparo a ella,  produciendo 
 * starvation al resto de transiciones.
 */