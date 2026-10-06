package verification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T4.1.3 - T-INVARIANT ANALYZER TESTS]
 *
 * Verifica:
 *
 * - reconocimiento de IT1;
 * - reconocimiento de IT2;
 * - reconocimiento de IT3;
 * - ejecución concurrente con interleaving;
 * - secuencias incompletas;
 * - secuencias inválidas;
 * - numeración incorrecta del log;
 * - formato inválido.
 */
class TInvariantAnalyzerTest {

    private final TInvariantAnalyzer analyzer =
            new TInvariantAnalyzer();

    /**
     * [IT1]
     *
     * T0 T1 T2 T3 T9
     */
    @Test
    void shouldRecognizeIt1() {

        List<String> log = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H1", "T1"),
                logLine(3, "H1", "T2"),
                logLine(4, "H1", "T3"),
                logLine(5, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                analyzer.analyzeLines(log);

        assertTrue(result.isValid());

        assertEquals(1, result.getIt1Count());
        assertEquals(0, result.getIt2Count());
        assertEquals(0, result.getIt3Count());

        assertEquals(1, result.getTotalCompleted());

        assertEquals(1, result.getT0Count());
        assertEquals(1, result.getT9Count());

        assertTrue(result.getIssues().isEmpty());
    }

    /**
     * [IT2]
     *
     * T0 T4 T5 T9
     */
    @Test
    void shouldRecognizeIt2() {

        List<String> log = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H2", "T4"),
                logLine(3, "H2", "T5"),
                logLine(4, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                analyzer.analyzeLines(log);

        assertTrue(result.isValid());

        assertEquals(0, result.getIt1Count());
        assertEquals(1, result.getIt2Count());
        assertEquals(0, result.getIt3Count());

        assertEquals(1, result.getTotalCompleted());
    }

    /**
     * [IT3]
     *
     * T0 T6 T7 T8 T9
     */
    @Test
    void shouldRecognizeIt3() {

        List<String> log = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H3", "T6"),
                logLine(3, "H3", "T7"),
                logLine(4, "H3", "T8"),
                logLine(5, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                analyzer.analyzeLines(log);

        assertTrue(result.isValid());

        assertEquals(0, result.getIt1Count());
        assertEquals(0, result.getIt2Count());
        assertEquals(1, result.getIt3Count());

        assertEquals(1, result.getTotalCompleted());
    }

    /**
     * [INTERLEAVING]
     *
     * Dos invariantes se ejecutan concurrentemente:
     *
     * IT1 = T0 T1 T2 T3 T9
     * IT3 = T0 T6 T7 T8 T9
     *
     * En el log global las transiciones aparecen intercaladas.
     */
    @Test
    void shouldRecognizeInterleavedInvariants() {

        List<String> log = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H0", "T0"),

                logLine(3, "H1", "T1"),
                logLine(4, "H3", "T6"),

                logLine(5, "H1", "T2"),
                logLine(6, "H3", "T7"),

                logLine(7, "H1", "T3"),
                logLine(8, "H4", "T9"),

                logLine(9, "H3", "T8"),
                logLine(10, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                analyzer.analyzeLines(log);

        assertTrue(result.isValid());

        assertEquals(1, result.getIt1Count());
        assertEquals(0, result.getIt2Count());
        assertEquals(1, result.getIt3Count());

        assertEquals(2, result.getTotalCompleted());

        assertEquals(2, result.getT0Count());
        assertEquals(2, result.getT9Count());
    }

    /**
     * [INCOMPLETE INVARIANT]
     *
     * IT1 comienza correctamente pero termina antes de T3.
     *
     * H1:
     *
     * T1 T2
     */
    @Test
    void shouldDetectIncompleteInvariant() {

        List<String> log = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H1", "T1"),
                logLine(3, "H1", "T2")
        );

        TInvariantAnalyzer.AnalysisResult result =
                analyzer.analyzeLines(log);

        assertFalse(result.isValid());

        assertEquals(0, result.getIt1Count());
        assertEquals(0, result.getTotalCompleted());

        assertTrue(
                result.getIssues()
                        .stream()
                        .anyMatch(issue ->
                                issue.contains(
                                        "Incomplete IT1"
                                )
                        )
        );
    }

    /**
     * [INVALID ORDER]
     *
     * Las transiciones de H1 aparecen fuera del orden
     * establecido para IT1.
     *
     * Esperado:
     *
     * T1 T2 T3
     *
     * Recibido:
     *
     * T2 T1 T3
     */
    @Test
    void shouldDetectTransitionOutOfSequence() {

        List<String> log = List.of(
                logLine(1, "H0", "T0"),
                logLine(2, "H1", "T2"),
                logLine(3, "H1", "T1"),
                logLine(4, "H1", "T3"),
                logLine(5, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                analyzer.analyzeLines(log);

        assertFalse(result.isValid());

        assertEquals(0, result.getIt1Count());

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
     * [LOG SEQUENCE]
     *
     * ExecutionLogger produce:
     *
     * 1, 2, 3, 4...
     *
     * Si falta un número, el log no representa
     * correctamente el contrato definido en T4.1.1.
     */
    @Test
    void shouldDetectInvalidEventSequence() {

        List<String> log = List.of(
                logLine(1, "H0", "T0"),

                // Se saltea el evento 2.
                logLine(3, "H2", "T4"),

                logLine(4, "H2", "T5"),
                logLine(5, "H4", "T9")
        );

        TInvariantAnalyzer.AnalysisResult result =
                analyzer.analyzeLines(log);

        assertFalse(result.isValid());

        assertTrue(
                result.getIssues()
                        .stream()
                        .anyMatch(issue ->
                                issue.contains(
                                        "Invalid event sequence"
                                )
                        )
        );
    }

    /**
     * [INVALID LOG FORMAT]
     *
     * T4.1.1 define exactamente siete campos:
     *
     * SEQUENCE|THREAD|TRANSITION|TIMESTAMP|POLICY|SEED|CONFIG
     */
    @Test
    void shouldRejectMalformedLogLine() {

        List<String> invalidLog = List.of(
                "1|H0|T0"
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> analyzer.analyzeLines(
                        invalidLog
                )
        );
    }

    /**
     * Genera una línea válida según el contrato definido
     * por T4.1.1.
     *
     * Los valores no relevantes para estos tests se mantienen
     * constantes.
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