package config;

import timing.TransitionTimingConfig;

import java.util.Map;

/**
 * [PAYMENT-TIMING-CONFIGURATION]
 *
 * Define una configuración temporal inicial para
 * la Red de Petri del sistema de pagos.
 *
 * [BASELINE]
 *
 * Estos valores constituyen el escenario temporal base.
 * No son valores definitivos del análisis experimental:
 * posteriormente serán variados y comparados.
 */
public final class PaymentTimingConfig {

    /*
     * [BASELINE-TIMES]
     *
     * T2 -> autorización de tarjeta
     * T3 -> captura de fondos
     * T5 -> procesamiento de alto riesgo
     * T7 -> validación de transferencia
     * T8 -> ejecución de transferencia
     */
    private static final Map<Integer, Long> BASELINE_DELAYS_MS =
            Map.of(
                    2, 120L,
                    3, 80L,
                    5, 150L,
                    7, 100L,
                    8, 120L
            );

    private PaymentTimingConfig() {
        /*
         * [UTILITY-CLASS]
         */
    }

    /**
     * [BASELINE-CONFIG]
     *
     * Construye una nueva configuración temporal
     * para las diez transiciones de la red oficial.
     */
    public static TransitionTimingConfig createBaseline() {

        return TransitionTimingConfig.fromMillis(
                PaymentPetriNetConfig.TRANSITIONS_COUNT,
                BASELINE_DELAYS_MS
        );
    }
}