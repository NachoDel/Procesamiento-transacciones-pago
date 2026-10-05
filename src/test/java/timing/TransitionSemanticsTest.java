package timing;

import java.util.Set;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [TRANSITION-SEMANTICS TESTS]
 *
 * Verifica la clasificación genérica entre
 * transiciones inmediatas y temporales.
 */
class TransitionSemanticsTest {

    /**
     * [TRANSITION-CLASSIFICATION]
     *
     * Las transiciones declaradas explícitamente como temporales
     * deben distinguirse de las inmediatas.
     */
    @Test
    void shouldClassifyTimedAndImmediateTransitions() {

        // [ARRANGE]
        TransitionSemantics semantics =
                TransitionSemantics.fromTimedTransitions(
                        5,
                        Set.of(1, 3)
                );

        // [ASSERT - TIMED]
        assertTrue(
                semantics.isTimed(1)
        );

        assertTrue(
                semantics.isTimed(3)
        );

        // [ASSERT - IMMEDIATE]
        assertTrue(
                semantics.isImmediate(0)
        );

        assertTrue(
                semantics.isImmediate(2)
        );

        assertFalse(
                semantics.isImmediate(3)
        );
    }

    /**
     * [ALL-IMMEDIATE]
     *
     * Una configuración sin transiciones temporales
     * debe clasificar toda la red como inmediata.
     */
    @Test
    void allImmediateShouldContainNoTimedTransitions() {

        // [ARRANGE]
        TransitionSemantics semantics =
                TransitionSemantics.allImmediate(3);

        // [ASSERT]
        assertTrue(
                semantics.isImmediate(0)
        );

        assertTrue(
                semantics.isImmediate(1)
        );

        assertTrue(
                semantics.isImmediate(2)
        );

        assertFalse(
                semantics.isTimed(0)
        );

        assertFalse(
                semantics.isTimed(1)
        );

        assertFalse(
                semantics.isTimed(2)
        );
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * No debe permitirse configurar como temporal una transición
     * que no exista dentro de la red.
     */
    @Test
    void shouldRejectTimedTransitionOutsideNetworkRange() {

        assertThrows(
                IllegalArgumentException.class,
                () ->
                        TransitionSemantics.fromTimedTransitions(
                                3,
                                Set.of(3)
                        )
        );
    }
}