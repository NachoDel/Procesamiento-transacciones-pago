package policy;

import java.util.List;
import java.util.OptionalInt;

/**
 * [POLICY-CONTRACT]
 *
 * Define el contrato utilizado para resolver conflictos entre
 * varias transiciones candidatas.
 *
 * [RESPONSIBILITY]
 * Una Policy solamente elige entre alternativas que ya fueron
 * determinadas como válidas por el sistema.
 *
 * No debe:
 * - modificar la Red de Petri;
 * - disparar transiciones;
 * - adquirir locks;
 * - conocer el marcado;
 * - conocer detalles internos del Monitor.
 *
 * [DESIGN]
 * Esta separación mantiene desacoplada la decisión de planificación
 * respecto de la lógica y sincronización del sistema.
 */
public interface Policy {

    /**
     * [POLICY-SELECTION]
     *
     * Selecciona una transición entre las candidatas recibidas.
     *
     * @param candidates transiciones válidas entre las que puede elegirse
     * @return la transición elegida, o OptionalInt.empty()
     *         si no existen candidatos
     */
    OptionalInt select(List<Integer> candidates);
}

// ~~~~~~~~~~~ INFO ~~~~~~~~~~~ \\
/**
 * Usamos OptionalInt en lugar de Integer para evitar boxing innecesario.
 * Es decir, decimos que puede existir una seleccion o puede que no
 * Entonces en el monitor nos permite hacer un if (selection.isPresent()) en lugar de if (selection != null)
 * Son mas palabras, pero es mas limpio y mas seguro, ya que no hay posibilidad de confundir null con un valor valido.
 */
// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ \\