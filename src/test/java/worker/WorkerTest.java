package worker;

import monitor.MonitorInterface;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * [T2.2.2 - WORKER TESTS]
 *
 * Verifica el comportamiento del Worker independientemente
 * de la implementación real del Monitor.
 */
class WorkerTest {

    /**
     * [SEQUENCE-ORDER]
     *
     * Verifica que un Worker ejecute las transiciones
     * exactamente en el orden configurado.
     */
    @Test
    void shouldExecuteTransitionSequenceInOrder() {

        // [ARRANGE]
        RecordingMonitor monitor =
                new RecordingMonitor();

        AtomicBoolean stopRequested =
                new AtomicBoolean(false);

        Worker worker =
                new Worker(
                        "H-test",
                        monitor,
                        List.of(1, 2, 3),
                        () -> stopRequested.get()
                );

        /*
         * El FakeMonitor solicitará detener nuevos ciclos
         * después de completar las tres llamadas esperadas.
         */
        monitor.setAfterFire(() -> {

            if (monitor.getCallCount() >= 3) {
                stopRequested.set(true);
            }
        });

        // [ACT]
        worker.run();

        // [ASSERT]
        assertEquals(
                List.of(1, 2, 3),
                monitor.getTransitions()
        );
    }

    /**
     * [REPEATED-SEQUENCE]
     *
     * Verifica que el Worker repita su secuencia mientras
     * no exista una solicitud de finalización.
     */
    @Test
    void shouldRepeatSequenceUntilStopIsRequested() {

        // [ARRANGE]
        RecordingMonitor monitor =
                new RecordingMonitor();

        AtomicBoolean stopRequested =
                new AtomicBoolean(false);

        Worker worker =
                new Worker(
                        "H-test",
                        monitor,
                        List.of(1, 2, 3),
                        stopRequested::get
                );

        /*
         * Dos ciclos completos:
         *
         * [1,2,3] + [1,2,3]
         */
        monitor.setAfterFire(() -> {

            if (monitor.getCallCount() >= 6) {
                stopRequested.set(true);
            }
        });

        // [ACT]
        worker.run();

        // [ASSERT]
        assertEquals(
                List.of(1, 2, 3, 1, 2, 3),
                monitor.getTransitions()
        );
    }

    /**
     * [GRACEFUL-STOP]
     *
     * Una solicitud normal de finalización no debe cortar
     * un ciclo que ya comenzó.
     */
    @Test
    void shouldFinishCurrentSequenceBeforeStoppingGracefully() {

        // [ARRANGE]
        RecordingMonitor monitor =
                new RecordingMonitor();

        AtomicBoolean stopRequested =
                new AtomicBoolean(false);

        Worker worker =
                new Worker(
                        "H-test",
                        monitor,
                        List.of(1, 2, 3),
                        stopRequested::get
                );

        /*
         * Pedimos detener la ejecución después de T1.
         *
         * Como el ciclo ya comenzó, el Worker todavía debe
         * completar T2 y T3.
         */
        monitor.setAfterFire(() -> {

            if (monitor.getCallCount() == 1) {
                stopRequested.set(true);
            }
        });

        // [ACT]
        worker.run();

        // [ASSERT]
        assertEquals(
                List.of(1, 2, 3),
                monitor.getTransitions()
        );
    }

    /**
     * [ABORTED-FIRING]
     *
     * Si Monitor.fireTransition() retorna false,
     * el Worker debe abandonar inmediatamente.
     */
    @Test
    void shouldStopWhenMonitorAbortsFiring() {

        // [ARRANGE]
        RecordingMonitor monitor =
                new RecordingMonitor();

        /*
         * La segunda llamada al Monitor retorna false.
         */
        monitor.setAbortOnCall(2);

        Worker worker =
                new Worker(
                        "H-test",
                        monitor,
                        List.of(1, 2, 3),
                        () -> false
                );

        // [ACT]
        worker.run();

        // [ASSERT]
        assertEquals(
                List.of(1, 2),
                monitor.getTransitions()
        );
    }

    /**
     * [INITIAL-STOP]
     *
     * Si la corrida ya está finalizada antes de arrancar,
     * no debe solicitarse ninguna transición.
     */
    @Test
    void shouldNotExecuteWhenStopIsAlreadyRequested() {

        // [ARRANGE]
        RecordingMonitor monitor =
                new RecordingMonitor();

        Worker worker =
                new Worker(
                        "H-test",
                        monitor,
                        List.of(1, 2, 3),
                        () -> true
                );

        // [ACT]
        worker.run();

        // [ASSERT]
        assertEquals(
                List.of(),
                monitor.getTransitions()
        );
    }

    /**
     * [CONFIGURATION-VALIDATION]
     *
     * Un Worker sin secuencia no tiene trabajo definido.
     */
    @Test
    void shouldRejectEmptyTransitionSequence() {

        RecordingMonitor monitor =
                new RecordingMonitor();

        assertThrows(
                IllegalArgumentException.class,
                () -> new Worker(
                        "H-test",
                        monitor,
                        List.of(),
                        () -> false
                )
        );
    }

    /**
     * [DEFENSIVE-COPY]
     *
     * Modificar la lista original después de construir
     * el Worker no debe modificar su configuración interna.
     */
    @Test
    void shouldDefensivelyCopyTransitionSequence() {

        // [ARRANGE]
        RecordingMonitor monitor =
                new RecordingMonitor();

        AtomicBoolean stopRequested =
                new AtomicBoolean(false);

        List<Integer> sequence =
                new ArrayList<>(
                        List.of(1, 2, 3)
                );

        Worker worker =
                new Worker(
                        "H-test",
                        monitor,
                        sequence,
                        stopRequested::get
                );

        /*
         * Modificamos la colección externa después
         * de construir el Worker.
         */
        sequence.clear();
        sequence.add(9);

        monitor.setAfterFire(() -> {

            if (monitor.getCallCount() >= 3) {
                stopRequested.set(true);
            }
        });

        // [ACT]
        worker.run();

        // [ASSERT]
        assertEquals(
                List.of(1, 2, 3),
                monitor.getTransitions()
        );
    }

    /**
     * [TEST-DOUBLE / FAKE-MONITOR]
     *
     * Implementación mínima de MonitorInterface utilizada
     * exclusivamente para probar Worker.
     *
     * Registra las transiciones solicitadas y permite simular
     * un disparo abortado sin involucrar concurrencia real,
     * Conditions, Policy ni PetriNet.
     */
    private static class RecordingMonitor
            implements MonitorInterface {

        private final List<Integer> transitions =
                Collections.synchronizedList(
                        new ArrayList<>()
                );

        private final AtomicInteger callCount =
                new AtomicInteger(0);

        private int abortOnCall = -1;

        private Runnable afterFire =
                () -> {
                };

        @Override
        public boolean fireTransition(int transition) {

            transitions.add(transition);

            int currentCall =
                    callCount.incrementAndGet();

            afterFire.run();

            return currentCall != abortOnCall;
        }

        void setAbortOnCall(int abortOnCall) {
            this.abortOnCall =
                    abortOnCall;
        }

        void setAfterFire(Runnable afterFire) {
            this.afterFire =
                    afterFire;
        }

        int getCallCount() {
            return callCount.get();
        }

        List<Integer> getTransitions() {
            return List.copyOf(transitions);
        }
    }
}

/**
 * FakeMonitor para pruebas de Worker.
 * Si usaramos el monitor normal y el test falla,
 * no sabríamos inmediatamente si falló Worker,
 * PetriNet, Policy o sincronización
 */