package verification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * [T4.1.2 - PLACE INVARIANT CHECKER TESTS]
 *
 * Tests unitarios del verificador de P-invariantes.
 *
 * Objetivo:
 * comprobar que los invariantes estructurales de la Red de Petri
 * sean aceptados para marcados válidos y que las violaciones
 * sean detectadas correctamente.
 */
class PlaceInvariantCheckerTest {

    private final PlaceInvariantChecker checker =
            new PlaceInvariantChecker();

    /**
     * [TEST 1 - INITIAL MARKING]
     *
     * M0 = (3,0,0,0,0,0,0,1,1,0)
     *
     * IP1 = P2 + P3 + P4 + P7 = 1
     * IP2 = P4 + P5 + P6 + P8 = 1
     * IP3 = P0 + P1 + P2 + P3 + P4 + P5 + P6 + P9 = 3
     */
    @Test
    void initialMarkingShouldSatisfyAllPlaceInvariants() {

        int[] marking = {
                3, 0, 0, 0, 0,
                0, 0, 1, 1, 0
        };

        PlaceInvariantChecker.VerificationResult result =
                checker.verify(marking);

        assertTrue(result.isValid());
        assertTrue(result.getViolatedInvariants().isEmpty());
    }

    /**
     * [TEST 2 - VALID REACHABLE MARKING]
     *
     * Marcado resultante luego de disparar T0 una vez:
     *
     * (2,1,0,0,0,0,0,1,1,0)
     *
     * Debe conservar los tres P-invariantes.
     */
    @Test
    void reachableMarkingShouldSatisfyAllPlaceInvariants() {

        int[] marking = {
                2, 1, 0, 0, 0,
                0, 0, 1, 1, 0
        };

        assertTrue(checker.isValid(marking));
    }

    /**
     * [TEST 3 - IP1 VIOLATION]
     *
     * Se elimina el token de P7 respecto de M0.
     *
     * IP1 pasa de:
     *
     * 0 + 0 + 0 + 1 = 1
     *
     * a:
     *
     * 0 + 0 + 0 + 0 = 0
     *
     * IP2 e IP3 permanecen válidos.
     */
    @Test
    void shouldDetectIp1Violation() {

        int[] marking = {
                3, 0, 0, 0, 0,
                0, 0, 0, 1, 0
        };

        PlaceInvariantChecker.VerificationResult result =
                checker.verify(marking);

        assertFalse(result.isValid());
        assertEquals(1, result.getViolatedInvariants().size());
        assertTrue(
                result.getViolatedInvariants()
                        .get(0)
                        .startsWith("IP1")
        );
    }

    /**
     * [TEST 4 - IP2 VIOLATION]
     *
     * Se elimina el token de P8 respecto de M0.
     *
     * IP2 queda con valor 0.
     *
     * IP1 e IP3 permanecen válidos.
     */
    @Test
    void shouldDetectIp2Violation() {

        int[] marking = {
                3, 0, 0, 0, 0,
                0, 0, 1, 0, 0
        };

        PlaceInvariantChecker.VerificationResult result =
                checker.verify(marking);

        assertFalse(result.isValid());
        assertEquals(1, result.getViolatedInvariants().size());
        assertTrue(
                result.getViolatedInvariants()
                        .get(0)
                        .startsWith("IP2")
        );
    }

    /**
     * [TEST 5 - IP3 VIOLATION]
     *
     * Se reduce P0 de 3 a 2 respecto de M0.
     *
     * IP3 queda con valor 2.
     *
     * IP1 e IP2 permanecen válidos.
     */
    @Test
    void shouldDetectIp3Violation() {

        int[] marking = {
                2, 0, 0, 0, 0,
                0, 0, 1, 1, 0
        };

        PlaceInvariantChecker.VerificationResult result =
                checker.verify(marking);

        assertFalse(result.isValid());
        assertEquals(1, result.getViolatedInvariants().size());
        assertTrue(
                result.getViolatedInvariants()
                        .get(0)
                        .startsWith("IP3")
        );
    }

    /**
     * [TEST 6 - NULL MARKING]
     *
     * Un marcado inexistente no puede verificarse.
     */
    @Test
    void shouldRejectNullMarking() {

        assertThrows(
                IllegalArgumentException.class,
                () -> checker.verify(null)
        );
    }

    /**
     * [TEST 7 - INVALID MARKING SIZE]
     *
     * La red oficial contiene exactamente 10 plazas P0..P9.
     */
    @Test
    void shouldRejectMarkingWithInvalidSize() {

        int[] invalidMarking = {
                3, 0, 0
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> checker.verify(invalidMarking)
        );
    }

    /**
     * [TEST 8 - NEGATIVE TOKENS]
     *
     * Un marcado de una Red de Petri no puede contener
     * una cantidad negativa de tokens.
     */
    @Test
    void shouldRejectNegativeTokens() {

        int[] invalidMarking = {
                3, 0, 0, 0, 0,
                0, 0, 1, -1, 0
        };

        assertThrows(
                IllegalArgumentException.class,
                () -> checker.verify(invalidMarking)
        );
    }
}