package worker;

import monitor.MonitorInterface;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [WORKER-COORDINATOR TESTS]
 *
 * Verifica el lifecycle de los Threads independientemente
 * de la Red de Petri real.
 */
class WorkerCoordinatorTest {

    /**
     * [START-AND-STOP]
     *
     * Todos los Workers deben iniciar y poder finalizar
     * después de una solicitud normal de parada.
     */
    @Test
    void shouldStartAndStopWorkersCleanly()
            throws InterruptedException {

        // [ARRANGE]
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

        // [ACT]
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

        // [ASSERT]
        assertTrue(
                terminated,
                "Workers should terminate after stop request"
        );

        assertTrue(
                coordinator.isTerminated()
        );
    }

    /**
     * [BLOCKED-CANCELLATION]
     *
     * Un Worker bloqueado dentro del Monitor debe poder
     * finalizar mediante interrupción.
     */
    @Test
    void shouldInterruptBlockedWorkers()
            throws InterruptedException {

        // [ARRANGE]
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

        // [ACT]
        coordinator.requestStop();
        coordinator.interruptAll();

        boolean terminated =
                coordinator.awaitTermination(
                        2,
                        TimeUnit.SECONDS
                );

        // [ASSERT]
        assertTrue(
                terminated,
                "Interrupted worker should terminate"
        );

        assertTrue(
                coordinator.isTerminated()
        );
    }

    /**
     * [LIFECYCLE-GUARD]
     *
     * Un conjunto de Threads no puede iniciarse dos veces.
     */
    @Test
    void shouldRejectSecondStart()
            throws InterruptedException {

        // [ARRANGE]
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

        // [ASSERT]
        assertThrows(
                IllegalStateException.class,
                coordinator::startAll
        );

        // [TEST-CLEANUP]
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

    /**
     * [HIGH-CONTENTION]
     *
     * Verifica que el Coordinator pueda cancelar y hacer join
     * correctamente de una cantidad elevada de Workers
     * bloqueados simultáneamente.
     *
     * [OBJECTIVE]
     * El test no evalúa exclusión mutua del Monitor real
     * —eso ya está cubierto por MonitorTest—.
     *
     * Evalúa robustez del lifecycle bajo alta contención.
     */
    @Test
    void shouldTerminateAllWorkersUnderHighContention()
            throws InterruptedException {

        // [ARRANGE]
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

        // [ACT - START]
        coordinator.startAll();

        /*
         * Confirmamos que los 32 Workers llegaron
         * efectivamente al punto bloqueante.
         */
        assertTrue(
                monitor.awaitAllEntered(
                        2,
                        TimeUnit.SECONDS
                ),
                "Not every worker entered the blocking monitor"
        );

        /*
         * [SHUTDOWN]
         *
         * Todos los Workers están simultáneamente bloqueados.
         */
        coordinator.requestStop();
        coordinator.interruptAll();

        boolean terminated =
                coordinator.awaitTermination(
                        3,
                        TimeUnit.SECONDS
                );

        // [ASSERT]
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
}