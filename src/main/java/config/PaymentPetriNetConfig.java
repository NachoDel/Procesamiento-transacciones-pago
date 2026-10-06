package config;

import petri.PetriNet;

import java.util.Set;

import timing.TransitionSemantics;

/**
 * [PAYMENT-PETRI-NET-CONFIGURATION]
 *
 * Centraliza la configuración formal de la Red de Petri
 * correspondiente al sistema de procesamiento de pagos.
 *
 * [FORMAL-MODEL]
 * Los valores provienen del análisis definitivo realizado
 * sobre la red oficial en PIPE.
 *
 * La lógica genérica permanece dentro de PetriNet.
 * Esta clase solamente contiene datos específicos del modelo.
 */
public final class PaymentPetriNetConfig {

    /**
     * [MODEL-SIZE]
     *
     * La red oficial contiene P0..P9 y T0..T9.
     */
    public static final int PLACES_COUNT = 10;

    public static final int TRANSITIONS_COUNT = 10;

    /**
     * [HIGH-RISK-FLOW]
     *
     * T4 inicia el flujo de alto riesgo y adquiere
     * simultáneamente los recursos representados por P7 y P8.
     *
     * Este valor podrá utilizarse para configurar PriorityPolicy.
     */
    public static final int HIGH_RISK_TRANSITION = 4;

    /**
     * [INITIAL-MARKING]
     *
     * Orden:
     * P0 P1 P2 P3 P4 P5 P6 P7 P8 P9
     *
     * M0 = (3,0,0,0,0,0,0,1,1,0)
     */
    private static final int[] INITIAL_MARKING = {
            3, 0, 0, 0, 0,
            0, 0, 1, 1, 0
    };

    /**
     * [INCIDENCE-MATRIX]
     *
     * Convención:
     *
     * incidenceMatrix[place][transition]
     *
     * Filas    -> P0..P9
     * Columnas -> T0..T9
     */
    private static final int[][] INCIDENCE_MATRIX = {
            // T0  T1  T2  T3  T4  T5  T6  T7  T8  T9

            {-1,  0,  0,  0,  0,  0,  0,  0,  0,  1}, // P0
            { 1, -1,  0,  0, -1,  0, -1,  0,  0,  0}, // P1
            { 0,  1, -1,  0,  0,  0,  0,  0,  0,  0}, // P2
            { 0,  0,  1, -1,  0,  0,  0,  0,  0,  0}, // P3
            { 0,  0,  0,  0,  1, -1,  0,  0,  0,  0}, // P4
            { 0,  0,  0,  0,  0,  0,  1, -1,  0,  0}, // P5
            { 0,  0,  0,  0,  0,  0,  0,  1, -1,  0}, // P6
            { 0, -1,  0,  1, -1,  1,  0,  0,  0,  0}, // P7
            { 0,  0,  0,  0, -1,  1, -1,  0,  1,  0}, // P8
            { 0,  0,  0,  1,  0,  1,  0,  0,  1, -1}  // P9
    };

    /**
     * [TIMED-TRANSITIONS]
     *
     * Transiciones temporales definidas por el modelo:
     *
     * T2, T3, T5, T7 y T8.
     *
     * Las restantes son inmediatas.
     */
    private static final Set<Integer> TIMED_TRANSITIONS =
            Set.of(2, 3, 5, 7, 8);

    private PaymentPetriNetConfig() {
        /*
         * [UTILITY-CLASS]
         *
         * Esta clase representa configuración estática
         * y no necesita instancias.
         */
    }

    /**
     * [PETRI-NET-FACTORY]
     *
     * Construye una nueva Red de Petri comenzando siempre
     * desde el marcado inicial oficial.
     */
    public static PetriNet createPetriNet() {
        return new PetriNet(
                INCIDENCE_MATRIX,
                INITIAL_MARKING
        );
    }

    /**
     * [TRANSITION-TYPE]
     *
     * Indica si una transición pertenece al conjunto
     * de transiciones temporales del modelo.
     */
    public static boolean isTimedTransition(int transition) {
        validateTransition(transition);

        return TIMED_TRANSITIONS.contains(transition);
    }

    /**
     * [TRANSITION-TYPE]
     *
     * Toda transición de la red que no sea temporal
     * se considera inmediata.
     */
    public static boolean isImmediateTransition(int transition) {
        validateTransition(transition);

        return !TIMED_TRANSITIONS.contains(transition);
    }

    private static void validateTransition(int transition) {

        if (transition < 0
                || transition >= TRANSITIONS_COUNT) {

            throw new IllegalArgumentException(
                    "Invalid transition index: " + transition
            );
        }
    }

    /**
     * [TRANSITION-SEMANTICS]
     *
     * Construye la clasificación temporal oficial de la red:
     *
     * T2, T3, T5, T7 y T8 -> temporales
     * resto                  -> inmediatas
     */
    public static TransitionSemantics createTransitionSemantics() {

        return TransitionSemantics.fromTimedTransitions(
                TRANSITIONS_COUNT,
                TIMED_TRANSITIONS
        );
    }
}