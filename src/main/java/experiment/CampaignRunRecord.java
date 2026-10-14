package experiment;

import java.util.Objects;
import java.util.Optional;

/**
 * [T4.2.2 - CAMPAIGN RUN RECORD]
 *
 * Representa el resultado observable de una corrida dentro
 * de una campaña experimental.
 *
 * [RESPONSIBILITY]
 *
 * Una corrida planificada puede terminar de tres maneras:
 *
 * SUCCESS
 *      ExperimentRunner produjo un resultado válido.
 *
 * INVALID_RESULT
 *      ExperimentRunner terminó y produjo un resultado,
 *      pero no cumple todos los criterios definidos por
 *      ExperimentRunResult.isSuccessful().
 *
 * ERROR
 *      La corrida terminó excepcionalmente antes de producir
 *      un ExperimentRunResult.
 *
 * [PROTOCOL]
 *
 * El protocolo experimental establece que una corrida fallida
 * debe conservarse como evidencia y no reemplazarse
 * silenciosamente.
 *
 * Esta clase permite representar tanto ejecuciones válidas
 * como inválidas o fallidas dentro del resultado consolidado
 * de la campaña.
 */
public final class CampaignRunRecord {

    /**
     * Estado final de una corrida de campaña.
     */
    public enum Status {

        /**
         * La corrida cumple todos los criterios
         * experimentales de éxito.
         */
        SUCCESS,

        /**
         * La corrida produjo un resultado completo,
         * pero ExperimentRunResult.isSuccessful()
         * devolvió false.
         */
        INVALID_RESULT,

        /**
         * La corrida terminó mediante una excepción.
         */
        ERROR
    }

    private final ExperimentRunConfig config;

    private final Status status;

    /**
     * Resultado disponible solamente cuando
     * ExperimentRunner logró finalizar normalmente.
     */
    private final ExperimentRunResult result;

    /**
     * Información de diagnóstico disponible
     * cuando status == ERROR.
     */
    private final String errorType;

    private final String errorMessage;

    private CampaignRunRecord(
            ExperimentRunConfig config,
            Status status,
            ExperimentRunResult result,
            String errorType,
            String errorMessage
    ) {

        this.config =
                Objects.requireNonNull(
                        config,
                        "Experiment run config cannot be null"
                );

        this.status =
                Objects.requireNonNull(
                        status,
                        "Campaign run status cannot be null"
                );

        this.result =
                result;

        this.errorType =
                errorType;

        this.errorMessage =
                errorMessage;

        validateState();
    }

    /**
     * Construye un registro a partir de una corrida
     * que terminó normalmente.
     *
     * El estado se determina automáticamente utilizando
     * ExperimentRunResult.isSuccessful().
     */
    public static CampaignRunRecord fromResult(
            ExperimentRunConfig config,
            ExperimentRunResult result
    ) {

        Objects.requireNonNull(
                result,
                "Experiment run result cannot be null"
        );

        validateMatchingRun(
                config,
                result
        );

        Status status =
                result.isSuccessful()
                        ? Status.SUCCESS
                        : Status.INVALID_RESULT;

        return new CampaignRunRecord(
                config,
                status,
                result,
                null,
                null
        );
    }

    /**
     * Construye un registro para una corrida que terminó
     * excepcionalmente.
     *
     * Se conserva el tipo de excepción y su mensaje para
     * facilitar la trazabilidad experimental.
     */
    public static CampaignRunRecord fromFailure(
            ExperimentRunConfig config,
            Throwable failure
    ) {

        Objects.requireNonNull(
                failure,
                "Failure cannot be null"
        );

        String message =
                failure.getMessage();

        if (message == null
                || message.isBlank()) {

            message =
                    "(no message)";
        }

        return new CampaignRunRecord(
                config,
                Status.ERROR,
                null,
                failure.getClass().getName(),
                message
        );
    }

    public ExperimentRunConfig getConfig() {
        return config;
    }

    public String getRunId() {
        return config.getRunId();
    }

    public ExperimentRunConfig.PolicyMode getPolicyMode() {
        return config.getPolicyMode();
    }

    public long getSeed() {
        return config.getSeed();
    }

    public String getTimingName() {
        return config.getTimingName();
    }

    public int getTarget() {
        return config.getTarget();
    }

    public Status getStatus() {
        return status;
    }

    public boolean isSuccessful() {
        return status == Status.SUCCESS;
    }

    /**
     * El resultado solamente existe para corridas que
     * terminaron normalmente.
     */
    public Optional<ExperimentRunResult> getResult() {

        return Optional.ofNullable(
                result
        );
    }

    public Optional<String> getErrorType() {

        return Optional.ofNullable(
                errorType
        );
    }

    public Optional<String> getErrorMessage() {

        return Optional.ofNullable(
                errorMessage
        );
    }

    /**
     * Valida consistencia interna entre status,
     * resultado y datos de error.
     */
    private void validateState() {

        switch (status) {

            case SUCCESS,
                 INVALID_RESULT -> {

                if (result == null) {

                    throw new IllegalArgumentException(
                            "Completed campaign record requires an ExperimentRunResult"
                    );
                }

                if (errorType != null
                        || errorMessage != null) {

                    throw new IllegalArgumentException(
                            "Completed campaign record cannot contain error information"
                    );
                }
            }

            case ERROR -> {

                if (result != null) {

                    throw new IllegalArgumentException(
                            "Failed campaign record cannot contain an ExperimentRunResult"
                    );
                }

                if (errorType == null
                        || errorMessage == null) {

                    throw new IllegalArgumentException(
                            "Failed campaign record requires error information"
                    );
                }
            }
        }
    }

    /**
     * Impide asociar accidentalmente un resultado de
     * una corrida con la configuración de otra.
     */
    private static void validateMatchingRun(
            ExperimentRunConfig config,
            ExperimentRunResult result
    ) {

        Objects.requireNonNull(
                config,
                "Experiment run config cannot be null"
        );

        if (!config.getRunId().equals(
                result.getRunId()
        )) {

            throw new IllegalArgumentException(
                    "ExperimentRunConfig and ExperimentRunResult belong to different runs"
            );
        }

        if (config.getTarget()
                != result.getTarget()) {

            throw new IllegalArgumentException(
                    "ExperimentRunConfig and ExperimentRunResult have different targets"
            );
        }
    }
}

