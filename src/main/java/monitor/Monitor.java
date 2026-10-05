package monitor;

import petri.PetriNet;
import policy.ConflictGroup;
import policy.Policy;
import timing.TransitionSemantics;
import timing.TransitionTimingConfig;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * [MONITOR]
 *
 * Protege el acceso concurrente a la Red de Petri y coordina:
 *
 * - exclusión mutua;
 * - espera por sensibilización;
 * - resolución de conflictos;
 * - semántica inmediata/temporal;
 * - espera temporal sin retener la sección crítica.
 *
 * [NETWORK-AGNOSTIC]
 * El Monitor no conoce plazas ni transiciones concretas.
 */
public final class Monitor implements MonitorInterface {

    private final PetriNet petriNet;

    /**
     * [POLICY]
     *
     * Se utiliza únicamente para resolver conflictos
     * estructurales entre alternativas válidas.
     */
    private final Policy policy;

    /**
     * [CONFLICT-GROUPS]
     *
     * Describe qué transiciones compiten entre sí.
     */
    private final List<ConflictGroup> conflictGroups;

    /**
     * [TRANSITION-SEMANTICS]
     *
     * Determina qué transiciones son inmediatas
     * y cuáles temporales.
     */
    private final TransitionSemantics transitionSemantics;

    /**
     * [TIMING-CONFIG]
     *
     * Contiene los tiempos configurados para
     * las transiciones temporales.
     */
    private final TransitionTimingConfig transitionTimingConfig;

    /**
     * [MUTUAL-EXCLUSION]
     *
     * Protege el marcado y el estado interno del Monitor.
     */
    private final ReentrantLock lock;

    /**
     * [WAIT-CONDITION]
     *
     * Una cola condicional por transición.
     */
    private final Condition[] transitionConditions;

    /**
     * [TIMING-CONDITION]
     *
     * Se utiliza exclusivamente para realizar esperas temporales.
     *
     * Condition.awaitNanos() libera el lock mientras espera
     * y lo recupera antes de retornar.
     *
     * De esta forma una transición temporal NO mantiene
     * bloqueado todo el Monitor durante su demora.
     */
    private final Condition timingCondition;

    /**
     * [WAITING-THREADS]
     *
     * waitingThreads[t] indica cuántos threads esperan
     * que la transición t pueda dispararse.
     */
    private final int[] waitingThreads;

    public Monitor(
            PetriNet petriNet,
            Policy policy,
            List<ConflictGroup> conflictGroups,
            TransitionSemantics transitionSemantics,
            TransitionTimingConfig transitionTimingConfig
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

        this.transitionTimingConfig =
                Objects.requireNonNull(
                        transitionTimingConfig,
                        "Transition timing config cannot be null"
                );

        if (transitionSemantics.getTransitionsCount()
                != petriNet.getTransitionsCount()) {

            throw new IllegalArgumentException(
                    "Transition semantics must match PetriNet transition count"
            );
        }

        if (transitionTimingConfig.getTransitionsCount()
                != petriNet.getTransitionsCount()) {

            throw new IllegalArgumentException(
                    "Transition timing config must match PetriNet transition count"
            );
        }

        validateConflictGroups(
                conflictGroups,
                petriNet.getTransitionsCount()
        );

        validateTimingConfiguration(
                transitionSemantics,
                transitionTimingConfig
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

        for (int transition = 0;
             transition < transitionsCount;
             transition++) {

            transitionConditions[transition] =
                    lock.newCondition();
        }

        this.timingCondition =
                lock.newCondition();
    }

    /**
     * [ATOMIC-FIRING]
     *
     * Solicita el disparo de una transición.
     *
     * Las transiciones temporales esperan su tiempo configurado
     * liberando el lock del Monitor.
     *
     * @return true si la transición fue disparada;
     *         false si la solicitud fue abortada por interrupción
     */
    @Override
    public boolean fireTransition(int transition) {

        lock.lock();

        try {

            while (true) {

                /*
                 * [WAIT-UNTIL-FIREABLE]
                 *
                 * Primero esperamos que el marcado y la semántica
                 * permitan ejecutar esta transición.
                 */
                while (!canFireNow(transition)) {

                    boolean resumed =
                            awaitTransition(transition);

                    if (!resumed) {

                        signalEnabledWaitingTransitions();

                        return false;
                    }
                }

                /*
                 * [TIMED-TRANSITION]
                 *
                 * Si es temporal, debe permanecer habilitada
                 * durante su espera configurada.
                 *
                 * La espera NO conserva el lock.
                 */
                if (transitionSemantics.isTimed(transition)) {

                    TimingWaitResult timingResult =
                            awaitTimedTransition(
                                    transition
                            );

                    if (timingResult
                            == TimingWaitResult.INTERRUPTED) {

                        signalEnabledWaitingTransitions();

                        return false;
                    }

                    /*
                     * [REVALIDATION]
                     *
                     * Si durante la espera el estado cambió
                     * y la transición dejó de poder dispararse,
                     * volvemos al inicio.
                     *
                     * Su tiempo se reiniciará cuando nuevamente
                     * quede en condiciones de ejecutarse.
                     */
                    if (timingResult
                            == TimingWaitResult.RETRY) {

                        continue;
                    }

                    /*
                     * Incluso al vencer el tiempo realizamos una
                     * última validación bajo exclusión mutua.
                     */
                    if (!canFireNow(transition)) {
                        continue;
                    }
                }

                /*
                 * [CRITICAL-SECTION]
                 *
                 * El disparo real ocurre con el lock tomado.
                 */
                boolean fired =
                        petriNet.fire(transition);

                if (fired) {

                    /*
                     * [TIMING-REVALIDATION]
                     *
                     * Cualquier cambio de marcado puede alterar
                     * las condiciones de los temporizadores activos.
                     *
                     * Despertamos a todos para que revaliden.
                     */
                    timingCondition.signalAll();

                    /*
                     * [CONDITIONAL-REACTIVATION]
                     *
                     * El nuevo marcado puede permitir continuar
                     * a threads bloqueados.
                     */
                    signalEnabledWaitingTransitions();
                }

                return fired;
            }

        } finally {

            lock.unlock();
        }
    }

    /**
     * [FIRING-SEMANTICS]
     *
     * Una transición puede dispararse ahora cuando:
     *
     * 1. está sensibilizada en PetriNet;
     * 2. si es temporal, no existe una inmediata sensibilizada.
     */
    private boolean canFireNow(int transition) {

        if (!petriNet.isEnabled(transition)) {
            return false;
        }

        if (transitionSemantics.isImmediate(transition)) {
            return true;
        }

        return !hasEnabledImmediateTransition();
    }

    /**
     * [IMMEDIATE-PRIORITY]
     *
     * Determina si existe alguna transición inmediata
     * sensibilizada.
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
     * [CONDITIONAL-WAIT]
     *
     * Suspende un thread hasta que pueda volver
     * a comprobar su transición.
     */
    private boolean awaitTransition(int transition) {

        waitingThreads[transition]++;

        try {

            transitionConditions[transition].await();

            return true;

        } catch (InterruptedException exception) {

            Thread.currentThread().interrupt();

            return false;

        } finally {

            waitingThreads[transition]--;
        }
    }

    /**
     * [TIMED-WAIT]
     *
     * Realiza la demora correspondiente a una transición temporal.
     *
     * [IMPORTANT]
     * Condition.awaitNanos() libera el ReentrantLock durante
     * la espera y vuelve a adquirirlo antes de retornar.
     *
     * Esto permite que otros threads entren al Monitor
     * durante el tiempo de la transición.
     */
    private TimingWaitResult awaitTimedTransition(
            int transition
    ) {

        long delayMillis =
                transitionTimingConfig.getDelayMillis(
                        transition
                );

        long remainingNanos =
                TimeUnit.MILLISECONDS.toNanos(
                        delayMillis
                );

        while (remainingNanos > 0) {

            try {

                remainingNanos =
                        timingCondition.awaitNanos(
                                remainingNanos
                        );

            } catch (InterruptedException exception) {

                /*
                 * [TIMED-INTERRUPTION]
                 *
                 * Una interrupción también puede cancelar
                 * un thread durante una espera temporal.
                 */
                Thread.currentThread().interrupt();

                return TimingWaitResult.INTERRUPTED;
            }

            /*
             * [TEMPORAL-REVALIDATION]
             *
             * Otro disparo pudo haber modificado el marcado
             * mientras este thread no tenía el lock.
             */
            if (!canFireNow(transition)) {

                return TimingWaitResult.RETRY;
            }
        }

        return TimingWaitResult.ELAPSED;
    }

    /**
     * [WAITING-THREAD-REACTIVATION]
     *
     * Reactiva las transiciones que pueden continuar.
     *
     * Policy solamente interviene dentro de un conflicto real.
     */
    private void signalEnabledWaitingTransitions() {

        List<Integer> enabledWaiting =
                collectEnabledWaitingTransitions();

        if (enabledWaiting.isEmpty()) {
            return;
        }

        boolean[] handled =
                new boolean[
                        petriNet.getTransitionsCount()
                ];

        for (ConflictGroup conflictGroup
                : conflictGroups) {

            List<Integer> conflictCandidates =
                    conflictGroup.filterCandidates(
                            enabledWaiting
                    );

            if (conflictCandidates.isEmpty()) {
                continue;
            }

            for (int transition
                    : conflictCandidates) {

                handled[transition] = true;
            }

            /*
             * Si solo existe una alternativa disponible,
             * no hace falta consultar Policy.
             */
            if (conflictCandidates.size() == 1) {

                signalTransition(
                        conflictCandidates.get(0)
                );

                continue;
            }

            int selectedTransition =
                    selectByPolicy(
                            conflictCandidates
                    );

            signalTransition(
                    selectedTransition
            );
        }

        /*
         * Transiciones independientes.
         */
        for (int transition
                : enabledWaiting) {

            if (!handled[transition]) {

                signalTransition(
                        transition
                );
            }
        }
    }

    /**
     * [ENABLED-WAITING]
     *
     * Recopila threads bloqueados cuya transición
     * puede ejecutarse actualmente.
     */
    private List<Integer> collectEnabledWaitingTransitions() {

        List<Integer> candidates =
                new ArrayList<>();

        for (int transition = 0;
             transition < waitingThreads.length;
             transition++) {

            if (waitingThreads[transition] > 0
                    && canFireNow(transition)) {

                candidates.add(
                        transition
                );
            }
        }

        return candidates;
    }

    /**
     * [POLICY-SELECTION]
     *
     * Policy solamente recibe alternativas pertenecientes
     * al mismo conflicto.
     */
    private int selectByPolicy(
            List<Integer> candidates
    ) {

        OptionalInt selected =
                policy.select(
                        List.copyOf(candidates)
                );

        if (selected.isEmpty()) {

            throw new IllegalStateException(
                    "Policy did not select a transition although conflict candidates exist"
            );
        }

        int selectedTransition =
                selected.getAsInt();

        if (!candidates.contains(
                selectedTransition
        )) {

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
     * Despierta un único thread asociado
     * a la transición indicada.
     */
    private void signalTransition(
            int transition
    ) {

        transitionConditions[transition].signal();
    }

    /**
     * [CONFLICT-CONFIGURATION-VALIDATION]
     */
    private static void validateConflictGroups(
            List<ConflictGroup> conflictGroups,
            int transitionsCount
    ) {

        Set<Integer> assignedTransitions =
                new HashSet<>();

        for (ConflictGroup conflictGroup
                : conflictGroups) {

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

                if (!assignedTransitions.add(
                        transition
                )) {

                    throw new IllegalArgumentException(
                            "Transition belongs to more than one conflict group: "
                                    + transition
                    );
                }
            }
        }
    }

    /**
     * [TIMING-CONFIGURATION-VALIDATION]
     *
     * Exige coherencia entre la clasificación temporal
     * y los tiempos configurados.
     *
     * Temporal  -> debe tener delay positivo.
     * Inmediata -> no debe tener delay.
     */
    private static void validateTimingConfiguration(
            TransitionSemantics semantics,
            TransitionTimingConfig timingConfig
    ) {

        for (int transition = 0;
             transition < semantics.getTransitionsCount();
             transition++) {

            if (semantics.isTimed(transition)
                    && !timingConfig.hasDelay(transition)) {

                throw new IllegalArgumentException(
                        "Timed transition must have a configured delay: "
                                + transition
                );
            }

            if (semantics.isImmediate(transition)
                    && timingConfig.hasDelay(transition)) {

                throw new IllegalArgumentException(
                        "Immediate transition cannot have a configured delay: "
                                + transition
                );
            }
        }
    }

    /**
     * [TIMING-WAIT-RESULT]
     *
     * Resultado interno de una espera temporal.
     */
    private enum TimingWaitResult {

        /**
         * Se cumplió completamente el tiempo configurado.
         */
        ELAPSED,

        /**
         * El marcado cambió y la transición debe
         * volver a esperar desde el inicio.
         */
        RETRY,

        /**
         * El thread recibió una interrupción.
         */
        INTERRUPTED
    }
}