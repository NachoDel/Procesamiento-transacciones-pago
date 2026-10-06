package experiment;

import monitor.PostFireObserver;

import java.util.Objects;

/**
 * [EXPERIMENT-POST-FIRE-OBSERVER]
 *
 * Compone las acciones post-disparo necesarias durante
 * una corrida experimental.
 *
 * Para cada disparo exitoso:
 *
 * 1. actualiza el progreso de la corrida;
 * 2. ejecuta la auditoría de invariantes y logging.
 *
 * [DESIGN]
 *
 * Monitor continúa conociendo solamente PostFireObserver.
 * No necesita conocer RunProgressTracker ni ExecutionAudit.
 *
 * [FAILURE-SEMANTICS]
 *
 * Ambos observers deben recibir todo disparo que efectivamente
 * ocurrió. Si uno falla, se intenta ejecutar igualmente el otro
 * y luego se propaga la excepción.
 */
public final class ExperimentPostFireObserver
        implements PostFireObserver {

    private final PostFireObserver progressObserver;

    private final PostFireObserver auditObserver;

    public ExperimentPostFireObserver(
            PostFireObserver progressObserver,
            PostFireObserver auditObserver
    ) {

        this.progressObserver =
                Objects.requireNonNull(
                        progressObserver,
                        "Progress observer cannot be null"
                );

        this.auditObserver =
                Objects.requireNonNull(
                        auditObserver,
                        "Audit observer cannot be null"
                );
    }

    @Override
    public void onSuccessfulFire(
            int transition,
            int[] marking
    ) {

        RuntimeException firstFailure =
                null;

        try {

            progressObserver.onSuccessfulFire(
                    transition,
                    marking
            );

        } catch (RuntimeException exception) {

            firstFailure =
                    exception;
        }

        try {

            auditObserver.onSuccessfulFire(
                    transition,
                    marking
            );

        } catch (RuntimeException exception) {

            if (firstFailure == null) {

                firstFailure =
                        exception;

            } else {

                firstFailure.addSuppressed(
                        exception
                );
            }
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
    }
}

