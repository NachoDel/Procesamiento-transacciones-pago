package worker;

import java.util.List;
import java.util.Objects;

/**
 * [WORKER-DEFINITION]
 *
 * Representa la configuración estructural de un Worker:
 * su identificador y la secuencia de transiciones que ejecuta.
 *
 * [MODEL-CONFIGURATION]
 * Esta clase contiene datos de configuración.
 * No crea threads ni conoce al Monitor o a la Red de Petri.
 */
public final class WorkerDefinition {

    private final String name;

    private final List<Integer> transitionSequence;

    public WorkerDefinition(
            String name,
            List<Integer> transitionSequence
    ) {

        this.name =
                Objects.requireNonNull(
                        name,
                        "Worker name cannot be null"
                );

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Worker name cannot be blank"
            );
        }

        Objects.requireNonNull(
                transitionSequence,
                "Transition sequence cannot be null"
        );

        if (transitionSequence.isEmpty()) {
            throw new IllegalArgumentException(
                    "Transition sequence cannot be empty"
            );
        }

        for (Integer transition : transitionSequence) {

            if (transition == null || transition < 0) {
                throw new IllegalArgumentException(
                        "Transition sequence must contain only non-negative indices"
                );
            }
        }

        // [DEFENSIVE-COPY]
        this.transitionSequence =
                List.copyOf(transitionSequence);
    }

    public String getName() {
        return name;
    }

    public List<Integer> getTransitionSequence() {
        return transitionSequence;
    }
}