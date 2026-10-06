package config;

import timing.TransitionTimingConfig;

import java.util.Map;

/**
 * [PAYMENT-TIMING-CONFIG]
 *
 * Configuración temporal específica del modelo de pagos.
 *
 * Centraliza los tiempos de las transiciones temporales para evitar
 * valores hardcodeados en Monitor, Workers o experimentos.
 *
 * [BASELINE]
 *
 * T2 = 120 ms
 * T3 =  80 ms
 * T5 = 150 ms
 * T7 = 100 ms
 * T8 = 120 ms
 *
 * [SCALING]
 *
 * También permite construir configuraciones proporcionales a la
 * baseline para futuras pruebas experimentales.
 */
public final class PaymentTimingConfig {

    private static final int TRANSITIONS_COUNT = 10;

    private static final long T2_BASELINE_MS = 120L;
    private static final long T3_BASELINE_MS = 80L;
    private static final long T5_BASELINE_MS = 150L;
    private static final long T7_BASELINE_MS = 100L;
    private static final long T8_BASELINE_MS = 120L;

    private PaymentTimingConfig() {
        /*
         * [UTILITY-CLASS]
         *
         * No requiere instancias.
         */
    }

    /**
     * [BASELINE-CONFIGURATION]
     *
     * Configuración temporal de referencia utilizada
     * por el sistema.
     */
    public static TransitionTimingConfig createBaseline() {

        return createScaled(1.0);
    }

    /**
     * [SCALED-CONFIGURATION]
     *
     * Genera una configuración temporal manteniendo las mismas
     * proporciones de la baseline.
     *
     * Ejemplos:
     *
     * factor = 0.9 -> configuración 10 % más rápida.
     * factor = 1.0 -> baseline.
     * factor = 1.2 -> configuración 20 % más lenta.
     *
     * @param factor factor multiplicativo positivo y finito
     * @return configuración temporal escalada
     */
    public static TransitionTimingConfig createScaled(
            double factor
    ) {

        validateScaleFactor(factor);

        return TransitionTimingConfig.fromMillis(
                TRANSITIONS_COUNT,
                Map.of(
                        2, scaleDelay(T2_BASELINE_MS, factor),
                        3, scaleDelay(T3_BASELINE_MS, factor),
                        5, scaleDelay(T5_BASELINE_MS, factor),
                        7, scaleDelay(T7_BASELINE_MS, factor),
                        8, scaleDelay(T8_BASELINE_MS, factor)
                )
        );
    }

    /**
     * [SCALE]
     *
     * Conserva tiempos enteros en milisegundos.
     *
     * Math.round evita introducir doubles dentro de la
     * configuración temporal utilizada por el Monitor.
     */
    private static long scaleDelay(
            long baselineMillis,
            double factor
    ) {

        long scaledDelay =
                Math.round(
                        baselineMillis * factor
                );

        if (scaledDelay <= 0) {

            throw new IllegalArgumentException(
                    "Scale factor produces a non-positive transition delay"
            );
        }

        return scaledDelay;
    }

    /**
     * [VALIDATION]
     *
     * No se aceptan factores nulos, negativos,
     * NaN ni infinitos.
     */
    private static void validateScaleFactor(
            double factor
    ) {

        if (!Double.isFinite(factor)
                || factor <= 0.0) {

            throw new IllegalArgumentException(
                    "Timing scale factor must be positive and finite"
            );
        }
    }
}