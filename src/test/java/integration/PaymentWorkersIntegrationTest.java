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
 * Integra por primera vez todos los componentes principales
 * del sistema real:
 *
 * - Red de Petri oficial;
 * - configuración oficial de conflictos;
 * - semántica inmediata/temporal;
 * - tiempos configurados;
 * - RandomPolicy;
 * - Monitor real;
 * - Workers H0, H1, H2, H3 y H4.
 *
 * [SCOPE]
 *
 * Este test es un smoke test de integración.
 *
 * Verifica:
 * - que los cinco Workers puedan arrancar;
 * - que el sistema real produzca progreso;
 * - que pueda solicitarse su finalización;
 * - que no queden Threads residuales.
 *
 * NO verifica todavía:
 * - distribución estadística entre los tres caminos;
 * - exactamente 200 T-invariantes;
 * - política priorizada;
 * - duración final de 20-40 segundos;
 * - logging experimental.
 */
class PaymentWorkersIntegrationTest {

    /**
     * [REAL-WORKERS]
     *
     * Los cinco Workers oficiales deben poder operar
     * conjuntamente sobre la Red de Petri real y luego
     * finalizar limpiamente.
     *
     * [IMPORTANT]
     *
     * No exigimos que los tres flujos aparezcan dentro
     * de esta corrida corta.
     *
     * Con RandomPolicy y disponibilidad dinámica de P7/P8,
     * la utilización concreta de cada flujo pertenece al
     * análisis de ejecuciones completas y no a este smoke test.
     */
    @Test
    void officialWorkersShouldMakeProgressAndTerminate()
            throws InterruptedException {

        // [ARRANGE - OFFICIAL PETRI NET]
        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        /*
         * [REAL-MONITOR]
         *
         * Se construye utilizando exclusivamente
         * configuraciones oficiales del sistema.
         */
        MonitorInterface realMonitor =
                new Monitor(
                        petriNet,
                        new RandomPolicy(2026L),
                        PaymentConflictConfig
                                .getConflictGroups(),
                        PaymentPetriNetConfig
                                .createTransitionSemantics(),
                        PaymentTimingConfig
                                .createBaseline()
                );

        /*
         * [TEST-OBSERVABILITY]
         *
         * Este decorator permite contar disparos exitosos
         * sin agregar logging ni modificar el Monitor productivo.
         */
        CountingMonitor monitor =
                new CountingMonitor(
                        realMonitor
                );

        /*
         * [REAL-WORKERS]
         *
         * Se utilizan las definiciones oficiales:
         *
         * H0 -> T0
         * H1 -> T1,T2,T3
         * H2 -> T4,T5
         * H3 -> T6,T7,T8
         * H4 -> T9
         */
        WorkerCoordinator coordinator =
                new WorkerCoordinator(
                        PaymentWorkerConfig
                                .getDefinitions(),
                        monitor
                );

        try {

            // [ACT - START]
            coordinator.startAll();

            /*
             * [PROGRESS]
             *
             * El objetivo no representa invariantes completos.
             *
             * Solamente exigimos que haya una cantidad mínima
             * de disparos exitosos que demuestre actividad real
             * de la red concurrente.
             */
            boolean progress =
                    waitForSuccessfulFirings(
                            monitor,
                            20,
                            5,
                            TimeUnit.SECONDS
                    );

            // [ASSERT - GLOBAL PROGRESS]
            assertTrue(
                    progress,
                    "Official workers did not make enough progress"
            );

        } finally {

            /*
             * [TEST-CLEANUP]
             *
             * Todavía no implementamos el protocolo definitivo
             * de finalización de una corrida experimental.
             *
             * Para este test:
             *
             * 1. evitamos nuevos ciclos;
             * 2. interrumpimos posibles esperas activas;
             * 3. esperamos la terminación de todos los Threads.
             */
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

        // [ASSERT - NO LEFTOVER THREADS]
        assertTrue(
                coordinator.isTerminated(),
                "All worker threads must be terminated"
        );
    }

    /**
     * [TEST-SYNCHRONIZATION]
     *
     * Espera hasta alcanzar una cantidad mínima de
     * disparos exitosos o hasta agotar el timeout.
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
     * [TEST-DOUBLE / MONITOR-DECORATOR]
     *
     * Mantiene el comportamiento completo del Monitor real
     * y agrega solamente una métrica para este test.
     *
     * [RESPONSIBILITY]
     *
     * No:
     * - modifica decisiones de Policy;
     * - modifica PetriNet;
     * - altera tiempos;
     * - introduce logging productivo.
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