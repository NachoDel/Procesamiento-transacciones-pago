package integration;

import config.PaymentConflictConfig;
import config.PaymentPetriNetConfig;
import config.PaymentTimingConfig;
import config.PaymentWorkerConfig;

import monitor.Monitor;
import monitor.MonitorInterface;

import petri.PetriNet;

import policy.RandomPolicy;

import worker.WorkerCoordinator;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [PAYMENT-SYSTEM-INTEGRATION]
 *
 * Integra:
 *
 * - Red de Petri oficial;
 * - configuración oficial de conflictos;
 * - semántica temporal;
 * - tiempos configurados;
 * - RandomPolicy;
 * - Monitor real;
 * - Workers H0, H1, H2, H3 y H4.
 *
 * [SCOPE]
 *
 * Estos tests todavía no verifican:
 *
 * - exactamente 200 T-invariantes;
 * - distribución estadística de políticas;
 * - logging experimental;
 * - duración final de 20-40 segundos.
 */
class PaymentWorkersIntegrationTest {

    /**
     * [REAL-WORKERS / SMOKE-TEST]
     *
     * Los cinco Workers oficiales deben poder operar
     * conjuntamente sobre la Red de Petri real,
     * producir progreso y finalizar limpiamente.
     */
    @Test
    void officialWorkersShouldMakeProgressAndTerminate()
            throws InterruptedException {

        // [ARRANGE]
        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        CountingMonitor monitor =
                createCountingMonitor(
                        petriNet,
                        2026L
                );

        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        PaymentWorkerConfig
                                .getDefinitions(),
                        monitor
                );

        try {

            // [ACT]
            coordinator.startAll();

            boolean progress =
                    waitForSuccessfulFirings(
                            monitor,
                            20,
                            5,
                            TimeUnit.SECONDS
                    );

            // [ASSERT - PROGRESS]
            assertTrue(
                    progress,
                    "Official workers did not make enough progress"
            );

        } finally {

            stopAndJoin(
                    coordinator
            );
        }

        // [ASSERT - NO LEFTOVER THREADS]
        assertTrue(
                coordinator.isTerminated(),
                "All worker threads must be terminated"
        );
    }

    /**
     * [MULTIPLE-RUNS]
     *
     * Verifica que el sistema pueda ejecutarse varias veces
     * consecutivas dentro de la misma JVM.
     *
     * [OBJECTIVE]
     *
     * Cada corrida construye:
     *
     * - una nueva PetriNet;
     * - un nuevo Monitor;
     * - nuevos Workers;
     * - nuevos Threads.
     *
     * De esta forma comprobamos que una ejecución anterior
     * no deja estado o Threads que afecten a la siguiente.
     */
    @Test
    void shouldSupportMultipleConsecutiveExecutions()
            throws InterruptedException {

        int runs = 3;

        for (int run = 0;
             run < runs;
             run++) {

            // [ARRANGE - FRESH RUN]
            PetriNet petriNet =
                    PaymentPetriNetConfig
                            .createPetriNet();

            CountingMonitor monitor =
                    createCountingMonitor(
                            petriNet,
                            2026L + run
                    );

            WorkerCoordinator coordinator =
                    new WorkerCoordinator(
                            PaymentWorkerConfig
                                    .getDefinitions(),
                            monitor
                    );

            try {

                // [ACT]
                coordinator.startAll();

                boolean progress =
                        waitForSuccessfulFirings(
                                monitor,
                                15,
                                4,
                                TimeUnit.SECONDS
                        );

                // [ASSERT - RUN PROGRESS]
                assertTrue(
                        progress,
                        "Run "
                                + run
                                + " did not make enough progress"
                );

            } finally {

                stopAndJoin(
                        coordinator
                );
            }

            // [ASSERT - CLEAN RUN END]
            assertTrue(
                    coordinator.isTerminated(),
                    "Run "
                            + run
                            + " left active worker threads"
            );
        }
    }

    /**
     * [REAL-MONITOR-FACTORY]
     *
     * Construye el Monitor real del sistema envuelto
     * exclusivamente con observabilidad de test.
     */
    private CountingMonitor createCountingMonitor(
            PetriNet petriNet,
            long seed
    ) {

        MonitorInterface realMonitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(seed),
                        PaymentConflictConfig
                                .getConflictGroups(),
                        PaymentPetriNetConfig
                                .createTransitionSemantics(),
                        PaymentTimingConfig
                                .createBaseline()
                );

        return new CountingMonitor(
                realMonitor
        );
    }

    /**
     * [TEST-SYNCHRONIZATION]
     *
     * Espera hasta alcanzar una cantidad mínima
     * de disparos exitosos.
     */
    private boolean waitForSuccessfulFirings(
            CountingMonitor monitor,
            int target,
            long timeout,
            TimeUnit unit
    ) throws InterruptedException {

        long deadline =
                System.nanoTime()
                        + unit.toNanos(timeout);

        while (monitor.getSuccessfulFirings()
                < target) {

            if (System.nanoTime()
                    >= deadline) {

                return false;
            }

            Thread.sleep(10);
        }

        return true;
    }

    /**
     * [TEST-CLEANUP]
     *
     * Protocolo de cleanup utilizado únicamente
     * por estos tests de integración.
     *
     * Todavía no representa el protocolo definitivo
     * de finalización de una corrida experimental.
     */
    private void stopAndJoin(
            WorkerCoordinator coordinator
    ) throws InterruptedException {

        coordinator.requestStop();
        coordinator.interruptAll();

        assertTrue(
                coordinator.awaitTermination(
                        3,
                        TimeUnit.SECONDS
                ),
                "Some worker threads remained active"
        );
    }

    /**
     * [TEST-DOUBLE / MONITOR-DECORATOR]
     *
     * Mantiene todo el comportamiento del Monitor real
     * y agrega únicamente un contador de disparos exitosos.
     */
    private static class CountingMonitor
            implements MonitorInterface {

        private final MonitorInterface delegate;

        private final AtomicInteger successfulFirings =
                new AtomicInteger(0);

        CountingMonitor(
                MonitorInterface delegate
        ) {

            this.delegate =
                    delegate;
        }

        @Override
        public boolean fireTransition(
                int transition
        ) {

            boolean fired =
                    delegate.fireTransition(
                            transition
                    );

            if (fired) {

                successfulFirings.incrementAndGet();
            }

            return fired;
        }

        int getSuccessfulFirings() {

            return successfulFirings.get();
        }
    }
}