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

    /**
     * [DIFFERENT-SEEDS]
     *
     * Verifica que dos políticas inicializadas con seeds distintas
     * puedan producir secuencias pseudoaleatorias diferentes.
     *
     * No se compara una única elección porque dos generadores distintos
     * podrían seleccionar legítimamente la misma transición en una llamada.
     *
     * En cambio, se compara una secuencia suficientemente larga de
     * decisiones sobre el mismo conjunto ordenado de candidatos.
     */
    @Test
    void differentSeedsShouldProduceDifferentSequences() {

        // [ARRANGE]
        Policy firstPolicy =
                new RandomPolicy(100L);

        Policy secondPolicy =
                new RandomPolicy(200L);

        List<Integer> candidates =
                List.of(1, 4, 6);

        List<Integer> firstSequence =
                new ArrayList<>();

        List<Integer> secondSequence =
                new ArrayList<>();

        /*
        * [ACT]
        *
        * Ambas políticas reciben exactamente la misma secuencia
        * de listas de candidatos.
        *
        * La única diferencia entre ellas es la seed.
        */
        for (int i = 0; i < 30; i++) {

            firstSequence.add(
                    firstPolicy
                            .select(candidates)
                            .orElseThrow()
            );

            secondSequence.add(
                    secondPolicy
                            .select(candidates)
                            .orElseThrow()
            );
        }

        // [ASSERT]
        assertNotEquals(
                firstSequence,
                secondSequence,
                "Different seeds should produce different decision sequences"
        );
    }

    /**
     * [DISTRIBUTION-SANITY]
     *
     * Verifica sobre una muestra grande que ninguna de las
     * alternativas quede sistemáticamente favorecida.
     *
     * [IMPORTANT]
     *
     * No se exige una proporción exacta de 33,33 %.
     * El objetivo es detectar sesgos evidentes sin convertir
     * el test en una prueba estadística frágil.
     */
    @Test
    void shouldProduceReasonablyUniformDistribution() {

    // [ARRANGE]
    Policy policy =
            new RandomPolicy(2026L);

    List<Integer> candidates =
            List.of(1, 4, 6);

    int sampleSize = 30_000;

    int transition1Count = 0;
    int transition4Count = 0;
    int transition6Count = 0;

    // [ACT]
    for (int i = 0; i < sampleSize; i++) {

            int selected =
                    policy.select(candidates)
                            .orElseThrow();

            switch (selected) {

            case 1 ->
                    transition1Count++;

            case 4 ->
                    transition4Count++;

            case 6 ->
                    transition6Count++;

            default ->
                    throw new AssertionError(
                            "RandomPolicy selected an invalid candidate: "
                                    + selected
                    );
            }
    }

    /*
    * [ASSERT / REASONABLE-DISTRIBUTION]
    *
    * Para una selección uniforme se espera aproximadamente
    * un tercio de la muestra por candidato.
    *
    * Utilizamos un intervalo deliberadamente amplio
    * [25 %, 42 %] para evitar fragilidad estadística.
    */
    int minimumExpected =
            (int) (sampleSize * 0.25);

    int maximumExpected =
            (int) (sampleSize * 0.42);

    assertTrue(
            transition1Count >= minimumExpected
                    && transition1Count <= maximumExpected,
            "T1 count outside reasonable range: "
                    + transition1Count
    );

    assertTrue(
            transition4Count >= minimumExpected
                    && transition4Count <= maximumExpected,
            "T4 count outside reasonable range: "
                    + transition4Count
    );

    assertTrue(
            transition6Count >= minimumExpected
                    && transition6Count <= maximumExpected,
            "T6 count outside reasonable range: "
                    + transition6Count
    );

    assertEquals(
            sampleSize,
            transition1Count
                    + transition4Count
                    + transition6Count
    );
    }
}