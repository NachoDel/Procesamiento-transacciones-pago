package monitor;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import petri.PetriNet;

/**
 * [T2.1.3 - MONITOR TESTS]
 *
 * Valida el comportamiento inicial del Monitor antes de
 * incorporar espera condicional, políticas y logging.
 */
class MonitorTest {

    /**
     * [BASIC-FIRING]
     *
     * Verifica que Monitor delega correctamente el disparo
     * hacia la Red de Petri.
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
                new Monitor(petriNet);

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
     * [DISABLED-TRANSITION]
     *
     * Verifica que un disparo imposible retorna false
     * sin corromper el marcado.
     */
    @Test
    void shouldReturnFalseWhenTransitionIsNotEnabled() {

        // [ARRANGE]
        int[][] incidenceMatrix = {
                {-1},
                { 1}
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{0, 1}
                );

        MonitorInterface monitor =
                new Monitor(petriNet);

        // [ACT]
        boolean fired =
                monitor.fireTransition(0);

        // [ASSERT]
        assertFalse(fired);

        assertArrayEquals(
                new int[]{0, 1},
                petriNet.getCurrentMarking()
        );
    }
    
    /**
     * [MUTUAL-EXCLUSION]
     *
     * Verifica que el Monitor impide el acceso concurrente a la Red de Petri.
     */
    @Test
    void shouldSerializeConcurrentAccessToPetriNet() throws InterruptedException {

        // [ARRANGE - CONCURRENCY PROBE]
        ConcurrencyProbePetriNet petriNet =
                new ConcurrencyProbePetriNet();

        MonitorInterface monitor =
                new Monitor(petriNet);

        int threadCount = 10;

        /*
        * [START-SYNCHRONIZATION]
        *
        * readyLatch permite saber que todos los threads están preparados.
        * startLatch hace que todos intenten entrar al Monitor casi al mismo tiempo.
        *
        * Esto aumenta deliberadamente la contención del test.
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

        /*
        * [ACT]
        *
        * Esperamos que todos estén preparados y luego los liberamos juntos.
        */
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
     * [TEST-DOUBLE / CONCURRENCY-PROBE]
     *
     * PetriNet artificial utilizada exclusivamente para verificar
     * la exclusión mutua del Monitor.
     *
     * [IMPORTANT]
     * Los contadores del probe son atómicos porque el propio mecanismo
     * de medición debe ser seguro frente a accesos concurrentes.
     */
    private static class ConcurrencyProbePetriNet extends PetriNet {

        /**
         * [CONCURRENCY-MEASUREMENT]
         *
         * Cantidad de threads que se encuentran actualmente
         * ejecutando fire().
         */
        private final AtomicInteger activeCalls =
                new AtomicInteger(0);

        /**
         * [CONCURRENCY-MEASUREMENT]
         *
         * Queda en true si alguna vez se detectan dos o más
         * ejecuciones simultáneas de fire().
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

            /*
            * [ENTER-PROBE]
            *
            * incrementAndGet() es atómico, por lo que el test
            * puede medir correctamente accesos simultáneos.
            */
            int concurrentCalls =
                    activeCalls.incrementAndGet();

            if (concurrentCalls > 1) {
                concurrentAccessDetected.set(true);
            }

            try {

                /*
                * [FORCED-CONTENTION]
                *
                * Demora exclusiva del test para ampliar la ventana
                * en la que dos threads coincidirían si el Monitor
                * no protegiera PetriNet correctamente.
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
}