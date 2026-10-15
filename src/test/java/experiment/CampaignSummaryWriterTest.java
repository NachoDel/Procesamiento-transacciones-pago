package experiment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import timing.TransitionTimingConfig;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T4.2.2 - CAMPAIGN SUMMARY WRITER TESTS]
 *
 * Verifica la persistencia del resultado consolidado
 * de una campaña experimental.
 *
 * Se cubren:
 *
 * - header estable;
 * - corrida exitosa;
 * - corrida inválida;
 * - corrida fallida;
 * - escaping CSV;
 * - campaña vacía;
 * - rechazo de registros nulos.
 */
class CampaignSummaryWriterTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldWriteSuccessfulRunToCsv()
            throws Exception {

        Path outputPath =
                tempDirectory.resolve(
                        "campaign.csv"
                );

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunResult result =
                createResult(
                        config,
                        70,
                        65,
                        65,
                        true,
                        true,
                        true
                );

        CampaignRunRecord record =
                CampaignRunRecord.fromResult(
                        config,
                        result
                );

        CampaignSummaryWriter writer =
                new CampaignSummaryWriter();

        writer.write(
                outputPath,
                List.of(record)
        );

        List<String> lines =
                Files.readAllLines(
                        outputPath
                );

        assertEquals(
                2,
                lines.size()
        );

        assertEquals(
                expectedHeader(),
                lines.get(0)
        );

        assertEquals(
                "RANDOM_BASELINE_2026,"
                        + "SUCCESS,"
                        + "RANDOM,"
                        + "2026,"
                        + "BASELINE,"
                        + "200,"
                        + "200,"
                        + "200,"
                        + "70,"
                        + "65,"
                        + "65,"
                        + "200,"
                        + "1234,"
                        + "true,"
                        + "true,"
                        + "true,"
                        + "results/logs/RANDOM_BASELINE_2026.log,"
                        + "true,,",
                lines.get(1)
        );
    }

    /**
     * [CROSS-PLATFORM-CSV]
     *
     * El separador representado en la columna logPath
     * debe ser estable incluso si se recibe una ruta
     * expresada con barras invertidas.
     */
    @Test
    void shouldNormalizeBackslashesInCsvLogPath()
            throws Exception {

        Path outputPath =
                tempDirectory.resolve(
                        "portable.csv"
                );

        ExperimentRunConfig config =
                new ExperimentRunConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2042L,
                        "BASELINE",
                        TransitionTimingConfig.noDelays(10),
                        200,
                        Path.of(
                                "results\\logs\\RANDOM_BASELINE_2042.log"
                        ),
                        Duration.ofSeconds(60),
                        Duration.ofSeconds(5)
                );

        CampaignRunRecord record =
                CampaignRunRecord.fromFailure(
                        config,
                        new IllegalStateException("Test failure")
                );

        new CampaignSummaryWriter().write(
                outputPath,
                List.of(record)
        );

        String row =
                Files.readAllLines(
                        outputPath
                ).get(1);

        assertTrue(
                row.contains(
                        "results/logs/RANDOM_BASELINE_2042.log"
                )
        );
    }

    @Test
    void shouldWriteInvalidResultToCsv()
            throws Exception {

        Path outputPath =
                tempDirectory.resolve(
                        "invalid.csv"
                );

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        2027L,
                        "VARIANT"
                );

        /*
         * IT1 + IT2 + IT3 = 199.
         *
         * La ejecución produjo un resultado,
         * pero no cumple el contrato global de éxito.
         */
        ExperimentRunResult result =
                createResult(
                        config,
                        70,
                        65,
                        64,
                        true,
                        true,
                        true
                );

        CampaignRunRecord record =
                CampaignRunRecord.fromResult(
                        config,
                        result
                );

        CampaignSummaryWriter writer =
                new CampaignSummaryWriter();

        writer.write(
                outputPath,
                List.of(record)
        );

        List<String> lines =
                Files.readAllLines(
                        outputPath
                );

        assertEquals(
                2,
                lines.size()
        );

        assertTrue(
                lines.get(1)
                        .startsWith(
                                "PRIORITY_VARIANT_2027,"
                                        + "INVALID_RESULT,"
                        )
        );

        assertTrue(
                lines.get(1)
                        .contains(
                                ",199,"
                        )
        );

        assertTrue(
                lines.get(1)
                        .endsWith(
                                ",false,,"
                        )
        );
    }

    @Test
    void shouldWriteFailedRunAndEscapeCsvMessage()
            throws Exception {

        Path outputPath =
                tempDirectory.resolve(
                        "failed.csv"
                );

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2028L,
                        "BASELINE"
                );

        IllegalStateException failure =
                new IllegalStateException(
                        "Timeout, target \"not reached\""
                );

        CampaignRunRecord record =
                CampaignRunRecord.fromFailure(
                        config,
                        failure
                );

        CampaignSummaryWriter writer =
                new CampaignSummaryWriter();

        writer.write(
                outputPath,
                List.of(record)
        );

        List<String> lines =
                Files.readAllLines(
                        outputPath
                );

        assertEquals(
                2,
                lines.size()
        );

        String row =
                lines.get(1);

        /*
         * Las métricas que requieren ExperimentRunResult
         * quedan vacías.
         */
        assertTrue(
                row.startsWith(
                        "RANDOM_BASELINE_2028,"
                                + "ERROR,"
                                + "RANDOM,"
                                + "2028,"
                                + "BASELINE,"
                                + "200,"
                )
        );

        /*
         * El tipo de error debe quedar registrado.
         */
        assertTrue(
                row.contains(
                        IllegalStateException.class.getName()
                )
        );

        /*
         * El mensaje contiene coma y comillas,
         * por lo que debe escribirse entre comillas
         * y duplicar las comillas internas.
         */
        assertTrue(
                row.endsWith(
                        "\"Timeout, target \"\"not reached\"\"\""
                )
        );
    }

    @Test
    void shouldWriteOnlyHeaderForEmptyCampaign()
            throws Exception {

        Path outputPath =
                tempDirectory.resolve(
                        "empty.csv"
                );

        CampaignSummaryWriter writer =
                new CampaignSummaryWriter();

        writer.write(
                outputPath,
                List.of()
        );

        List<String> lines =
                Files.readAllLines(
                        outputPath
                );

        assertEquals(
                1,
                lines.size()
        );

        assertEquals(
                expectedHeader(),
                lines.get(0)
        );
    }

    @Test
    void shouldRejectNullRecordInsideCampaign()
            throws Exception {

        Path outputPath =
                tempDirectory.resolve(
                        "invalid-record.csv"
                );

        CampaignSummaryWriter writer =
                new CampaignSummaryWriter();

        List<CampaignRunRecord> records =
                new java.util.ArrayList<>();

        records.add(null);

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        writer.write(
                                outputPath,
                                records
                        )
        );
    }

    /**
     * Configuración mínima válida utilizada por los tests.
     */
    private ExperimentRunConfig createConfig(
            ExperimentRunConfig.PolicyMode policyMode,
            long seed,
            String timingName
    ) {

        return new ExperimentRunConfig(
                policyMode,
                seed,
                timingName,
                TransitionTimingConfig.noDelays(
                        10
                ),
                200,
                Path.of(
                        "results/logs/"
                                + policyMode.name()
                                + "_"
                                + timingName
                                + "_"
                                + seed
                                + ".log"
                ),
                Duration.ofSeconds(60),
                Duration.ofSeconds(5)
        );
    }

    /**
     * Construye un resultado completo asociado
     * a la configuración indicada.
     */
    private ExperimentRunResult createResult(
            ExperimentRunConfig config,
            int it1,
            int it2,
            int it3,
            boolean pInvariantsValid,
            boolean tInvariantsValid,
            boolean workersTerminated
    ) {

        return new ExperimentRunResult(
                config.getPolicyMode(),
                config.getSeed(),
                config.getTimingName(),
                config.getTarget(),
                200,
                200,
                it1,
                it2,
                it3,

                /*
                 * 1.234 segundos.
                 *
                 * getDurationMillis() debe producir 1234.
                 */
                1_234_000_000L,

                pInvariantsValid,
                tInvariantsValid,
                workersTerminated,
                config.getLogPath()
        );
    }

    private String expectedHeader() {

        return "runId,"
                + "status,"
                + "policy,"
                + "seed,"
                + "timing,"
                + "target,"
                + "admissions,"
                + "completions,"
                + "it1,"
                + "it2,"
                + "it3,"
                + "totalTInvariants,"
                + "durationMillis,"
                + "pInvariantsValid,"
                + "tInvariantsValid,"
                + "workersTerminated,"
                + "logPath,"
                + "successful,"
                + "errorType,"
                + "errorMessage";
    }
}
