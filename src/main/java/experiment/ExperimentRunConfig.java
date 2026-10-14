package experiment;

import timing.TransitionTimingConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Objects;

/**
 * [EXPERIMENT-RUN-CONFIG]
 *
 * Parámetros inmutables necesarios para ejecutar
 * una corrida experimental.
 *
 * [RESPONSIBILITY]
 *
 * Esta clase solamente describe una corrida.
 *
 * No:
 * - construye Workers;
 * - ejecuta Threads;
 * - conoce PetriNet;
 * - controla el shutdown;
 * - calcula métricas.
 */
public final class ExperimentRunConfig {

    /**
     * [POLICY-MODE]
     *
     * Políticas oficiales disponibles para una corrida.
     */
    public enum PolicyMode {

        RANDOM,

        PRIORITY
    }

    private final PolicyMode policyMode;

    private final long seed;

    /**
     * Nombre lógico de la configuración temporal.
     *
     * Ejemplos:
     *
     * BASELINE
     * VARIANT
     */
    private final String timingName;

    private final TransitionTimingConfig timingConfig;

    private final int target;

    private final Path logPath;

    /**
     * [EXECUTION-TIMEOUT]
     *
     * Tiempo máximo permitido para que la corrida
     * alcance el target experimental.
     */
    private final Duration executionTimeout;

    /**
     * [SHUTDOWN-TIMEOUT]
     *
     * Tiempo máximo permitido para esperar la terminación
     * de los Workers una vez solicitado el shutdown.
     */
    private final Duration shutdownTimeout;

    public ExperimentRunConfig(
            PolicyMode policyMode,
            long seed,
            String timingName,
            TransitionTimingConfig timingConfig,
            int target,
            Path logPath,
            Duration executionTimeout,
            Duration shutdownTimeout
    ) {

        this.policyMode =
                Objects.requireNonNull(
                        policyMode,
                        "Policy mode cannot be null"
                );

        this.seed =
                seed;

        this.timingName =
                Objects.requireNonNull(
                        timingName,
                        "Timing name cannot be null"
                );

        if (timingName.isBlank()) {

            throw new IllegalArgumentException(
                    "Timing name cannot be blank"
            );
        }

        this.timingConfig =
                Objects.requireNonNull(
                        timingConfig,
                        "Timing config cannot be null"
                );

        if (target <= 0) {

            throw new IllegalArgumentException(
                    "Target must be positive"
            );
        }

        this.target =
                target;

        this.logPath =
                Objects.requireNonNull(
                        logPath,
                        "Log path cannot be null"
                );

        this.executionTimeout =
                Objects.requireNonNull(
                        executionTimeout,
                        "Execution timeout cannot be null"
                );

        if (executionTimeout.isZero()
                || executionTimeout.isNegative()) {

            throw new IllegalArgumentException(
                    "Execution timeout must be positive"
            );
        }

        this.shutdownTimeout =
                Objects.requireNonNull(
                        shutdownTimeout,
                        "Shutdown timeout cannot be null"
                );

        if (shutdownTimeout.isZero()
                || shutdownTimeout.isNegative()) {

            throw new IllegalArgumentException(
                    "Shutdown timeout must be positive"
            );
        }
    }

    public PolicyMode getPolicyMode() {
        return policyMode;
    }

    public long getSeed() {
        return seed;
    }

    public String getTimingName() {
        return timingName;
    }

    public TransitionTimingConfig getTimingConfig() {
        return timingConfig;
    }

    public int getTarget() {
        return target;
    }

    public Path getLogPath() {
        return logPath;
    }

    public Duration getExecutionTimeout() {
        return executionTimeout;
    }

    public Duration getShutdownTimeout() {
        return shutdownTimeout;
    }

    /**
     * [RUN-ID]
     *
     * Identificador reproducible de la corrida.
     *
     * Ejemplo:
     *
     * RANDOM_BASELINE_2026
     */
    public String getRunId() {

        return policyMode.name()
                + "_"
                + timingName
                + "_"
                + seed;
    }
}