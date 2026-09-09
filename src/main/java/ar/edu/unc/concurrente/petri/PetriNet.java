package ar.edu.unc.concurrente.petri;

import java.util.Arrays;

/**
 * [PETRI-NET]
 *
 * Representa de forma genérica una Red de Petri mediante:
 * - una matriz de incidencia;
 * - un marcado inicial;
 * - un marcado actual.
 *
 * La clase no conoce ninguna transición o plaza concreta del sistema
 * de procesamiento de pagos.
 *
 * [CONCURRENCY]
 * Esta clase NO es thread-safe deliberadamente.
 * La exclusión mutua será responsabilidad de la clase Monitor.
 */
public class PetriNet {

    /**
     * [MODEL - INCIDENCE MATRIX]
     *
     * Matriz de incidencia I de la Red de Petri.
     *
     * Convención:
     * incidenceMatrix[place][transition]
     *
     * Cada fila representa una plaza.
     * Cada columna representa una transición.
     *
     * Valores típicos:
     *  -1 : la transición consume un token de la plaza.
     *   0 : la transición no modifica la plaza.
     *  +1 : la transición agrega un token a la plaza.
     */
    private final int[][] incidenceMatrix;

    /**
     * [MODEL - INITIAL MARKING]
     *
     * Marcado inicial M0 de la Red de Petri.
     *
     * Se conserva para representar explícitamente el estado inicial
     * con el que fue creada la red.
     */
    private final int[] initialMarking;

    /**
     * [MODEL - CURRENT STATE]
     *
     * Marcado actual Mj de la Red de Petri.
     *
     * Este es el estado mutable principal del modelo.
     * Solo debe cambiar cuando una transición válida es disparada.
     */
    private int[] currentMarking;

    /** [METADATA] Cantidad de plazas de la red. */
    private final int placesCount;

    /** [METADATA] Cantidad de transiciones de la red. */
    private final int transitionsCount;

    /**
     * Construye una Red de Petri genérica.
     *
     * @param incidenceMatrix matriz de incidencia [plaza][transición]
     * @param initialMarking marcado inicial de la red
     */
    public PetriNet(int[][] incidenceMatrix, int[] initialMarking) {
        // [VALIDATION]
        // La configuración debe representar una matriz válida y
        // un marcado compatible antes de crear el objeto.
        validateConfiguration(incidenceMatrix, initialMarking);

        /*
         * [DEFENSIVE-COPY]
         * Se copian los datos recibidos para impedir que quien creó
         * la PetriNet pueda modificar posteriormente su estado interno.
         */
        this.incidenceMatrix = copyMatrix(incidenceMatrix);
        this.initialMarking =
                Arrays.copyOf(initialMarking, initialMarking.length);
        this.currentMarking =
                Arrays.copyOf(initialMarking, initialMarking.length);

        this.placesCount = incidenceMatrix.length;
        this.transitionsCount = incidenceMatrix[0].length;
    }

    /**
     * [ENABLING]
     *
     * Determina si una transición está sensibilizada para el marcado actual.
     *
     * Para esta implementación basada en la matriz de incidencia,
     * una transición puede dispararse si el marcado resultante no
     * contiene cantidades negativas de tokens.
     *
     * @param transition índice de la transición
     * @return true si puede dispararse; false en caso contrario
     */
    public boolean isEnabled(int transition) {
        validateTransition(transition);

        /*
         * [STATE-EQUATION / ENABLED CHECK]
         *
         * Se evalúa la columna correspondiente a la transición:
         *
         * M' = M + I(:, transition)
         *
         * Si alguna plaza quedara con tokens negativos,
         * la transición no puede dispararse.
         */
        for (int place = 0; place < placesCount; place++) {
            int nextTokens =
                    currentMarking[place]
                            + incidenceMatrix[place][transition];

            if (nextTokens < 0) {
                return false;
            }
        }

        return true;
    }

    /**
     * [STATE-EQUATION]
     *
     * Calcula el marcado que resultaría de disparar una transición,
     * pero NO modifica todavía el estado interno de la red.
     *
     * @param transition transición a evaluar
     * @return nuevo marcado calculado
     */
    public int[] calculateNextMarking(int transition) {
        validateTransition(transition);

        if (!isEnabled(transition)) {
            throw new IllegalStateException(
                    "Transition T" + transition + " is not enabled"
            );
        }

        // [DEFENSIVE-COPY]
        // El cálculo se realiza sobre una copia del marcado actual.
        int[] nextMarking =
                Arrays.copyOf(currentMarking, currentMarking.length);

        /*
         * [STATE-EQUATION]
         *
         * Para una única transición:
         *
         * M(j+1) = M(j) + I(:, transition)
         */
        for (int place = 0; place < placesCount; place++) {
            nextMarking[place] +=
                    incidenceMatrix[place][transition];
        }

        return nextMarking;
    }

    /**
     * [FIRING]
     *
     * Intenta disparar una transición.
     *
     * Si está sensibilizada, actualiza el marcado.
     * Si no lo está, el estado de la red permanece sin cambios.
     *
     * @param transition transición solicitada
     * @return true si se disparó; false si no estaba sensibilizada
     */
    public boolean fire(int transition) {
        validateTransition(transition);

        if (!isEnabled(transition)) {
            return false;
        }

        /*
         * [STATE UPDATE]
         * Único punto de este método en el que se modifica
         * efectivamente el marcado actual.
         */
        currentMarking = calculateNextMarking(transition);

        return true;
    }

    /**
     * [STATE QUERY]
     *
     * Devuelve el marcado actual.
     *
     * Se devuelve una copia para impedir modificaciones externas
     * sobre el estado interno de la PetriNet.
     */
    public int[] getCurrentMarking() {
        return Arrays.copyOf(
                currentMarking,
                currentMarking.length
        );
    }

    /**
     * [STATE QUERY]
     *
     * Devuelve una copia del marcado inicial.
     */
    public int[] getInitialMarking() {
        return Arrays.copyOf(
                initialMarking,
                initialMarking.length
        );
    }

    /** @return cantidad de plazas de la red */
    public int getPlacesCount() {
        return placesCount;
    }

    /** @return cantidad de transiciones de la red */
    public int getTransitionsCount() {
        return transitionsCount;
    }

    /**
     * [VALIDATION]
     *
     * Garantiza que el índice recibido corresponda
     * a una transición existente.
     */
    private void validateTransition(int transition) {
        if (transition < 0 || transition >= transitionsCount) {
            throw new IllegalArgumentException(
                    "Invalid transition index: " + transition
            );
        }
    }

    /**
     * [CONFIGURATION VALIDATION]
     *
     * Verifica invariantes estructurales mínimos necesarios para
     * representar la red.
     */
    private static void validateConfiguration(
            int[][] incidenceMatrix,
            int[] initialMarking
    ) {
        if (incidenceMatrix == null || initialMarking == null) {
            throw new IllegalArgumentException(
                    "Incidence matrix and initial marking cannot be null"
            );
        }

        if (incidenceMatrix.length == 0) {
            throw new IllegalArgumentException(
                    "Incidence matrix cannot be empty"
            );
        }

        /*
         * Cada fila representa una plaza.
         * Por eso la cantidad de filas debe coincidir con
         * la cantidad de componentes del marcado.
         */
        if (incidenceMatrix.length != initialMarking.length) {
            throw new IllegalArgumentException(
                    "The number of matrix rows must match the number of places"
            );
        }

        if (incidenceMatrix[0] == null
                || incidenceMatrix[0].length == 0) {
            throw new IllegalArgumentException(
                    "Petri net must contain at least one transition"
            );
        }

        int transitions = incidenceMatrix[0].length;

        /*
         * [MATRIX INVARIANT]
         * Todas las filas deben poseer la misma cantidad
         * de columnas.
         */
        for (int[] row : incidenceMatrix) {
            if (row == null || row.length != transitions) {
                throw new IllegalArgumentException(
                        "Incidence matrix must be rectangular"
                );
            }
        }

        /*
         * [MARKING INVARIANT]
         * Un marcado válido nunca puede contener
         * cantidades negativas de tokens.
         */
        for (int tokens : initialMarking) {
            if (tokens < 0) {
                throw new IllegalArgumentException(
                        "Initial marking cannot contain negative tokens"
                );
            }
        }
    }

    /**
     * [DEFENSIVE-COPY]
     *
     * Realiza una copia profunda de la matriz.
     * Copiar solamente int[][] no alcanza porque cada fila
     * también es un array independiente.
     */
    private static int[][] copyMatrix(int[][] matrix) {
        int[][] copy = new int[matrix.length][];

        for (int i = 0; i < matrix.length; i++) {
            copy[i] =
                    Arrays.copyOf(matrix[i], matrix[i].length);
        }

        return copy;
    }
}

/* ~~~~~~~~~ INFO ~~~~~~~~~ */

/*
* Unica matriz de incidencia para determinar sensibilización 
* funciona correctamente bajo las características de la RdP 
* ordinaria/pura que estamos ejecutando. Una representación 
* totalmente general de redes con auto-bucles o arcos 
* especiales podría necesitar Pre, Post u otras matrices.
* No hace falta resolver hoy un problema que nuestro TP no tiene. 
*/

/* 
* NO es THREAD-SAFE
*/


/*~~~~~~~~~~~~~~~~~~~~~~~~~*/