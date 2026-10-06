package timing;

import java.util.Objects;
import java.util.Set;

/**
 * [TRANSITION-SEMANTICS]
 *
 * Describe la clasificación temporal de las transiciones.
 *
 * Las transiciones declaradas en timedTransitions son
 * temporales. Todas las restantes son inmediatas.
 */
public final class TransitionSemantics {

    private final int transitionsCount;

    private final Set<Integer> timedTransitions;

    private TransitionSemantics(
            int transitionsCount,
            Set<Integer> timedTransitions
    ) {

        if (transitionsCount <= 0) {
            throw new IllegalArgumentException(
                    "Transitions count must be positive"
            );
        }

        Objects.requireNonNull(
                timedTransitions,
                "Timed transitions cannot be null"
        );

        for (Integer transition : timedTransitions) {

            if (transition == null
                    || transition < 0
                    || transition >= transitionsCount) {

                throw new IllegalArgumentException(
                        "Invalid timed transition: "
                                + transition
                );
            }
        }

        this.transitionsCount =
                transitionsCount;

        // [DEFENSIVE-COPY]
        this.timedTransitions =
                Set.copyOf(timedTransitions);
    }

    public static TransitionSemantics fromTimedTransitions(
            int transitionsCount,
            Set<Integer> timedTransitions
    ) {

        return new TransitionSemantics(
                transitionsCount,
                timedTransitions
        );
    }

    public static TransitionSemantics allImmediate(
            int transitionsCount
    ) {

        return new TransitionSemantics(
                transitionsCount,
                Set.of()
        );
    }

    public boolean isTimed(int transition) {

        validateTransition(transition);

        return timedTransitions.contains(
                transition
        );
    }

    public boolean isImmediate(int transition) {

        validateTransition(transition);

        return !timedTransitions.contains(
                transition
        );
    }

    public int getTransitionsCount() {
        return transitionsCount;
    }

    private void validateTransition(
            int transition
    ) {

        if (transition < 0
                || transition >= transitionsCount) {

            throw new IllegalArgumentException(
                    "Invalid transition index: "
                            + transition
            );
        }
    }
}