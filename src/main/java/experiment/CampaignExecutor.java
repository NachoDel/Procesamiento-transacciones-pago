package experiment;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * [T4.2.2 - CAMPAIGN EXECUTOR]
 *
 * Ejecuta secuencialmente una colección de corridas
 * experimentales y mantiene actualizado el resultado
 * consolidado de la campaña.
 *
 * [RESPONSIBILITY]
 *
 * CampaignExecutor:
 *
 * - recibe una lista de ExperimentRunConfig;
 * - ejecuta cada corrida mediante ExperimentRunner;
 * - convierte cada intento en CampaignRunRecord;
 * - conserva corridas exitosas, inválidas y fallidas;
 * - actualiza el CSV consolidado después de cada intento;
 * - devuelve el conjunto completo de registros producidos.
 *
 * No:
 *
 * - construye la matriz experimental;
 * - decide los timings;
 * - calibra configuraciones;
 * - interpreta resultados;
 * - reemplaza silenciosamente corridas fallidas.
 *
 * [PERSISTENCE]
 *
 * El resumen se actualiza después de cada corrida.
 *
 * De esta manera, si la campaña se interrumpe luego de
 * varias ejecuciones, los resultados ya obtenidos quedan
 * persistidos en disco.
 */
public final class CampaignExecutor {

    /**
     * [RUN-EXECUTOR]
     *
     * Abstracción mínima utilizada para desacoplar
     * CampaignExecutor de la implementación concreta
     * de ExperimentRunner durante los tests.
     *
     * En producción se utiliza:
     *
     * ExperimentRunner::run
     */
    @FunctionalInterface
    interface RunExecutor {

        ExperimentRunResult run(
                ExperimentRunConfig config
        ) throws IOException, InterruptedException;
    }

    private final RunExecutor runExecutor;

    private final CampaignSummaryWriter summaryWriter;

    /**
     * Constructor utilizado en producción.
     */
    public CampaignExecutor() {

        ExperimentRunner experimentRunner =
                new ExperimentRunner();

        this.runExecutor =
                experimentRunner::run;

        this.summaryWriter =
                new CampaignSummaryWriter();
    }

    /**
     * Constructor package-private utilizado por tests.
     *
     * Permite ejecutar campañas simuladas sin esperar
     * corridas reales de 20-40 segundos.
     */
    CampaignExecutor(
            RunExecutor runExecutor,
            CampaignSummaryWriter summaryWriter
    ) {

        this.runExecutor =
                Objects.requireNonNull(
                        runExecutor,
                        "Run executor cannot be null"
                );

        this.summaryWriter =
                Objects.requireNonNull(
                        summaryWriter,
                        "Campaign summary writer cannot be null"
                );
    }

    /**
     * Ejecuta todas las configuraciones recibidas.
     *
     * Cada corrida produce exactamente un CampaignRunRecord.
     *
     * Una falla de una corrida individual no detiene
     * automáticamente la campaña.
     *
     * Excepción:
     *
     * si el thread coordinador es interrumpido, se registra
     * la corrida actual como ERROR, se persiste el estado
     * alcanzado y se propaga InterruptedException.
     *
     * @param configs configuraciones a ejecutar
     * @param summaryPath archivo CSV consolidado
     * @return registros producidos durante la campaña
     * @throws IOException si no puede persistirse el resumen
     * @throws InterruptedException si se interrumpe el
     *                              thread coordinador
     */
    public List<CampaignRunRecord> execute(
            List<ExperimentRunConfig> configs,
            Path summaryPath
    ) throws IOException, InterruptedException {

        Objects.requireNonNull(
                configs,
                "Campaign configs cannot be null"
        );

        Objects.requireNonNull(
                summaryPath,
                "Campaign summary path cannot be null"
        );

        validateConfigs(
                configs
        );

        List<CampaignRunRecord> records =
                new ArrayList<>();

        /*
         * [INITIAL-SUMMARY]
         *
         * Creamos el archivo desde el comienzo.
         *
         * Una campaña que todavía no ejecutó ninguna
         * corrida tendrá al menos su header.
         */
        summaryWriter.write(
                summaryPath,
                records
        );

        for (ExperimentRunConfig config
                : configs) {

            CampaignRunRecord record;

            try {

                ExperimentRunResult result =
                        runExecutor.run(
                                config
                        );

                record =
                        CampaignRunRecord.fromResult(
                                config,
                                result
                        );

            } catch (InterruptedException exception) {

                /*
                 * [INTERRUPTION]
                 *
                 * Una interrupción del thread coordinador
                 * no debe convertirse en una falla normal
                 * y continuar con las siguientes corridas.
                 *
                 * Registramos el intento actual, persistimos
                 * la evidencia alcanzada y propagamos
                 * inmediatamente la interrupción.
                 */
                Thread.currentThread()
                        .interrupt();

                record =
                        CampaignRunRecord.fromFailure(
                                config,
                                exception
                        );

                records.add(
                        record
                );

                summaryWriter.write(
                        summaryPath,
                        records
                );

                throw exception;

            } catch (IOException
                     | RuntimeException exception) {

                /*
                 * [RUN-FAILURE]
                 *
                 * Una corrida individual fallida forma parte
                 * de la evidencia experimental.
                 *
                 * Se registra como ERROR y la campaña continúa
                 * con la siguiente configuración.
                 */
                record =
                        CampaignRunRecord.fromFailure(
                                config,
                                exception
                        );
            }

            records.add(
                    record
            );

            /*
             * [INCREMENTAL-PERSISTENCE]
             *
             * Después de cada intento se reemplaza el CSV
             * consolidado por la versión que contiene todos
             * los registros obtenidos hasta el momento.
             *
             * Esto evita perder toda la campaña si existe
             * una interrupción externa posterior.
             */
            summaryWriter.write(
                    summaryPath,
                    records
            );
        }

        return List.copyOf(
                records
        );
    }

    /**
     * Comprueba que la colección no contenga
     * configuraciones nulas.
     *
     * Una lista vacía es válida y genera únicamente
     * el header del archivo consolidado.
     */
    private static void validateConfigs(
            List<ExperimentRunConfig> configs
    ) {

        for (ExperimentRunConfig config
                : configs) {

            if (config == null) {

                throw new IllegalArgumentException(
                        "Campaign configs cannot contain null"
                );
            }
        }
    }
}
