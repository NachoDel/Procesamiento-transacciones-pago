package experiment;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Objects;

/**
 * [T4.2.2 - CAMPAIGN SUMMARY WRITER]
 *
 * Persiste el resultado consolidado de una campaña
 * experimental en formato CSV.
 *
 * [RESPONSIBILITY]
 *
 * El writer solamente transforma CampaignRunRecord
 * en filas persistentes.
 *
 * No:
 *
 * - ejecuta corridas;
 * - decide políticas;
 * - calcula invariantes;
 * - modifica resultados;
 * - reemplaza corridas fallidas.
 *
 * [PROTOCOL]
 *
 * Una corrida fallida debe conservarse como evidencia.
 * Por ese motivo las filas ERROR también forman parte
 * del archivo consolidado.
 */
public final class CampaignSummaryWriter {

    /**
     * Separador utilizado por el archivo consolidado.
     */
    private static final String SEPARATOR = ",";

    /**
     * Header estable del CSV.
     *
     * Las columnas están pensadas para permitir el análisis
     * posterior de T4.2.3 sin volver a parsear todos los logs.
     */
    private static final String HEADER =
            String.join(
                    SEPARATOR,
                    "runId",
                    "status",
                    "policy",
                    "seed",
                    "timing",
                    "target",
                    "admissions",
                    "completions",
                    "it1",
                    "it2",
                    "it3",
                    "totalTInvariants",
                    "durationMillis",
                    "pInvariantsValid",
                    "tInvariantsValid",
                    "workersTerminated",
                    "logPath",
                    "successful",
                    "errorType",
                    "errorMessage"
            );

    /**
     * Escribe el resumen completo de una campaña.
     *
     * Si el archivo ya existe se reemplaza.
     *
     * @param outputPath archivo CSV de salida
     * @param records corridas de la campaña
     * @throws IOException si el archivo no puede escribirse
     */
    public void write(
            Path outputPath,
            List<CampaignRunRecord> records
    ) throws IOException {

        Objects.requireNonNull(
                outputPath,
                "Output path cannot be null"
        );

        Objects.requireNonNull(
                records,
                "Campaign records cannot be null"
        );

        validateRecords(
                records
        );

        Path parent =
                outputPath.getParent();

        if (parent != null) {

            Files.createDirectories(
                    parent
            );
        }

        try (BufferedWriter writer =
                     Files.newBufferedWriter(
                             outputPath,
                             StandardCharsets.UTF_8,
                             StandardOpenOption.CREATE,
                             StandardOpenOption.TRUNCATE_EXISTING,
                             StandardOpenOption.WRITE
                     )) {

            writer.write(
                    HEADER
            );

            writer.newLine();

            for (CampaignRunRecord record
                    : records) {

                writer.write(
                        toCsvRow(
                                record
                        )
                );

                writer.newLine();
            }
        }
    }

    /**
     * Convierte una corrida en una fila CSV.
     *
     * SUCCESS e INVALID_RESULT tienen ExperimentRunResult.
     *
     * ERROR no tiene resultado, por lo que las métricas
     * que dependen de una corrida completada quedan vacías.
     */
    private static String toCsvRow(
            CampaignRunRecord record
    ) {

        ExperimentRunConfig config =
                record.getConfig();

        ExperimentRunResult result =
                record.getResult()
                        .orElse(null);

        String admissions =
                result == null
                        ? ""
                        : Integer.toString(
                                result.getAdmissions()
                        );

        String completions =
                result == null
                        ? ""
                        : Integer.toString(
                                result.getCompletions()
                        );

        String it1 =
                result == null
                        ? ""
                        : Integer.toString(
                                result.getIt1Count()
                        );

        String it2 =
                result == null
                        ? ""
                        : Integer.toString(
                                result.getIt2Count()
                        );

        String it3 =
                result == null
                        ? ""
                        : Integer.toString(
                                result.getIt3Count()
                        );

        String totalTInvariants =
                result == null
                        ? ""
                        : Integer.toString(
                                result.getTotalTInvariants()
                        );

        String durationMillis =
                result == null
                        ? ""
                        : Long.toString(
                                result.getDurationMillis()
                        );

        String pInvariantsValid =
                result == null
                        ? ""
                        : Boolean.toString(
                                result.arePInvariantsValid()
                        );

        String tInvariantsValid =
                result == null
                        ? ""
                        : Boolean.toString(
                                result.areTInvariantsValid()
                        );

        String workersTerminated =
                result == null
                        ? ""
                        : Boolean.toString(
                                result.areWorkersTerminated()
                        );

        String successful =
                Boolean.toString(
                        record.isSuccessful()
                );

        String errorType =
                record.getErrorType()
                        .orElse("");

        String errorMessage =
                record.getErrorMessage()
                        .orElse("");

        return String.join(
                SEPARATOR,
                escapeCsv(
                        record.getRunId()
                ),
                escapeCsv(
                        record.getStatus().name()
                ),
                escapeCsv(
                        record.getPolicyMode().name()
                ),
                Long.toString(
                        record.getSeed()
                ),
                escapeCsv(
                        record.getTimingName()
                ),
                Integer.toString(
                        record.getTarget()
                ),
                admissions,
                completions,
                it1,
                it2,
                it3,
                totalTInvariants,
                durationMillis,
                pInvariantsValid,
                tInvariantsValid,
                workersTerminated,
                escapeCsv(
                        config.getLogPath().toString()
                ),
                successful,
                escapeCsv(
                        errorType
                ),
                escapeCsv(
                        errorMessage
                )
        );
    }

    /**
     * Valida que la lista no contenga posiciones nulas.
     *
     * Una campaña vacía sí es válida: en ese caso se genera
     * un CSV únicamente con el header.
     */
    private static void validateRecords(
            List<CampaignRunRecord> records
    ) {

        for (CampaignRunRecord record
                : records) {

            if (record == null) {

                throw new IllegalArgumentException(
                        "Campaign records cannot contain null"
                );
            }
        }
    }

    /**
     * Escaping CSV mínimo y determinista.
     *
     * Se agregan comillas cuando el valor contiene:
     *
     * - coma;
     * - comillas;
     * - salto de línea.
     *
     * Las comillas internas se duplican.
     */
    private static String escapeCsv(
            String value
    ) {

        Objects.requireNonNull(
                value,
                "CSV value cannot be null"
        );

        boolean requiresQuotes =
                value.contains(",")
                        || value.contains("\"")
                        || value.contains("\n")
                        || value.contains("\r");

        if (!requiresQuotes) {

            return value;
        }

        String escaped =
                value.replace(
                        "\"",
                        "\"\""
                );

        return "\""
                + escaped
                + "\"";
    }
}
