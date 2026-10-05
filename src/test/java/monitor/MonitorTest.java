package monitor;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import petri.PetriNet;
import policy.ConflictGroup;
import policy.Policy;
import policy.PriorityPolicy;
import policy.RandomPolicy;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [MONITOR TESTS]
 *
 * Valida exclusión mutua, disparo atómico, espera/reactivación
 * condicional e integración del Monitor con las políticas
 * de resolución de conflictos.
 */
class MonitorTest {

    /**
     * [BASIC-FIRING]
     *
     * Verifica que el Monitor delega correctamente un disparo
     * habilitado hacia la Red de Petri.
     */
    @Test
    void shouldFireEnabledTransitionThroughMonitor() {

        // [ARRANGE]
        int[][] incidenceMatrix = {
                {-1},
                { 1}
        };

        int[] initialMarking = {1, 0};

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        initialMarking
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of()
                );

        // [ACT]
        boolean fired =
                monitor.fireTransition(0);

        // [ASSERT]
        assertTrue(fired);

        assertArrayEquals(
                new int[]{0, 1},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [MUTUAL-EXCLUSION]
     *
     * Verifica que el Monitor impide que dos threads ejecuten
     * simultáneamente PetriNet.fire().
     */
    @Test
    void shouldSerializeConcurrentAccessToPetriNet()
            throws InterruptedException {

        // [ARRANGE - CONCURRENCY PROBE]
        ConcurrencyProbePetriNet petriNet =
                new ConcurrencyProbePetriNet();

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of()
                );

        int threadCount = 10;

        /*
         * [START-SYNCHRONIZATION]
         *
         * readyLatch permite saber que todos los threads están preparados.
         * startLatch hace que todos intenten entrar al Monitor casi juntos.
         * finishedLatch permite esperar su finalización de forma acotada.
         */
        CountDownLatch readyLatch =
                new CountDownLatch(threadCount);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishedLatch =
                new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {

            Thread thread = new Thread(() -> {

                readyLatch.countDown();

                try {

                    startLatch.await();
                    monitor.fireTransition(0);

                } catch (InterruptedException exception) {

                    Thread.currentThread().interrupt();

                } finally {

                    finishedLatch.countDown();
                }
            });

            thread.start();
        }

        // [ACT]
        readyLatch.await();
        startLatch.countDown();

        assertTrue(
                finishedLatch.await(5, TimeUnit.SECONDS),
                "Threads did not finish within the expected time"
        );

        // [ASSERT - MUTUAL EXCLUSION]
        assertFalse(
                petriNet.wasConcurrentAccessDetected(),
                "Two threads entered PetriNet.fire() simultaneously"
        );
    }

    /**
     * [CONDITIONAL-WAIT]
     *
     * Verifica que un thread se bloquee cuando su transición
     * no está habilitada y continúe cuando otro disparo la habilite.
     */
    @Test
    void shouldBlockAndResumeWhenTransitionBecomesEnabled()
            throws InterruptedException {

        /*
         * [ARRANGE]
         *
         *       T0        T1
         * P0 ------> P1 ------> P0
         *
         * M0 = [1, 0]
         *
         * Inicialmente:
         * - T0 está habilitada.
         * - T1 está deshabilitada.
         */
        int[][] incidenceMatrix = {
                {-1,  1},
                { 1, -1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0}
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of()
                );

        AtomicBoolean result =
                new AtomicBoolean(false);

        /*
         * [WAITING-THREAD]
         *
         * Este thread solicita T1, que inicialmente
         * no puede dispararse.
         */
        Thread waitingThread =
                new Thread(() ->
                        result.set(
                                monitor.fireTransition(1)
                        )
                );

        waitingThread.start();

        waitUntilThreadIsWaiting(waitingThread);

        // [ASSERT - BLOCKED]
        assertTrue(waitingThread.isAlive());

        /*
         * [ACT]
         *
         * T0 mueve el token desde P0 hacia P1.
         * El nuevo marcado sensibiliza T1.
         */
        assertTrue(
                monitor.fireTransition(0)
        );

        waitingThread.join(2000);

        // [ASSERT - RESUMED]
        assertFalse(
                waitingThread.isAlive(),
                "Waiting thread should have resumed"
        );

        assertTrue(result.get());

        /*
         * T1 también terminó disparándose:
         *
         * P0 -> P1 -> P0
         */
        assertArrayEquals(
                new int[]{1, 0},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [INTERRUPTION]
     *
     * Verifica que un thread suspendido pueda abandonar
     * fireTransition() si recibe una interrupción.
     */
    @Test
    void shouldStopWaitingWhenThreadIsInterrupted()
            throws InterruptedException {

        // [ARRANGE]
        int[][] incidenceMatrix = {
                {-1,  1},
                { 1, -1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0}
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of()
                );

        AtomicBoolean result =
                new AtomicBoolean(true);

        AtomicBoolean interruptedStatus =
                new AtomicBoolean(false);

        Thread waitingThread =
                new Thread(() -> {

                    result.set(
                            monitor.fireTransition(1)
                    );

                    interruptedStatus.set(
                            Thread.currentThread().isInterrupted()
                    );
                });

        waitingThread.start();

        waitUntilThreadIsWaiting(waitingThread);

        // [ACT]
        waitingThread.interrupt();

        waitingThread.join(2000);

        // [ASSERT - THREAD TERMINATION]
        assertFalse(
                waitingThread.isAlive(),
                "Interrupted thread should have finished"
        );

        // [ASSERT - FIRE RESULT]
        assertFalse(
                result.get(),
                "Interrupted firing request should return false"
        );

        // [ASSERT - INTERRUPTED STATUS]
        assertTrue(
                interruptedStatus.get(),
                "Interrupted status should be preserved"
        );

        /*
         * [STATE-INTEGRITY]
         *
         * T1 nunca llegó a dispararse.
         */
        assertArrayEquals(
                new int[]{1, 0},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [POLICY-INTEGRATION]
     *
     * Verifica que, cuando varias transiciones pertenecientes
     * al mismo conflicto quedan habilitadas simultáneamente,
     * sea Policy quien determine cuál debe reactivarse.
     */
    @Test
    void shouldUsePolicyToSelectWhichWaitingTransitionResumes()
            throws InterruptedException {

        /*
         * [ARRANGE]
         *
         *                  T1 -> P2
         *                 /
         * P0 --T0--> P1
         *                 \
         *                  T2 -> P3
         *
         * M0 = [1, 0, 0, 0]
         *
         * Después de T0:
         * - T1 queda habilitada.
         * - T2 queda habilitada.
         *
         * Ambas compiten por el único token de P1.
         */
        int[][] incidenceMatrix = {
                {-1,  0,  0},
                { 1, -1, -1},
                { 0,  1,  0},
                { 0,  0,  1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0, 0, 0}
                );

        /*
         * [CONTROLLED-POLICY]
         *
         * Se fuerza la elección de T2 para comprobar
         * que el Monitor respeta la decisión de Policy.
         */
        FixedTransitionPolicy policy =
                new FixedTransitionPolicy(2);

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        policy,
                        List.of(
                                new ConflictGroup(
                                        Set.of(1, 2)
                                )
                        )
                );

        AtomicBoolean transition1Result =
                new AtomicBoolean(false);

        AtomicBoolean transition2Result =
                new AtomicBoolean(false);

        Thread transition1Thread =
                new Thread(() ->
                        transition1Result.set(
                                monitor.fireTransition(1)
                        )
                );

        Thread transition2Thread =
                new Thread(() ->
                        transition2Result.set(
                                monitor.fireTransition(2)
                        )
                );

        transition1Thread.start();
        transition2Thread.start();

        waitUntilThreadIsWaiting(transition1Thread);
        waitUntilThreadIsWaiting(transition2Thread);

        // [ACT]
        assertTrue(
                monitor.fireTransition(0)
        );

        /*
         * La Policy eligió T2.
         */
        transition2Thread.join(2000);

        // [ASSERT - POLICY DECISION]
        assertFalse(
                transition2Thread.isAlive(),
                "Transition selected by the policy should have resumed"
        );

        assertTrue(
                transition2Result.get(),
                "Selected transition should have fired successfully"
        );

        /*
         * T2 consumió el único token de P1.
         * T1 debe continuar esperando.
         */
        assertTrue(
                transition1Thread.isAlive(),
                "Non-selected transition should still be waiting"
        );

        // [ASSERT - POLICY CANDIDATES]
        assertEquals(
                List.of(1, 2),
                policy.getLastCandidates()
        );

        // [ASSERT - MARKING]
        assertArrayEquals(
                new int[]{0, 0, 0, 1},
                petriNet.getCurrentMarking()
        );

        /*
         * [TEST-CLEANUP]
         *
         * T1 quedó legítimamente bloqueada.
         * Se interrumpe para no dejar threads activos.
         */
        transition1Thread.interrupt();
        transition1Thread.join(2000);

        assertFalse(
                transition1Thread.isAlive(),
                "Waiting thread should terminate after interruption"
        );

        assertFalse(
                transition1Result.get(),
                "Interrupted firing request should return false"
        );
    }

    /**
     * [PRIORITY-POLICY-INTEGRATION]
     *
     * Verifica que el Monitor respete una PriorityPolicy real
     * cuando varias transiciones del mismo conflicto quedan
     * habilitadas simultáneamente.
     */
    @Test
    void shouldReactivatePriorityTransitionWhenConflictOccurs()
            throws InterruptedException {

        /*
         * [ARRANGE]
         *
         *                  T1 -> P2
         *                 /
         * P0 --T0--> P1
         *                 \
         *                  T2 -> P3
         *
         * T1 y T2 pertenecen al mismo conflicto.
         * T2 se configura como transición prioritaria.
         */
        int[][] incidenceMatrix = {
                {-1,  0,  0},
                { 1, -1, -1},
                { 0,  1,  0},
                { 0,  0,  1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0, 0, 0}
                );

        Policy policy =
                new PriorityPolicy(
                        2,
                        new RandomPolicy(2026L)
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        policy,
                        List.of(
                                new ConflictGroup(
                                        Set.of(1, 2)
                                )
                        )
                );

        AtomicBoolean transition1Result =
                new AtomicBoolean(false);

        AtomicBoolean transition2Result =
                new AtomicBoolean(false);

        Thread transition1Thread =
                new Thread(() ->
                        transition1Result.set(
                                monitor.fireTransition(1)
                        )
                );

        Thread transition2Thread =
                new Thread(() ->
                        transition2Result.set(
                                monitor.fireTransition(2)
                        )
                );

        transition1Thread.start();
        transition2Thread.start();

        waitUntilThreadIsWaiting(transition1Thread);
        waitUntilThreadIsWaiting(transition2Thread);

        // [ACT]
        assertTrue(
                monitor.fireTransition(0)
        );

        transition2Thread.join(2000);

        // [ASSERT - PRIORITY]
        assertFalse(
                transition2Thread.isAlive(),
                "Priority transition should have resumed"
        );

        assertTrue(
                transition2Result.get(),
                "Priority transition should have fired successfully"
        );

        /*
         * La transición no prioritaria perdió el conflicto
         * y debe seguir esperando.
         */
        assertTrue(
                transition1Thread.isAlive(),
                "Non-priority transition should remain waiting"
        );

        // [ASSERT - MARKING]
        assertArrayEquals(
                new int[]{0, 0, 0, 1},
                petriNet.getCurrentMarking()
        );

        // [TEST-CLEANUP]
        transition1Thread.interrupt();
        transition1Thread.join(2000);

        assertFalse(
                transition1Thread.isAlive(),
                "Waiting thread should terminate after interruption"
        );

        assertFalse(
                transition1Result.get(),
                "Interrupted firing request should return false"
        );
    }

    /**
     * [POLICY-SCOPE]
     *
     * Verifica que Policy no sea utilizada como scheduler global.
     *
     * Transiciones independientes que pueden continuar
     * simultáneamente deben reactivarse sin competir mediante Policy.
     */
    @Test
    void shouldNotUsePolicyForIndependentTransitions()
            throws InterruptedException {

        /*
         * [ARRANGE]
         *
         * T0 produce un token en P1 y otro en P2.
         *
         * P1 --T1-->
         * P2 --T2-->
         *
         * T1 y T2 son independientes:
         * no pertenecen a ningún ConflictGroup.
         */
        int[][] incidenceMatrix = {
                {-1,  0,  0},
                { 1, -1,  0},
                { 1,  0, -1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0, 0}
                );

        RecordingPolicy policy =
                new RecordingPolicy();

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        policy,
                        List.of()
                );

        Thread firstThread =
                new Thread(() ->
                        monitor.fireTransition(1)
                );

        Thread secondThread =
                new Thread(() ->
                        monitor.fireTransition(2)
                );

        firstThread.start();
        secondThread.start();

        waitUntilThreadIsWaiting(firstThread);
        waitUntilThreadIsWaiting(secondThread);

        // [ACT]
        assertTrue(
                monitor.fireTransition(0)
        );

        firstThread.join(2000);
        secondThread.join(2000);

        // [ASSERT - THREADS]
        assertFalse(
                firstThread.isAlive(),
                "First independent transition should have resumed"
        );

        assertFalse(
                secondThread.isAlive(),
                "Second independent transition should have resumed"
        );

        /*
         * [ASSERT - POLICY]
         *
         * Al no existir conflicto estructural,
         * Policy nunca debe ser consultada.
         */
        assertEquals(
                0,
                policy.getInvocationCount()
        );

        // [ASSERT - MARKING]
        assertArrayEquals(
                new int[]{0, 0, 0},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [TEST-SYNCHRONIZATION]
     *
     * Espera de forma acotada hasta que el thread entre
     * en estado WAITING.
     *
     * Se utiliza exclusivamente para coordinar tests.
     */
    private void waitUntilThreadIsWaiting(Thread thread)
            throws InterruptedException {

        long timeout =
                System.currentTimeMillis() + 2000;

        while (thread.getState() != Thread.State.WAITING
                && System.currentTimeMillis() < timeout) {

            Thread.sleep(5);
        }

        assertEquals(
                Thread.State.WAITING,
                thread.getState(),
                "Thread did not enter WAITING state"
        );
    }

    /**
     * [TEST-DOUBLE / CONCURRENCY-PROBE]
     *
     * PetriNet artificial utilizada exclusivamente para detectar
     * accesos simultáneos a fire().
     */
    private static class ConcurrencyProbePetriNet extends PetriNet {

        /**
         * [CONCURRENCY-MEASUREMENT]
         *
         * Cantidad de threads ejecutando fire() actualmente.
         */
        private final AtomicInteger activeCalls =
                new AtomicInteger(0);

        /**
         * [CONCURRENCY-MEASUREMENT]
         *
         * Se activa si alguna vez existen dos o más threads
         * ejecutando fire() simultáneamente.
         */
        private final AtomicBoolean concurrentAccessDetected =
                new AtomicBoolean(false);

        ConcurrencyProbePetriNet() {
            super(
                    new int[][]{{0}},
                    new int[]{0}
            );
        }

        @Override
        public boolean fire(int transition) {

            int concurrentCalls =
                    activeCalls.incrementAndGet();

            if (concurrentCalls > 1) {
                concurrentAccessDetected.set(true);
            }

            try {

                /*
                 * [FORCED-CONTENTION]
                 *
                 * Amplía deliberadamente la ventana de ejecución
                 * para detectar accesos concurrentes.
                 */
                Thread.sleep(20);

            } catch (InterruptedException exception) {

                Thread.currentThread().interrupt();

            } finally {

                activeCalls.decrementAndGet();
            }

            return true;
        }

        boolean wasConcurrentAccessDetected() {
            return concurrentAccessDetected.get();
        }
    }

    /**
     * [TEST-DOUBLE / FIXED-POLICY]
     *
     * Policy determinista utilizada para controlar exactamente
     * qué transición debe ganar un conflicto.
     */
    private static class FixedTransitionPolicy implements Policy {

        private final int selectedTransition;

        private List<Integer> lastCandidates =
                List.of();

        FixedTransitionPolicy(int selectedTransition) {
            this.selectedTransition =
                    selectedTransition;
        }

        @Override
        public OptionalInt select(
                List<Integer> candidates
        ) {

            /*
             * [CANDIDATE-RECORDING]
             *
             * Guardamos una copia para verificar qué alternativas
             * entregó realmente el Monitor.
             */
            lastCandidates =
                    List.copyOf(candidates);

            if (!candidates.contains(selectedTransition)) {
                return OptionalInt.empty();
            }

            return OptionalInt.of(
                    selectedTransition
            );
        }

        List<Integer> getLastCandidates() {
            return lastCandidates;
        }
    }

    /**
     * [TEST-DOUBLE / RECORDING-POLICY]
     *
     * Permite verificar cuántas veces el Monitor consulta
     * realmente a Policy.
     */
    private static class RecordingPolicy implements Policy {

        private final AtomicInteger invocationCount =
                new AtomicInteger(0);

        @Override
        public OptionalInt select(
                List<Integer> candidates
        ) {

            invocationCount.incrementAndGet();

            if (candidates.isEmpty()) {
                return OptionalInt.empty();
            }

            return OptionalInt.of(
                    candidates.get(0)
            );
        }

        int getInvocationCount() {
            return invocationCount.get();
        }
    }
}