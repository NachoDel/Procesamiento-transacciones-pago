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
import timing.TransitionSemantics;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [MONITOR TESTS]
 *
 * Valida:
 *
 * - disparo mediante Monitor;
 * - exclusión mutua;
 * - espera condicional;
 * - interrupción;
 * - alcance de Policy;
 * - PriorityPolicy;
 * - prioridad de inmediatas frente a temporales.
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

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0}
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        )
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
     * Verifica que dos threads no puedan ejecutar
     * PetriNet.fire() simultáneamente.
     */
    @Test
    void shouldSerializeConcurrentAccessToPetriNet()
            throws InterruptedException {

        // [ARRANGE]
        ConcurrencyProbePetriNet petriNet =
                new ConcurrencyProbePetriNet();

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        )
                );

        int threadCount = 10;

        CountDownLatch readyLatch =
                new CountDownLatch(threadCount);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishedLatch =
                new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {

            Thread thread =
                    new Thread(() -> {

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
                finishedLatch.await(
                        5,
                        TimeUnit.SECONDS
                ),
                "Threads did not finish within the expected time"
        );

        // [ASSERT]
        assertFalse(
                petriNet.wasConcurrentAccessDetected(),
                "Two threads entered PetriNet.fire() simultaneously"
        );
    }

    /**
     * [CONDITIONAL-WAIT]
     *
     * Verifica que un thread espere cuando su transición
     * no está sensibilizada y continúe cuando otro disparo
     * la habilita.
     */
    @Test
    void shouldBlockAndResumeWhenTransitionBecomesEnabled()
            throws InterruptedException {

        /*
         *       T0        T1
         * P0 ------> P1 ------> P0
         *
         * M0 = [1, 0]
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
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        )
                );

        AtomicBoolean result =
                new AtomicBoolean(false);

        Thread waitingThread =
                new Thread(() ->
                        result.set(
                                monitor.fireTransition(1)
                        )
                );

        waitingThread.start();

        waitUntilThreadIsWaiting(
                waitingThread
        );

        // [ASSERT - BLOCKED]
        assertTrue(
                waitingThread.isAlive()
        );

        // [ACT]
        assertTrue(
                monitor.fireTransition(0)
        );

        waitingThread.join(2000);

        // [ASSERT]
        assertFalse(
                waitingThread.isAlive(),
                "Waiting thread should have resumed"
        );

        assertTrue(
                result.get()
        );

        assertArrayEquals(
                new int[]{1, 0},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [INTERRUPTION]
     *
     * Verifica que un thread suspendido pueda abandonar
     * fireTransition() al recibir una interrupción.
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
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        )
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

        waitUntilThreadIsWaiting(
                waitingThread
        );

        // [ACT]
        waitingThread.interrupt();

        waitingThread.join(2000);

        // [ASSERT]
        assertFalse(
                waitingThread.isAlive(),
                "Interrupted thread should have finished"
        );

        assertFalse(
                result.get(),
                "Interrupted firing request should return false"
        );

        assertTrue(
                interruptedStatus.get(),
                "Interrupted status should be preserved"
        );

        assertArrayEquals(
                new int[]{1, 0},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [POLICY-INTEGRATION]
     *
     * Verifica que Policy determine cuál transición debe
     * reactivarse cuando existen múltiples alternativas
     * dentro del mismo conflicto estructural.
     */
    @Test
    void shouldUsePolicyToSelectWhichWaitingTransitionResumes()
            throws InterruptedException {

        /*
         *                  T1 -> P2
         *                 /
         * P0 --T0--> P1
         *                 \
         *                  T2 -> P3
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
                        ),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
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

        waitUntilThreadIsWaiting(
                transition1Thread
        );

        waitUntilThreadIsWaiting(
                transition2Thread
        );

        // [ACT]
        assertTrue(
                monitor.fireTransition(0)
        );

        transition2Thread.join(2000);

        // [ASSERT - SELECTED]
        assertFalse(
                transition2Thread.isAlive(),
                "Transition selected by Policy should have resumed"
        );

        assertTrue(
                transition2Result.get()
        );

        /*
         * T2 consumió el único token de P1.
         * T1 pierde el conflicto y sigue esperando.
         */
        assertTrue(
                transition1Thread.isAlive(),
                "Non-selected transition should still be waiting"
        );

        assertEquals(
                List.of(1, 2),
                policy.getLastCandidates()
        );

        assertArrayEquals(
                new int[]{0, 0, 0, 1},
                petriNet.getCurrentMarking()
        );

        // [TEST-CLEANUP]
        transition1Thread.interrupt();
        transition1Thread.join(2000);

        assertFalse(
                transition1Thread.isAlive()
        );

        assertFalse(
                transition1Result.get()
        );
    }

    /**
     * [PRIORITY-POLICY-INTEGRATION]
     *
     * Verifica que PriorityPolicy sea respetada cuando
     * existe un conflicto real.
     */
    @Test
    void shouldReactivatePriorityTransitionWhenConflictOccurs()
            throws InterruptedException {

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
                        ),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
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

        waitUntilThreadIsWaiting(
                transition1Thread
        );

        waitUntilThreadIsWaiting(
                transition2Thread
        );

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
                transition2Result.get()
        );

        assertTrue(
                transition1Thread.isAlive(),
                "Non-priority transition should remain waiting"
        );

        assertArrayEquals(
                new int[]{0, 0, 0, 1},
                petriNet.getCurrentMarking()
        );

        // [TEST-CLEANUP]
        transition1Thread.interrupt();
        transition1Thread.join(2000);

        assertFalse(
                transition1Thread.isAlive()
        );

        assertFalse(
                transition1Result.get()
        );
    }

    /**
     * [POLICY-SCOPE]
     *
     * Verifica que Policy NO sea utilizada como scheduler global.
     *
     * Transiciones independientes pueden continuar sin competir
     * mediante Policy.
     */
    @Test
    void shouldNotUsePolicyForIndependentTransitions()
            throws InterruptedException {

        /*
         * T0 produce un token tanto en P1 como en P2.
         *
         * P1 --T1-->
         * P2 --T2-->
         *
         * T1 y T2 son independientes.
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
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        )
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

        waitUntilThreadIsWaiting(
                firstThread
        );

        waitUntilThreadIsWaiting(
                secondThread
        );

        // [ACT]
        assertTrue(
                monitor.fireTransition(0)
        );

        firstThread.join(2000);
        secondThread.join(2000);

        // [ASSERT]
        assertFalse(
                firstThread.isAlive()
        );

        assertFalse(
                secondThread.isAlive()
        );

        assertEquals(
                0,
                policy.getInvocationCount()
        );

        assertArrayEquals(
                new int[]{0, 0, 0},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [IMMEDIATE-PRIORITY]
     *
     * Una transición temporal no puede dispararse mientras
     * exista una transición inmediata sensibilizada.
     */
    @Test
    void timedTransitionShouldWaitForEnabledImmediateTransition()
            throws InterruptedException {

        /*
         * [ARRANGE]
         *
         * T0 consume P0.
         * T1 consume P1.
         *
         * Inicialmente ambas están sensibilizadas.
         *
         * T0 -> inmediata
         * T1 -> temporal
         */
        int[][] incidenceMatrix = {
                {-1,  0},
                { 0, -1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 1}
                );

        TransitionSemantics semantics =
                TransitionSemantics.fromTimedTransitions(
                        2,
                        Set.of(1)
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        semantics
                );

        AtomicBoolean timedResult =
                new AtomicBoolean(false);

        Thread timedThread =
                new Thread(() ->
                        timedResult.set(
                                monitor.fireTransition(1)
                        )
                );

        // [ACT - TIMED REQUEST]
        timedThread.start();

        waitUntilThreadIsWaiting(
                timedThread
        );

        /*
         * T1 está sensibilizada matemáticamente,
         * pero debe esperar a T0 inmediata.
         */
        assertTrue(
                timedThread.isAlive()
        );

        assertArrayEquals(
                new int[]{1, 1},
                petriNet.getCurrentMarking()
        );

        // [ACT - IMMEDIATE]
        assertTrue(
                monitor.fireTransition(0)
        );

        timedThread.join(2000);

        // [ASSERT]
        assertFalse(
                timedThread.isAlive(),
                "Timed transition should resume after immediate transition fires"
        );

        assertTrue(
                timedResult.get()
        );

        assertArrayEquals(
                new int[]{0, 0},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [IMMEDIATE-DRAIN]
     *
     * Verifica que una transición inmediata consuma el token
     * intermedio antes de permitir un segundo depósito temporal.
     *
     * Reproduce genéricamente la propiedad que necesitamos
     * posteriormente para P9.
     */
    @Test
    void immediateTransitionShouldPreventTimedBufferAccumulation()
            throws InterruptedException {

        /*
         * T0 y T1 depositan en P2.
         * T2 consume P2.
         *
         * T0 -> temporal
         * T1 -> temporal
         * T2 -> inmediata
         *
         * M0 = [1,1,0,0]
         */
        int[][] incidenceMatrix = {
                {-1,  0,  0},
                { 0, -1,  0},
                { 1,  1, -1},
                { 0,  0,  1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 1, 0, 0}
                );

        TransitionSemantics semantics =
                TransitionSemantics.fromTimedTransitions(
                        3,
                        Set.of(0, 1)
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        semantics
                );

        /*
         * [FIRST-TIMED-DEPOSIT]
         *
         * T2 inmediata todavía no está sensibilizada,
         * por lo que T0 puede ejecutarse.
         */
        assertTrue(
                monitor.fireTransition(0)
        );

        assertArrayEquals(
                new int[]{0, 1, 1, 0},
                petriNet.getCurrentMarking()
        );

        AtomicBoolean secondTimedResult =
                new AtomicBoolean(false);

        Thread secondTimedThread =
                new Thread(() ->
                        secondTimedResult.set(
                                monitor.fireTransition(1)
                        )
                );

        /*
         * T1 podría dispararse según PetriNet,
         * pero T2 inmediata está sensibilizada.
         */
        secondTimedThread.start();

        waitUntilThreadIsWaiting(
                secondTimedThread
        );

        // [ASSERT - BUFFER REMAINS AT ONE]
        assertEquals(
                1,
                petriNet.getCurrentMarking()[2]
        );

        // [ACT - IMMEDIATE DRAIN]
        assertTrue(
                monitor.fireTransition(2)
        );

        secondTimedThread.join(2000);

        // [ASSERT]
        assertFalse(
                secondTimedThread.isAlive(),
                "Timed transition should resume after immediate drain"
        );

        assertTrue(
                secondTimedResult.get()
        );

        assertArrayEquals(
                new int[]{0, 0, 1, 1},
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [TEST-SYNCHRONIZATION]
     *
     * Espera de forma acotada hasta que el thread
     * ingrese al estado WAITING.
     */
    private void waitUntilThreadIsWaiting(
            Thread thread
    ) throws InterruptedException {

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
     * PetriNet artificial utilizada para detectar accesos
     * concurrentes a fire().
     */
    private static class ConcurrencyProbePetriNet
            extends PetriNet {

        private final AtomicInteger activeCalls =
                new AtomicInteger(0);

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
                 * Amplía deliberadamente la ventana para
                 * detectar accesos simultáneos.
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
     * Permite controlar exactamente cuál transición
     * debe ganar un conflicto.
     */
    private static class FixedTransitionPolicy
            implements Policy {

        private final int selectedTransition;

        private List<Integer> lastCandidates =
                List.of();

        FixedTransitionPolicy(
                int selectedTransition
        ) {

            this.selectedTransition =
                    selectedTransition;
        }

        @Override
        public OptionalInt select(
                List<Integer> candidates
        ) {

            lastCandidates =
                    List.copyOf(candidates);

            if (!candidates.contains(
                    selectedTransition
            )) {

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
     * Permite comprobar si Monitor consultó Policy.
     */
    private static class RecordingPolicy
            implements Policy {

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