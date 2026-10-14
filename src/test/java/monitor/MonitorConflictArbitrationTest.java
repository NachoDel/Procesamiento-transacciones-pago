package monitor;

import org.junit.jupiter.api.Test;
import petri.PetriNet;
import policy.ConflictGroup;
import policy.Policy;
import policy.PriorityPolicy;
import policy.RandomPolicy;
import timing.TransitionSemantics;
import timing.TransitionTimingConfig;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [CONFLICT-ARBITRATION]
 *
 * Verifica que las solicitudes pertenecientes a un mismo
 * grupo de conflicto sean efectivamente gobernadas por Policy.
 *
 * Se cubren dos situaciones:
 *
 * 1. una transición ya habilitada no puede saltear Policy
 *    solamente porque consiguió primero el lock;
 *
 * 2. una solicitud prioritaria debe poder conservar el turno
 *    del conflicto mientras espera que se liberen los recursos
 *    necesarios para quedar estructuralmente habilitada.
 */
class MonitorConflictArbitrationTest {

    /**
     * [DIRECT-POLICY-BYPASS]
     *
     * Reproduce el caso donde:
     *
     * - T2 ya está esperando;
     * - T1 y T2 se encuentran habilitadas;
     * - llega T1 al Monitor;
     * - T1 no debe disparar directamente sin pasar por Policy.
     *
     * Este test protege la corrección introducida para evitar
     * que el orden de adquisición del lock sustituya a Policy.
     */
    @Test
    void shouldArbitrateEnabledPendingConflictRequestsBeforeFiring()
            throws InterruptedException {

        /*
         * T1 y T2 compiten por P0.
         *
         * Ambas son estructuralmente válidas mientras P0 = 1.
         *
         * Inicialmente T2 se mantiene temporalmente fuera
         * de la ejecución mediante transitionAvailability,
         * únicamente para conseguir que su solicitud quede
         * pendiente dentro del Monitor.
         *
         * Luego habilitamos T2 y hacemos ingresar T1.
         *
         * En ese instante:
         *
         * - T1 está habilitada;
         * - T2 está habilitada;
         * - T2 ya tiene una solicitud pendiente.
         *
         * Policy debe arbitrar entre [T1, T2].
         */
        int[][] incidenceMatrix = {
                // T0  T1  T2
                { 0,  -1,  -1},  // P0
                { 0,   1,   0},  // P1
                { 0,   0,   1}   // P2
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{
                                1,
                                0,
                                0
                        }
                );

        RecordingFixedPolicy policy =
                new RecordingFixedPolicy(
                        2
                );

        AtomicBoolean transition2Available =
                new AtomicBoolean(false);

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        policy,
                        List.of(
                                new ConflictGroup(
                                        Set.of(
                                                1,
                                                2
                                        )
                                )
                        ),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        ),
                        TransitionTimingConfig.noDelays(
                                petriNet.getTransitionsCount()
                        ),
                        (transition, marking) -> {
                            /*
                             * No-op observer.
                             */
                        },
                        transition ->
                                transition != 2
                                        || transition2Available.get()
                );

        AtomicBoolean transition1Result =
                new AtomicBoolean(false);

        AtomicBoolean transition2Result =
                new AtomicBoolean(false);

        Thread transition2Thread =
                new Thread(
                        () ->
                                transition2Result.set(
                                        monitor.fireTransition(2)
                                ),
                        "transition-2"
                );

        /*
         * T2 entra al Monitor pero todavía está marcada
         * como no disponible.
         *
         * Por lo tanto debe quedar esperando.
         */
        transition2Thread.start();

        assertTrue(
                waitUntilWaiting(
                        transition2Thread
                ),
                "Transition 2 should initially wait"
        );

        /*
         * A partir de ahora T2 sí puede participar.
         *
         * No emitimos ninguna señal manual.
         *
         * La llegada de T1 debe provocar el arbitraje
         * del conflicto.
         */
        transition2Available.set(
                true
        );

        Thread transition1Thread =
                new Thread(
                        () ->
                                transition1Result.set(
                                        monitor.fireTransition(1)
                                ),
                        "transition-1"
                );

        transition1Thread.start();

        try {

            /*
             * Policy selecciona T2.
             *
             * Por lo tanto T1 no puede disparar directamente
             * solamente porque consiguió entrar al Monitor.
             */
            assertTrue(
                    waitUntilWaiting(
                            transition1Thread
                    ),
                    "Non-selected conflict transition should wait"
            );

            transition2Thread.join(
                    2000
            );

            assertFalse(
                    transition2Thread.isAlive(),
                    "Policy-selected transition should complete"
            );

            assertTrue(
                    transition2Result.get()
            );

            assertFalse(
                    transition1Result.get()
            );

            /*
             * T2 consumió P0 y produjo P2.
             */
            assertArrayEquals(
                    new int[]{
                            0,
                            0,
                            1
                    },
                    petriNet.getCurrentMarking()
            );

            /*
             * Ambas transiciones eran candidatas válidas
             * cuando ocurrió la decisión.
             */
            assertEquals(
                    List.of(
                            1,
                            2
                    ),
                    policy.getLastCandidates()
            );

            /*
             * T1 permanece esperando porque perdió
             * el arbitraje y P0 ya fue consumida.
             */
            assertTrue(
                    transition1Thread.isAlive()
            );

        } finally {

            transition1Thread.interrupt();
            transition2Thread.interrupt();

            transition1Thread.join(
                    2000
            );

            transition2Thread.join(
                    2000
            );
        }
    }

    /**
     * [PERSISTENT-PRIORITY-GRANT]
     *
     * Reproduce el problema observado experimentalmente:
     *
     * la transición prioritaria requiere dos recursos
     * simultáneamente y puede permanecer esperando mientras
     * alternativas de menor prioridad vuelven a consumir
     * dichos recursos.
     *
     * El objetivo de este test es exigir que la solicitud
     * prioritaria conserve el turno del conflicto hasta que
     * los recursos necesarios queden disponibles.
     */
    @Test
    void shouldPreservePriorityGrantUntilRequiredResourcesBecomeAvailable()
            throws InterruptedException {

        /*
         * T0 -> libera el segundo recurso.
         *
         * T1 -> alternativa normal:
         *       necesita P1 + P2.
         *
         * T2 -> alternativa prioritaria:
         *       necesita P1 + P2 + P3.
         *
         * Inicialmente:
         *
         * P1 = token de ruteo.
         * P2 = recurso A libre.
         * P3 = recurso B ocupado.
         *
         * T1 puede dispararse.
         * T2 todavía no.
         *
         * Sin grant persistente, T1 puede consumir P2
         * antes de que T2 reúna ambos recursos.
         *
         * Con prioridad efectiva, T2 conserva el turno
         * mientras espera que T0 libere P3.
         */
        int[][] incidenceMatrix = {
                // T0  T1  T2
                {-1,   0,   0},  // P0 - recurso B ocupado
                { 0,  -1,  -1},  // P1 - token de ruteo
                { 0,  -1,  -1},  // P2 - recurso A
                { 1,   0,  -1},  // P3 - recurso B
                { 0,   1,   0},  // P4 - salida T1
                { 0,   0,   1}   // P5 - salida T2
        };

        PetriNet petriNet =
                new PetriNet(
                        incidenceMatrix,
                        new int[]{
                                1,
                                1,
                                1,
                                0,
                                0,
                                0
                        }
                );

        Policy policy =
                new PriorityPolicy(
                        2,
                        new RandomPolicy(
                                2026L
                        )
                );

        MonitorInterface monitor =
                new Monitor(
                        petriNet,
                        policy,
                        List.of(
                                new ConflictGroup(
                                        Set.of(
                                                1,
                                                2
                                        )
                                )
                        ),
                        TransitionSemantics.allImmediate(
                                petriNet.getTransitionsCount()
                        ),
                        TransitionTimingConfig.noDelays(
                                petriNet.getTransitionsCount()
                        )
                );

        AtomicBoolean priorityResult =
                new AtomicBoolean(false);

        AtomicBoolean normalResult =
                new AtomicBoolean(false);

        Thread priorityThread =
                new Thread(
                        () ->
                                priorityResult.set(
                                        monitor.fireTransition(2)
                                ),
                        "priority-transition"
                );

        Thread normalThread =
                new Thread(
                        () ->
                                normalResult.set(
                                        monitor.fireTransition(1)
                                ),
                        "normal-transition"
                );

        /*
         * La solicitud prioritaria entra primero.
         *
         * Como todavía falta P3, debe quedar esperando.
         */
        priorityThread.start();

        assertTrue(
                waitUntilWaiting(
                        priorityThread
                ),
                "Priority request should initially wait for resource B"
        );

        /*
         * Ahora ingresa una alternativa de menor prioridad
         * que sí podría dispararse inmediatamente.
         */
        normalThread.start();

        try {

            /*
             * PriorityPolicy debe impedir que T1 tome
             * el recurso A mientras existe una solicitud
             * prioritaria pendiente para T2.
             *
             * Con el Monitor actual esperamos que este
             * assertion quede rojo.
             */
            assertTrue(
                    waitUntilWaiting(
                            normalThread
                    ),
                    "Lower-priority transition should wait"
            );

            assertFalse(
                    normalResult.get()
            );

            /*
             * Todavía no debería haberse modificado
             * el marcado.
             */
            assertArrayEquals(
                    new int[]{
                            1,
                            1,
                            1,
                            0,
                            0,
                            0
                    },
                    petriNet.getCurrentMarking()
            );

            /*
             * T0 representa la finalización del trabajo
             * que actualmente posee el recurso B.
             *
             * Al dispararse libera P3.
             */
            assertTrue(
                    monitor.fireTransition(0)
            );

            priorityThread.join(
                    2000
            );

            assertFalse(
                    priorityThread.isAlive(),
                    "Priority transition should fire once both resources are free"
            );

            assertTrue(
                    priorityResult.get()
            );

            /*
             * T2 consumió:
             *
             * - P1;
             * - P2;
             * - P3.
             *
             * Y produjo P5.
             */
            assertArrayEquals(
                    new int[]{
                            0,
                            0,
                            0,
                            0,
                            0,
                            1
                    },
                    petriNet.getCurrentMarking()
            );

            /*
             * T1 perdió el conflicto.
             */
            assertTrue(
                    normalThread.isAlive()
            );

            assertFalse(
                    normalResult.get()
            );

        } finally {

            priorityThread.interrupt();
            normalThread.interrupt();

            priorityThread.join(
                    2000
            );

            normalThread.join(
                    2000
            );
        }
    }

    /**
     * Espera hasta que un thread se encuentre bloqueado
     * dentro del Monitor.
     *
     * @return true si entra en WAITING/TIMED_WAITING;
     *         false si termina o vence el timeout.
     */
    private static boolean waitUntilWaiting(
            Thread thread
    ) throws InterruptedException {

        long deadline =
                System.nanoTime()
                        + 2_000_000_000L;

        while (System.nanoTime()
                < deadline) {

            Thread.State state =
                    thread.getState();

            if (state == Thread.State.WAITING
                    || state
                    == Thread.State.TIMED_WAITING) {

                return true;
            }

            if (state
                    == Thread.State.TERMINATED) {

                return false;
            }

            Thread.sleep(
                    5
            );
        }

        return false;
    }

    /**
     * Policy controlada utilizada para verificar
     * el arbitraje del primer escenario.
     */
    private static final class RecordingFixedPolicy
            implements Policy {

        private final int selectedTransition;

        private volatile List<Integer> lastCandidates =
                List.of();

        private RecordingFixedPolicy(
                int selectedTransition
        ) {

            this.selectedTransition =
                    selectedTransition;
        }

        @Override
        public OptionalInt select(
                List<Integer> candidates
        ) {

            lastCandidates =
                    List.copyOf(
                            candidates
                    );

            if (!candidates.contains(
                    selectedTransition
            )) {

                throw new IllegalStateException(
                        "Expected transition is not a candidate: "
                                + selectedTransition
                );
            }

            return OptionalInt.of(
                    selectedTransition
            );
        }

        private List<Integer> getLastCandidates() {
            return lastCandidates;
        }
    }
}