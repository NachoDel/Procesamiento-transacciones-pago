package worker;

import monitor.MonitorInterface;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

/**
 * [WORKER-COORDINATOR]
 *
 * Administra el ciclo de vida de un conjunto de Workers:
 *
 * - construcción;
 * - creación de Threads;
 * - start;
 * - solicitud de parada global;
 * - condiciones de parada específicas;
 * - interrupción;
 * - join.
 *
 * [RESPONSIBILITY]
 *
 * No conoce:
 *
 * - la Red de Petri;
 * - las políticas;
 * - transiciones concretas;
 * - TARGET;
 * - reglas experimentales.
 */
public final class WorkerCoordinator {

    /**
     * [GLOBAL-STOP-SIGNAL]
     *
     * Señal compartida por todos los Workers.
     *
     * true significa que ningún Worker debe iniciar
     * un nuevo ciclo de trabajo.
     */
    private final AtomicBoolean stopRequested =
            new AtomicBoolean(false);

    /**
     * [THREADS]
     *
     * Threads reales asociados a los Workers configurados.
     */
    private final List<Thread> threads;

    /**
     * [LIFECYCLE]
     *
     * Impide iniciar dos veces el mismo conjunto de Threads.
     */
    private final AtomicBoolean started =
            new AtomicBoolean(false);

    /**
     * [DEFAULT-CONSTRUCTOR]
     *
     * Mantiene el comportamiento existente:
     * todos los Workers dependen únicamente del stop global.
     */
    public WorkerCoordinator(
            List<WorkerDefinition> definitions,
            MonitorInterface monitor
    ) {

        this(
                definitions,
                monitor,
                Map.of()
        );
    }

    /**
     * [PER-WORKER-STOP-CONDITIONS]
     *
     * Permite agregar condiciones externas específicas
     * para Workers concretos.
     *
     * El Coordinator no interpreta esas condiciones.
     *
     * Ejemplo de integración futura:
     *
     * H0 -> tracker::isAdmissionClosed
     *
     * stop efectivo =
     * stop global OR stop específico
     */
    public WorkerCoordinator(
            List<WorkerDefinition> definitions,
            MonitorInterface monitor,
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
                specificStopConditions,
                "Specific stop conditions cannot be null"
        );

        if (definitions.isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one worker definition is required"
            );
        }

        List<Worker> workers =
                WorkerFactory.createWorkers(
                        definitions,
                        monitor,
                        stopRequested::get,
                        specificStopConditions
                );

        /*
         * [THREAD-NAMING]
         *
         * El nombre lógico del Worker pasa a ser también
         * el nombre real del Thread.
         *
         * Es útil para diagnóstico y logging.
         */
        this.threads =
                workers.stream()
                        .map(worker ->
                                new Thread(
                                        worker,
                                        worker.getName()
                                )
                        )
                        .toList();
    }

    /**
     * [START]
     *
     * Inicia exactamente una vez todos los Threads.
     */
    public void startAll() {

        if (!started.compareAndSet(
                false,
                true
        )) {

            throw new IllegalStateException(
                    "Workers have already been started"
            );
        }

        for (Thread thread : threads) {

            thread.start();
        }
    }

    /**
     * [GLOBAL-GRACEFUL-STOP]
     *
     * Solicita que todos los Workers dejen de comenzar
     * nuevos ciclos.
     *
     * IMPORTANTE:
     *
     * Esto no despierta automáticamente un Worker que ya
     * se encuentre bloqueado dentro del Monitor.
     */
    public void requestStop() {

        stopRequested.set(true);
    }

    /**
     * [FORCED-CANCELLATION]
     *
     * Interrumpe todos los Threads.
     *
     * El Monitor soporta interrupción tanto durante
     * Condition.await() como durante esperas temporales.
     */
    public void interruptAll() {

        for (Thread thread : threads) {

            thread.interrupt();
        }
    }

    /**
     * [JOIN]
     *
     * Espera como máximo el tiempo total indicado para
     * que todos los Threads terminen.
     *
     * @return true si todos finalizaron dentro del timeout
     */
    public boolean awaitTermination(
            long timeout,
            TimeUnit unit
    ) throws InterruptedException {

        if (timeout < 0) {

            throw new IllegalArgumentException(
                    "Timeout cannot be negative"
            );
        }

        Objects.requireNonNull(
                unit,
                "TimeUnit cannot be null"
        );

        long timeoutNanos =
                unit.toNanos(
                        timeout
                );

        long deadline =
                System.nanoTime()
                        + timeoutNanos;

        for (Thread thread : threads) {

            while (thread.isAlive()) {

                long remainingNanos =
                        deadline
                                - System.nanoTime();

                if (remainingNanos <= 0) {

                    return false;
                }

                long remainingMillis =
                        Math.max(
                                1L,
                                TimeUnit.NANOSECONDS
                                        .toMillis(
                                                remainingNanos
                                        )
                        );

                thread.join(
                        remainingMillis
                );
            }
        }

        return true;
    }

    /**
     * [LIFECYCLE-STATE]
     *
     * Permite verificar que no quedaron Threads activos.
     */
    public boolean isTerminated() {

        return threads.stream()
                .noneMatch(
                        Thread::isAlive
                );
    }
}