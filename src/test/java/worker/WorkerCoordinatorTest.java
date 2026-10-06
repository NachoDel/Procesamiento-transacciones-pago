package worker;

import monitor.MonitorInterface;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [WORKER-COORDINATOR TESTS]
 *
 * Verifica:
 *
 * - lifecycle general;
 * - stop global;
 * - interrupción;
 * - alta contención;
 * - condiciones específicas por Worker;
 * - continuidad de otros Workers durante un stop selectivo.
 */
class WorkerCoordinatorTest {

    @Test
    void shouldStartAndStopWorkersCleanly()
            throws InterruptedException {

        CountingMonitor monitor =
                new CountingMonitor();

        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        List.of(
                                new WorkerDefinition(
                                        "H-test-0",
                                        List.of(0)
                                ),
                                new WorkerDefinition(
                                        "H-test-1",
                                        List.of(1)
                                )
                        ),
                        monitor
                );

        coordinator.startAll();

        waitUntilAtLeast(
                monitor,
                10
        );

        coordinator.requestStop();

        boolean terminated =
                coordinator.awaitTermination(
                        2,
                        TimeUnit.SECONDS
                );

        assertTrue(
                terminated,
                "Workers should terminate after stop request"
        );

        assertTrue(
                coordinator.isTerminated()
        );
    }

    @Test
    void shouldInterruptBlockedWorkers()
            throws InterruptedException {

        BlockingMonitor monitor =
                new BlockingMonitor();

        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        List.of(
                                new WorkerDefinition(
                                        "H-blocked",
                                        List.of(0)
                                )
                        ),
                        monitor
                );

        coordinator.startAll();

        assertTrue(
                monitor.awaitEntered(
                        2,
                        TimeUnit.SECONDS
                ),
                "Worker never entered the blocking monitor"
        );

        coordinator.requestStop();
        coordinator.interruptAll();

        boolean terminated =
                coordinator.awaitTermination(
                        2,
                        TimeUnit.SECONDS
                );

        assertTrue(
                terminated,
                "Interrupted worker should terminate"
        );

        assertTrue(
                coordinator.isTerminated()
        );
    }

    @Test
    void shouldRejectSecondStart()
            throws InterruptedException {

        CountingMonitor monitor =
                new CountingMonitor();

        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        List.of(
                                new WorkerDefinition(
                                        "H-test",
                                        List.of(0)
                                )
                        ),
                        monitor
                );

        coordinator.startAll();

        assertThrows(
                IllegalStateException.class,
                coordinator::startAll
        );

        coordinator.requestStop();
        coordinator.interruptAll();

        assertTrue(
                coordinator.awaitTermination(
                        2,
                        TimeUnit.SECONDS
                ),
                "Worker should terminate during test cleanup"
        );

        assertTrue(
                coordinator.isTerminated()
        );
    }

    @Test
    void shouldTerminateAllWorkersUnderHighContention()
            throws InterruptedException {

        int workerCount = 32;

        ManyBlockingMonitor monitor =
                new ManyBlockingMonitor(
                        workerCount
                );

        List<WorkerDefinition> definitions =
                IntStream.range(
                                0,
                                workerCount
                        )
                        .mapToObj(index ->
                                new WorkerDefinition(
                                        "H-stress-" + index,
                                        List.of(0)
                                )
                        )
                        .toList();

        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        definitions,
                        monitor
                );

        coordinator.startAll();

        assertTrue(
                monitor.awaitAllEntered(
                        2,
                        TimeUnit.SECONDS
                ),
                "Not every worker entered the blocking monitor"
        );

        coordinator.requestStop();
        coordinator.interruptAll();

        boolean terminated =
                coordinator.awaitTermination(
                        3,
                        TimeUnit.SECONDS
                );

        assertTrue(
                terminated,
                "All contending workers should terminate after interruption"
        );

        assertTrue(
                coordinator.isTerminated(),
                "No worker threads should remain active"
        );
    }

    /**
     * [SELECTIVE-STOP]
     *
     * Reproduce genéricamente el contrato acordado
     * para el cierre de admisión.
     *
     * H0:
     * - ejecuta transición 0;
     * - cuando alcanza TARGET, una condición externa cambia a true;
     * - no debe ejecutar una admisión TARGET + 1.
     *
     * H1:
     * - no posee condición específica;
     * - debe continuar trabajando después del cierre de H0.
     */
    @Test
    void shouldStopOnlyWorkerWhoseSpecificConditionBecomesTrue()
            throws InterruptedException {

        int target = 25;

        AtomicBoolean admissionClosed =
                new AtomicBoolean(false);

        AdmissionAwareMonitor monitor =
                new AdmissionAwareMonitor(
                        target,
                        admissionClosed
                );

        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        List.of(
                                new WorkerDefinition(
                                        "H0",
                                        List.of(0)
                                ),
                                new WorkerDefinition(
                                        "H1",
                                        List.of(1)
                                )
                        ),
                        monitor,
                        Map.of(
                                "H0",
                                admissionClosed::get
                        )
                );

        coordinator.startAll();

        /*
         * [TARGET-REACHED]
         *
         * La condición específica cambia exactamente
         * durante la llamada que alcanza TARGET.
         */
        assertTrue(
                monitor.awaitAdmissionClosed(
                        2,
                        TimeUnit.SECONDS
                ),
                "Admission condition was never closed"
        );

        int processingCountAtClosure =
                monitor.getProcessingCount();

        /*
         * [DRAIN-CONTINUITY]
         *
         * H1 debe seguir trabajando aunque H0 ya no
         * pueda comenzar nuevos ciclos.
         */
        waitUntilProcessingExceeds(
                monitor,
                processingCountAtClosure
        );

        /*
         * [EXACT-TARGET]
         *
         * La condición específica fue publicada antes
         * de que H0 pudiera comenzar otro ciclo.
         */
        assertEquals(
                target,
                monitor.getAdmissionCount(),
                "H0 must not execute admission TARGET + 1"
        );

        assertTrue(
                monitor.getProcessingCount()
                        > processingCountAtClosure,
                "H1 should continue after H0 admission closes"
        );

        /*
         * [GLOBAL-SHUTDOWN]
         *
         * Una vez finalizada la fase selectiva,
         * el stop global sigue funcionando normalmente.
         */
        coordinator.requestStop();
        coordinator.interruptAll();

        assertTrue(
                coordinator.awaitTermination(
                        2,
                        TimeUnit.SECONDS
                ),
                "Workers should terminate after global stop"
        );

        assertTrue(
                coordinator.isTerminated()
        );
    }

    /**
     * [INVALID-CONFIGURATION]
     *
     * Una condición específica no puede apuntar a
     * un Worker inexistente.
     */
    @Test
    void shouldRejectSpecificStopConditionForUnknownWorker() {

        CountingMonitor monitor =
                new CountingMonitor();

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new WorkerCoordinator(
                                List.of(
                                        new WorkerDefinition(
                                                "H0",
                                                List.of(0)
                                        )
                                ),
                                monitor,
                                Map.of(
                                        "UNKNOWN",
                                        () -> false
                                )
                        )
        );
    }

    /**
     * [TEST-SYNCHRONIZATION]
     *
     * Espera de forma acotada hasta observar
     * una cantidad mínima de llamadas.
     */
    private void waitUntilAtLeast(
            CountingMonitor monitor,
            int expectedCalls
    ) throws InterruptedException {

        long timeout =
                System.currentTimeMillis()
                        + 2000;

        while (monitor.getCallCount()
                < expectedCalls
                && System.currentTimeMillis()
                < timeout) {

            Thread.sleep(5);
        }

        assertTrue(
                monitor.getCallCount()
                        >= expectedCalls,
                "Workers did not make enough progress"
        );
    }

    /**
     * [TEST-SYNCHRONIZATION]
     *
     * Espera que el Worker que continúa activo realice
     * al menos una operación adicional después del
     * cierre del Worker selectivo.
     */
    private void waitUntilProcessingExceeds(
            AdmissionAwareMonitor monitor,
            int previousCount
    ) throws InterruptedException {

        long timeout =
                System.currentTimeMillis()
                        + 2000;

        while (monitor.getProcessingCount()
                <= previousCount
                && System.currentTimeMillis()
                < timeout) {

            Thread.sleep(5);
        }

        assertTrue(
                monitor.getProcessingCount()
                        > previousCount,
                "Non-stopped worker did not continue processing"
        );
    }

    /**
     * [TEST-DOUBLE]
     *
     * Monitor que acepta inmediatamente cualquier disparo.
     */
    private static class CountingMonitor
            implements MonitorInterface {

        private final AtomicInteger callCount =
                new AtomicInteger(0);

        @Override
        public boolean fireTransition(
                int transition
        ) {

            callCount.incrementAndGet();

            return true;
        }

        int getCallCount() {

            return callCount.get();
        }
    }

    /**
     * [TEST-DOUBLE]
     *
     * Monitor que mantiene un único Worker bloqueado
     * hasta recibir una interrupción.
     */
    private static class BlockingMonitor
            implements MonitorInterface {

        private final CountDownLatch entered =
                new CountDownLatch(1);

        private final CountDownLatch blocker =
                new CountDownLatch(1);

        @Override
        public boolean fireTransition(
                int transition
        ) {

            entered.countDown();

            try {

                blocker.await();

                return true;

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                return false;
            }
        }

        boolean awaitEntered(
                long timeout,
                TimeUnit unit
        ) throws InterruptedException {

            return entered.await(
                    timeout,
                    unit
            );
        }
    }

    /**
     * [TEST-DOUBLE / HIGH-CONTENTION]
     *
     * Permite bloquear simultáneamente una cantidad conocida
     * de Workers y comprobar después su cancelación conjunta.
     */
    private static class ManyBlockingMonitor
            implements MonitorInterface {

        private final CountDownLatch allEntered;

        private final CountDownLatch blocker =
                new CountDownLatch(1);

        ManyBlockingMonitor(
                int expectedWorkers
        ) {

            this.allEntered =
                    new CountDownLatch(
                            expectedWorkers
                    );
        }

        @Override
        public boolean fireTransition(
                int transition
        ) {

            allEntered.countDown();

            try {

                blocker.await();

                return true;

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

                return false;
            }
        }

        boolean awaitAllEntered(
                long timeout,
                TimeUnit unit
        ) throws InterruptedException {

            return allEntered.await(
                    timeout,
                    unit
            );
        }
    }

    /**
     * [TEST-DOUBLE / SELECTIVE-STOP]
     *
     * Simula el evento que posteriormente producirá
     * RunProgressTracker dentro de PostFireObserver.
     *
     * La transición 0 representa únicamente para este test
     * al Worker cuya condición debe cerrarse.
     *
     * La transición 1 representa otro Worker que debe
     * continuar activo durante el drenaje.
     */
    private static class AdmissionAwareMonitor
            implements MonitorInterface {

        private final int target;

        private final AtomicBoolean admissionClosed;

        private final AtomicInteger admissionCount =
                new AtomicInteger(0);

        private final AtomicInteger processingCount =
                new AtomicInteger(0);

        private final CountDownLatch admissionClosedLatch =
                new CountDownLatch(1);

        AdmissionAwareMonitor(
                int target,
                AtomicBoolean admissionClosed
        ) {

            this.target =
                    target;

            this.admissionClosed =
                    admissionClosed;
        }

        @Override
        public boolean fireTransition(
                int transition
        ) {

            if (transition == 0) {

                int currentAdmissions =
                        admissionCount
                                .incrementAndGet();

                if (currentAdmissions == target) {

                    /*
                     * [SYNCHRONOUS-CLOSE]
                     *
                     * Reproduce la garantía del contrato real:
                     * la condición cambia antes de que la llamada
                     * que alcanzó TARGET retorne al Worker.
                     */
                    admissionClosed.set(
                            true
                    );

                    admissionClosedLatch
                            .countDown();
                }

                return true;
            }

            if (transition == 1) {

                processingCount
                        .incrementAndGet();

                return true;
            }

            throw new IllegalArgumentException(
                    "Unexpected transition in test monitor: "
                            + transition
            );
        }

        boolean awaitAdmissionClosed(
                long timeout,
                TimeUnit unit
        ) throws InterruptedException {

            return admissionClosedLatch.await(
                    timeout,
                    unit
            );
        }

        int getAdmissionCount() {

            return admissionCount.get();
        }

        int getProcessingCount() {

            return processingCount.get();
        }
    }
}