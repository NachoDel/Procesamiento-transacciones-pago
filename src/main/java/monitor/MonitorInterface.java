package monitor;

/**
 * [MONITOR-CONTRACT]
 *
 * Contrato público utilizado por los hilos para solicitar
 * el disparo de una transición de la Red de Petri.
 *
 * [TP-REQUIREMENT]
 * El enunciado establece explícitamente esta interfaz.
 *
 * Los workers deberán depender de MonitorInterface y no
 * de la implementación concreta de Monitor.
 */
public interface MonitorInterface {

    /**
     * [FIRE-TRANSITION]
     *
     * Solicita al Monitor el disparo de una transición.
     *
     * @param transition índice de la transición solicitada
     * @return true si la transición fue disparada;
     *         false si no pudo dispararse
     */
    boolean fireTransition(int transition);
}