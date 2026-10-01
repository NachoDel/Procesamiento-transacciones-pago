package monitor;

import petri.PetriNet;
import policy.Policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * [MONITOR]
 *
 * Protege el acceso concurrente a la Red de Petri y coordina
 * la espera/reactivación de los hilos que solicitan transiciones.
 *
 * [NETWORK-AGNOSTIC]
 * El Monitor no conoce transiciones particulares de la red.
 * Toda la lógica se basa en índices y en el estado informado
 * por PetriNet.
 */
public final class Monitor implements MonitorInterface {

    /**
     * [SHARED-RESOURCE]
     *
     * Estado compartido protegido por el Monitor.
     */
    private final PetriNet petriNet;

    /**
     * [POLICY]
     *
     * Estrategia utilizada para elegir qué transición reactivar
     * cuando existen varias alternativas válidas.
     */
    private final Policy policy;

    /**
     * [MUTUAL-EXCLUSION]
     *
     * Garantiza que solo un hilo pueda evaluar/modificar
     * el estado de la Red de Petri a la vez.
     */
    private final ReentrantLock lock;

    /**
     * [WAIT-CONDITION]
     *
     * Cada transición posee su propia Condition.
     *
     * transitionConditions[t] representa la cola de hilos
     * que están esperando poder disparar la transición t.
     */
    private final Condition[] transitionConditions;

    /**
     * [WAITING-THREADS]
     *
     * waitingThreads[t] indica cuántos hilos están actualmente
     * esperando por la transición t.
     *
     * Este vector permite distinguir entre:
     *
     * - transición habilitada sin hilos esperando;
     * - transición habilitada con hilos esperando.
     */
    private final int[] waitingThreads;

    public Monitor(PetriNet petriNet, Policy policy) {

        this.petriNet = Objects.requireNonNull(
                petriNet,
                "PetriNet cannot be null"
        );

        this.policy = Objects.requireNonNull(
                policy,
                "Policy cannot be null"
        );

        /*
         * [MUTUAL-EXCLUSION]
         *
         * ReentrantLock permite asociar múltiples Conditions
         * al mismo Monitor.
         */
        this.lock = new ReentrantLock();

        int transitionsCount =
                petriNet.getTransitionsCount();

        this.transitionConditions =
                new Condition[transitionsCount];

        this.waitingThreads =
                new int[transitionsCount];

        /*
         * [GENERIC-CONFIGURATION]
         *
         * Se crea una condición por transición sin conocer
         * ningún identificador concreto de la red.
         */
        for (int transition = 0;
             transition < transitionsCount;
             transition++) {

            transitionConditions[transition] =
                    lock.newCondition();
        }
    }

    /**
     * [ATOMIC-FIRING]
     *
     * Solicita el disparo de una transición.
     *
     * Si la transición no está habilitada, el hilo se suspende
     * sin busy waiting hasta que otro disparo modifique el marcado.
     *
     * @return true si la transición finalmente fue disparada;
     *         false si el hilo fue interrumpido mientras esperaba
     */
    @Override
    public boolean fireTransition(int transition) {

        /*
         * [ENTER-CRITICAL-SECTION]
         *
         * Si otro hilo está ejecutando fireTransition(),
         * el hilo actual queda esperando la adquisición del lock.
         */
        lock.lock();

        try {

            /*
             * [RECHECK-CONDITION]
             *
             * La condición siempre debe reevaluarse luego de despertar.
             *
             * Un signal() significa "algo cambió", no garantiza
             * que la transición continúe habilitada cuando el hilo
             * recupere el lock.
             */
            while (!petriNet.isEnabled(transition)) {

                boolean resumed =
                        awaitTransition(transition);

                if (!resumed) {

                    /*
                     * [INTERRUPTION-HANDOFF]
                     *
                     * Si este hilo fue interrumpido, intentamos
                     * reactivar otra transición válida antes
                     * de abandonar el Monitor.
                     */
                    signalNextEnabledWaitingTransition();

                    return false;
                }
            }

            /*
             * [CRITICAL-SECTION]
             *
             * La sensibilización fue comprobada bajo el mismo lock
             * con el que se realiza el disparo.
             */
            boolean fired =
                    petriNet.fire(transition);

            /*
             * [SIGNAL]
             *
             * Un disparo modifica el marcado y puede haber habilitado
             * a uno o más hilos que se encontraban suspendidos.
             */
            if (fired) {
                signalNextEnabledWaitingTransition();
            }

            return fired;

        } finally {

            lock.unlock();
        }
    }

    /**
     * [WAIT-CONDITION]
     *
     * Suspende el hilo actual en la Condition correspondiente.
     *
     * Condition.await():
     * - libera temporalmente el lock;
     * - suspende el hilo;
     * - vuelve a adquirir el lock antes de retornar.
     */
    private boolean awaitTransition(int transition) {

        waitingThreads[transition]++;

        try {

            transitionConditions[transition].await();

            return true;

        } catch (InterruptedException exception) {

            /*
             * [INTERRUPTION]
             *
             * Se restaura el estado de interrupción para no ocultar
             * la señal recibida por el hilo.
             */
            Thread.currentThread().interrupt();

            return false;

        } finally {

            /*
             * [WAITING-COUNTER]
             *
             * Al salir de await(), ya sea por signal o interrupción,
             * el hilo deja de pertenecer a la cola lógica de espera.
             */
            waitingThreads[transition]--;
        }
    }

    /**
     * [POLICY + REACTIVATION]
     *
     * Determina qué transiciones:
     *
     * 1. están habilitadas;
     * 2. poseen al menos un hilo esperando.
     *
     * La Policy decide cuál de ellas debe continuar.
     */
    private void signalNextEnabledWaitingTransition() {

        List<Integer> candidates =
                new ArrayList<>();

        /*
         * [CANDIDATE-BUILDING]
         *
         * El recorrido ascendente también mantiene un orden
         * estable de candidatos para políticas reproducibles.
         */
        for (int transition = 0;
             transition < waitingThreads.length;
             transition++) {

            if (waitingThreads[transition] > 0
                    && petriNet.isEnabled(transition)) {

                candidates.add(transition);
            }
        }

        if (candidates.isEmpty()) {
            return;
        }

        /*
         * [POLICY]
         *
         * La política recibe solamente alternativas válidas.
         * No conoce la Red de Petri ni las colas internas.
         */
        OptionalInt selected =
                policy.select(List.copyOf(candidates));

        /*
         * [DEFENSIVE-VALIDATION]
         *
         * Si existen candidatos, una Policy correcta debe
         * seleccionar uno de ellos.
         */
        if (selected.isEmpty()) {
            throw new IllegalStateException(
                    "Policy did not select a transition although candidates exist"
            );
        }

        int selectedTransition =
                selected.getAsInt();

        if (!candidates.contains(selectedTransition)) {
            throw new IllegalStateException(
                    "Policy selected a transition that is not a valid candidate: "
                            + selectedTransition
            );
        }

        /*
         * [SIGNAL]
         *
         * Se despierta un solo hilo de la cola elegida.
         *
         * Ese hilo deberá recuperar el lock y volver a comprobar
         * la sensibilización antes de disparar.
         */
        transitionConditions[selectedTransition].signal();
    }
}