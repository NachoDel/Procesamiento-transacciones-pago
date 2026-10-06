package worker;

import monitor.MonitorInterface;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * [WORKER-FACTORY]
 *
 * Construye Workers a partir de definiciones externas.
 *
 * La Factory no conoce qué significa cada transición;
 * solamente transforma configuración en objetos ejecutables.
 */
public final class WorkerFactory {

    private WorkerFactory() {
    }

    public static List<Worker> createWorkers(
            List<WorkerDefinition> definitions,
            MonitorInterface monitor,
            BooleanSupplier stopRequested
    ) {

        Objects.requireNonNull(
                definitions,
                "Worker definitions cannot be null"
        );

        Objects.requireNonNull(
                monitor,
                "Monitor cannot be null"
        );

        Objects.requireNonNull(
                stopRequested,
                "Stop condition cannot be null"
        );

        return definitions.stream()
                .map(definition ->
                        new Worker(
                                definition.getName(),
                                monitor,
                                definition.getTransitionSequence(),
                                stopRequested
                        )
                )
                .toList();
    }
}