package verification;

import logging.ExecutionLogger;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EXECUTION-AUDIT TESTS]
 *
 * Verifica la composición entre:
 *
 * - PlaceInvariantChecker;
 * - ExecutionLogger.
 */
class ExecutionAuditTest {

    @TempDir
    Path tempDir;

    /**
     * [VALID-FIRING]
     *
     * Un marcado válido debe verificarse y producir
     * exactamente una entrada de log.
     */
    @Test
    void shouldVerifyAndLogSuccessfulFiring()
            throws Exception {

        Path logFile =
                tempDir.resolve(
                        "valid-run.log"
                );

        int[] validMarkingAfterT0 = {
                2, 1, 0, 0, 0,
                0, 0, 1, 1, 0
        };

        try (ExecutionLogger logger =
                     new ExecutionLogger(
                             logFile,
                             "RANDOM",
                             2026L,
                             "test"
                     )) {

            ExecutionAudit audit =
                    new ExecutionAudit(
                            new PlaceInvariantChecker(),
                            logger
                    );

            // [ACT]
            audit.onSuccessfulFire(
                    0,
                    validMarkingAfterT0
            );
        }

        List<String> lines =
                Files.readAllLines(
                        logFile
                );

        // [ASSERT]
        assertEquals(
                1,
                lines.size()
        );

        assertTrue(
                lines.get(0)
                        .contains("|T0|")
        );
    }

    /**
     * [VIOLATION-EVIDENCE]
     *
     * Si un marcado viola un P-invariante:
     *
     * - el disparo debe quedar registrado;
     * - la violación debe reportarse.
     */
    @Test
    void shouldLogBeforeReportingInvariantViolation()
            throws Exception {

        Path logFile =
                tempDir.resolve(
                        "invalid-run.log"
                );

        int[] invalidMarking = {
                3, 0, 0, 0, 0,
                0, 0, 0, 1, 0
        };

        try (ExecutionLogger logger =
                     new ExecutionLogger(
                             logFile,
                             "RANDOM",
                             2026L,
                             "test"
                     )) {

            ExecutionAudit audit =
                    new ExecutionAudit(
                            new PlaceInvariantChecker(),
                            logger
                    );

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () ->
                                    audit.onSuccessfulFire(
                                            0,
                                            invalidMarking
                                    )
                    );

            assertTrue(
                    exception.getMessage()
                            .contains("IP1")
            );
        }

        List<String> lines =
                Files.readAllLines(
                        logFile
                );

        /*
         * El evento que produjo el estado inválido
         * conserva evidencia en el log.
         */
        assertEquals(
                1,
                lines.size()
        );

        assertTrue(
                lines.get(0)
                        .contains("|T0|")
        );
    }
}