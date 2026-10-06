package config;

import worker.WorkerDefinition;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * [PAYMENT-WORKER-CONFIG TESTS]
 *
 * Verifica que la configuración Java coincida con
 * la asignación formal de Workers de la Red de Petri.
 */
class PaymentWorkerConfigTest {

    /**
     * [WORKER-COUNT]
     *
     * El análisis estructural establece exactamente cinco Workers.
     */
    @Test
    void shouldDefineExactlyFiveWorkers() {

        List<WorkerDefinition> definitions =
                PaymentWorkerConfig.getDefinitions();

        assertEquals(
                5,
                definitions.size()
        );
    }

    /**
     * [FORMAL-WORKER-MAPPING]
     *
     * Verifica la asignación definitiva:
     *
     * H0 -> T0
     * H1 -> T1,T2,T3
     * H2 -> T4,T5
     * H3 -> T6,T7,T8
     * H4 -> T9
     */
    @Test
    void shouldMatchFormalWorkerAssignment() {

        List<WorkerDefinition> definitions =
                PaymentWorkerConfig.getDefinitions();

        assertWorker(
                definitions.get(0),
                "H0",
                List.of(0)
        );

        assertWorker(
                definitions.get(1),
                "H1",
                List.of(1, 2, 3)
        );

        assertWorker(
                definitions.get(2),
                "H2",
                List.of(4, 5)
        );

        assertWorker(
                definitions.get(3),
                "H3",
                List.of(6, 7, 8)
        );

        assertWorker(
                definitions.get(4),
                "H4",
                List.of(9)
        );
    }

    /**
     * [IMMUTABLE-CONFIGURATION]
     *
     * La configuración estructural no debe poder modificarse
     * accidentalmente durante una ejecución.
     */
    @Test
    void definitionsShouldBeImmutable() {

        List<WorkerDefinition> definitions =
                PaymentWorkerConfig.getDefinitions();

        assertThrows(
                UnsupportedOperationException.class,
                () -> definitions.add(
                        new WorkerDefinition(
                                "HX",
                                List.of(0)
                        )
                )
        );
    }

    private void assertWorker(
            WorkerDefinition definition,
            String expectedName,
            List<Integer> expectedSequence
    ) {

        assertEquals(
                expectedName,
                definition.getName()
        );

        assertEquals(
                expectedSequence,
                definition.getTransitionSequence()
        );
    }
}