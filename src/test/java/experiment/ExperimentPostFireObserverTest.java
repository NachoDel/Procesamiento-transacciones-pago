package experiment;

import monitor.PostFireObserver;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [EXPERIMENT-POST-FIRE-OBSERVER TESTS]
 *
 * Verifica que progreso y auditoría reciban exactamente
 * los mismos disparos exitosos.
 */
class ExperimentPostFireObserverTest {

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

        assertThrows(
                IllegalStateException.class,
                () ->
                        observer.onSuccessfulFire(
                                0,
                                new int[0]
                        )
        );

        assertEquals(
                1,
                auditedEvents.get()
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

        assertThrows(
                IllegalStateException.class,
                () ->
                        observer.onSuccessfulFire(
                                0,
                                new int[0]
                        )
        );

        assertEquals(
                1,
                tracker.getAdmissions()
        );

        assertTrue(
                tracker.isAdmissionClosed()
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
