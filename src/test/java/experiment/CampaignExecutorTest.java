package experiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import timing.TransitionTimingConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T4.2.2 - CAMPAIGN EXECUTOR TESTS]
 *
 * Verifica la orquestación de una campaña sin ejecutar
 * corridas concurrentes reales.
 *
 * Se utiliza CampaignExecutor.RunExecutor para simular:
 *
 * - SUCCESS;
 * - INVALID_RESULT;
 * - ERROR;
 * - InterruptedException;
 * - campaña vacía.
 *
 * También se comprueba que el CSV consolidado conserve
 * las corridas procesadas.
 */
class CampaignExecutorTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldExecuteSuccessfulRunsAndPersistSummary()
            throws Exception {

        ExperimentRunConfig first =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunConfig second =
                createConfig(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        2027L,
                        "VARIANT"
                );

        CampaignExecutor.RunExecutor fakeExecutor =
                config ->
                        createSuccessfulResult(
                                config
                        );

        CampaignExecutor executor =
                new CampaignExecutor(
                        fakeExecutor,
                        new CampaignSummaryWriter()
                );

        Path summaryPath =
                tempDirectory.resolve(
                        "campaign.csv"
                );

        List<CampaignRunRecord> records =
                executor.execute(
                        List.of(
                                first,
                                second
                        ),
                        summaryPath
                );

        assertEquals(
                2,
                records.size()
        );

        assertEquals(
                CampaignRunRecord.Status.SUCCESS,
                records.get(0).getStatus()
        );

        assertEquals(
                CampaignRunRecord.Status.SUCCESS,
                records.get(1).getStatus()
        );

        assertTrue(
                records.get(0).isSuccessful()
        );

        assertTrue(
                records.get(1).isSuccessful()
        );

        List<String> lines =
                Files.readAllLines(
                        summaryPath
                );

        /*
         * Header + 2 corridas.
         */
        assertEquals(
                3,
                lines.size()
        );

        assertTrue(
                lines.get(1)
                        .startsWith(
                                "RANDOM_BASELINE_2026,SUCCESS"
                        )
        );

        assertTrue(
                lines.get(2)
                        .startsWith(
                                "PRIORITY_VARIANT_2027,SUCCESS"
                        )
        );
    }

    @Test
    void shouldRegisterInvalidResultAndContinueCampaign()
            throws Exception {

        ExperimentRunConfig invalidConfig =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunConfig successfulConfig =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2027L,
                        "BASELINE"
                );

        AtomicInteger executions =
                new AtomicInteger();

        CampaignExecutor.RunExecutor fakeExecutor =
                config -> {

                    int execution =
                            executions.incrementAndGet();

                    if (execution == 1) {

                        return createInvalidResult(
                                config
                        );
                    }

                    return createSuccessfulResult(
                            config
                    );
                };

        CampaignExecutor executor =
                new CampaignExecutor(
                        fakeExecutor,
                        new CampaignSummaryWriter()
                );

        Path summaryPath =
                tempDirectory.resolve(
                        "invalid-and-success.csv"
                );

        List<CampaignRunRecord> records =
                executor.execute(
                        List.of(
                                invalidConfig,
                                successfulConfig
                        ),
                        summaryPath
                );

        assertEquals(
                2,
                executions.get()
        );

        assertEquals(
                2,
                records.size()
        );

        assertEquals(
                CampaignRunRecord.Status.INVALID_RESULT,
                records.get(0).getStatus()
        );

        assertEquals(
                CampaignRunRecord.Status.SUCCESS,
                records.get(1).getStatus()
        );

        assertFalse(
                records.get(0).isSuccessful()
        );

        assertTrue(
                records.get(1).isSuccessful()
        );

        List<String> lines =
                Files.readAllLines(
                        summaryPath
                );

        assertEquals(
                3,
                lines.size()
        );

        assertTrue(
                lines.get(1)
                        .contains(
                                ",INVALID_RESULT,"
                        )
        );

        assertTrue(
                lines.get(2)
                        .contains(
                                ",SUCCESS,"
                        )
        );
    }

    @Test
    void shouldRegisterRuntimeFailureAndContinueCampaign()
            throws Exception {

        ExperimentRunConfig failingConfig =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunConfig successfulConfig =
                createConfig(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        2026L,
                        "BASELINE"
                );

        AtomicInteger executions =
                new AtomicInteger();

        CampaignExecutor.RunExecutor fakeExecutor =
                config -> {

                    int execution =
                            executions.incrementAndGet();

                    if (execution == 1) {

                        throw new IllegalStateException(
                                "forced experimental failure"
                        );
                    }

                    return createSuccessfulResult(
                            config
                    );
                };

        CampaignExecutor executor =
                new CampaignExecutor(
                        fakeExecutor,
                        new CampaignSummaryWriter()
                );

        Path summaryPath =
                tempDirectory.resolve(
                        "failure-and-success.csv"
                );

        List<CampaignRunRecord> records =
                executor.execute(
                        List.of(
                                failingConfig,
                                successfulConfig
                        ),
                        summaryPath
                );

        /*
         * La campaña debe continuar después del error
         * de la primera corrida.
         */
        assertEquals(
                2,
                executions.get()
        );

        assertEquals(
                2,
                records.size()
        );

        CampaignRunRecord failedRecord =
                records.get(0);

        assertEquals(
                CampaignRunRecord.Status.ERROR,
                failedRecord.getStatus()
        );

        assertEquals(
                IllegalStateException.class.getName(),
                failedRecord
                        .getErrorType()
                        .orElseThrow()
        );

        assertEquals(
                "forced experimental failure",
                failedRecord
                        .getErrorMessage()
                        .orElseThrow()
        );

        assertEquals(
                CampaignRunRecord.Status.SUCCESS,
                records.get(1).getStatus()
        );

        List<String> lines =
                Files.readAllLines(
                        summaryPath
                );

        assertEquals(
                3,
                lines.size()
        );

        assertTrue(
                lines.get(1)
                        .contains(
                                ",ERROR,"
                        )
        );

        assertTrue(
                lines.get(2)
                        .contains(
                                ",SUCCESS,"
                        )
        );
    }

    @Test
    void shouldRegisterIOExceptionAndContinueCampaign()
            throws Exception {

        ExperimentRunConfig failingConfig =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2028L,
                        "VARIANT"
                );

        ExperimentRunConfig successfulConfig =
                createConfig(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        2028L,
                        "VARIANT"
                );

        AtomicInteger executions =
                new AtomicInteger();

        CampaignExecutor.RunExecutor fakeExecutor =
                config -> {

                    int execution =
                            executions.incrementAndGet();

                    if (execution == 1) {

                        throw new IOException(
                                "forced log failure"
                        );
                    }

                    return createSuccessfulResult(
                            config
                    );
                };

        CampaignExecutor executor =
                new CampaignExecutor(
                        fakeExecutor,
                        new CampaignSummaryWriter()
                );

        Path summaryPath =
                tempDirectory.resolve(
                        "io-failure.csv"
                );

        List<CampaignRunRecord> records =
                executor.execute(
                        List.of(
                                failingConfig,
                                successfulConfig
                        ),
                        summaryPath
                );

        assertEquals(
                2,
                executions.get()
        );

        assertEquals(
                CampaignRunRecord.Status.ERROR,
                records.get(0).getStatus()
        );

        assertEquals(
                IOException.class.getName(),
                records.get(0)
                        .getErrorType()
                        .orElseThrow()
        );

        assertEquals(
                CampaignRunRecord.Status.SUCCESS,
                records.get(1).getStatus()
        );
    }

    @Test
    void shouldStopCampaignWhenCoordinatorIsInterrupted()
            throws Exception {

        ExperimentRunConfig first =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunConfig interrupted =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2027L,
                        "BASELINE"
                );

        ExperimentRunConfig neverExecuted =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2028L,
                        "BASELINE"
                );

        AtomicInteger executions =
                new AtomicInteger();

        CampaignExecutor.RunExecutor fakeExecutor =
                config -> {

                    int execution =
                            executions.incrementAndGet();

                    if (execution == 2) {

                        throw new InterruptedException(
                                "forced interruption"
                        );
                    }

                    return createSuccessfulResult(
                            config
                    );
                };

        CampaignExecutor executor =
                new CampaignExecutor(
                        fakeExecutor,
                        new CampaignSummaryWriter()
                );

        Path summaryPath =
                tempDirectory.resolve(
                        "interrupted.csv"
                );

        try {

            InterruptedException exception =
                    assertThrows(
                            InterruptedException.class,
                            () ->
                                    executor.execute(
                                            List.of(
                                                    first,
                                                    interrupted,
                                                    neverExecuted
                                            ),
                                            summaryPath
                                    )
                    );

            assertEquals(
                    "forced interruption",
                    exception.getMessage()
            );

            /*
             * Solamente deben haberse intentado:
             *
             * 1. primera corrida exitosa;
             * 2. corrida interrumpida.
             *
             * La tercera nunca debe ejecutarse.
             */
            assertEquals(
                    2,
                    executions.get()
            );

            /*
             * CampaignExecutor restaura el flag de interrupción
             * antes de propagar InterruptedException.
             */
            assertTrue(
                    Thread.currentThread()
                            .isInterrupted()
            );

            /*
             * El CSV debe conservar:
             *
             * header
             * +
             * primera corrida SUCCESS
             * +
             * segunda corrida ERROR
             */
            List<String> lines =
                    Files.readAllLines(
                            summaryPath
                    );

            assertEquals(
                    3,
                    lines.size()
            );

            assertTrue(
                    lines.get(1)
                            .contains(
                                    ",SUCCESS,"
                            )
            );

            assertTrue(
                    lines.get(2)
                            .contains(
                                    ",ERROR,"
                            )
            );

            assertTrue(
                    lines.get(2)
                            .contains(
                                    InterruptedException.class.getName()
                            )
            );

        } finally {

            /*
             * IMPORTANTE:
             *
             * Limpiamos el flag para no contaminar otros
             * tests ejecutados por el mismo thread de JUnit.
             */
            Thread.interrupted();
        }
    }

    @Test
    void shouldCreateHeaderForEmptyCampaign()
            throws Exception {

        CampaignExecutor.RunExecutor fakeExecutor =
                config -> {

                    throw new AssertionError(
                            "Run executor must not be called"
                    );
                };

        CampaignExecutor executor =
                new CampaignExecutor(
                        fakeExecutor,
                        new CampaignSummaryWriter()
                );

        Path summaryPath =
                tempDirectory.resolve(
                        "empty.csv"
                );

        List<CampaignRunRecord> records =
                executor.execute(
                        List.of(),
                        summaryPath
                );

        assertTrue(
                records.isEmpty()
        );

        List<String> lines =
                Files.readAllLines(
                        summaryPath
                );

        assertEquals(
                1,
                lines.size()
        );

        assertTrue(
                lines.get(0)
                        .startsWith(
                                "runId,status,policy"
                        )
        );
    }

    @Test
    void shouldRejectNullConfigInsideCampaign() {

        CampaignExecutor executor =
                new CampaignExecutor(
                        config ->
                                createSuccessfulResult(
                                        config
                                ),
                        new CampaignSummaryWriter()
                );

        List<ExperimentRunConfig> configs =
                new java.util.ArrayList<>();

        configs.add(null);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        executor.execute(
                                configs,
                                tempDirectory.resolve(
                                        "invalid.csv"
                                )
                        )
        );
    }

    /**
     * Configuración mínima para las pruebas del executor.
     *
     * No se ejecuta realmente ExperimentRunner, por lo que
     * la configuración temporal solamente debe ser válida
     * como objeto de configuración.
     */
    private ExperimentRunConfig createConfig(
            ExperimentRunConfig.PolicyMode policy,
            long seed,
            String timing
    ) {

        String runId =
                policy.name()
                        + "_"
                        + timing
                        + "_"
                        + seed;

        return new ExperimentRunConfig(
                policy,
                seed,
                timing,
                TransitionTimingConfig.noDelays(
                        10
                ),
                200,
                Path.of(
                        "results",
                        "logs",
                        runId + ".log"
                ),
                Duration.ofSeconds(60),
                Duration.ofSeconds(5)
        );
    }

    /**
     * Resultado que cumple completamente
     * ExperimentRunResult.isSuccessful().
     */
    private ExperimentRunResult createSuccessfulResult(
            ExperimentRunConfig config
    ) {

        return new ExperimentRunResult(
                config.getPolicyMode(),
                config.getSeed(),
                config.getTimingName(),
                config.getTarget(),
                200,
                200,
                70,
                65,
                65,
                25_000_000_000L,
                true,
                true,
                true,
                config.getLogPath()
        );
    }

    /**
     * Resultado terminado pero formalmente inválido.
     *
     * IT1 + IT2 + IT3 = 199.
     */
    private ExperimentRunResult createInvalidResult(
            ExperimentRunConfig config
    ) {

        return new ExperimentRunResult(
                config.getPolicyMode(),
                config.getSeed(),
                config.getTimingName(),
                config.getTarget(),
                200,
                200,
                70,
                65,
                64,
                25_000_000_000L,
                true,
                true,
                true,
                config.getLogPath()
        );
    }
}
