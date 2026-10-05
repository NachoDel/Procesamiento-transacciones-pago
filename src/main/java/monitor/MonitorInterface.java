package monitor;

/**
 * [MONITOR-CONTRACT]
 *
 * Contrato público utilizado por los Workers para solicitar
 * el disparo de transiciones de la Red de Petri.
 *
 * [RESPONSIBILITY]
 * Los consumidores del Monitor solamente necesitan conocer
 * esta interfaz y no su implementación concreta.
 */
public interface MonitorInterface {

    /**
     * [FIRE-TRANSITION]
     *
     * Solicita el disparo de una transición.
     *
     * Semántica actual:
     *
     * true:
     * la transición finalmente fue disparada.
     *
     * false:
     * la solicitud fue abortada, por ejemplo debido a una
     * interrupción mientras el thread se encontraba esperando.
     *
     * @param transition índice de la transición solicitada
     * @return resultado de la solicitud
     */
    boolean fireTransition(int transition);
}