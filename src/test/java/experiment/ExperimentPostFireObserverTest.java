package experiment;

import monitor.PostFireObserver;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EXPERIMENT-POST-FIRE-OBSERVER TESTS]
 *
 * Verifica:
 *
 * - notificación de progreso;
 * - notificación de auditoría;
 * - cierre de admisión;
 * - conteo de finalizaciones;
 * - ejecución de ambos observers aunque uno falle;
 * - publicación del primer error asincrónico.
 */
class ExperimentPostFireObserverTest {

    @Test
    void shouldStartWithoutFailure() {

        ExperimentPostFireObserver observer =
                new ExperimentPostFireObserver(
                        PostFireObserver.noop(),
                        PostFireObserver.noop()
                );

        assertFalse(
                observer.hasFailure()
        );

        assertTrue(
                observer.getFailure().isEmpty()
        );
    }

    @Test
    void shouldNotifyProgressAndAudit() {

        RunProgressTracker tracker =
                new RunProgressTracker(2);

        AtomicInteger auditedEvents =
                new AtomicInteger(0);

        PostFireObserver audit =
                (transition, marking) ->
                        auditedEvents.incrementAndGet();

        ExperimentPostFireObserver observer =
                new ExperimentPostFireObserver(
                        tracker,
                        audit
                );

        observer.onSuccessfulFire(
                0,
                new int[0]
        );

        assertEquals(
                1,
                tracker.getAdmissions()
        );

        assertEquals(
                1,
                auditedEvents.get()
        );

        assertFalse(
                observer.hasFailure()
        );
    }

    @Test
    void shouldCloseAdmissionAndStillNotifyAudit() {

        RunProgressTracker tracker =
                new RunProgressTracker(1);

        AtomicInteger auditedEvents =
                new AtomicInteger(0);

        ExperimentPostFireObserver observer =
                new ExperimentPostFireObserver(
                        tracker,
                        (transition, marking) ->
                                auditedEvents.incrementAndGet()
                );

        observer.onSuccessfulFire(
                0,
                new int[0]
        );

        assertTrue(
                tracker.isAdmissionClosed()
        );

        assertEquals(
                1,
                auditedEvents.get()
        );

        assertFalse(
                observer.hasFailure()
        );
    }

    @Test
    void shouldTrackCompletionAndNotifyAudit() {

        RunProgressTracker tracker =
                new RunProgressTracker(1);

        AtomicInteger auditedEvents =
                new AtomicInteger(0);

        ExperimentPostFireObserver observer =
                new ExperimentPostFireObserver(
                        tracker,
                        (transition, marking) ->
                                auditedEvents.incrementAndGet()
                );

        observer.onSuccessfulFire(
                9,
                new int[0]
        );

        assertTrue(
                tracker.isTargetCompleted()
        );

        assertEquals(
                1,
                auditedEvents.get()
        );

        assertFalse(
                observer.hasFailure()
        );
    }

    @Test
    void shouldStillAuditWhenProgressObserverFails() {

        PostFireObserver failingProgress =
                (transition, marking) -> {
                    throw new IllegalStateException(
                            "progress failure"
                    );
                };

        AtomicInteger auditedEvents =
                new AtomicInteger(0);

        PostFireObserver audit =
                (transition, marking) ->
                        auditedEvents.incrementAndGet();

        ExperimentPostFireObserver observer =
                new ExperimentPostFireObserver(
                        failingProgress,
                        audit
                );

        IllegalStateException failure =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                observer.onSuccessfulFire(
                                        0,
                                        new int[0]
                                )
                );

        assertEquals(
                "progress failure",
                failure.getMessage()
        );

        assertEquals(
                1,
                auditedEvents.get()
        );

        assertTrue(
                observer.hasFailure()
        );

        assertTrue(
                observer.getFailure().isPresent()
        );

        assertEquals(
                "progress failure",
                observer.getFailure()
                        .orElseThrow()
                        .getMessage()
        );
    }

    @Test
    void shouldStillUpdateProgressWhenAuditFails() {

        RunProgressTracker tracker =
                new RunProgressTracker(1);

        PostFireObserver failingAudit =
                (transition, marking) -> {
                    throw new IllegalStateException(
                            "audit failure"
                    );
                };

        ExperimentPostFireObserver observer =
                new ExperimentPostFireObserver(
                        tracker,
                        failingAudit
                );

        IllegalStateException failure =
                assertThrows(
                        IllegalStateException.class,
                        () ->
                                observer.onSuccessfulFire(
                                        0,
                                        new int[0]
                                )
                );

        assertEquals(
                "audit failure",
                failure.getMessage()
        );

        assertEquals(
                1,
                tracker.getAdmissions()
        );

        assertTrue(
                tracker.isAdmissionClosed()
        );

        assertTrue(
                observer.hasFailure()
        );

        assertEquals(
                "audit failure",
                observer.getFailure()
                        .orElseThrow()
                        .getMessage()
        );
    }

    @Test
    void shouldRejectNullObservers() {

        PostFireObserver observer =
                PostFireObserver.noop();

        assertThrows(
                NullPointerException.class,
                () ->
                        new ExperimentPostFireObserver(
                                null,
                                observer
                        )
        );

        assertThrows(
                NullPointerException.class,
                () ->
                        new ExperimentPostFireObserver(
                                observer,
                                null
                        )
        );
    }
}