package monitor;

/**
 * [POST-FIRE-OBSERVER]
 *
 * Acción ejecutada inmediatamente después de que una transición
 * fue disparada y su nuevo marcado quedó comprometido.
 *
 * [CONCURRENCY]
 * Monitor invoca este callback mientras todavía mantiene
 * su lock, por lo que el marcado recibido corresponde
 * exactamente al disparo informado.
 *
 * [RESPONSIBILITY]
 * Permite integrar verificación y logging sin acoplar
 * Monitor a implementaciones concretas.
 */
@FunctionalInterface
public interface PostFireObserver {

    /**
     * Se ejecuta exactamente una vez por cada disparo exitoso.
     *
     * @param transition transición que fue disparada
     * @param marking marcado comprometido posterior al disparo
     */
    void onSuccessfulFire(
            int transition,
            int[] marking
    );

    /**
     * [NO-OP]
     *
     * Útil para tests que verifican exclusivamente
     * sincronización o temporalidad.
     */
    static PostFireObserver noop() {

        return (transition, marking) -> {
        };
    }
}