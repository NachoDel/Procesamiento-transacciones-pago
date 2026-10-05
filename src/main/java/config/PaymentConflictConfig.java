package config;

import policy.ConflictGroup;

import java.util.List;
import java.util.Set;

/**
 * [PAYMENT-CONFLICT-CONFIGURATION]
 *
 * Centraliza los conflictos estructurales de la
 * Red de Petri oficial.
 *
 * [ROUTING-CONFLICT]
 *
 * Desde P1 compiten:
 *
 * T1 -> pago con tarjeta
 * T4 -> pago de alto riesgo
 * T6 -> transferencia bancaria
 *
 * Policy debe resolver este conflicto cuando existan
 * múltiples alternativas disponibles simultáneamente.
 */
public final class PaymentConflictConfig {

    private static final List<ConflictGroup> CONFLICT_GROUPS =
            List.of(
                    new ConflictGroup(
                            Set.of(1, 4, 6)
                    )
            );

    private PaymentConflictConfig() {
        /*
         * [UTILITY-CLASS]
         *
         * Configuración estática.
         */
    }

    public static List<ConflictGroup> getConflictGroups() {
        return CONFLICT_GROUPS;
    }
}