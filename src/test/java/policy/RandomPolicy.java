package policy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [T3.1.1 - RANDOM POLICY TESTS]
 *
 * Valida que RandomPolicy:
 *
 * - seleccione solamente candidatos válidos;
 * - sea reproducible con seed fija;
 * - no modifique la colección recibida;
 * - maneje correctamente la ausencia de candidatos.
 */
class RandomPolicyTest {

    /**
     * [SINGLE-CANDIDATE]
     *
     * Si existe una única alternativa, debe elegirse siempre,
     * independientemente de la seed.
     */
    @Test
    void shouldSelectOnlyCandidate() {

        // [ARRANGE]
        Policy policy =
                new RandomPolicy(1234L);

        List<Integer> candidates =
                List.of(4);

        // [ACT]
        OptionalInt selected =
                policy.select(candidates);

        // [ASSERT]
        assertTrue(selected.isPresent());
        assertEquals(4, selected.getAsInt());
    }

    /**
     * [VALID-CANDIDATE]
     *
     * La transición seleccionada siempre debe pertenecer
     * al conjunto recibido.
     */
    @Test
    void shouldSelectOneOfAvailableCandidates() {

        // [ARRANGE]
        Policy policy =
                new RandomPolicy(42L);

        List<Integer> candidates =
                List.of(1, 4, 6);

        // [ACT]
        OptionalInt selected =
                policy.select(candidates);

        // [ASSERT]
        assertTrue(selected.isPresent());

        assertTrue(
                candidates.contains(selected.getAsInt())
        );
    }

    /**
     * [REPRODUCIBILITY]
     *
     * Dos políticas construidas con la misma seed y alimentadas
     * con la misma secuencia de candidatos deben producir
     * exactamente la misma secuencia de decisiones.
     */
    @Test
    void sameSeedShouldProduceSameSequence() {

        // [ARRANGE]
        Policy firstPolicy =
                new RandomPolicy(2026L);

        Policy secondPolicy =
                new RandomPolicy(2026L);

        List<Integer> candidates =
                List.of(1, 4, 6);

        // [ACT + ASSERT]
        for (int i = 0; i < 20; i++) {

            OptionalInt firstSelection =
                    firstPolicy.select(candidates);

            OptionalInt secondSelection =
                    secondPolicy.select(candidates);

            assertEquals(
                    firstSelection,
                    secondSelection
            );
        }
    }

    /**
     * [NO-CANDIDATES]
     *
     * Una lista vacía representa que no hay ninguna alternativa
     * disponible para resolver.
     */
    @Test
    void shouldReturnEmptyWhenThereAreNoCandidates() {

        // [ARRANGE]
        Policy policy =
                new RandomPolicy(42L);

        // [ACT]
        OptionalInt selected =
                policy.select(List.of());

        // [ASSERT]
        assertTrue(selected.isEmpty());
    }

    /**
     * [NO-SIDE-EFFECTS]
     *
     * Una política solamente toma una decisión.
     * No debe reordenar, eliminar ni agregar candidatos.
     */
    @Test
    void shouldNotModifyCandidates() {

        // [ARRANGE]
        Policy policy =
                new RandomPolicy(42L);

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
     * null no representa "sin candidatos".
     * Para eso existe una lista vacía.
     */
    @Test
    void shouldRejectNullCandidates() {

        // [ARRANGE]
        Policy policy =
                new RandomPolicy(42L);

        // [ACT + ASSERT]
        assertThrows(
                NullPointerException.class,
                () -> policy.select(null)
        );
    }
}