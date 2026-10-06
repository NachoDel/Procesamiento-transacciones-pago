package policy;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * [CONFLICT-GROUP]
 *
 * Representa un conjunto de transiciones que compiten
 * estructuralmente entre sí.
 *
 * [RESPONSIBILITY]
 * ConflictGroup solamente describe qué transiciones forman
 * parte del mismo conflicto.
 *
 * No selecciona ninguna transición.
 * Esa responsabilidad pertenece a Policy.
 */
public final class ConflictGroup {

    private final Set<Integer> transitions;

    public ConflictGroup(Set<Integer> transitions) {

        Objects.requireNonNull(
                transitions,
                "Conflict transitions cannot be null"
        );

        if (transitions.size() < 2) {
            throw new IllegalArgumentException(
                    "A conflict group must contain at least two transitions"
            );
        }

        for (Integer transition : transitions) {

            if (transition == null || transition < 0) {
                throw new IllegalArgumentException(
                        "Conflict transitions must contain only non-negative indices"
                );
            }
        }

        // [DEFENSIVE-COPY]
        this.transitions =
                Set.copyOf(transitions);
    }

    /**
     * [CONFLICT-CANDIDATES]
     *
     * Conserva solamente los candidatos que pertenecen
     * al conflicto representado por este objeto.
     *
     * Se mantiene el orden original de candidates.
     */
    public List<Integer> filterCandidates(
            List<Integer> candidates
    ) {

        Objects.requireNonNull(
                candidates,
                "Candidates cannot be null"
        );

        return candidates.stream()
                .filter(transitions::contains)
                .toList();
    }

    public Set<Integer> getTransitions() {
        return transitions;
    }
}