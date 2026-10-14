package experiment;

import config.PaymentTimingConfig;
import timing.TransitionTimingConfig;

import java.nio.file.Path;
import java.util.List;

/**
 * [T4.2.2 - FORMAL CAMPAIGN]
 *
 * Punto de entrada de la campaña experimental formal.
 *
 * Matriz:
 *
 * 2 políticas
 * x
 * 2 configuraciones temporales
 * x
 * 5 seeds
 * =
 * 20 corridas
 *
 * Cada corrida procesa exactamente 200 invariantes.
 *
 * [FINAL CALIBRATED VARIANT]
 *
 * Luego de corregir el arbitraje de conflictos del Monitor,
 * las configuraciones temporales fueron recalibradas.
 *
 * VARIANT_SCALE = 1.05
 *
 * Evidencia de calibración definitiva:
 *
 * RANDOM + BASELINE
 * duration = 31.526 s
 *
 * RANDOM + VARIANT(1.05)
 * duration = 33.398 s
 *
 * PRIORITY + BASELINE
 * duration = 36.490 s
 *
 * PRIORITY + VARIANT(1.05)
 * duration = 38.422 s
 *
 * Todas las configuraciones:
 *
 * - completaron 200 invariantes;
 * - mantuvieron P-invariantes válidos;
 * - mantuvieron T-invariantes válidos;
 * - finalizaron correctamente los Workers;
 * - permanecieron dentro del rango requerido de 20 a 40 s.
 *
 * Una vez iniciada la campaña formal este valor NO debe
 * modificarse.
 */
public final class FormalCampaignMain {

    /**
     * Configuración temporal VARIANT congelada
     * para toda la campaña formal definitiva.
     */
    private static final double VARIANT_SCALE =
            1.05;

    /**
     * Archivo consolidado oficial de T4.2.2.
     */
    private static final Path SUMMARY_PATH =
            Path.of(
                    "results",
                    "summary",
                    "formal-campaign.csv"
            );

    private FormalCampaignMain() {
        /*
         * Utility class.
         */
    }

    public static void main(String[] args)
            throws Exception {

        TransitionTimingConfig variantTiming =
                PaymentTimingConfig.createScaled(
                        VARIANT_SCALE
                );

        List<ExperimentRunConfig> configs =
                CampaignPlan.createFormalCampaign(
                        variantTiming
                );

        System.out.println();
        System.out.println(
                "===== FORMAL EXPERIMENTAL CAMPAIGN ====="
        );

        System.out.println(
                "Runs: "
                        + configs.size()
        );

        System.out.println(
                "Target per run: "
                        + CampaignPlan.TARGET
        );

        System.out.println(
                "Variant scale: "
                        + VARIANT_SCALE
        );

        System.out.println(
                "Summary: "
                        + SUMMARY_PATH
        );

        System.out.println();

        printPlan(
                configs
        );

        System.out.println();
        System.out.println(
                "===== STARTING CAMPAIGN ====="
        );

        System.out.println(
                "This execution may take several minutes."
        );

        System.out.println();

        CampaignExecutor executor =
                new CampaignExecutor();

        long campaignStart =
                System.nanoTime();

        List<CampaignRunRecord> records =
                executor.execute(
                        configs,
                        SUMMARY_PATH
                );

        long campaignDurationNanos =
                System.nanoTime()
                        - campaignStart;

        printResult(
                records,
                campaignDurationNanos
        );
    }

    /**
     * Muestra antes de ejecutar la matriz exacta
     * que formará parte de la campaña.
     */
    private static void printPlan(
            List<ExperimentRunConfig> configs
    ) {

        System.out.println(
                "Formal run matrix:"
        );

        for (int index = 0;
             index < configs.size();
             index++) {

            ExperimentRunConfig config =
                    configs.get(index);

            System.out.printf(
                    "  %02d/%02d  %s%n",
                    index + 1,
                    configs.size(),
                    config.getRunId()
            );
        }
    }

    /**
     * Resume el estado final sin realizar todavía
     * interpretación estadística.
     *
     * La interpretación pertenece a T4.2.3.
     */
    private static void printResult(
            List<CampaignRunRecord> records,
            long campaignDurationNanos
    ) {

        long successful =
                records.stream()
                        .filter(
                                record ->
                                        record.getStatus()
                                                == CampaignRunRecord.Status.SUCCESS
                        )
                        .count();

        long invalid =
                records.stream()
                        .filter(
                                record ->
                                        record.getStatus()
                                                == CampaignRunRecord.Status.INVALID_RESULT
                        )
                        .count();

        long errors =
                records.stream()
                        .filter(
                                record ->
                                        record.getStatus()
                                                == CampaignRunRecord.Status.ERROR
                        )
                        .count();

        double campaignSeconds =
                campaignDurationNanos
                        / 1_000_000_000.0;

        System.out.println();
        System.out.println(
                "===== FORMAL CAMPAIGN FINISHED ====="
        );

        System.out.println(
                "Recorded runs: "
                        + records.size()
        );

        System.out.println(
                "SUCCESS: "
                        + successful
        );

        System.out.println(
                "INVALID_RESULT: "
                        + invalid
        );

        System.out.println(
                "ERROR: "
                        + errors
        );

        System.out.printf(
                "Total campaign duration: %.3f s%n",
                campaignSeconds
        );

        System.out.println(
                "Summary: "
                        + SUMMARY_PATH
        );

        System.out.println();

        if (records.size() == 20
                && successful == 20) {

            System.out.println(
                    "FORMAL CAMPAIGN STATUS: SUCCESS"
            );

        } else {

            System.out.println(
                    "FORMAL CAMPAIGN STATUS: REVIEW REQUIRED"
            );
        }
    }
}
