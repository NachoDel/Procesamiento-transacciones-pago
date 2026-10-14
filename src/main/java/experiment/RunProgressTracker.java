package experiment;

import monitor.PostFireObserver;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * [RUN-PROGRESS-TRACKER]
 *
 * Mantiene el progreso online de una corrida experimental.
 *
 * [RUNTIME-RULE]
 *
 * T0 representa la admisión de una nueva transacción.
 * T9 representa la finalización de una transacción.
 *
 * El tracker no controla Workers ni conoce WorkerCoordinator.
 * Solamente observa disparos exitosos.
 *
 * [TARGET]
 *
 * Para una corrida con target N:
 *
 * - la admisión se considera cerrada cuando T0 fue disparada N veces;
 * - la corrida se considera drenada cuando T9 fue disparada N veces.
 *
 * [THREAD-SAFETY]
 *
 * PostFireObserver es invocado por Monitor dentro de su sección
 * crítica, pero el estado también será leído desde el thread
 * encargado de coordinar la corrida.
 *
 * AtomicInteger garantiza visibilidad entre threads sin acoplar
 * este componente al lock interno de Monitor.
 */
public final class RunProgressTracker
        implements PostFireObserver {

    /**
     * [DOMAIN-BOUNDARIES]
     *
     * T0 abre una nueva operación lógica.
     * T9 completa una operación lógica.
     */
    private static final int ADMISSION_TRANSITION = 0;

    private static final int COMPLETION_TRANSITION = 9;

    private final int target;

    private final AtomicInteger admissions =
            new AtomicInteger(0);

    private final AtomicInteger completions =
            new AtomicInteger(0);

    public RunProgressTracker(
            int target
    ) {

        if (target <= 0) {
            throw new IllegalArgumentException(
                    "Target must be positive"
            );
        }

        this.target =
                target;
    }

    /**
     * [POST-FIRE-PROGRESS]
     *
     * Se ejecuta exclusivamente después de un disparo
     * confirmado por Monitor.
     *
     * El marcado no es necesario para el conteo:
     * la validez estructural del estado pertenece a
     * PlaceInvariantChecker / ExecutionAudit.
     */
    @Override
    public void onSuccessfulFire(
            int transition,
            int[] marking
    ) {

        if (transition
                == ADMISSION_TRANSITION) {

            int currentAdmissions =
                    admissions.incrementAndGet();

            /*
             * [DEFENSIVE-GUARD]
             *
             * El stop específico de H0 debe impedir que
             * esto ocurra durante una corrida válida.
             *
             * Si sucede, se informa inmediatamente porque
             * ya no puede garantizarse exactamente TARGET.
             */
            if (currentAdmissions > target) {

                throw new IllegalStateException(
                        "Admission target exceeded: target="
                                + target
                                + ", admissions="
                                + currentAdmissions
                );
            }

            return;
        }

        if (transition
                == COMPLETION_TRANSITION) {

            int currentCompletions =
                    completions.incrementAndGet();

            if (currentCompletions > target) {

                throw new IllegalStateException(
                        "Completion target exceeded: target="
                                + target
                                + ", completions="
                                + currentCompletions
                );
            }
        }
    }

    public int getTarget() {
        return target;
    }

    public int getAdmissions() {
        return admissions.get();
    }

    public int getCompletions() {
        return completions.get();
    }

    /**
     * H0 utilizará esta condición para decidir si puede
     * comenzar un nuevo ciclo.
     */
    public boolean isAdmissionClosed() {

        return admissions.get()
                >= target;
    }

    /**
     * Indica que todas las operaciones admitidas para
     * esta corrida alcanzaron T9.
     */
    public boolean isTargetCompleted() {

        return completions.get()
                >= target;
    }
}
