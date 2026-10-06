package verification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T4.1.4 - NEGATIVE VERIFICATION TESTS]
 *
 * Pruebas negativas deliberadas sobre los mecanismos
 * de verificación formal implementados en:
 *
 * - T4.1.2 -> PlaceInvariantChecker
 * - T4.1.3 -> TInvariantAnalyzer
 *
 * [PURPOSE]
 *
 * Estos tests introducen errores de forma intencional para
 * demostrar que los verificadores no solamente aceptan casos
 * correctos, sino que también detectan violaciones.
 *
 * Los errores existen únicamente dentro de los tests.
 * No se modifica la lógica de producción.
 */
class VerificationNegativeTest {

    private final PlaceInvariantChecker placeInvariantChecker =
            new PlaceInvariantChecker();

    private final TInvariantAnalyzer tInvariantAnalyzer =
            new TInvariantAnalyzer();

    /**
     * [NEGATIVE TEST - P-INVARIANT]
     *
     * Partimos conceptualmente del marcado inicial:
     *
     * M0 = (3,0,0,0,0,0,0,1,1,0)
     *
     * y eliminamos deliberadamente el token de P7:
     *
     * M' = (3,0,0,0,0,0,0,0,1,0)
     *
     * Esto viola:
     *
     * IP1 = P2 + P3 + P4 + P7 = 1
     *
     * porque:
     *
     * 0 + 0 + 0 + 0 = 0
     */
    @Test
    void shouldDetectInjectedPlaceInvariantViolation() {

        int[] invalidMarking = {
                3, 0, 0, 0, 0,
                0, 0, 0, 1, 0
        };

        PlaceInvariantChecker.VerificationResult result =
                placeInvariantChecker.verify(
                        invalidMarking
                );

        assertFalse(
                result.isValid()
        );

        assertEquals(
                1,
                result.getViolatedInvariants().size()
        );

        assertTrue(
                result.getViolatedInvariants()
                        .get(0)
                        .startsWith("IP1")
        );
    }

    /**
     * [NEGATIVE TEST - MISSING TRANSITION]
     *
     * IT1 correcto:
     *
     * T0 -> T1 -> T2 -> T3 -> T9
     *
     * Se elimina deliberadamente T2:
     *
     * T0 -> T1 -> T3 -> T9
     *
     * La secuencia de H1 deja de corresponder al patrón
     * definido para IT1.
     */
    @Test
    void shouldDetectMissingTransitionInTInvariant() {

        List<String> invalidLog = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H1", "T1"),

                // T2 fue eliminada deliberadamente.

                logLine(3, "H1", "T3"),
                logLine(4, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                tInvariantAnalyzer.analyzeLines(
                        invalidLog
                );

        assertFalse(
                result.isValid()
        );

        assertEquals(
                0,
                result.getIt1Count()
        );

        assertTrue(
                result.getIssues()
                        .stream()
                        .anyMatch(issue ->
                                issue.contains(
                                        "Invalid IT1"
                                )
                        )
        );
    }

    /**
     * [NEGATIVE TEST - OUT OF SEQUENCE]
     *
     * IT1 espera:
     *
     * T1 -> T2 -> T3
     *
     * Se inyecta deliberadamente:
     *
     * T2 -> T1 -> T3
     *
     * Las mismas transiciones están presentes,
     * pero su orden es incorrecto.
     */
    @Test
    void shouldDetectTransitionOutsideExpectedOrder() {

        List<String> invalidLog = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H1", "T2"),
                logLine(3, "H1", "T1"),
                logLine(4, "H1", "T3"),
                logLine(5, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                tInvariantAnalyzer.analyzeLines(
                        invalidLog
                );

        assertFalse(
                result.isValid()
        );

        assertEquals(
                0,
                result.getIt1Count()
        );

        assertTrue(
                result.getIssues()
                        .stream()
                        .anyMatch(issue ->
                                issue.contains(
                                        "Invalid IT1"
                                )
                        )
        );
    }

    /**
     * [NEGATIVE TEST - INCOMPLETE INVARIANT]
     *
     * IT3 completo requiere:
     *
     * T0 -> T6 -> T7 -> T8 -> T9
     *
     * Se finaliza deliberadamente el log cuando H3
     * solamente ejecutó:
     *
     * T6 -> T7
     *
     * Es un prefijo válido, pero el T-invariante no
     * llegó a completarse.
     */
    @Test
    void shouldDetectIncompleteTInvariant() {

        List<String> incompleteLog = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H3", "T6"),
                logLine(3, "H3", "T7")
        );

        TInvariantAnalyzer.AnalysisResult result =
                tInvariantAnalyzer.analyzeLines(
                        incompleteLog
                );

        assertFalse(
                result.isValid()
        );

        assertEquals(
                0,
                result.getIt3Count()
        );

        assertEquals(
                0,
                result.getTotalCompleted()
        );

        assertTrue(
                result.getIssues()
                        .stream()
                        .anyMatch(issue ->
                                issue.contains(
                                        "Incomplete IT3"
                                )
                        )
        );
    }

    /**
     * Genera una entrada válida según el contrato de T4.1.1:
     *
     * SEQUENCE|THREAD|TRANSITION|TIMESTAMP|POLICY|SEED|CONFIG
     */
    private static String logLine(
            long sequence,
            String thread,
            String transition
    ) {

        return sequence
                + "|"
                + thread
                + "|"
                + transition
                + "|"
                + (1000L + sequence)
                + "|PRIORITY"
                + "|2026"
                + "|timing=A";
    }
}