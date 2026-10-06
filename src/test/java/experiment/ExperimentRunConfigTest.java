package experiment;

import org.junit.jupiter.api.Test;

import timing.TransitionTimingConfig;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * [EXPERIMENT-RUN-CONFIG TESTS]
 *
 * Verifica el contrato de configuración de una
 * corrida experimental.
 */
class ExperimentRunConfigTest {

    private TransitionTimingConfig createTiming() {

        return TransitionTimingConfig.noDelays(
                10
        );
    }

    @Test
    void shouldExposeConfiguredValues() {

        TransitionTimingConfig timing =
                createTiming();

        Path logPath =
                Path.of(
                        "results/logs/RANDOM_BASELINE_2026.log"
                );

        Duration timeout =
                Duration.ofSeconds(5);

        ExperimentRunConfig config =
                new ExperimentRunConfig(
                        ExperimentRunConfig.PolicyMode.RANDOM,
                        2026L,
                        "BASELINE",
                        timing,
                        200,
                        logPath,
                        timeout
                );

        assertEquals(
                ExperimentRunConfig.PolicyMode.RANDOM,
                config.getPolicyMode()
        );

        assertEquals(
                2026L,
                config.getSeed()
        );

        assertEquals(
                "BASELINE",
                config.getTimingName()
        );

        assertSame(
                timing,
                config.getTimingConfig()
        );

        assertEquals(
                200,
                config.getTarget()
        );

        assertEquals(
                logPath,
                config.getLogPath()
        );

        assertEquals(
                timeout,
                config.getShutdownTimeout()
        );
    }

    @Test
    void shouldBuildStableRunId() {

        ExperimentRunConfig config =
                new ExperimentRunConfig(
                        ExperimentRunConfig.PolicyMode.PRIORITY,
                        2028L,
                        "VARIANT",
                        createTiming(),
                        200,
                        Path.of("run.log"),
                        Duration.ofSeconds(3)
                );

        assertEquals(
                "PRIORITY_VARIANT_2028",
                config.getRunId()
        );
    }

    @Test
    void shouldRejectNullPolicyMode() {

        assertThrows(
                NullPointerException.class,
                () ->
                        new ExperimentRunConfig(
                                null,
                                2026L,
                                "BASELINE",
                                createTiming(),
                                200,
                                Path.of("run.log"),
                                Duration.ofSeconds(3)
                        )
        );
    }

    @Test
    void shouldRejectBlankTimingName() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExperimentRunConfig(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                " ",
                                createTiming(),
                                200,
                                Path.of("run.log"),
                                Duration.ofSeconds(3)
                        )
        );
    }

    @Test
    void shouldRejectNonPositiveTarget() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExperimentRunConfig(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                createTiming(),
                                0,
                                Path.of("run.log"),
                                Duration.ofSeconds(3)
                        )
        );
    }

    @Test
    void shouldRejectNullTimingConfig() {

        assertThrows(
                NullPointerException.class,
                () ->
                        new ExperimentRunConfig(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                null,
                                200,
                                Path.of("run.log"),
                                Duration.ofSeconds(3)
                        )
        );
    }

    @Test
    void shouldRejectNullLogPath() {

        assertThrows(
                NullPointerException.class,
                () ->
                        new ExperimentRunConfig(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                createTiming(),
                                200,
                                null,
                                Duration.ofSeconds(3)
                        )
        );
    }

    @Test
    void shouldRejectNonPositiveShutdownTimeout() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExperimentRunConfig(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                createTiming(),
                                200,
                                Path.of("run.log"),
                                Duration.ZERO
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        new ExperimentRunConfig(
                                ExperimentRunConfig.PolicyMode.RANDOM,
                                2026L,
                                "BASELINE",
                                createTiming(),
                                200,
                                Path.of("run.log"),
                                Duration.ofSeconds(-1)
                        )
        );
    }
}

