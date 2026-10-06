package worker;

import monitor.MonitorInterface;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * [WORKER-FACTORY]
 *
 * Construye Workers a partir de definiciones externas.
 *
 * La Factory no conoce qué significa cada transición;
 * solamente transforma configuración en objetos ejecutables.
 *
 * [STOP-COMPOSITION]
 *
 * Cada Worker puede combinar:
 *
 * - una condición global de parada;
 * - una condición específica opcional.
 *
 * stop efectivo =
 * globalStop OR specificStop
 */
public final class WorkerFactory {

    private static final BooleanSupplier NEVER_STOP =
            () -> false;

    private WorkerFactory() {
    }

    /**
     * [BACKWARD-COMPATIBILITY]
     *
     * Construye todos los Workers utilizando únicamente
     * la condición global de parada.
     */
    public static List<Worker> createWorkers(
            List<WorkerDefinition> definitions,
            MonitorInterface monitor,
            BooleanSupplier globalStopRequested
    ) {

        return createWorkers(
                definitions,
                monitor,
                globalStopRequested,
                Map.of()
        );
    }

    /**
     * [PER-WORKER-STOP]
     *
     * Construye Workers permitiendo una condición adicional
     * de parada para Workers concretos.
     *
     * Un Worker sin condición específica utiliza únicamente
     * la señal global.
     *
     * @param specificStopConditions condiciones indexadas por
     *                               nombre lógico del Worker
     */
    public static List<Worker> createWorkers(
            List<WorkerDefinition> definitions,
            MonitorInterface monitor,
            BooleanSupplier globalStopRequested,
            Map<String, BooleanSupplier> specificStopConditions
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
                globalStopRequested,
                "Global stop condition cannot be null"
        );

        Objects.requireNonNull(
                specificStopConditions,
                "Specific stop conditions cannot be null"
        );

        validateSpecificStopConditions(
                definitions,
                specificStopConditions
        );

        return definitions.stream()
                .map(definition -> {

                    BooleanSupplier specificStopRequested =
                            specificStopConditions.getOrDefault(
                                    definition.getName(),
                                    NEVER_STOP
                            );

                    /*
                     * [EFFECTIVE-STOP]
                     *
                     * El Worker continúa siendo completamente
                     * agnóstico al origen de las señales.
                     *
                     * Solo recibe una BooleanSupplier final.
                     */
                    BooleanSupplier effectiveStopRequested =
                            () ->
                                    globalStopRequested
                                            .getAsBoolean()
                                    || specificStopRequested
                                            .getAsBoolean();

                    return new Worker(
                            definition.getName(),
                            monitor,
                            definition.getTransitionSequence(),
                            effectiveStopRequested
                    );
                })
                .toList();
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * Las condiciones específicas solamente pueden referirse
     * a Workers presentes en las definiciones recibidas.
     */
    private static void validateSpecificStopConditions(
            List<WorkerDefinition> definitions,
            Map<String, BooleanSupplier> specificStopConditions
    ) {

        for (Map.Entry<String, BooleanSupplier> entry
                : specificStopConditions.entrySet()) {

            String workerName =
                    Objects.requireNonNull(
                            entry.getKey(),
                            "Specific stop worker name cannot be null"
                    );

            Objects.requireNonNull(
                    entry.getValue(),
                    "Specific stop condition cannot be null"
            );

            boolean workerExists =
                    definitions.stream()
                            .anyMatch(definition ->
                                    definition.getName()
                                            .equals(workerName)
                            );

            if (!workerExists) {

                throw new IllegalArgumentException(
                        "Specific stop condition references an unknown worker: "
                                + workerName
                );
            }
        }
    }
}