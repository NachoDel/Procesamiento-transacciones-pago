package experiment;

import config.PaymentTimingConfig;
import timing.TransitionTimingConfig;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * [T4.2.2 - CAMPAIGN PLAN]
 *
 * Construye la matriz formal de configuraciones que deben
 * ejecutarse durante la campaña experimental.
 *
 * [PROTOCOL]
 *
 * La campaña formal contiene:
 *
 * 2 políticas
 * x
 * 2 configuraciones temporales
 * x
 * 5 seeds
 * =
 * 20 corridas
 *
 * Cada corrida utiliza:
 *
 * TARGET = 200
 *
 * [VARIANT]
 *
 * La configuración temporal VARIANT NO se hardcodea aquí.
 *
 * El protocolo exige calibrarla antes de iniciar la campaña
 * formal y mantenerla fija durante las 20 corridas.
 *
 * Por ese motivo createFormalCampaign(...) recibe la
 * configuración VARIANT ya calibrada.
 */
public final class CampaignPlan {

    public static final int TARGET = 200;

    private static final List<Long> SEEDS =
            List.of(
                    2026L,
                    2027L,
                    2028L,
                    2029L,
                    2030L
            );

    /**
     * Una corrida formal debería encontrarse dentro del
     * rango de 20 a 40 segundos.
     *
     * Se utiliza un margen superior de 60 segundos para
     * detectar falta de progreso sin invalidar una corrida
     * solamente por una pequeña variación temporal.
     */
    private static final Duration EXECUTION_TIMEOUT =
            Duration.ofSeconds(60);

    /**
     * Tiempo máximo destinado exclusivamente al shutdown
     * de los Workers.
     */
    private static final Duration SHUTDOWN_TIMEOUT =
            Duration.ofSeconds(5);

    private static final Path LOG_DIRECTORY =
            Path.of(
                    "results",
                    "logs"
            );

    private CampaignPlan() {
        /*
         * Utility class.
         */
    }

    /**
     * Construye las 20 configuraciones de la campaña formal.
     *
     * Orden utilizado:
     *
     * RANDOM   x BASELINE x seeds
     * RANDOM   x VARIANT  x seeds
     * PRIORITY x BASELINE x seeds
     * PRIORITY x VARIANT  x seeds
     *
     * Este orden coincide con la matriz documentada
     * en docs/protocolo-experimentos.md.
     *
     * @param variantTiming configuración VARIANT ya calibrada
     * @return lista inmutable con exactamente 20 corridas
     */
    public static List<ExperimentRunConfig> createFormalCampaign(
            TransitionTimingConfig variantTiming
    ) {

        Objects.requireNonNull(
                variantTiming,
                "Variant timing config cannot be null"
        );

        TransitionTimingConfig baselineTiming =
                PaymentTimingConfig.createBaseline();

        List<ExperimentRunConfig> configs =
                new ArrayList<>();

        addPolicyTimingRuns(
                configs,
                ExperimentRunConfig.PolicyMode.RANDOM,
                "BASELINE",
                baselineTiming
        );

        addPolicyTimingRuns(
                configs,
                ExperimentRunConfig.PolicyMode.RANDOM,
                "VARIANT",
                variantTiming
        );

        addPolicyTimingRuns(
                configs,
                ExperimentRunConfig.PolicyMode.PRIORITY,
                "BASELINE",
                baselineTiming
        );

        addPolicyTimingRuns(
                configs,
                ExperimentRunConfig.PolicyMode.PRIORITY,
                "VARIANT",
                variantTiming
        );

        return List.copyOf(
                configs
        );
    }

    /**
     * Agrega las cinco seeds oficiales para una combinación
     * determinada de política y configuración temporal.
     */
    private static void addPolicyTimingRuns(
            List<ExperimentRunConfig> configs,
            ExperimentRunConfig.PolicyMode policyMode,
            String timingName,
            TransitionTimingConfig timingConfig
    ) {

        for (long seed : SEEDS) {

            configs.add(
                    createRunConfig(
                            policyMode,
                            timingName,
                            timingConfig,
                            seed
                    )
            );
        }
    }

    /**
     * Construye una configuración individual respetando
     * las convenciones de paths y timeouts de la campaña.
     */
    private static ExperimentRunConfig createRunConfig(
            ExperimentRunConfig.PolicyMode policyMode,
            String timingName,
            TransitionTimingConfig timingConfig,
            long seed
    ) {

        String runId =
                policyMode.name()
                        + "_"
                        + timingName
                        + "_"
                        + seed;

        Path logPath =
                LOG_DIRECTORY.resolve(
                        runId + ".log"
                );

        return new ExperimentRunConfig(
                policyMode,
                seed,
                timingName,
                timingConfig,
                TARGET,
                logPath,
                EXECUTION_TIMEOUT,
                SHUTDOWN_TIMEOUT
        );
    }

    /**
     * Seeds oficiales de la campaña.
     *
     * La lista retornada es inmutable.
     */
    public static List<Long> getSeeds() {
        return SEEDS;
    }

    public static Duration getExecutionTimeout() {
        return EXECUTION_TIMEOUT;
    }

    public static Duration getShutdownTimeout() {
        return SHUTDOWN_TIMEOUT;
    }
}
