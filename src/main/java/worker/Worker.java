package worker;

import monitor.MonitorInterface;

import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

/**
 * [WORKER]
 *
 * Representa un trabajador concurrente encargado de solicitar
 * al Monitor una secuencia ordenada de transiciones.
 *
 * [RESPONSIBILITY]
 * Worker conoce:
 * - qué secuencia debe ejecutar;
 * - cuándo debe dejar de iniciar nuevos ciclos.
 *
 * Worker NO conoce:
 * - el marcado de la Red de Petri;
 * - sensibilización de transiciones;
 * - políticas;
 * - Conditions;
 * - locks;
 * - estructura interna del Monitor.
 *
 * Toda solicitud de disparo se realiza exclusivamente mediante
 * MonitorInterface.fireTransition().
 */
public final class Worker implements Runnable {

    /**
     * [WORKER-ID]
     *
     * Identificador lógico del worker.
     *
     * Ejemplos futuros del modelo real:
     * H0, H1, H2, H3, H4.
     */
    private final String name;

    /**
     * [MONITOR-DEPENDENCY]
     *
     * Único punto de acceso del Worker a la Red de Petri.
     */
    private final MonitorInterface monitor;

    /**
     * [TRANSITION-SEQUENCE]
     *
     * Secuencia ordenada que este Worker debe ejecutar
     * en cada ciclo.
     *
     * Se almacena una copia inmutable para impedir que
     * la configuración cambie durante la ejecución.
     */
    private final List<Integer> transitionSequence;

    /**
     * [STOP-CONDITION]
     *
     * Criterio externo que indica que el Worker no debe
     * comenzar un nuevo ciclo de trabajo.
     *
     * El Worker no decide por sí mismo cuándo finaliza
     * una corrida global.
     */
    private final BooleanSupplier stopRequested;

    public Worker(
            String name,
            MonitorInterface monitor,
            List<Integer> transitionSequence,
            BooleanSupplier stopRequested
    ) {

        this.name =
                Objects.requireNonNull(
                        name,
                        "Worker name cannot be null"
                );

        if (name.isBlank()) {
            throw new IllegalArgumentException(
                    "Worker name cannot be blank"
            );
        }

        this.monitor =
                Objects.requireNonNull(
                        monitor,
                        "Monitor cannot be null"
                );

        Objects.requireNonNull(
                transitionSequence,
                "Transition sequence cannot be null"
        );

        if (transitionSequence.isEmpty()) {
            throw new IllegalArgumentException(
                    "Transition sequence cannot be empty"
            );
        }

        for (Integer transition : transitionSequence) {

            if (transition == null || transition < 0) {
                throw new IllegalArgumentException(
                        "Transition sequence must contain only non-negative indices"
                );
            }
        }

        // [DEFENSIVE-COPY]
        this.transitionSequence =
                List.copyOf(transitionSequence);

        this.stopRequested =
                Objects.requireNonNull(
                        stopRequested,
                        "Stop condition cannot be null"
                );
    }

    /**
     * [WORKER-LIFECYCLE]
     *
     * Ejecuta ciclos completos de la secuencia asignada hasta
     * recibir una solicitud de finalización.
     *
     * [GRACEFUL-STOP]
     * stopRequested se consulta entre ciclos completos.
     *
     * Una vez iniciado un ciclo, el Worker intenta terminar toda
     * su secuencia para no abandonar voluntariamente una operación
     * lógica a mitad de camino.
     *
     * [INTERRUPTION]
     * Una interrupción sí puede abortar inmediatamente la ejecución.
     * Si Monitor.fireTransition() retorna false, se interpreta que
     * la solicitud fue abortada y el Worker finaliza.
     */
    @Override
    public void run() {

        while (!stopRequested.getAsBoolean()
                && !Thread.currentThread().isInterrupted()) {

            for (int transition : transitionSequence) {

                /*
                 * [INTERRUPTION-CHECK]
                 *
                 * Una interrupción representa una solicitud
                 * inmediata de cierre.
                 */
                if (Thread.currentThread().isInterrupted()) {
                    return;
                }

                /*
                 * [MONITOR-CALL]
                 *
                 * El Worker nunca consulta directamente PetriNet.
                 * Toda sincronización ocurre dentro del Monitor.
                 */
                boolean fired =
                        monitor.fireTransition(transition);

                /*
                 * [ABORTED-FIRING]
                 *
                 * Según el contrato actual de MonitorInterface,
                 * false significa que la solicitud fue abortada,
                 * por ejemplo debido a una interrupción mientras
                 * el thread esperaba una Condition.
                 */
                if (!fired) {
                    return;
                }
            }
        }
    }

    /**
     * [WORKER-ID]
     *
     * Se mantiene disponible para configuración, diagnóstico
     * y futura integración con logging.
     */
    public String getName() {
        return name;
    }
}