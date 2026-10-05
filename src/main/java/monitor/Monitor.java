package monitor;

import petri.PetriNet;
import policy.ConflictGroup;
import policy.Policy;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * [MONITOR]
 *
 * Protege el acceso concurrente a la Red de Petri y coordina
 * la espera/reactivación de los threads.
 *
 * [NETWORK-AGNOSTIC]
 * El Monitor no conoce plazas ni transiciones concretas.
 *
 * No contiene lógica del estilo:
 *
 * if (transition == 4) { ... }
 *
 * La estructura específica de conflictos se recibe mediante
 * objetos ConflictGroup.
 */
public final class Monitor implements MonitorInterface {

    /**
     * [SHARED-RESOURCE]
     *
     * Red de Petri protegida por el Monitor.
     */
    private final PetriNet petriNet;

    /**
     * [POLICY]
     *
     * Estrategia utilizada exclusivamente para resolver
     * conflictos reales entre transiciones alternativas.
     */
    private final Policy policy;

    /**
     * [CONFLICT-GROUPS]
     *
     * Describe qué transiciones compiten estructuralmente
     * entre sí.
     */
    private final List<ConflictGroup> conflictGroups;

    /**
     * [MUTUAL-EXCLUSION]
     *
     * Protege toda evaluación y modificación del marcado.
     */
    private final ReentrantLock lock;

    /**
     * [WAIT-CONDITION]
     *
     * Una Condition por transición.
     *
     * transitionConditions[t] representa la cola de threads
     * que esperan poder disparar la transición t.
     */
    private final Condition[] transitionConditions;

    /**
     * [WAITING-THREADS]
     *
     * waitingThreads[t] indica cuántos threads se encuentran
     * esperando por la transición t.
     */
    private final int[] waitingThreads;

    public Monitor(
            PetriNet petriNet,
            Policy policy,
            List<ConflictGroup> conflictGroups
    ) {

        this.petriNet =
                Objects.requireNonNull(
                        petriNet,
                        "PetriNet cannot be null"
                );

        this.policy =
                Objects.requireNonNull(
                        policy,
                        "Policy cannot be null"
                );

        Objects.requireNonNull(
                conflictGroups,
                "Conflict groups cannot be null"
        );

        /*
         * [CONFIGURATION-VALIDATION]
         *
         * Se validan los grupos respecto de la cantidad real
         * de transiciones de la Red de Petri.
         */
        validateConflictGroups(
                conflictGroups,
                petriNet.getTransitionsCount()
        );

        // [DEFENSIVE-COPY]
        this.conflictGroups =
                List.copyOf(conflictGroups);

        this.lock =
                new ReentrantLock();

        int transitionsCount =
                petriNet.getTransitionsCount();

        this.transitionConditions =
                new Condition[transitionsCount];

        this.waitingThreads =
                new int[transitionsCount];

        /*
         * [GENERIC-CONFIGURATION]
         *
         * Se crea una Condition por transición sin conocer
         * ninguna transición concreta de la red.
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
     * Si la transición no está sensibilizada, el thread queda
     * suspendido mediante Condition.await(), sin busy waiting.
     *
     * @return true si la transición fue disparada;
     *         false si la solicitud fue abortada por interrupción
     */
    @Override
    public boolean fireTransition(int transition) {

        lock.lock();

        try {

            /*
             * [RECHECK-CONDITION]
             *
             * Se utiliza while porque un thread reactivado debe
             * volver a comprobar la sensibilización.
             *
             * signal() significa "algo cambió", no constituye
             * permiso irrevocable para disparar.
             */
            while (!petriNet.isEnabled(transition)) {

                boolean resumed =
                        awaitTransition(transition);

                if (!resumed) {

                    /*
                     * [INTERRUPTION-HANDOFF]
                     *
                     * Antes de abandonar el Monitor intentamos
                     * reactivar otros threads que ahora puedan
                     * continuar.
                     */
                    signalEnabledWaitingTransitions();

                    return false;
                }
            }

            /*
             * [CRITICAL-SECTION]
             *
             * Sensibilización y disparo se evalúan bajo
             * el mismo lock.
             */
            boolean fired =
                    petriNet.fire(transition);

            if (fired) {

                /*
                 * [REACTIVATION]
                 *
                 * El nuevo marcado puede haber habilitado
                 * threads que estaban suspendidos.
                 */
                signalEnabledWaitingTransitions();
            }

            return fired;

        } finally {

            lock.unlock();
        }
    }

    /**
     * [WAIT-CONDITION]
     *
     * Suspende el thread actual en la Condition asociada
     * a la transición solicitada.
     *
     * Condition.await():
     * - libera temporalmente el lock;
     * - suspende el thread;
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
             * Restauramos el estado de interrupción para que
             * la señal no se pierda al salir del Monitor.
             */
            Thread.currentThread().interrupt();

            return false;

        } finally {

            /*
             * [WAITING-COUNTER]
             *
             * Al salir de await(), por signal o interrupción,
             * el thread deja de pertenecer a la cola lógica.
             */
            waitingThreads[transition]--;
        }
    }

    /**
     * [WAITING-THREAD-REACTIVATION]
     *
     * Reactiva los threads que pueden continuar.
     *
     * [POLICY-SCOPE]
     * Policy interviene solamente cuando dos o más transiciones
     * habilitadas pertenecen al mismo ConflictGroup.
     *
     * Las transiciones independientes no compiten mediante Policy.
     */
    private void signalEnabledWaitingTransitions() {

        List<Integer> enabledWaiting =
                collectEnabledWaitingTransitions();

        if (enabledWaiting.isEmpty()) {
            return;
        }

        /*
         * handled[t] indica que la transición t ya fue tratada
         * como integrante de algún conflicto estructural.
         */
        boolean[] handled =
                new boolean[
                        petriNet.getTransitionsCount()
                ];

        /*
         * [CONFLICT-RESOLUTION]
         *
         * Cada grupo se resuelve independientemente.
         */
        for (ConflictGroup conflictGroup : conflictGroups) {

            List<Integer> conflictCandidates =
                    conflictGroup.filterCandidates(
                            enabledWaiting
                    );

            if (conflictCandidates.isEmpty()) {
                continue;
            }

            for (int transition : conflictCandidates) {
                handled[transition] = true;
            }

            /*
             * [NO-EFFECTIVE-CONFLICT]
             *
             * Si solamente una transición del grupo está disponible,
             * no existe una decisión que Policy deba resolver.
             */
            if (conflictCandidates.size() == 1) {

                signalTransition(
                        conflictCandidates.get(0)
                );

                continue;
            }

            /*
             * [POLICY]
             *
             * Recién cuando existen dos o más alternativas
             * del mismo conflicto delegamos la decisión.
             */
            int selectedTransition =
                    selectByPolicy(
                            conflictCandidates
                    );

            signalTransition(
                    selectedTransition
            );
        }

        /*
         * [INDEPENDENT-TRANSITIONS]
         *
         * Una transición habilitada y con threads esperando
         * que no pertenece a ningún conflicto puede continuar
         * sin pasar por Policy.
         */
        for (int transition : enabledWaiting) {

            if (!handled[transition]) {
                signalTransition(transition);
            }
        }
    }

    /**
     * [ENABLED-WAITING]
     *
     * Reúne las transiciones que:
     *
     * 1. poseen al menos un thread esperando;
     * 2. están sensibilizadas actualmente.
     *
     * El recorrido ascendente mantiene un orden estable.
     */
    private List<Integer> collectEnabledWaitingTransitions() {

        List<Integer> candidates =
                new ArrayList<>();

        for (int transition = 0;
             transition < waitingThreads.length;
             transition++) {

            if (waitingThreads[transition] > 0
                    && petriNet.isEnabled(transition)) {

                candidates.add(transition);
            }
        }

        return candidates;
    }

    /**
     * [POLICY-SELECTION]
     *
     * Solicita a Policy una elección únicamente entre
     * candidatos pertenecientes al mismo conflicto.
     */
    private int selectByPolicy(
            List<Integer> candidates
    ) {

        OptionalInt selected =
                policy.select(
                        List.copyOf(candidates)
                );

        /*
         * [DEFENSIVE-VALIDATION]
         *
         * Una Policy correcta debe seleccionar una transición
         * válida cuando existen candidatos.
         */
        if (selected.isEmpty()) {
            throw new IllegalStateException(
                    "Policy did not select a transition although conflict candidates exist"
            );
        }

        int selectedTransition =
                selected.getAsInt();

        if (!candidates.contains(selectedTransition)) {
            throw new IllegalStateException(
                    "Policy selected a transition outside the conflict candidates: "
                            + selectedTransition
            );
        }

        return selectedTransition;
    }

    /**
     * [SIGNAL]
     *
     * Despierta un único thread de la cola de la transición.
     *
     * El thread deberá recuperar el lock y volver a comprobar
     * la sensibilización antes de disparar.
     */
    private void signalTransition(int transition) {

        transitionConditions[transition].signal();
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * Comprueba que:
     *
     * - todas las transiciones existan en la PetriNet;
     * - una transición no pertenezca a dos grupos distintos.
     *
     * Los grupos solapados harían ambigua la resolución
     * mediante Policy.
     */
    private static void validateConflictGroups(
            List<ConflictGroup> conflictGroups,
            int transitionsCount
    ) {

        Set<Integer> assignedTransitions =
                new HashSet<>();

        for (ConflictGroup conflictGroup : conflictGroups) {

            if (conflictGroup == null) {
                throw new IllegalArgumentException(
                        "Conflict groups cannot contain null"
                );
            }

            for (int transition
                    : conflictGroup.getTransitions()) {

                if (transition < 0
                        || transition >= transitionsCount) {

                    throw new IllegalArgumentException(
                            "Conflict transition does not exist in PetriNet: "
                                    + transition
                    );
                }

                if (!assignedTransitions.add(transition)) {
                    throw new IllegalArgumentException(
                            "Transition belongs to more than one conflict group: "
                                    + transition
                    );
                }
            }
        }
    }
}