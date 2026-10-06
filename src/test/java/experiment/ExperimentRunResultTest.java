package experiment;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EXPERIMENT-RUN-RESULT TESTS]
 *
 * Verifica la representación y el criterio de aceptación
 * de una corrida experimental terminada.
 */
class ExperimentRunResultTest {

    private ExperimentRunResult createSuccessfulResult() {

        return new ExperimentRunResult(
                ExperimentRunConfig.PolicyMode.RANDOM,
                2026L,
                "BASELINE",
                200,
                200,
                200,
                70,
                60,
                70,
                TimeUnit.SECONDS.toNanos(25),
                true,
                true,
                true,
                Path.of(
                        "results/logs/RANDOM_BASELINE_2026.log"
                )
        );
    }

    @Test
    void shouldExposeRunMetrics() {

        ExperimentRunResult result =
                createSuccessfulResult();

        assertEquals(
                ExperimentRunConfig.PolicyMode.RANDOM,
                result.getPolicyMode()
        );

        assertEquals(
                2026L,
                result.getSeed()
        );

        assertEquals(
                "BASELINE",
                result.getTimingName()
        );

        assertEquals(
                200,
                result.getAdmissions()
        );

        assertEquals(
                200,
                result.getCompletions()
        );

        assertEquals(
                70,
                result.getIt1Count()
        );

        assertEquals(
                60,
                result.getIt2Count()
        );

        assertEquals(
                70,
                result.getIt3Count()
        );

        assertEquals(
                200,
                result.getTotalTInvariants()
        );

        assertEquals(
                25_000L,
                result.getDurationMillis()
        );
    }

    @Test
    void shouldBuildStableRunId() {

        ExperimentRunResult result =
                createSuccessfulResult();

        assertEquals(
                "RANDOM_BASELINE_2026",
                result.getRunId()
        );
    }

    @Test
    void shouldAcceptCompleteValidRun() {

        assertTrue(
                createSuccessfulResult()
                        .isSuccessful()
        );
    }

    @Test
    void shouldRejectRunWithMissingAdmissions() {

        ExperimentRunResult result =
                new ExperimentRunResult(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE",
                        200,
                        199,
                        200,
                        70,
                        60,
                        70,
                        1L,
                        true,
                        true,
                        true,
                        Path.of("run.log")
                );

        assertFalse(
                result.isSuccessful()
        );
    }

    @Test
    void shouldRejectRunWithWrongInvariantTotal() {

        ExperimentRunResult result =
                new ExperimentRunResult(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        2026L,
                        "BASELINE",
                        200,
                        200,
                        200,
                        70,
                        60,
                        69,
                        1L,
                        true,
                        true,
                        true,
                        Path.of("run.log")
                );

        assertFalse(
                result.isSuccessful()
        );
    }

    @Test
    void shouldRejectRunWhenVerificationFails() {

        ExperimentRunResult result =
                new ExperimentRunResult(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE",
                        200,
                        200,
                        200,
                        70,
                        60,
                        70,
                        1L,
                        true,
                        false,
                        true,
                        Path.of("run.log")
                );

        assertFalse(
                result.isSuccessful()
        );
    }

    @Test
    void shouldRejectRunWhenWorkersRemainAlive() {

        ExperimentRunResult result =
                new ExperimentRunResult(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE",
                        200,
                        200,
                        200,
                        70,
                        60,
                        70,
                        1L,
                        true,
                        true,
                        false,
                        Path.of("run.log")
                );

        assertFalse(
                result.isSuccessful()
        );
    }

    @Test
    void shouldRejectInvalidConstructorValues() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExperimentRunResult(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                0,
                                0,
                                0,
                                0,
                                0,
                                0,
                                0L,
                                true,
                                true,
                                true,
                                Path.of("run.log")
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExperimentRunResult(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                1,
                                -1,
                                0,
                                0,
                                0,
                                0,
                                0L,
                                true,
                                true,
                                true,
                                Path.of("run.log")
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExperimentRunResult(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                1,
                                0,
                                0,
                                0,
                                0,
                                0,
                                -1L,
                                true,
                                true,
                                true,
                                Path.of("run.log")
                        )
        );
    }
}
