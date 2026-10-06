package experiment;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [RUN-PROGRESS-TRACKER TESTS]
 *
 * Verifica exclusivamente el contrato de progreso de
 * una corrida experimental.
 *
 * No prueba lifecycle de Workers ni sincronización del Monitor.
 */
class RunProgressTrackerTest {

    @Test
    void shouldStartWithNoProgress() {

        RunProgressTracker tracker =
                new RunProgressTracker(3);

        assertEquals(
                3,
                tracker.getTarget()
        );

        assertEquals(
                0,
                tracker.getAdmissions()
        );

        assertEquals(
                0,
                tracker.getCompletions()
        );

        assertFalse(
                tracker.isAdmissionClosed()
        );

        assertFalse(
                tracker.isTargetCompleted()
        );
    }

    @Test
    void shouldRejectNonPositiveTarget() {

        assertThrows(
                IllegalArgumentException.class,
                () -> new RunProgressTracker(0)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new RunProgressTracker(-1)
        );
    }

    @Test
    void shouldCountSuccessfulT0AsAdmission() {

        RunProgressTracker tracker =
                new RunProgressTracker(3);

        tracker.onSuccessfulFire(
                0,
                new int[0]
        );

        assertEquals(
                1,
                tracker.getAdmissions()
        );

        assertFalse(
                tracker.isAdmissionClosed()
        );
    }

    @Test
    void shouldCloseAdmissionExactlyAtTarget() {

        RunProgressTracker tracker =
                new RunProgressTracker(3);

        tracker.onSuccessfulFire(
                0,
                new int[0]
        );

        tracker.onSuccessfulFire(
                0,
                new int[0]
        );

        assertFalse(
                tracker.isAdmissionClosed()
        );

        tracker.onSuccessfulFire(
                0,
                new int[0]
        );

        assertEquals(
                3,
                tracker.getAdmissions()
        );

        assertTrue(
                tracker.isAdmissionClosed()
        );
    }

    @Test
    void shouldCountSuccessfulT9AsCompletion() {

        RunProgressTracker tracker =
                new RunProgressTracker(3);

        tracker.onSuccessfulFire(
                9,
                new int[0]
        );

        assertEquals(
                1,
                tracker.getCompletions()
        );

        assertFalse(
                tracker.isTargetCompleted()
        );
    }

    @Test
    void shouldCompleteTargetExactlyAtExpectedCount() {

        RunProgressTracker tracker =
                new RunProgressTracker(2);

        tracker.onSuccessfulFire(
                9,
                new int[0]
        );

        assertFalse(
                tracker.isTargetCompleted()
        );

        tracker.onSuccessfulFire(
                9,
                new int[0]
        );

        assertEquals(
                2,
                tracker.getCompletions()
        );

        assertTrue(
                tracker.isTargetCompleted()
        );
    }

    @Test
    void shouldIgnoreTransitionsOtherThanT0AndT9() {

        RunProgressTracker tracker =
                new RunProgressTracker(2);

        for (int transition = 1;
             transition <= 8;
             transition++) {

            tracker.onSuccessfulFire(
                    transition,
                    new int[0]
            );
        }

        assertEquals(
                0,
                tracker.getAdmissions()
        );

        assertEquals(
                0,
                tracker.getCompletions()
        );

        assertFalse(
                tracker.isAdmissionClosed()
        );

        assertFalse(
                tracker.isTargetCompleted()
        );
    }

    @Test
    void shouldFailIfAdmissionTargetIsExceeded() {

        RunProgressTracker tracker =
                new RunProgressTracker(1);

        tracker.onSuccessfulFire(
                0,
                new int[0]
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tracker.onSuccessfulFire(
                                0,
                                new int[0]
                        )
        );
    }

    @Test
    void shouldFailIfCompletionTargetIsExceeded() {

        RunProgressTracker tracker =
                new RunProgressTracker(1);

        tracker.onSuccessfulFire(
                9,
                new int[0]
        );

        assertThrows(
                IllegalStateException.class,
                () ->
                        tracker.onSuccessfulFire(
                                9,
                                new int[0]
                        )
        );
    }
}
