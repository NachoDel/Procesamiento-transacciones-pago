package timing;

import java.util.Map;
import java.util.Objects;

/**
 * [TRANSITION-TIMING-CONFIG]
 *
 * Centraliza los tiempos asociados a las transiciones
 * temporales de una Red de Petri.
 *
 * [DESIGN]
 * Esta clase no sabe cuáles transiciones son inmediatas
 * o temporales. Solamente almacena tiempos por índice.
 *
 * La coherencia con TransitionSemantics será validada
 * posteriormente por el Monitor.
 */
public final class TransitionTimingConfig {

    private final long[] delayMillis;

    private TransitionTimingConfig(
            int transitionsCount,
            Map<Integer, Long> configuredDelays
    ) {

        if (transitionsCount <= 0) {
            throw new IllegalArgumentException(
                    "Transitions count must be positive"
            );
        }

        Objects.requireNonNull(
                configuredDelays,
                "Configured delays cannot be null"
        );

        this.delayMillis =
                new long[transitionsCount];

        for (Map.Entry<Integer, Long> entry
                : configuredDelays.entrySet()) {

            Integer transition =
                    entry.getKey();

            Long delay =
                    entry.getValue();

            if (transition == null
                    || transition < 0
                    || transition >= transitionsCount) {

                throw new IllegalArgumentException(
                        "Invalid transition index: "
                                + transition
                );
            }

            if (delay == null || delay <= 0) {
                throw new IllegalArgumentException(
                        "Configured delay must be positive for transition "
                                + transition
                );
            }

            delayMillis[transition] =
                    delay;
        }
    }

    /**
     * [FACTORY]
     *
     * Construye una configuración a partir de tiempos
     * expresados en milisegundos.
     *
     * Las transiciones no presentes en el Map quedan
     * configuradas con tiempo 0.
     */
    public static TransitionTimingConfig fromMillis(
            int transitionsCount,
            Map<Integer, Long> configuredDelays
    ) {

        return new TransitionTimingConfig(
                transitionsCount,
                configuredDelays
        );
    }

    /**
     * [NO-DELAYS]
     *
     * Configuración sin tiempos.
     *
     * Resultará útil para tests donde todas las
     * transiciones son inmediatas.
     */
    public static TransitionTimingConfig noDelays(
            int transitionsCount
    ) {

        return new TransitionTimingConfig(
                transitionsCount,
                Map.of()
        );
    }

    /**
     * [DELAY]
     *
     * @return tiempo configurado en milisegundos;
     *         0 si la transición no posee tiempo.
     */
    public long getDelayMillis(int transition) {

        validateTransition(transition);

        return delayMillis[transition];
    }

    public boolean hasDelay(int transition) {

        return getDelayMillis(transition) > 0;
    }

    public int getTransitionsCount() {
        return delayMillis.length;
    }

    private void validateTransition(
            int transition
    ) {

        if (transition < 0
                || transition >= delayMillis.length) {

            throw new IllegalArgumentException(
                    "Invalid transition index: "
                            + transition
            );
        }
    }
}