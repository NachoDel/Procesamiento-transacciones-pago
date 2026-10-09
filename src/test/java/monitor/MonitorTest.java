package monitor;

import petri.PetriNet;
import policy.ConflictGroup;
import policy.Policy;
import policy.PriorityPolicy;
import policy.RandomPolicy;
import timing.TransitionSemantics;
import timing.TransitionTimingConfig;

import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

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
 * - prioridad de inmediatas frente a temporales;
 * - tiempos configurados;
 * - liberación del lock durante espera temporal;
 * - interrupción durante temporización;
 * - notificación post-disparo;
 * - preservación del orden real de disparos.
 */
class MonitorTest {

    @Test
    void shouldFireEnabledTransitionThroughMonitor() {

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
                        ),
                        TransitionTimingConfig.noDelays(
                                petriNet.getTransitionsCount()
                        )
                );

        boolean fired =
                monitor.fireTransition(0);

        assertTrue(fired);

        assertArrayEquals(
                new int[]{0, 1},
                petriNet.getCurrentMarking()
        );
    }

    @Test
    void shouldSerializeConcurrentAccessToPetriNet()
            throws InterruptedException {

        ConcurrencyProbePetriNet petriNet =
                new ConcurrencyProbePetriNet();

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        ),
                        TransitionTimingConfig.noDelays(
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

        readyLatch.await();
        startLatch.countDown();

        assertTrue(
                finishedLatch.await(
                        5,
                        TimeUnit.SECONDS
                ),
                "Threads did not finish within the expected time"
        );

        assertFalse(
                petriNet.wasConcurrentAccessDetected(),
                "Two threads entered PetriNet.fire() simultaneously"
        );
    }

    @Test
    void shouldBlockAndResumeWhenTransitionBecomesEnabled()
            throws InterruptedException {

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
                        ),
                        TransitionTimingConfig.noDelays(
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

        assertTrue(
                waitingThread.isAlive()
        );

        assertTrue(
                monitor.fireTransition(0)
        );

        waitingThread.join(2000);

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

    @Test
    void shouldStopWaitingWhenThreadIsInterrupted()
            throws InterruptedException {

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
                        ),
                        TransitionTimingConfig.noDelays(
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
                            Thread.currentThread()
                                    .isInterrupted()
                    );
                });

        waitingThread.start();

        waitUntilThreadIsWaiting(
                waitingThread
        );

        waitingThread.interrupt();

        waitingThread.join(2000);

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

    @Test
    void shouldUsePolicyToSelectWhichWaitingTransitionResumes()
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
                        ),
                        TransitionTimingConfig.noDelays(
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

        assertTrue(
                monitor.fireTransition(0)
        );

        transition2Thread.join(2000);

        assertFalse(
                transition2Thread.isAlive(),
                "Transition selected by Policy should have resumed"
        );

        assertTrue(
                transition2Result.get()
        );

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

        transition1Thread.interrupt();
        transition1Thread.join(2000);

        assertFalse(
                transition1Thread.isAlive()
        );

        assertFalse(
                transition1Result.get()
        );
    }

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
                        ),
                        TransitionTimingConfig.noDelays(
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

        assertTrue(
                monitor.fireTransition(0)
        );

        transition2Thread.join(2000);

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

        transition1Thread.interrupt();
        transition1Thread.join(2000);

        assertFalse(
                transition1Thread.isAlive()
        );

        assertFalse(
                transition1Result.get()
        );
    }

    @Test
    void shouldNotUsePolicyForIndependentTransitions()
            throws InterruptedException {

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
                        ),
                        TransitionTimingConfig.noDelays(
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

        assertTrue(
                monitor.fireTransition(0)
        );

        firstThread.join(2000);
        secondThread.join(2000);

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

    @Test
    void timedTransitionShouldWaitForEnabledImmediateTransition()
            throws InterruptedException {

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
                        semantics,
                        TransitionTimingConfig.fromMillis(
                                2,
                                Map.of(
                                        1, 20L
                                )
                        )
                );

        AtomicBoolean timedResult =
                new AtomicBoolean(false);

        Thread timedThread =
                new Thread(() ->
                        timedResult.set(
                                monitor.fireTransition(1)
                        )
                );

        timedThread.start();

        waitUntilThreadIsWaiting(
                timedThread
        );

        assertTrue(
                timedThread.isAlive()
        );

        assertArrayEquals(
                new int[]{1, 1},
                petriNet.getCurrentMarking()
        );

        assertTrue(
                monitor.fireTransition(0)
        );

        timedThread.join(2000);

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

    @Test
    void immediateTransitionShouldPreventTimedBufferAccumulation()
            throws InterruptedException {

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
                        semantics,
                        TransitionTimingConfig.fromMillis(
                                3,
                                Map.of(
                                        0, 10L,
                                        1, 10L
                                )
                        )
                );

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

        secondTimedThread.start();

        waitUntilThreadIsWaiting(
                secondTimedThread
        );

        assertEquals(
                1,
                petriNet.getCurrentMarking()[2]
        );

        assertTrue(
                monitor.fireTransition(2)
        );

        secondTimedThread.join(2000);

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

    @Test
    void timedTransitionShouldRespectConfiguredDelay() {

        PetriNet petriNet =
                new PetriNet(
                        new int[][]{
                                {-1}
                        },
                        new int[]{1}
                );

        TransitionSemantics semantics =
                TransitionSemantics.fromTimedTransitions(
                        1,
                        Set.of(0)
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        semantics,
                        TransitionTimingConfig.fromMillis(
                                1,
                                Map.of(
                                        0, 80L
                                )
                        )
                );

        long startNanos =
                System.nanoTime();

        assertTrue(
                monitor.fireTransition(0)
        );

        long elapsedMillis =
                TimeUnit.NANOSECONDS.toMillis(
                        System.nanoTime()
                                - startNanos
                );

        assertTrue(
                elapsedMillis >= 60,
                "Timed transition fired too early: "
                        + elapsedMillis
                        + " ms"
        );
    }

    @Test
    void timedTransitionShouldReleaseMonitorLockWhileWaiting()
            throws InterruptedException {

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
                        Set.of(0, 1)
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        semantics,
                        TransitionTimingConfig.fromMillis(
                                2,
                                Map.of(
                                        0, 500L,
                                        1, 50L
                                )
                        )
                );

        AtomicBoolean longResult =
                new AtomicBoolean(false);

        AtomicBoolean shortResult =
                new AtomicBoolean(false);

        Thread longTimedThread =
                new Thread(() ->
                        longResult.set(
                                monitor.fireTransition(0)
                        )
                );

        Thread shortTimedThread =
                new Thread(() ->
                        shortResult.set(
                                monitor.fireTransition(1)
                        )
                );

        longTimedThread.start();

        waitUntilThreadIsTimedWaiting(
                longTimedThread
        );

        shortTimedThread.start();

        shortTimedThread.join(300);

        assertFalse(
                shortTimedThread.isAlive(),
                "Short timed transition should finish while long transition is still waiting"
        );

        assertTrue(
                shortResult.get()
        );

        assertTrue(
                longTimedThread.isAlive(),
                "Long timed transition should still be waiting"
        );

        longTimedThread.join(1000);

        assertFalse(
                longTimedThread.isAlive()
        );

        assertTrue(
                longResult.get()
        );

        assertArrayEquals(
                new int[]{0, 0},
                petriNet.getCurrentMarking()
        );
    }

    @Test
    void timedTransitionShouldStopWhenInterruptedDuringDelay()
            throws InterruptedException {

        PetriNet petriNet =
                new PetriNet(
                        new int[][]{
                                {-1}
                        },
                        new int[]{1}
                );

        TransitionSemantics semantics =
                TransitionSemantics.fromTimedTransitions(
                        1,
                        Set.of(0)
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        semantics,
                        TransitionTimingConfig.fromMillis(
                                1,
                                Map.of(
                                        0, 1000L
                                )
                        )
                );

        AtomicBoolean result =
                new AtomicBoolean(true);

        AtomicBoolean interruptedStatus =
                new AtomicBoolean(false);

        Thread timedThread =
                new Thread(() -> {

                    result.set(
                            monitor.fireTransition(0)
                    );

                    interruptedStatus.set(
                            Thread.currentThread()
                                    .isInterrupted()
                    );
                });

        timedThread.start();

        waitUntilThreadIsTimedWaiting(
                timedThread
        );

        timedThread.interrupt();

        timedThread.join(1000);

        assertFalse(
                timedThread.isAlive(),
                "Interrupted timed thread should terminate"
        );

        assertFalse(
                result.get(),
                "Interrupted timed firing should return false"
        );

        assertTrue(
                interruptedStatus.get(),
                "Interrupted status should be preserved"
        );

        assertArrayEquals(
                new int[]{1},
                petriNet.getCurrentMarking()
        );
    }

    @Test
    void shouldNotifyPostFireObserverAfterSuccessfulFiring() {

        int[][] incidenceMatrix = {
                {-1},
                { 1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0}
                );

        RecordingPostFireObserver observer =
                new RecordingPostFireObserver();

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        ),
                        TransitionTimingConfig.noDelays(
                                petriNet.getTransitionsCount()
                        ),
                        observer
                );

        assertTrue(
                monitor.fireTransition(0)
        );

        assertEquals(
                1,
                observer.getInvocationCount()
        );

        assertEquals(
                0,
                observer.getLastTransition()
        );

        assertArrayEquals(
                new int[]{0, 1},
                observer.getLastMarking()
        );
    }

    /**
     * [ABORTED-FIRING / OBSERVER]
     *
     * Una solicitud interrumpida que nunca llega a disparar
     * NO debe producir una notificación post-fire.
     */
    @Test
    void shouldNotNotifyPostFireObserverWhenFiringIsInterrupted()
            throws InterruptedException {

        int[][] incidenceMatrix = {
                {-1,  1},
                { 1, -1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{1, 0}
                );

        RecordingPostFireObserver observer =
                new RecordingPostFireObserver();

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        ),
                        TransitionTimingConfig.noDelays(
                                petriNet.getTransitionsCount()
                        ),
                        observer
                );

        AtomicBoolean result =
                new AtomicBoolean(true);

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

        waitingThread.interrupt();

        waitingThread.join(2000);

        assertFalse(
                waitingThread.isAlive()
        );

        assertFalse(
                result.get()
        );

        assertEquals(
                0,
                observer.getInvocationCount(),
                "Observer must not be notified for an aborted firing"
        );
    }

    /**
     * [POST-FIRE-ORDER]
     *
     * El orden recibido por PostFireObserver debe coincidir
     * exactamente con el orden real de commits de PetriNet.fire().
     *
     * Los Threads compiten concurrentemente, pero Monitor
     * serializa los commits y ejecuta el observer con el mismo lock.
     */
    @Test
    void shouldPreserveRealFiringOrderInPostFireObserver()
            throws InterruptedException {

        int threadCount = 8;

        RecordingOrderPetriNet petriNet =
                new RecordingOrderPetriNet(
                        threadCount
                );

        RecordingOrderObserver observer =
                new RecordingOrderObserver();

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        ),
                        TransitionTimingConfig.noDelays(
                                petriNet.getTransitionsCount()
                        ),
                        observer
                );

        CountDownLatch readyLatch =
                new CountDownLatch(
                        threadCount
                );

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishedLatch =
                new CountDownLatch(
                        threadCount
                );

        AtomicInteger successfulFirings =
                new AtomicInteger(0);

        for (int i = 0;
             i < threadCount;
             i++) {

            int transition = i;

            Thread thread =
                    new Thread(() -> {

                        readyLatch.countDown();

                        try {

                            startLatch.await();

                            if (monitor.fireTransition(
                                    transition
                            )) {

                                successfulFirings
                                        .incrementAndGet();
                            }

                        } catch (InterruptedException exception) {

                            Thread.currentThread().interrupt();

                        } finally {

                            finishedLatch.countDown();
                        }
                    });

            thread.start();
        }

        readyLatch.await();

        startLatch.countDown();

        assertTrue(
                finishedLatch.await(
                        5,
                        TimeUnit.SECONDS
                ),
                "Concurrent firing threads did not finish"
        );

        assertEquals(
                threadCount,
                successfulFirings.get()
        );

        assertEquals(
                threadCount,
                petriNet.getFiringOrder().size()
        );

        assertEquals(
                petriNet.getFiringOrder(),
                observer.getObservedOrder(),
                "Observer order must match the real PetriNet commit order"
        );
    }

    /**
     * [DYNAMIC-AVAILABILITY / DRAIN]
     *
     * Una transición inmediata puede continuar sensibilizada
     * estructuralmente en PetriNet pero dejar de participar
     * de la ejecución por una decisión de lifecycle.
     *
     * En ese caso no debe bloquear indefinidamente
     * una transición temporal pendiente.
     */
    @Test
    void inactiveImmediateTransitionShouldNotBlockTimedTransition()
            throws InterruptedException {

        /*
         * T0 consume una unidad de P0.
         *
         * P0 comienza en 2, por lo que después del primer
         * disparo T0 continúa estructuralmente sensibilizada.
         *
         * T1 consume P1 y es temporal.
         */
        int[][] incidenceMatrix = {
                {-1,  0},
                { 0, -1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{2, 1}
                );

        TransitionSemantics semantics =
                TransitionSemantics.fromTimedTransitions(
                        2,
                        Set.of(1)
                );

        AtomicBoolean immediateAvailable =
                new AtomicBoolean(true);

        PostFireObserver observer =
                (transition, marking) -> {

                    /*
                     * [PHASE-CHANGE]
                     *
                     * Después de ejecutar T0 una vez,
                     * simulamos el cierre de admisión.
                     *
                     * T0 seguirá sensibilizada porque P0
                     * todavía contiene un token.
                     */
                    if (transition == 0) {

                        immediateAvailable.set(
                                false
                        );
                    }
                };

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        List.of(),
                        semantics,
                        TransitionTimingConfig.fromMillis(
                                2,
                                Map.of(
                                        1, 20L
                                )
                        ),
                        observer,
                        transition ->
                                transition != 0
                                        || immediateAvailable.get()
                );

        AtomicBoolean timedResult =
                new AtomicBoolean(false);

        Thread timedThread =
                new Thread(() ->
                        timedResult.set(
                                monitor.fireTransition(1)
                        )
                );

        /*
         * [ARRANGE]
         *
         * T1 comienza primero.
         *
         * Como T0 está habilitada y disponible,
         * la prioridad de inmediatas obliga a T1 a esperar.
         */
        timedThread.start();

        waitUntilThreadIsWaiting(
                timedThread
        );

        assertTrue(
                timedThread.isAlive()
        );

        /*
         * [ACT]
         *
         * Ejecutamos T0.
         *
         * El observer desactiva T0 antes de que
         * fireTransition(0) retorne.
         */
        assertTrue(
                monitor.fireTransition(0)
        );

        /*
         * T0 sigue habilitada estructuralmente:
         *
         * P0 = 1
         *
         * pero ya no participa de la prioridad.
         */
        assertTrue(
                petriNet.isEnabled(0)
        );

        timedThread.join(2000);

        // [ASSERT]
        assertFalse(
                timedThread.isAlive(),
                "Inactive immediate transition must not block timed drain"
        );

        assertTrue(
                timedResult.get()
        );

        assertArrayEquals(
                new int[]{1, 0},
                petriNet.getCurrentMarking()
        );
    }

    private void waitUntilThreadIsWaiting(
            Thread thread
    ) throws InterruptedException {

        long timeout =
                System.currentTimeMillis()
                        + 2000;

        while (thread.getState()
                != Thread.State.WAITING
                && System.currentTimeMillis()
                < timeout) {

            Thread.sleep(5);
        }

        assertEquals(
                Thread.State.WAITING,
                thread.getState(),
                "Thread did not enter WAITING state"
        );
    }

    private void waitUntilThreadIsTimedWaiting(
            Thread thread
    ) throws InterruptedException {

        long timeout =
                System.currentTimeMillis()
                        + 2000;

        while (thread.getState()
                != Thread.State.TIMED_WAITING
                && System.currentTimeMillis()
                < timeout) {

            Thread.sleep(5);
        }

        assertEquals(
                Thread.State.TIMED_WAITING,
                thread.getState(),
                "Thread did not enter TIMED_WAITING state"
        );
    }

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

                concurrentAccessDetected.set(
                        true
                );
            }

            try {

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
                    List.copyOf(
                            candidates
                    );

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

    private static class RecordingPostFireObserver
            implements PostFireObserver {

        private int invocationCount = 0;

        private int lastTransition = -1;

        private int[] lastMarking;

        @Override
        public void onSuccessfulFire(
                int transition,
                int[] marking
        ) {

            invocationCount++;

            lastTransition =
                    transition;

            lastMarking =
                    marking.clone();
        }

        int getInvocationCount() {

            return invocationCount;
        }

        int getLastTransition() {

            return lastTransition;
        }

        int[] getLastMarking() {

            return lastMarking.clone();
        }
    }

    /**
     * [TEST-DOUBLE / ORDERED-PETRI-NET]
     *
     * Registra el orden exacto en que PetriNet.fire()
     * realiza commits.
     *
     * Todas las transiciones son siempre habilitadas.
     */
    private static class RecordingOrderPetriNet
            extends PetriNet {

        private final List<Integer> firingOrder =
                new CopyOnWriteArrayList<>();

        RecordingOrderPetriNet(
                int transitionsCount
        ) {

            super(
                    new int[][]{
                            new int[transitionsCount]
                    },
                    new int[]{0}
            );
        }

        @Override
        public boolean fire(
                int transition
        ) {

            boolean fired =
                    super.fire(
                            transition
                    );

            if (fired) {

                firingOrder.add(
                        transition
                );
            }

            return fired;
        }

        List<Integer> getFiringOrder() {

            return List.copyOf(
                    firingOrder
            );
        }
    }

    /**
     * [TEST-DOUBLE / ORDERED-OBSERVER]
     *
     * Registra el orden de las notificaciones recibidas
     * después de cada commit.
     */
    private static class RecordingOrderObserver
            implements PostFireObserver {

        private final List<Integer> observedOrder =
                new CopyOnWriteArrayList<>();

        @Override
        public void onSuccessfulFire(
                int transition,
                int[] marking
        ) {

            observedOrder.add(
                    transition
            );
        }

        List<Integer> getObservedOrder() {

            return List.copyOf(
                    observedOrder
            );
        }
    }
}