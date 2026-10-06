package experiment;

import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * [EXPERIMENT-RUN-RESULT]
 *
 * Resultado inmutable producido al finalizar
 * una corrida experimental.
 *
 * [RESPONSIBILITY]
 *
 * Conserva:
 *
 * - configuración identificatoria de la corrida;
 * - progreso alcanzado;
 * - distribución de T-invariantes;
 * - duración;
 * - estado de verificaciones;
 * - estado final de los Workers;
 * - archivo de log asociado.
 *
 * Esta clase no ejecuta verificaciones ni controla Threads.
 */
public final class ExperimentRunResult {

    private final ExperimentRunConfig.PolicyMode policyMode;

    private final long seed;

    private final String timingName;

    private final int target;

    private final int admissions;

    private final int completions;

    private final int it1Count;

    private final int it2Count;

    private final int it3Count;

    /**
     * Duración medida con System.nanoTime().
     */
    private final long durationNanos;

    private final boolean pInvariantsValid;

    private final boolean tInvariantsValid;

    private final boolean workersTerminated;

    private final Path logPath;

    public ExperimentRunResult(
            ExperimentRunConfig.PolicyMode policyMode,
            long seed,
            String timingName,
            int target,
            int admissions,
            int completions,
            int it1Count,
            int it2Count,
            int it3Count,
            long durationNanos,
            boolean pInvariantsValid,
            boolean tInvariantsValid,
            boolean workersTerminated,
            Path logPath
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

        if (target <= 0) {
            throw new IllegalArgumentException(
                    "Target must be positive"
            );
        }

        validateNonNegative(
                admissions,
                "Admissions"
        );

        validateNonNegative(
                completions,
                "Completions"
        );

        validateNonNegative(
                it1Count,
                "IT1 count"
        );

        validateNonNegative(
                it2Count,
                "IT2 count"
        );

        validateNonNegative(
                it3Count,
                "IT3 count"
        );

        if (durationNanos < 0) {
            throw new IllegalArgumentException(
                    "Duration cannot be negative"
            );
        }

        this.target =
                target;

        this.admissions =
                admissions;

        this.completions =
                completions;

        this.it1Count =
                it1Count;

        this.it2Count =
                it2Count;

        this.it3Count =
                it3Count;

        this.durationNanos =
                durationNanos;

        this.pInvariantsValid =
                pInvariantsValid;

        this.tInvariantsValid =
                tInvariantsValid;

        this.workersTerminated =
                workersTerminated;

        this.logPath =
                Objects.requireNonNull(
                        logPath,
                        "Log path cannot be null"
                );
    }

    public ExperimentRunConfig.PolicyMode getPolicyMode() {
        return policyMode;
    }

    public long getSeed() {
        return seed;
    }

    public String getTimingName() {
        return timingName;
    }

    public int getTarget() {
        return target;
    }

    public int getAdmissions() {
        return admissions;
    }

    public int getCompletions() {
        return completions;
    }

    public int getIt1Count() {
        return it1Count;
    }

    public int getIt2Count() {
        return it2Count;
    }

    public int getIt3Count() {
        return it3Count;
    }

    public int getTotalTInvariants() {

        return it1Count
                + it2Count
                + it3Count;
    }

    public long getDurationNanos() {
        return durationNanos;
    }

    public long getDurationMillis() {

        return TimeUnit.NANOSECONDS.toMillis(
                durationNanos
        );
    }

    public boolean arePInvariantsValid() {
        return pInvariantsValid;
    }

    public boolean areTInvariantsValid() {
        return tInvariantsValid;
    }

    public boolean areWorkersTerminated() {
        return workersTerminated;
    }

    public Path getLogPath() {
        return logPath;
    }

    /**
     * [RUN-ID]
     *
     * Mantiene la misma convención que ExperimentRunConfig.
     */
    public String getRunId() {

        return policyMode.name()
                + "_"
                + timingName
                + "_"
                + seed;
    }

    /**
     * [SUCCESS-CRITERIA]
     *
     * Una corrida es válida únicamente cuando:
     *
     * - se admitieron exactamente TARGET operaciones;
     * - finalizaron exactamente TARGET operaciones;
     * - el análisis identifica TARGET T-invariantes;
     * - los P-invariantes fueron válidos;
     * - los T-invariantes fueron válidos;
     * - no quedaron Workers activos.
     */
    public boolean isSuccessful() {

        return admissions == target
                && completions == target
                && getTotalTInvariants() == target
                && pInvariantsValid
                && tInvariantsValid
                && workersTerminated;
    }

    private static void validateNonNegative(
            int value,
            String fieldName
    ) {

        if (value < 0) {
            throw new IllegalArgumentException(
                    fieldName
                            + " cannot be negative"
            );
        }
    }
}
