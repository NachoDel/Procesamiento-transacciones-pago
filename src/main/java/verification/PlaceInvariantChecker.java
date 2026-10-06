package verification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * [T4.1.2 - PLACE INVARIANT CHECKER]
 *
 * Verifica los P-invariantes definidos para la Red de Petri
 * del sistema de procesamiento de pagos.
 *
 * [RESPONSIBILITY]
 * Esta clase NO modifica el marcado ni dispara transiciones.
 * Solamente recibe un marcado ya comprometido y comprueba
 * que las propiedades estructurales de la red se mantengan.
 *
 * [CONCURRENCY]
 * La clase no posee estado mutable, por lo que puede utilizarse
 * de forma segura desde el Monitor mientras este mantiene
 * la exclusión mutua.
 */
public final class PlaceInvariantChecker {

    /**
     * [MODEL-SIZE]
     *
     * La red oficial contiene las plazas P0..P9.
     */
    private static final int PLACES_COUNT = 10;

    /**
     * [P-INVARIANT 1]
     *
     * P2 + P3 + P4 + P7 = 1
     */
    private static final int[] IP1_PLACES = {2, 3, 4, 7};
    private static final int IP1_EXPECTED_VALUE = 1;

    /**
     * [P-INVARIANT 2]
     *
     * P4 + P5 + P6 + P8 = 1
     */
    private static final int[] IP2_PLACES = {4, 5, 6, 8};
    private static final int IP2_EXPECTED_VALUE = 1;

    /**
     * [P-INVARIANT 3]
     *
     * P0 + P1 + P2 + P3 + P4 + P5 + P6 + P9 = 3
     */
    private static final int[] IP3_PLACES = {
            0, 1, 2, 3, 4, 5, 6, 9
    };

    private static final int IP3_EXPECTED_VALUE = 3;

    /**
     * Verifica los tres P-invariantes sobre un marcado.
     *
     * @param marking marcado actual de la Red de Petri
     * @return resultado detallado de la verificación
     */
    public VerificationResult verify(int[] marking) {

        validateMarking(marking);

        List<String> violatedInvariants =
                new ArrayList<>();

        verifyInvariant(
                "IP1",
                marking,
                IP1_PLACES,
                IP1_EXPECTED_VALUE,
                violatedInvariants
        );

        verifyInvariant(
                "IP2",
                marking,
                IP2_PLACES,
                IP2_EXPECTED_VALUE,
                violatedInvariants
        );

        verifyInvariant(
                "IP3",
                marking,
                IP3_PLACES,
                IP3_EXPECTED_VALUE,
                violatedInvariants
        );

        return new VerificationResult(
                violatedInvariants.isEmpty(),
                violatedInvariants,
                marking
        );
    }

    /**
     * [CONVENIENCE]
     *
     * Permite consultar directamente si un marcado cumple
     * todos los P-invariantes.
     */
    public boolean isValid(int[] marking) {
        return verify(marking).isValid();
    }

    /**
     * Evalúa un único P-invariante.
     *
     * Si el valor observado es distinto del esperado,
     * agrega una descripción de la violación al resultado.
     */
    private static void verifyInvariant(
            String name,
            int[] marking,
            int[] places,
            int expectedValue,
            List<String> violatedInvariants
    ) {

        int actualValue = sumPlaces(
                marking,
                places
        );

        if (actualValue != expectedValue) {
            violatedInvariants.add(
                    name
                            + " expected=" + expectedValue
                            + " actual=" + actualValue
            );
        }
    }

    /**
     * Suma los tokens presentes en las plazas que forman
     * parte de un P-invariante.
     */
    private static int sumPlaces(
            int[] marking,
            int[] places
    ) {

        int sum = 0;

        for (int place : places) {
            sum += marking[place];
        }

        return sum;
    }

    /**
     * [INPUT VALIDATION]
     *
     * El checker espera un marcado completo de la red oficial.
     */
    private static void validateMarking(int[] marking) {

        if (marking == null) {
            throw new IllegalArgumentException(
                    "Marking cannot be null"
            );
        }

        if (marking.length != PLACES_COUNT) {
            throw new IllegalArgumentException(
                    "Marking must contain exactly "
                            + PLACES_COUNT
                            + " places"
            );
        }

        for (int tokens : marking) {
            if (tokens < 0) {
                throw new IllegalArgumentException(
                        "Marking cannot contain negative tokens"
                );
            }
        }
    }

    /**
     * [VERIFICATION RESULT]
     *
     * Resultado inmutable de verificar un marcado.
     *
     * Conserva:
     * - si todos los invariantes se cumplen;
     * - cuáles fueron violados;
     * - una copia del marcado que fue analizado.
     */
    public static final class VerificationResult {

        private final boolean valid;

        private final List<String> violatedInvariants;

        private final int[] marking;

        private VerificationResult(
                boolean valid,
                List<String> violatedInvariants,
                int[] marking
        ) {

            this.valid = valid;

            this.violatedInvariants =
                    List.copyOf(violatedInvariants);

            this.marking =
                    Arrays.copyOf(
                            marking,
                            marking.length
                    );
        }

        /**
         * @return true si todos los P-invariantes se cumplen
         */
        public boolean isValid() {
            return valid;
        }

        /**
         * @return lista inmutable con las violaciones detectadas
         */
        public List<String> getViolatedInvariants() {
            return violatedInvariants;
        }

        /**
         * @return copia del marcado que fue verificado
         */
        public int[] getMarking() {
            return Arrays.copyOf(
                    marking,
                    marking.length
            );
        }
    }
}
