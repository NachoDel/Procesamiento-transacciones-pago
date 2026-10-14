package experiment;

import monitor.PostFireObserver;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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
 * ocurrió.
 *
 * Si uno falla:
 *
 * - se intenta ejecutar igualmente el otro;
 * - se conserva el primer error observado;
 * - luego se propaga la excepción.
 *
 * [ASYNC-FAILURE]
 *
 * El error puede producirse dentro de un Worker.
 * ExperimentRunner necesita poder consultar posteriormente
 * si ocurrió una falla asincrónica.
 */
public final class ExperimentPostFireObserver
        implements PostFireObserver {

    private final PostFireObserver progressObserver;

    private final PostFireObserver auditObserver;

    /**
     * [FIRST-FAILURE]
     *
     * Conserva únicamente el primer error producido durante
     * una acción post-disparo.
     *
     * AtomicReference garantiza visibilidad entre el Worker
     * que produjo el error y el thread encargado de coordinar
     * la corrida experimental.
     */
    private final AtomicReference<RuntimeException> failure =
            new AtomicReference<>();

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

        /*
         * [PROGRESS]
         *
         * El tracker recibe primero el disparo para publicar
         * inmediatamente cambios como el cierre de admisión.
         */
        try {

            progressObserver.onSuccessfulFire(
                    transition,
                    marking
            );

        } catch (RuntimeException exception) {

            firstFailure =
                    exception;
        }

        /*
         * [AUDIT]
         *
         * La auditoría se intenta incluso si el observer
         * de progreso falló.
         *
         * De esta forma no se pierde la evidencia del disparo
         * que efectivamente ocurrió.
         */
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

        /*
         * [FAILURE-PUBLICATION]
         *
         * El primer error queda disponible para que
         * ExperimentRunner pueda detectarlo desde otro thread.
         */
        if (firstFailure != null) {

            failure.compareAndSet(
                    null,
                    firstFailure
            );

            throw firstFailure;
        }
    }

    /**
     * @return true si alguna acción post-disparo falló
     *         durante la corrida.
     */
    public boolean hasFailure() {

        return failure.get() != null;
    }

    /**
     * @return primer error post-disparo observado,
     *         si existe.
     */
    public Optional<RuntimeException> getFailure() {

        return Optional.ofNullable(
                failure.get()
        );
    }
}
