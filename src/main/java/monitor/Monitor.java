package monitor;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import petri.PetriNet;

/**
 * [MONITOR]
 *
 * Monitor de concurrencia encargado de proteger el acceso
 * compartido a la Red de Petri.
 *
 * [RESPONSIBILITY]
 * Su responsabilidad principal es garantizar que la evaluación
 * y modificación del marcado ocurran en exclusión mutua.
 *
 * [NETWORK-AGNOSTIC]
 * El Monitor no conoce ninguna plaza ni transición particular.
 *
 * No contiene lógica como:
 *
 * if (transition == 4) { ... }
 *
 * Toda la lógica estructural de la red permanece en PetriNet.
 *
 * [CONCURRENCY]
 * PetriNet deliberadamente no es thread-safe.
 * Monitor es la frontera de sincronización que protege su acceso.
 */
public final class Monitor implements MonitorInterface {

    /**
     * [SHARED-RESOURCE]
     *
     * Red de Petri cuyo marcado constituye el estado compartido
     * entre los distintos hilos del sistema.
     */
    private final PetriNet petriNet;

    /**
     * [MUTUAL-EXCLUSION]
     *
     * Lock que protege la sección crítica del Monitor.
     *
     * Se utiliza ReentrantLock en lugar de synchronized porque
     * en T2.2.1 necesitaremos asociar Conditions para implementar
     * bloqueo y reactivación de hilos sin busy waiting.
     *
     * [FAIRNESS]
     * No activamos fairness del lock.
     *
     * La política de selección de transiciones del TP es una
     * responsabilidad distinta de la política de adquisición
     * del lock.
     */
    private final Lock lock;

    /**
     * Construye el Monitor que controla una determinada Red de Petri.
     *
     * @param petriNet modelo de Red de Petri a ejecutar
     */
    public Monitor(PetriNet petriNet) {

        // [DEPENDENCY-VALIDATION]
        // Un Monitor no tiene sentido sin una RdP que proteger.
        this.petriNet = Objects.requireNonNull(
                petriNet,
                "PetriNet cannot be null"
        );

        this.lock = new ReentrantLock();
    }

    /**
     * [ATOMIC-FIRING]
     *
     * Ejecuta una solicitud de disparo en exclusión mutua.
     *
     * Desde el punto de vista de los hilos, esta operación es atómica:
     * ningún otro hilo puede evaluar o modificar la RdP mediante
     * este Monitor hasta que finalice la sección crítica.
     *
     * @param transition índice de la transición solicitada
     * @return true si la transición fue disparada;
     *         false si no estaba sensibilizada
     */
    @Override
    public boolean fireTransition(int transition) {

        /*
         * [ENTER-CRITICAL-SECTION]
         *
         * Si otro hilo está ejecutando fireTransition(),
         * el hilo actual queda esperando la adquisición del lock.
         */
        lock.lock();

        try {

            /*
             * [CRITICAL-SECTION]
             *
             * La comprobación de sensibilización y la actualización
             * del marcado quedan protegidas como una única operación
             * desde el punto de vista concurrente.
             *
             * PetriNet.fire() encapsula ambas operaciones.
             */
            return petriNet.fire(transition);

        } finally {

            /*
             * [EXIT-CRITICAL-SECTION]
             *
             * El unlock se realiza en finally para garantizar que
             * el lock se libere incluso si ocurre una excepción.
             *
             * No liberar un lock podría bloquear permanentemente
             * a los demás hilos.
             */
            lock.unlock();
        }
    }
}