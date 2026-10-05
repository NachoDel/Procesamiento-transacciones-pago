package monitor;

import petri.PetriNet;
import policy.ConflictGroup;
import policy.Policy;
import timing.TransitionSemantics;

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
 * La estructura de conflictos y la clasificación temporal
 * se reciben externamente mediante configuración.
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
     * conflictos estructurales entre alternativas válidas.
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
     * [TRANSITION-SEMANTICS]
     *
     * Clasifica las transiciones como inmediatas o temporales.
     *
     * El Monitor no conoce cuáles son T2, T3, etc.;
     * solamente consulta esta configuración genérica.
     */
    private final TransitionSemantics transitionSemantics;

    /**
     * [MUTUAL-EXCLUSION]
     *
     * Protege toda consulta y modificación del marcado.
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
     * actualmente esperando la transición t.
     */
    private final int[] waitingThreads;

    public Monitor(
            PetriNet petriNet,
            Policy policy,
            List<ConflictGroup> conflictGroups,
            TransitionSemantics transitionSemantics
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

        this.transitionSemantics =
                Objects.requireNonNull(
                        transitionSemantics,
                        "Transition semantics cannot be null"
                );

        if (transitionSemantics.getTransitionsCount()
                != petriNet.getTransitionsCount()) {

            throw new IllegalArgumentException(
                    "Transition semantics must match PetriNet transition count"
            );
        }

        /*
         * [CONFIGURATION-VALIDATION]
         *
         * Se valida que todos los conflictos hagan referencia
         * a transiciones reales y que no existan grupos solapados.
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
     * Si la transición no puede dispararse actualmente,
     * el thread queda suspendido mediante Condition.await().
     *
     * @return true si la transición finalmente fue disparada;
     *         false si la solicitud fue abortada por interrupción
     */
    @Override
    public boolean fireTransition(int transition) {

        lock.lock();

        try {

            /*
             * [RECHECK-CONDITION]
             *
             * Se usa while y no if porque un signal solamente
             * indica que el estado pudo haber cambiado.
             *
             * Al despertar, el thread debe comprobar nuevamente
             * si realmente puede disparar.
             */
            while (!canFireNow(transition)) {

                boolean resumed =
                        awaitTransition(transition);

                if (!resumed) {

                    /*
                     * [INTERRUPTION-HANDOFF]
                     *
                     * El thread abandona su solicitud.
                     *
                     * Antes de salir intentamos despertar otros
                     * threads que puedan continuar.
                     */
                    signalEnabledWaitingTransitions();

                    return false;
                }
            }

            /*
             * [CRITICAL-SECTION]
             *
             * La verificación anterior y el disparo real
             * están protegidos por el mismo lock.
             */
            boolean fired =
                    petriNet.fire(transition);

            if (fired) {

                /*
                 * [REACTIVATION]
                 *
                 * El nuevo marcado puede habilitar otros threads.
                 */
                signalEnabledWaitingTransitions();
            }

            return fired;

        } finally {

            lock.unlock();
        }
    }

    /**
     * [FIRING-SEMANTICS]
     *
     * Una transición puede dispararse ahora si:
     *
     * 1. está sensibilizada según PetriNet;
     * 2. si es temporal, no existe ninguna transición
     *    inmediata sensibilizada.
     *
     * [IMMEDIATE-PRIORITY]
     *
     * Las transiciones inmediatas poseen precedencia
     * sobre las temporales.
     */
    private boolean canFireNow(int transition) {

        if (!petriNet.isEnabled(transition)) {
            return false;
        }

        /*
         * Una inmediata sensibilizada puede dispararse
         * directamente.
         */
        if (transitionSemantics.isImmediate(transition)) {
            return true;
        }

        /*
         * Una temporal solamente puede continuar cuando
         * el marcado no posee ninguna inmediata sensibilizada.
         */
        return !hasEnabledImmediateTransition();
    }

    /**
     * [IMMEDIATE-DETECTION]
     *
     * Busca genéricamente una transición inmediata
     * sensibilizada en el marcado actual.
     */
    private boolean hasEnabledImmediateTransition() {

        for (int transition = 0;
             transition < petriNet.getTransitionsCount();
             transition++) {

            if (transitionSemantics.isImmediate(transition)
                    && petriNet.isEnabled(transition)) {

                return true;
            }
        }

        return false;
    }

    /**
     * [WAIT-CONDITION]
     *
     * Suspende el thread actual en la Condition asociada
     * a la transición solicitada.
     *
     * Condition.await():
     *
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
             * Restauramos el estado de interrupción para
             * que la señal no se pierda.
             */
            Thread.currentThread().interrupt();

            return false;

        } finally {

            /*
             * [WAITING-COUNTER]
             *
             * Al abandonar await(), ya sea por signal o por
             * interrupción, el thread deja la cola lógica.
             */
            waitingThreads[transition]--;
        }
    }

    /**
     * [WAITING-THREAD-REACTIVATION]
     *
     * Reactiva threads que pueden continuar según:
     *
     * - sensibilización;
     * - semántica inmediata/temporal;
     * - conflictos estructurales;
     * - Policy cuando existe un conflicto efectivo.
     */
    private void signalEnabledWaitingTransitions() {

        List<Integer> enabledWaiting =
                collectEnabledWaitingTransitions();

        if (enabledWaiting.isEmpty()) {
            return;
        }

        /*
         * handled[t] indica que la transición t ya fue
         * procesada como parte de un ConflictGroup.
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
             * Si solamente una alternativa del grupo puede
             * dispararse ahora, Policy no tiene nada que decidir.
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
             * Policy interviene únicamente cuando existen
             * múltiples alternativas del mismo conflicto.
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
         * Una transición válida que no pertenece a un
         * conflicto puede continuar sin pasar por Policy.
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
     * Reúne solamente las transiciones que:
     *
     * - poseen al menos un thread esperando;
     * - pueden dispararse AHORA según canFireNow().
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
                    && canFireNow(transition)) {

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
         * Una Policy correcta debe producir una transición
         * válida cuando recibe candidatos.
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
     * Despierta un único thread de la cola correspondiente.
     *
     * El thread deberá recuperar el lock y volver a evaluar
     * canFireNow() antes de disparar.
     */
    private void signalTransition(int transition) {

        transitionConditions[transition].signal();
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * Verifica que:
     *
     * - todas las transiciones de los grupos existan;
     * - una transición no pertenezca a dos grupos diferentes.
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