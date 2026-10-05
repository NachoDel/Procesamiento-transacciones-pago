package config;

import org.junit.jupiter.api.Test;

import timing.TransitionTimingConfig;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * [PAYMENT-TIMING-CONFIG TESTS]
 *
 * Verifica el escenario temporal base utilizado
 * para la Red de Petri oficial.
 */
class PaymentTimingConfigTest {

    @Test
    void shouldDefineBaselineOnlyForTimedTransitions() {

        TransitionTimingConfig timing =
                PaymentTimingConfig.createBaseline();

        /*
         * [TIMED-TRANSITIONS]
         */
        assertEquals(
                120L,
                timing.getDelayMillis(2)
        );

        assertEquals(
                80L,
                timing.getDelayMillis(3)
        );

        assertEquals(
                150L,
                timing.getDelayMillis(5)
        );

        assertEquals(
                100L,
                timing.getDelayMillis(7)
        );

        assertEquals(
                120L,
                timing.getDelayMillis(8)
        );

        /*
         * [IMMEDIATE-TRANSITIONS]
         *
         * T0, T1, T4, T6 y T9 no poseen delay.
         */
        assertEquals(
                0L,
                timing.getDelayMillis(0)
        );

        assertEquals(
                0L,
                timing.getDelayMillis(1)
        );

        assertEquals(
                0L,
                timing.getDelayMillis(4)
        );

        assertEquals(
                0L,
                timing.getDelayMillis(6)
        );

        assertEquals(
                0L,
                timing.getDelayMillis(9)
        );
    }
}