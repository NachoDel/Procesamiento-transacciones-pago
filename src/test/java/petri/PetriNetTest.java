package petri;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * [T2.1.2 - PETRI NET TESTS]
 *
 * Tests unitarios del modelo matemático de la Red de Petri.
 *
 * Objetivo:
 * verificar sensibilización, cálculo del siguiente marcado y disparo
 * antes de integrar PetriNet con el Monitor concurrente.
 *
 * [IMPORTANT]
 * Estos tests NO prueban concurrencia.
 * PetriNet todavía se prueba de forma puramente secuencial.
 */
class PetriNetTest {

    /**
     * [TEST-FIXTURE]
     *
     * Construye una Red de Petri mínima:
     *
     *        T0
     * P0 ----------> P1
     *
     * Marcado inicial:
     *
     * M0 = [1, 0]
     *
     * Matriz de incidencia:
     *
     *       T0
     * P0   -1
     * P1   +1
     *
     * Esta red permite comprobar fácilmente los resultados
     * esperados de forma manual.
     */
    private PetriNet createSimplePetriNet() {

        int[][] incidenceMatrix = {
                {-1},
                { 1}
        };

        int[] initialMarking = {1, 0};

        return new PetriNet(
                incidenceMatrix,
                initialMarking
        );
    }

    /**
     * [TEST 1 - ENABLED TRANSITION]
     *
     * T0 requiere consumir un token de P0.
     *
     * Como el marcado inicial es:
     *
     * P0 = 1
     * P1 = 0
     *
     * T0 debe encontrarse sensibilizada.
     */
    @Test
    void shouldDetectEnabledTransition() {

        // [ARRANGE]
        PetriNet net = createSimplePetriNet();

        // [ACT + ASSERT]
        assertTrue(net.isEnabled(0));
    }

    /**
     * [TEST 2 - NEXT MARKING]
     *
     * Verifica la ecuación de estado para una transición:
     *
     * M' = M + I(:, T0)
     *
     * [1,0] + [-1,+1] = [0,1]
     *
     * calculateNextMarking() debe calcular el nuevo marcado
     * SIN modificar el estado actual de la red.
     */
    @Test
    void shouldCalculateCorrectNextMarkingWithoutChangingCurrentState() {

        // [ARRANGE]
        PetriNet net = createSimplePetriNet();

        // [ACT]
        int[] nextMarking =
                net.calculateNextMarking(0);

        // [ASSERT - CALCULATED STATE]
        assertArrayEquals(
                new int[]{0, 1},
                nextMarking
        );

        /*
         * [ASSERT - CURRENT STATE]
         *
         * calculateNextMarking() es una operación de cálculo,
         * no un disparo.
         *
         * Por lo tanto M actual debe seguir siendo M0.
         */
        assertArrayEquals(
                new int[]{1, 0},
                net.getCurrentMarking()
        );
    }

    /**
     * [TEST 3 - FIRING]
     *
     * Verifica que disparar una transición sensibilizada:
     *
     * 1. retorne true;
     * 2. actualice el marcado;
     * 3. deje la red exactamente en el estado esperado.
     */
    @Test
    void shouldFireEnabledTransition() {

        // [ARRANGE]
        PetriNet net = createSimplePetriNet();

        // [ACT]
        boolean fired = net.fire(0);

        // [ASSERT]
        assertTrue(fired);

        assertArrayEquals(
                new int[]{0, 1},
                net.getCurrentMarking()
        );
    }

    /**
     * [TEST 4 - DISABLED TRANSITION]
     *
     * Una vez disparada T0:
     *
     * M = [0,1]
     *
     * Intentar volver a disparar T0 produciría:
     *
     * P0 = -1
     *
     * Como un marcado no puede contener tokens negativos,
     * T0 debe encontrarse no sensibilizada.
     */
    @Test
    void shouldDetectDisabledTransition() {

        // [ARRANGE]
        PetriNet net = createSimplePetriNet();

        // Primer disparo válido.
        assertTrue(net.fire(0));

        // [ASSERT]
        assertFalse(net.isEnabled(0));
    }

    /**
     * [TEST 5 - INVALID FIRING PRESERVES STATE]
     *
     * Un intento de disparar una transición no sensibilizada
     * NO debe modificar el marcado de la red.
     *
     * Esta propiedad es fundamental porque posteriormente
     * el Monitor dependerá de que un disparo fallido no
     * corrompa el estado compartido.
     */
    @Test
    void invalidFiringShouldNotModifyCurrentMarking() {

        // [ARRANGE]
        PetriNet net = createSimplePetriNet();

        // Primer disparo válido: [1,0] -> [0,1].
        assertTrue(net.fire(0));

        int[] markingBeforeInvalidFire =
                net.getCurrentMarking();

        // [ACT]
        boolean firedAgain = net.fire(0);

        int[] markingAfterInvalidFire =
                net.getCurrentMarking();

        // [ASSERT]
        assertFalse(firedAgain);

        assertArrayEquals(
                markingBeforeInvalidFire,
                markingAfterInvalidFire
        );
    }

    /**
     * [TEST 6 - CONSECUTIVE FIRINGS]
     *
     * Valida la evolución de la red a través de varios disparos.
     *
     * Red utilizada:
     *
     *        T0          T1
     * P0 ----------> P1 ----------> P0
     *
     * Matriz:
     *
     *       T0   T1
     * P0   -1   +1
     * P1   +1   -1
     *
     * Secuencia esperada:
     *
     * [1,0] --T0--> [0,1] --T1--> [1,0]
     */
    @Test
    void shouldSupportConsecutiveTransitions() {

        // [ARRANGE]
        int[][] incidenceMatrix = {
                {-1,  1},
                { 1, -1}
        };

        int[] initialMarking = {1, 0};

        PetriNet net =
                new PetriNet(
                        incidenceMatrix,
                        initialMarking
                );

        // [ASSERT - INITIAL STATE]
        assertArrayEquals(
                new int[]{1, 0},
                net.getCurrentMarking()
        );

        // [ACT - T0]
        assertTrue(net.fire(0));

        // [ASSERT]
        assertArrayEquals(
                new int[]{0, 1},
                net.getCurrentMarking()
        );

        // [ACT - T1]
        assertTrue(net.fire(1));

        // [ASSERT]
        assertArrayEquals(
                new int[]{1, 0},
                net.getCurrentMarking()
        );
    }
}

/* ~~~~~~~~~ INFO ~~~~~~~~~ */

/** 
 * Para correr
 * mvn -Dtest=PetriNetTest test
 */

/**
 * ARRANGE
* Preparar el escenario.
*
* ACT
* Ejecutar la operación que quiero probar.
*
* ASSERT
* Comprobar el resultado.
*/


/*~~~~~~~~~~~~~~~~~~~~~~~~~*/