package config;

import worker.WorkerDefinition;

import java.util.List;

/**
 * [PAYMENT-SYSTEM-CONFIGURATION]
 *
 * Define la distribución oficial de responsabilidades entre
 * los cinco Workers del sistema de procesamiento de pagos.
 *
 * [FORMAL-MODEL]
 * La configuración proviene del análisis formal de la Red de Petri:
 *
 * H0 -> T0
 * H1 -> T1, T2, T3
 * H2 -> T4, T5
 * H3 -> T6, T7, T8
 * H4 -> T9
 *
 * [DESIGN]
 * El conocimiento específico de la red queda centralizado aquí
 * y no dentro de Worker ni Monitor.
 */
public final class PaymentWorkerConfig {

    private static final List<WorkerDefinition> DEFINITIONS =
            List.of(
                    new WorkerDefinition(
                            "H0",
                            List.of(0)
                    ),
                    new WorkerDefinition(
                            "H1",
                            List.of(1, 2, 3)
                    ),
                    new WorkerDefinition(
                            "H2",
                            List.of(4, 5)
                    ),
                    new WorkerDefinition(
                            "H3",
                            List.of(6, 7, 8)
                    ),
                    new WorkerDefinition(
                            "H4",
                            List.of(9)
                    )
            );

    private PaymentWorkerConfig() {
        /*
         * [UTILITY-CLASS]
         *
         * La configuración es estática y no necesita instancias.
         */
    }

    /**
     * [WORKER-DEFINITIONS]
     *
     * Devuelve la configuración estructural oficial de los Workers.
     */
    public static List<WorkerDefinition> getDefinitions() {
        return DEFINITIONS;
    }
}

/**
 * Aca se definen los Workers y 
 * sus secuencias de transiciones.
 */