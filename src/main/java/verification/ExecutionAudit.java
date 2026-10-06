package verification;

import logging.ExecutionLogger;
import monitor.PostFireObserver;

import java.util.Arrays;
import java.util.Objects;

/**
 * [EXECUTION-AUDIT]
 *
 * Integra la verificación online de P-invariantes
 * con el registro de cada disparo confirmado.
 *
 * [ORDER]
 *
 * Para cada disparo exitoso:
 *
 * 1. verifica el marcado comprometido;
 * 2. registra exactamente una entrada de log;
 * 3. si existe una violación, falla con diagnóstico.
 *
 * El log se genera incluso cuando el marcado viola
 * un P-invariante, conservando evidencia del disparo
 * que produjo el estado inválido.
 */
public final class ExecutionAudit
        implements PostFireObserver {

    private final PlaceInvariantChecker invariantChecker;

    private final ExecutionLogger executionLogger;

    public ExecutionAudit(
            PlaceInvariantChecker invariantChecker,
            ExecutionLogger executionLogger
    ) {

        this.invariantChecker =
                Objects.requireNonNull(
                        invariantChecker,
                        "PlaceInvariantChecker cannot be null"
                );

        this.executionLogger =
                Objects.requireNonNull(
                        executionLogger,
                        "ExecutionLogger cannot be null"
                );
    }

    @Override
    public void onSuccessfulFire(
            int transition,
            int[] marking
    ) {

        /*
         * [P-INVARIANTS]
         *
         * Se verifica el mismo marcado comprometido
         * que produjo el disparo.
         */
        PlaceInvariantChecker.VerificationResult result =
                invariantChecker.verify(
                        marking
                );

        /*
         * [EXECUTION-LOG]
         *
         * Todo disparo exitoso genera exactamente
         * una entrada de log.
         */
        executionLogger.logTransition(
                transition
        );

        /*
         * [FAIL-FAST]
         *
         * La violación se informa después de haber dejado
         * evidencia del disparo en el log.
         */
        if (!result.isValid()) {

            throw new IllegalStateException(
                    "Place invariant violation after T"
                            + transition
                            + ": "
                            + result.getViolatedInvariants()
                            + ", marking="
                            + Arrays.toString(
                                    result.getMarking()
                            )
            );
        }
    }
}