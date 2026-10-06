package timing;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [TRANSITION-TIMING-CONFIG TESTS]
 *
 * Verifica la configuración genérica de tiempos
 * asociados a transiciones.
 */
class TransitionTimingConfigTest {

    /**
     * [CONFIGURED-DELAYS]
     *
     * Los tiempos configurados deben recuperarse
     * exactamente en milisegundos.
     */
    @Test
    void shouldReturnConfiguredDelays() {

        TransitionTimingConfig timing =
                TransitionTimingConfig.fromMillis(
                        5,
                        Map.of(
                                1, 100L,
                                3, 250L
                        )
                );

        assertEquals(
                100L,
                timing.getDelayMillis(1)
        );

        assertEquals(
                250L,
                timing.getDelayMillis(3)
        );

        assertTrue(
                timing.hasDelay(1)
        );

        assertTrue(
                timing.hasDelay(3)
        );
    }

    /**
     * [UNCONFIGURED-TRANSITION]
     *
     * Una transición sin tiempo configurado
     * debe poseer delay cero.
     */
    @Test
    void unconfiguredTransitionShouldHaveZeroDelay() {

        TransitionTimingConfig timing =
                TransitionTimingConfig.fromMillis(
                        3,
                        Map.of(
                                1, 100L
                        )
                );

        assertEquals(
                0L,
                timing.getDelayMillis(0)
        );

        assertFalse(
                timing.hasDelay(0)
        );
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * Un tiempo temporal debe ser estrictamente positivo.
     */
    @Test
    void shouldRejectNonPositiveDelay() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TransitionTimingConfig.fromMillis(
                                3,
                                Map.of(
                                        1, 0L
                                )
                        )
        );
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * No puede configurarse una transición inexistente.
     */
    @Test
    void shouldRejectTransitionOutsideNetworkRange() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TransitionTimingConfig.fromMillis(
                                3,
                                Map.of(
                                        3, 100L
                                )
                        )
        );
    }

    /**
     * [DEFENSIVE-COPY]
     *
     * Cambios posteriores sobre el Map original
     * no deben modificar la configuración construida.
     */
    @Test
    void shouldDefensivelyCopyConfiguredDelays() {

        Map<Integer, Long> delays =
                new HashMap<>();

        delays.put(
                1,
                100L
        );

        TransitionTimingConfig timing =
                TransitionTimingConfig.fromMillis(
                        3,
                        delays
                );

        /*
         * Modificamos la colección original después
         * de crear la configuración.
         */
        delays.put(
                1,
                999L
        );

        assertEquals(
                100L,
                timing.getDelayMillis(1)
        );
    }
}