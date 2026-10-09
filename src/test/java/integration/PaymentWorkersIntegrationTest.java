package integration;

import config.PaymentConflictConfig;
import config.PaymentPetriNetConfig;
import config.PaymentTimingConfig;
import config.PaymentWorkerConfig;

import monitor.Monitor;
import monitor.MonitorInterface;
import monitor.PostFireObserver;

import petri.PetriNet;

import policy.RandomPolicy;

import worker.WorkerCoordinator;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntPredicate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * - Workers H0, H1, H2, H3 y H4;
 * - shutdown selectivo de H0;
 * - drenaje de operaciones admitidas.
 *
 * [SCOPE]
 *
 * Estos tests todavía no representan la campaña experimental
 * definitiva de 200 T-invariantes.
 *
 * Utilizan targets reducidos para mantener rápida la suite.
 */
class PaymentWorkersIntegrationTest {

    /**
     * [OFFICIAL-INITIAL-MARKING]
     *
     * M0 = (3,0,0,0,0,0,0,1,1,0)
     */
    private static final int[] INITIAL_MARKING = {
            3, 0, 0, 0, 0,
            0, 0, 1, 1, 0
    };

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
     * Cada corrida construye una nueva PetriNet,
     * Monitor, Workers y Threads.
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
     * [DRAIN-SHUTDOWN-INTEGRATION]
     *
     * Valida sobre la Red de Petri y Workers oficiales
     * el contrato acordado para una corrida:
     *
     * H0 admite exactamente TARGET transacciones
     *          ↓
     * se cierra únicamente H0
     *          ↓
     * H1-H4 continúan
     *          ↓
     * T9 alcanza TARGET
     *          ↓
     * la red vuelve a M0
     *          ↓
     * shutdown global
     *
     * [IMPORTANT]
     *
     * DrainProgressObserver es únicamente un test-double.
     *
     * No reemplaza al RunProgressTracker productivo
     * perteneciente al runner experimental.
     */
    @Test
    void shouldDrainExactlyTargetAdmissionsAndReturnToInitialMarking()
            throws InterruptedException {

        // [ARRANGE]
        int target = 12;

        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        DrainProgressObserver progress =
                new DrainProgressObserver(
                        target
                );

        MonitorInterface monitor =
                createRealMonitor(
                        petriNet,
                        2026L,
                        progress,
                        progress::isTransitionAvailable
                );

        /*
         * [SELECTIVE-STOP]
         *
         * Solamente H0 recibe la condición adicional
         * de cierre de admisión.
         *
         * H1-H4 dependen únicamente del stop global
         * administrado por WorkerCoordinator.
         */
        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        PaymentWorkerConfig
                                .getDefinitions(),
                        monitor,
                        Map.of(
                                "H0",
                                progress::isAdmissionClosed
                        )
                );

        try {

            // [ACT]
            coordinator.startAll();

            /*
             * [DRAIN]
             *
             * Esperamos hasta que T9 haya finalizado
             * todas las transacciones admitidas.
             */
            boolean drained =
                    progress.awaitTargetCompleted(
                            10,
                            TimeUnit.SECONDS
                    );

            assertTrue(
                    drained,
                    "The payment system did not drain within the expected time"
            );

            // [ASSERT - EXACT ADMISSIONS]
            assertTrue(
                    progress.isAdmissionClosed(),
                    "Admission must be closed after reaching the target"
            );

            assertEquals(
                    target,
                    progress.getAdmissions(),
                    "H0 must admit exactly TARGET transactions"
            );

            /*
             * [ASSERT - EXACT COMPLETIONS]
             *
             * Cada disparo exitoso de T9 representa
             * una transacción finalizada.
             */
            assertEquals(
                    target,
                    progress.getCompletions(),
                    "All admitted transactions must reach T9"
            );

            /*
             * [ASSERT - DRAINED MARKING]
             *
             * Una vez que todas las transacciones admitidas
             * finalizaron, la red debe haber regresado a M0.
             */
            assertArrayEquals(
                    INITIAL_MARKING,
                    petriNet.getCurrentMarking(),
                    "PetriNet must return to the official initial marking after drain"
            );

        } finally {

            /*
             * [GLOBAL-SHUTDOWN]
             *
             * Después del drenaje se solicita la parada
             * global y se despiertan posibles Workers bloqueados.
             */
            stopAndJoin(
                    coordinator
            );
        }

        // [ASSERT - NO LEFTOVER THREADS]
        assertTrue(
                coordinator.isTerminated(),
                "All workers must terminate after the drained run"
        );
    }

    /**
     * [DEFAULT-AVAILABILITY]
     *
     * En las ejecuciones normales todas las transiciones
     * permanecen disponibles.
     */
    private MonitorInterface createRealMonitor(
            PetriNet petriNet,
            long seed,
            PostFireObserver postFireObserver
    ) {

        return createRealMonitor(
                petriNet,
                seed,
                postFireObserver,
                transition -> true
        );
    }

    /**
     * [DYNAMIC-AVAILABILITY]
     *
     * Permite que un escenario de ejecución controle
     * qué transiciones continúan participando.
     */
    private MonitorInterface createRealMonitor(
            PetriNet petriNet,
            long seed,
            PostFireObserver postFireObserver,
            IntPredicate transitionAvailability
    ) {

        return new Monitor(
                petriNet,
                new RandomPolicy(seed),
                PaymentConflictConfig
                        .getConflictGroups(),
                PaymentPetriNetConfig
                        .createTransitionSemantics(),
                PaymentTimingConfig
                        .createBaseline(),
                postFireObserver,
                transitionAvailability
        );
    }

    /**
     * [REAL-MONITOR / COUNTING-DECORATOR]
     *
     * Construye el Monitor real del sistema envuelto
     * exclusivamente con observabilidad de test.
     */
    private CountingMonitor createCountingMonitor(
            PetriNet petriNet,
            long seed
    ) {

        MonitorInterface realMonitor =
                createRealMonitor(
                        petriNet,
                        seed,
                        PostFireObserver.noop()
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
     * Solicita stop global, despierta Workers bloqueados
     * y espera que todos los Threads finalicen.
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

    /**
     * [TEST-DOUBLE / DRAIN-PROGRESS]
     *
     * Simula exclusivamente dentro del test la parte mínima
     * del contrato que posteriormente implementará
     * RunProgressTracker.
     *
     * Observa:
     *
     * T0 -> admisión
     * T9 -> finalización
     *
     * [THREAD-SAFETY]
     *
     * Utiliza AtomicInteger, AtomicBoolean y CountDownLatch
     * porque sus valores son consultados desde threads
     * diferentes a los que ejecutan los disparos.
     */
    private static class DrainProgressObserver
            implements PostFireObserver {

        private static final int ADMISSION_TRANSITION = 0;

        private static final int COMPLETION_TRANSITION = 9;

        private final int target;

        private final AtomicInteger admissions =
                new AtomicInteger(0);

        private final AtomicInteger completions =
                new AtomicInteger(0);

        private final AtomicBoolean admissionClosed =
                new AtomicBoolean(false);

        private final CountDownLatch targetCompleted =
                new CountDownLatch(1);

        DrainProgressObserver(
                int target
        ) {

            if (target <= 0) {

                throw new IllegalArgumentException(
                        "Target must be positive"
                );
            }

            this.target =
                    target;
        }

        /**
         * [SYNCHRONOUS-PROGRESS]
         *
         * Monitor invoca este método después del commit
         * y antes de retornar fireTransition().
         *
         * Esto permite cerrar H0 exactamente cuando
         * ocurre T0 #TARGET.
         */
        @Override
        public void onSuccessfulFire(
                int transition,
                int[] marking
        ) {

            if (transition
                    == ADMISSION_TRANSITION) {

                int currentAdmissions =
                        admissions.incrementAndGet();

                if (currentAdmissions == target) {

                    admissionClosed.set(
                            true
                    );
                }

                return;
            }

            if (transition
                    == COMPLETION_TRANSITION) {

                int currentCompletions =
                        completions.incrementAndGet();

                if (currentCompletions == target) {

                    targetCompleted.countDown();
                }
            }
        }

        boolean isAdmissionClosed() {

            return admissionClosed.get();
        }

        int getAdmissions() {

            return admissions.get();
        }

        int getCompletions() {

            return completions.get();
        }

        boolean awaitTargetCompleted(
                long timeout,
                TimeUnit unit
        ) throws InterruptedException {

            return targetCompleted.await(
                    timeout,
                    unit
            );
        }
        /**
         * [EXECUTION-AVAILABILITY]
         *
         * La transición de admisión participa hasta alcanzar TARGET.
         *
         * Las restantes transiciones permanecen siempre disponibles
         * para permitir el drenaje de operaciones en vuelo.
         */
        boolean isTransitionAvailable(
                int transition
        ) {

            return transition
                    != ADMISSION_TRANSITION
                    || !admissionClosed.get();
        }
    }
}