package config;

import petri.PetriNet;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [PAYMENT-PETRI-NET-CONFIG TESTS]
 *
 * Verifica que la configuración Java reproduzca los datos
 * formales de la Red de Petri oficial.
 */
class PaymentPetriNetConfigTest {

    /**
     * [INITIAL-MARKING]
     *
     * Verifica el marcado inicial definitivo del modelo.
     */
    @Test
    void shouldCreateOfficialInitialMarking() {

        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        assertArrayEquals(
                new int[]{
                        3, 0, 0, 0, 0,
                        0, 0, 1, 1, 0
                },
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [MODEL-SIZE]
     *
     * La red oficial contiene diez plazas
     * y diez transiciones.
     */
    @Test
    void shouldContainTenPlacesAndTenTransitions() {

        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        assertEquals(
                10,
                petriNet.getPlacesCount()
        );

        assertEquals(
                10,
                petriNet.getTransitionsCount()
        );
    }

    /**
     * [T-INVARIANT - CARD]
     *
     * IT1:
     * T0 -> T1 -> T2 -> T3 -> T9
     *
     * Un ciclo completo debe devolver la red a M0.
     */
    @Test
    void cardFlowShouldReturnToInitialMarking() {

        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        assertTrue(petriNet.fire(0));
        assertTrue(petriNet.fire(1));
        assertTrue(petriNet.fire(2));
        assertTrue(petriNet.fire(3));
        assertTrue(petriNet.fire(9));

        assertArrayEquals(
                petriNet.getInitialMarking(),
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [T-INVARIANT - HIGH-RISK]
     *
     * IT2:
     * T0 -> T4 -> T5 -> T9
     */
    @Test
    void highRiskFlowShouldReturnToInitialMarking() {

        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        assertTrue(petriNet.fire(0));
        assertTrue(petriNet.fire(4));
        assertTrue(petriNet.fire(5));
        assertTrue(petriNet.fire(9));

        assertArrayEquals(
                petriNet.getInitialMarking(),
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [T-INVARIANT - BANK-TRANSFER]
     *
     * IT3:
     * T0 -> T6 -> T7 -> T8 -> T9
     */
    @Test
    void bankTransferFlowShouldReturnToInitialMarking() {

        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        assertTrue(petriNet.fire(0));
        assertTrue(petriNet.fire(6));
        assertTrue(petriNet.fire(7));
        assertTrue(petriNet.fire(8));
        assertTrue(petriNet.fire(9));

        assertArrayEquals(
                petriNet.getInitialMarking(),
                petriNet.getCurrentMarking()
        );
    }

    /**
     * [TRANSITION-CLASSIFICATION]
     *
     * Verifica la clasificación formal utilizada luego
     * para implementar la semántica temporal.
     */
    @Test
    void shouldClassifyTimedAndImmediateTransitions() {

        assertTrue(
                PaymentPetriNetConfig.isTimedTransition(2)
        );

        assertTrue(
                PaymentPetriNetConfig.isTimedTransition(3)
        );

        assertTrue(
                PaymentPetriNetConfig.isTimedTransition(5)
        );

        assertTrue(
                PaymentPetriNetConfig.isTimedTransition(7)
        );

        assertTrue(
                PaymentPetriNetConfig.isTimedTransition(8)
        );

        assertFalse(
                PaymentPetriNetConfig.isTimedTransition(9)
        );

        assertTrue(
                PaymentPetriNetConfig.isImmediateTransition(9)
        );

        assertTrue(
                PaymentPetriNetConfig.isImmediateTransition(0)
        );
    }
}