package experiment;

import config.PaymentTimingConfig;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EXPERIMENT-RUNNER INTEGRATION TEST]
 *
 * Verifica corridas experimentales completas utilizando
 * los componentes reales del sistema:
 *
 * - PetriNet;
 * - Monitor;
 * - Workers;
 * - Policy;
 * - timing;
 * - auditoría de P-invariantes;
 * - logging;
 * - cierre de admisión;
 * - drenaje;
 * - análisis de T-invariantes;
 * - timeout y cleanup.
 *
 * [TEST-TIMING]
 *
 * Se utiliza una versión acelerada de la configuración
 * temporal para mantener los tests rápidos sin eliminar
 * la semántica temporal del modelo.
 */
class ExperimentRunnerTest {

    private static final int TARGET = 20;

    private static final long SEED = 2026L;

    private static final Set<String> PAYMENT_WORKERS =
            Set.of(
                    "H0",
                    "H1",
                    "H2",
                    "H3",
                    "H4"
            );

    @TempDir
    Path tempDirectory;

    @Test
    void shouldCompleteRandomRunWithExactTargetAndValidInvariants()
            throws Exception {

        Path logPath =
                tempDirectory.resolve(
                        "RANDOM_TEST_FAST_2026.log"
                );

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        logPath
                );

        ExperimentRunner runner =
                new ExperimentRunner();

        ExperimentRunResult result =
                runner.run(
                        config
                );

        assertSuccessfulRun(
                result,
                ExperimentRunConfig.PolicyMode.RANDOM,
                "RANDOM_TEST_FAST_2026",
                logPath
        );
    }

    @Test
    void shouldCompletePriorityRunWithExactTargetAndValidInvariants()
            throws Exception {

        Path logPath =
                tempDirectory.resolve(
                        "PRIORITY_TEST_FAST_2026.log"
                );

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        logPath
                );

        ExperimentRunner runner =
                new ExperimentRunner();

        ExperimentRunResult result =
                runner.run(
                        config
                );

        /*
         * [PRIORITY]
         *
         * Recorre específicamente la rama:
         *
         * PriorityPolicy(
         *     HIGH_RISK_TRANSITION,
         *     RandomPolicy(seed)
         * )
         *
         * No se exige una distribución exacta de IT1/IT2/IT3.
         * La política modifica la decisión frente a conflictos,
         * pero la concurrencia puede producir distribuciones
         * diferentes entre ejecuciones.
         */
        assertSuccessfulRun(
                result,
                ExperimentRunConfig.PolicyMode.PRIORITY,
                "PRIORITY_TEST_FAST_2026",
                logPath
        );
    }

    @Test
    void shouldStopWorkersWhenExecutionTimeoutExpires()
            throws Exception {

        Path logPath =
                tempDirectory.resolve(
                        "RANDOM_TIMEOUT_2026.log"
                );

        /*
         * [FORCED-TIMEOUT]
         *
         * Un nanosegundo hace imposible completar TARGET
         * operaciones antes de vencer el timeout.
         *
         * El objetivo del test no es validar rendimiento,
         * sino verificar la ruta excepcional del runner.
         */
        ExperimentRunConfig config =
                new ExperimentRunConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        SEED,
                        "TIMEOUT_TEST",
                        PaymentTimingConfig.createScaled(
                                0.01
                        ),
                        200,
                        logPath,
                        Duration.ofNanos(1),
                        Duration.ofSeconds(2)
                );

        ExperimentRunner runner =
                new ExperimentRunner();

        IllegalStateException exception =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                runner.run(
                                        config
                                )
                );

        /*
         * [TIMEOUT-CONTRACT]
         *
         * La corrida debe informar explícitamente
         * que no alcanzó el target.
         */
        assertTrue(
                exception.getMessage()
                        .contains(
                                "timed out before reaching target"
                        )
        );

        /*
         * [LOGGER-EVIDENCE]
         *
         * ExecutionLogger debe haber sido creado y cerrado
         * normalmente por try-with-resources incluso cuando
         * la corrida termina excepcionalmente.
         */
        assertTrue(
                Files.exists(
                        logPath
                )
        );

        /*
         * [FAILURE-CLEANUP]
         *
         * El bloque finally de ExperimentRunner debe:
         *
         * - solicitar stop global;
         * - interrumpir Workers bloqueados;
         * - esperar su terminación.
         *
         * Por lo tanto, al propagarse el timeout no debe
         * quedar ningún H0-H4 vivo.
         */
        assertNoPaymentWorkersAlive();
    }

    /**
     * Construye una configuración rápida para tests
     * end-to-end exitosos.
     */
    private ExperimentRunConfig createConfig(
            ExperimentRunConfig.PolicyMode policyMode,
            Path logPath
    ) {

        return new ExperimentRunConfig(
                policyMode,
                SEED,
                "TEST_FAST",

                /*
                 * 1 % de la baseline.
                 *
                 * Todos los delays temporales continúan
                 * siendo positivos.
                 */
                PaymentTimingConfig.createScaled(
                        0.01
                ),

                TARGET,

                logPath,

                /*
                 * Timeout global de ejecución.
                 */
                Duration.ofSeconds(10),

                /*
                 * Timeout de shutdown.
                 */
                Duration.ofSeconds(2)
        );
    }

    /**
     * Contrato común que debe cumplir toda corrida
     * experimental exitosa.
     */
    private void assertSuccessfulRun(
            ExperimentRunResult result,
            ExperimentRunConfig.PolicyMode expectedPolicy,
            String expectedRunId,
            Path expectedLogPath
    ) throws Exception {

        /*
         * [EXACT-ADMISSION]
         */
        assertEquals(
                TARGET,
                result.getAdmissions()
        );

        /*
         * [COMPLETE-DRAIN]
         */
        assertEquals(
                TARGET,
                result.getCompletions()
        );

        /*
         * [T-INVARIANTS]
         */
        assertEquals(
                TARGET,
                result.getTotalTInvariants()
        );

        assertEquals(
                TARGET,
                result.getIt1Count()
                        + result.getIt2Count()
                        + result.getIt3Count()
        );

        /*
         * [FORMAL-VERIFICATION]
         */
        assertTrue(
                result.arePInvariantsValid()
        );

        assertTrue(
                result.areTInvariantsValid()
        );

        /*
         * [WORKER-LIFECYCLE]
         */
        assertTrue(
                result.areWorkersTerminated()
        );

        /*
         * [OVERALL-SUCCESS]
         */
        assertTrue(
                result.isSuccessful()
        );

        /*
         * [IDENTIFICATION]
         */
        assertEquals(
                expectedPolicy,
                result.getPolicyMode()
        );

        assertEquals(
                SEED,
                result.getSeed()
        );

        assertEquals(
                "TEST_FAST",
                result.getTimingName()
        );

        assertEquals(
                expectedRunId,
                result.getRunId()
        );

        assertEquals(
                expectedLogPath,
                result.getLogPath()
        );

        /*
         * [MONOTONIC-DURATION]
         */
        assertTrue(
                result.getDurationNanos() > 0
        );

        /*
         * [LOG-EVIDENCE]
         */
        assertTrue(
                Files.exists(
                        expectedLogPath
                )
        );

        assertFalse(
                Files.readAllLines(
                        expectedLogPath
                ).isEmpty()
        );

        /*
         * También comprobamos que una corrida exitosa
         * no deje Workers activos.
         */
        assertNoPaymentWorkersAlive();
    }

    /**
     * Comprueba que no quede ningún Worker oficial
     * de la simulación vivo después de una corrida.
     */
    private void assertNoPaymentWorkersAlive() {

        boolean paymentWorkerAlive =
                Thread.getAllStackTraces()
                        .keySet()
                        .stream()
                        .filter(
                                Thread::isAlive
                        )
                        .anyMatch(
                                thread ->
                                        PAYMENT_WORKERS.contains(
                                                thread.getName()
                                        )
                        );

        assertFalse(
                paymentWorkerAlive,
                "No payment Worker should remain alive after ExperimentRunner finishes"
        );
    }
}