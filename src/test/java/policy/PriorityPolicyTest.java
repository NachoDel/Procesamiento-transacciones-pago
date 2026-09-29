package policy;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T3.1.2 - PRIORITY POLICY TESTS]
 *
 * Verifica la política de prioridad utilizada para favorecer
 * el flujo de pago de alto riesgo.
 */
class PriorityPolicyTest {

    /**
     * [STRICT-PRIORITY]
     *
     * Si la transición prioritaria forma parte de los candidatos,
     * debe ser seleccionada independientemente del fallback.
     */
    @Test
    void shouldSelectPriorityTransitionWhenAvailable() {

        // [ARRANGE]
        Policy fallback =
                new FixedPolicy(1);

        Policy policy =
                new PriorityPolicy(
                        4,
                        fallback
                );

        List<Integer> candidates =
                List.of(1, 4, 6);

        // [ACT]
        OptionalInt selected =
                policy.select(candidates);

        // [ASSERT]
        assertTrue(selected.isPresent());

        assertEquals(
                4,
                selected.getAsInt()
        );
    }

    /**
     * [FALLBACK]
     *
     * Si la transición prioritaria no está disponible,
     * la decisión debe delegarse a la Policy de fallback.
     */
    @Test
    void shouldUseFallbackWhenPriorityTransitionIsNotAvailable() {

        // [ARRANGE]
        Policy fallback =
                new FixedPolicy(6);

        Policy policy =
                new PriorityPolicy(
                        4,
                        fallback
                );

        List<Integer> candidates =
                List.of(1, 6);

        // [ACT]
        OptionalInt selected =
                policy.select(candidates);

        // [ASSERT]
        assertTrue(selected.isPresent());

        assertEquals(
                6,
                selected.getAsInt()
        );
    }

    /**
     * [NO-CANDIDATES]
     *
     * Si no existen alternativas, no hay ninguna
     * transición que pueda seleccionarse.
     */
    @Test
    void shouldReturnEmptyWhenThereAreNoCandidates() {

        // [ARRANGE]
        Policy policy =
                new PriorityPolicy(
                        4,
                        new FixedPolicy(1)
                );

        // [ACT]
        OptionalInt selected =
                policy.select(List.of());

        // [ASSERT]
        assertTrue(selected.isEmpty());
    }

    /**
     * [NO-SIDE-EFFECTS]
     *
     * La política no debe modificar la colección
     * de candidatos recibida.
     */
    @Test
    void shouldNotModifyCandidates() {

        // [ARRANGE]
        Policy policy =
                new PriorityPolicy(
                        4,
                        new FixedPolicy(1)
                );

        List<Integer> candidates =
                new ArrayList<>(
                        List.of(1, 4, 6)
                );

        List<Integer> originalCandidates =
                new ArrayList<>(candidates);

        // [ACT]
        policy.select(candidates);

        // [ASSERT]
        assertEquals(
                originalCandidates,
                candidates
        );
    }

    /**
     * [INPUT-VALIDATION]
     *
     * null no representa ausencia de candidatos.
     * Para ello se utiliza una lista vacía.
     */
    @Test
    void shouldRejectNullCandidates() {

        // [ARRANGE]
        Policy policy =
                new PriorityPolicy(
                        4,
                        new FixedPolicy(1)
                );

        // [ACT + ASSERT]
        assertThrows(
                NullPointerException.class,
                () -> policy.select(null)
        );
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * Una transición se identifica mediante un índice
     * no negativo.
     */
    @Test
    void shouldRejectNegativePriorityTransition() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new PriorityPolicy(
                        -1,
                        new FixedPolicy(0)
                )
        );
    }

    /**
     * [TEST-DOUBLE / FIXED-POLICY]
     *
     * Policy determinista utilizada exclusivamente
     * para controlar el resultado del fallback.
     */
    private static class FixedPolicy implements Policy {

        private final int selectedTransition;

        FixedPolicy(int selectedTransition) {
            this.selectedTransition =
                    selectedTransition;
        }

        @Override
        public OptionalInt select(List<Integer> candidates) {

            if (!candidates.contains(selectedTransition)) {
                return OptionalInt.empty();
            }

            return OptionalInt.of(
                    selectedTransition
            );
        }
    }
}