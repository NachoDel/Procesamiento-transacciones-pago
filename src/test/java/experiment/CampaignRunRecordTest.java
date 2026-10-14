package experiment;

import org.junit.jupiter.api.Test;
import timing.TransitionTimingConfig;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T4.2.2 - CAMPAIGN RUN RECORD TESTS]
 *
 * Verifica la representación de una corrida dentro
 * de una campaña experimental.
 *
 * Se cubren:
 *
 * - corrida exitosa;
 * - corrida terminada pero inválida;
 * - corrida finalizada con error;
 * - rechazo de resultados pertenecientes a otra corrida.
 */
class CampaignRunRecordTest {

    @Test
    void shouldCreateSuccessfulRecordFromSuccessfulResult() {

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunResult result =
                createResult(
                        config,
                        200,
                        200,
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

        assertEquals(
                CampaignRunRecord.Status.SUCCESS,
                record.getStatus()
        );

        assertTrue(
                record.isSuccessful()
        );

        assertTrue(
                record.getResult().isPresent()
        );

        assertEquals(
                result,
                record.getResult().orElseThrow()
        );

        assertTrue(
                record.getErrorType().isEmpty()
        );

        assertTrue(
                record.getErrorMessage().isEmpty()
        );

        assertEquals(
                config.getRunId(),
                record.getRunId()
        );

        assertEquals(
                ExperimentRunConfig.PolicyMode.RANDOM,
                record.getPolicyMode()
        );

        assertEquals(
                2026L,
                record.getSeed()
        );

        assertEquals(
                "BASELINE",
                record.getTimingName()
        );

        assertEquals(
                200,
                record.getTarget()
        );
    }

    @Test
    void shouldCreateInvalidRecordWhenResultDoesNotMeetSuccessCriteria() {

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        2027L,
                        "VARIANT"
                );

        /*
         * Total de T-invariantes = 199.
         *
         * La corrida produjo un resultado, pero no cumple
         * ExperimentRunResult.isSuccessful().
         */
        ExperimentRunResult result =
                createResult(
                        config,
                        200,
                        200,
                        70,
                        65,
                        64,
                        true,
                        true,
                        true
                );

        assertFalse(
                result.isSuccessful()
        );

        CampaignRunRecord record =
                CampaignRunRecord.fromResult(
                        config,
                        result
                );

        assertEquals(
                CampaignRunRecord.Status.INVALID_RESULT,
                record.getStatus()
        );

        assertFalse(
                record.isSuccessful()
        );

        assertTrue(
                record.getResult().isPresent()
        );

        assertTrue(
                record.getErrorType().isEmpty()
        );

        assertTrue(
                record.getErrorMessage().isEmpty()
        );
    }

    @Test
    void shouldCreateErrorRecordFromFailure() {

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2028L,
                        "BASELINE"
                );

        IllegalStateException failure =
                new IllegalStateException(
                        "Experimental run timed out"
                );

        CampaignRunRecord record =
                CampaignRunRecord.fromFailure(
                        config,
                        failure
                );

        assertEquals(
                CampaignRunRecord.Status.ERROR,
                record.getStatus()
        );

        assertFalse(
                record.isSuccessful()
        );

        assertTrue(
                record.getResult().isEmpty()
        );

        assertEquals(
                IllegalStateException.class.getName(),
                record.getErrorType().orElseThrow()
        );

        assertEquals(
                "Experimental run timed out",
                record.getErrorMessage().orElseThrow()
        );
    }

    @Test
    void shouldUseFallbackMessageWhenFailureHasNoMessage() {

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2029L,
                        "BASELINE"
                );

        RuntimeException failure =
                new RuntimeException();

        CampaignRunRecord record =
                CampaignRunRecord.fromFailure(
                        config,
                        failure
                );

        assertEquals(
                "(no message)",
                record.getErrorMessage().orElseThrow()
        );
    }

    @Test
    void shouldRejectResultBelongingToDifferentRun() {

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunConfig otherConfig =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2030L,
                        "BASELINE"
                );

        ExperimentRunResult otherResult =
                createResult(
                        otherConfig,
                        200,
                        200,
                        70,
                        65,
                        65,
                        true,
                        true,
                        true
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CampaignRunRecord.fromResult(
                                config,
                                otherResult
                        )
        );
    }

    @Test
    void shouldRejectResultWithDifferentTarget() {

        ExperimentRunConfig config =
                createConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE"
                );

        ExperimentRunResult result =
                new ExperimentRunResult(
                        config.getPolicyMode(),
                        config.getSeed(),
                        config.getTimingName(),
                        100,
                        100,
                        100,
                        34,
                        33,
                        33,
                        1_000_000L,
                        true,
                        true,
                        true,
                        config.getLogPath()
                );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        CampaignRunRecord.fromResult(
                                config,
                                result
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
     * Construye un resultado asociado a la configuración
     * indicada.
     */
    private ExperimentRunResult createResult(
            ExperimentRunConfig config,
            int admissions,
            int completions,
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
                admissions,
                completions,
                it1,
                it2,
                it3,
                1_000_000L,
                pInvariantsValid,
                tInvariantsValid,
                workersTerminated,
                config.getLogPath()
        );
    }
}
