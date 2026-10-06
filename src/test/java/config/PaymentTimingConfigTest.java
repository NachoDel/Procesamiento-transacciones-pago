package config;

import org.junit.jupiter.api.Test;
import timing.TransitionTimingConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [PAYMENT-TIMING-CONFIG TESTS]
 *
 * Verifica:
 *
 * - valores de la configuración baseline;
 * - clasificación temporal esperada;
 * - escalado proporcional;
 * - validación de factores inválidos.
 */
class PaymentTimingConfigTest {

    @Test
    void shouldCreateBaselineConfiguration() {

        TransitionTimingConfig config =
                PaymentTimingConfig.createBaseline();

        assertEquals(
                10,
                config.getTransitionsCount()
        );

        assertEquals(
                120L,
                config.getDelayMillis(2)
        );

        assertEquals(
                80L,
                config.getDelayMillis(3)
        );

        assertEquals(
                150L,
                config.getDelayMillis(5)
        );

        assertEquals(
                100L,
                config.getDelayMillis(7)
        );

        assertEquals(
                120L,
                config.getDelayMillis(8)
        );
    }

    /**
     * [TEMPORAL-SCOPE]
     *
     * Solamente T2, T3, T5, T7 y T8
     * deben poseer demora.
     */
    @Test
    void shouldConfigureOnlyTimedTransitions() {

        TransitionTimingConfig config =
                PaymentTimingConfig.createBaseline();

        assertFalse(config.hasDelay(0));
        assertFalse(config.hasDelay(1));

        assertTrue(config.hasDelay(2));
        assertTrue(config.hasDelay(3));

        assertFalse(config.hasDelay(4));

        assertTrue(config.hasDelay(5));

        assertFalse(config.hasDelay(6));

        assertTrue(config.hasDelay(7));
        assertTrue(config.hasDelay(8));

        assertFalse(config.hasDelay(9));
    }

    /**
     * [FASTER-CONFIGURATION]
     *
     * factor = 0.9
     */
    @Test
    void shouldCreateScaledFasterConfiguration() {

        TransitionTimingConfig config =
                PaymentTimingConfig.createScaled(
                        0.9
                );

        assertEquals(
                108L,
                config.getDelayMillis(2)
        );

        assertEquals(
                72L,
                config.getDelayMillis(3)
        );

        assertEquals(
                135L,
                config.getDelayMillis(5)
        );

        assertEquals(
                90L,
                config.getDelayMillis(7)
        );

        assertEquals(
                108L,
                config.getDelayMillis(8)
        );
    }

    /**
     * [SLOWER-CONFIGURATION]
     *
     * factor = 1.2
     */
    @Test
    void shouldCreateScaledSlowerConfiguration() {

        TransitionTimingConfig config =
                PaymentTimingConfig.createScaled(
                        1.2
                );

        assertEquals(
                144L,
                config.getDelayMillis(2)
        );

        assertEquals(
                96L,
                config.getDelayMillis(3)
        );

        assertEquals(
                180L,
                config.getDelayMillis(5)
        );

        assertEquals(
                120L,
                config.getDelayMillis(7)
        );

        assertEquals(
                144L,
                config.getDelayMillis(8)
        );
    }

    /**
     * [INVALID-SCALE]
     *
     * Un factor inválido no debe producir
     * una configuración temporal.
     */
    @Test
    void shouldRejectInvalidScaleFactors() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaymentTimingConfig.createScaled(
                                0.0
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaymentTimingConfig.createScaled(
                                -1.0
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaymentTimingConfig.createScaled(
                                Double.NaN
                        )
        );

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PaymentTimingConfig.createScaled(
                                Double.POSITIVE_INFINITY
                        )
        );
    }
}