package config;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import policy.ConflictGroup;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * [PAYMENT-CONFLICT-CONFIG TESTS]
 *
 * Verifica que la configuración Java de conflictos coincida
 * con el análisis formal de la Red de Petri oficial.
 */
class PaymentConflictConfigTest {

    /**
     * [ROUTING-CONFLICT]
     *
     * Desde P1 existen tres alternativas excluyentes:
     *
     * T1 -> pago con tarjeta
     * T4 -> pago de alto riesgo
     * T6 -> transferencia bancaria
     *
     * Es el conflicto que deben resolver RandomPolicy
     * y PriorityPolicy.
     */
    @Test
    void shouldDefineRoutingConflictBetweenThreeFlows() {

        // [ACT]
        List<ConflictGroup> groups =
                PaymentConflictConfig.getConflictGroups();

        // [ASSERT - GROUP COUNT]
        assertEquals(
                1,
                groups.size()
        );

        // [ASSERT - ROUTING TRANSITIONS]
        assertEquals(
                Set.of(1, 4, 6),
                groups.get(0).getTransitions()
        );
    }
}