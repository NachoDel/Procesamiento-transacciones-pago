package verification;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * [T4.1.3 - T-INVARIANT ANALYZER]
 *
 * Analiza el log generado por T4.1.1 y verifica las secuencias
 * correspondientes a los T-invariantes de la Red de Petri.
 *
 * [CONCURRENCY]
 *
 * La secuencia global puede contener interleaving entre distintos
 * workers. Por ese motivo, las expresiones regulares se aplican
 * sobre la proyección de transiciones correspondiente a cada
 * worker lógico.
 *
 * Formato esperado:
 *
 * SEQUENCE|THREAD|TRANSITION|TIMESTAMP|POLICY|SEED|CONFIG
 */
public final class TInvariantAnalyzer {

    /*
     * [T-INVARIANT PATHS]
     *
     * IT1 completo:
     * T0 + (T1 T2 T3) + T9
     *
     * IT2 completo:
     * T0 + (T4 T5) + T9
     *
     * IT3 completo:
     * T0 + (T6 T7 T8) + T9
     *
     * T0 y T9 son ejecutadas por workers comunes.
     * Las regex reconocen la parte específica de cada flujo.
     */

    private static final Pattern IT1_PATTERN =
            Pattern.compile(
                    "\\bT1\\s+T2\\s+T3\\b"
            );

    private static final Pattern IT2_PATTERN =
            Pattern.compile(
                    "\\bT4\\s+T5\\b"
            );

    private static final Pattern IT3_PATTERN =
            Pattern.compile(
                    "\\bT6\\s+T7\\s+T8\\b"
            );

    /*
     * [COMPLETE-SEQUENCE]
     *
     * Permiten cero o más ciclos completos del worker.
     */
    private static final Pattern H1_COMPLETE_PATTERN =
            Pattern.compile(
                    "^(?:T1 T2 T3(?: |$))*$"
            );

    private static final Pattern H2_COMPLETE_PATTERN =
            Pattern.compile(
                    "^(?:T4 T5(?: |$))*$"
            );

    private static final Pattern H3_COMPLETE_PATTERN =
            Pattern.compile(
                    "^(?:T6 T7 T8(?: |$))*$"
            );

    /*
     * [VALID-PREFIX]
     *
     * Permiten detectar un ciclo correcto pero incompleto
     * al final del log.
     *
     * Ejemplos:
     *
     * H1: T1
     * H1: T1 T2
     * H3: T6 T7
     */
    private static final Pattern H1_PREFIX_PATTERN =
            Pattern.compile(
                    "^(?:T1 T2 T3(?: |$))*(?:T1(?: T2)?)?$"
            );

    private static final Pattern H2_PREFIX_PATTERN =
            Pattern.compile(
                    "^(?:T4 T5(?: |$))*(?:T4)?$"
            );

    private static final Pattern H3_PREFIX_PATTERN =
            Pattern.compile(
                    "^(?:T6 T7 T8(?: |$))*(?:T6(?: T7)?)?$"
            );

    /*
     * H0 solamente ejecuta T0.
     * H4 solamente ejecuta T9.
     */
    private static final Pattern H0_PATTERN =
            Pattern.compile(
                    "^(?:T0(?: |$))*$"
            );

    private static final Pattern H4_PATTERN =
            Pattern.compile(
                    "^(?:T9(?: |$))*$"
            );

    /**
     * Analiza un archivo completo de log.
     *
     * @param logPath archivo generado por ExecutionLogger
     * @return resultado del análisis
     * @throws IOException si el archivo no puede leerse
     */
    public AnalysisResult analyze(Path logPath)
            throws IOException {

        Objects.requireNonNull(
                logPath,
                "Log path cannot be null"
        );

        return analyzeLines(
                Files.readAllLines(logPath)
        );
    }

    /**
     * Analiza directamente líneas de log.
     *
     * Esta variante facilita los tests unitarios.
     */
    public AnalysisResult analyzeLines(
            List<String> lines
    ) {

        Objects.requireNonNull(
                lines,
                "Log lines cannot be null"
        );

        List<LogEvent> events =
                parseEvents(lines);

        Map<String, List<String>> transitionsByWorker =
                createWorkerMap();

        List<String> globalTransitions =
                new ArrayList<>();

        List<String> issues =
                new ArrayList<>();

        long expectedSequence = 1L;

        for (LogEvent event : events) {

            /*
             * [LOG-ORDER]
             *
             * ExecutionLogger comienza en 1 y escribe
             * secuencias consecutivas.
             */
            if (event.sequence != expectedSequence) {

                issues.add(
                        "Invalid event sequence: expected "
                                + expectedSequence
                                + " but found "
                                + event.sequence
                );
            }

            expectedSequence++;

            globalTransitions.add(
                    event.transition
            );

            List<String> workerTransitions =
                    transitionsByWorker.get(
                            event.threadName
                    );

            if (workerTransitions == null) {

                issues.add(
                        "Unknown worker: "
                                + event.threadName
                );

                continue;
            }

            workerTransitions.add(
                    event.transition
            );
        }

        String h0Sequence =
                join(
                        transitionsByWorker.get("H0")
                );

        String h1Sequence =
                join(
                        transitionsByWorker.get("H1")
                );

        String h2Sequence =
                join(
                        transitionsByWorker.get("H2")
                );

        String h3Sequence =
                join(
                        transitionsByWorker.get("H3")
                );

        String h4Sequence =
                join(
                        transitionsByWorker.get("H4")
                );

        /*
         * [WORKER RESPONSIBILITY]
         *
         * Detectamos si H0 o H4 ejecutaron una transición
         * que no les corresponde.
         */
        if (!H0_PATTERN.matcher(h0Sequence).matches()) {

            issues.add(
                    "Invalid H0 sequence: "
                            + h0Sequence
            );
        }

        if (!H4_PATTERN.matcher(h4Sequence).matches()) {

            issues.add(
                    "Invalid H4 sequence: "
                            + h4Sequence
            );
        }

        /*
         * [REGEX VALIDATION]
         *
         * Cada camino se clasifica como:
         *
         * - completo;
         * - incompleto;
         * - inválido.
         */
        validatePathSequence(
                "IT1",
                h1Sequence,
                H1_COMPLETE_PATTERN,
                H1_PREFIX_PATTERN,
                issues
        );

        validatePathSequence(
                "IT2",
                h2Sequence,
                H2_COMPLETE_PATTERN,
                H2_PREFIX_PATTERN,
                issues
        );

        validatePathSequence(
                "IT3",
                h3Sequence,
                H3_COMPLETE_PATTERN,
                H3_PREFIX_PATTERN,
                issues
        );

        /*
         * [COUNT]
         *
         * Las regex cuentan ciclos completos de cada flujo.
         */
        int it1Count =
                countMatches(
                        IT1_PATTERN,
                        h1Sequence
                );

        int it2Count =
                countMatches(
                        IT2_PATTERN,
                        h2Sequence
                );

        int it3Count =
                countMatches(
                        IT3_PATTERN,
                        h3Sequence
                );

        int totalCompleted =
                it1Count
                        + it2Count
                        + it3Count;

        /*
         * [COMMON TRANSITIONS]
         *
         * Cada T-invariante completo requiere exactamente:
         *
         * - un T0;
         * - un T9.
         *
         * Por lo tanto:
         *
         * count(T0) = count(T9)
         *           = IT1 + IT2 + IT3
         */
        int t0Count =
                countTransition(
                        events,
                        "T0"
                );

        int t9Count =
                countTransition(
                        events,
                        "T9"
                );

        if (t0Count != totalCompleted) {

            issues.add(
                    "T0 count does not match completed invariants: "
                            + "T0=" + t0Count
                            + ", completed=" + totalCompleted
            );
        }

        if (t9Count != totalCompleted) {

            issues.add(
                    "T9 count does not match completed invariants: "
                            + "T9=" + t9Count
                            + ", completed=" + totalCompleted
            );
        }

        return new AnalysisResult(
                String.join(
                        " ",
                        globalTransitions
                ),
                it1Count,
                it2Count,
                it3Count,
                totalCompleted,
                t0Count,
                t9Count,
                issues.isEmpty(),
                issues
        );
    }

    /**
     * Construye las secuencias específicas de cada worker.
     */
    private static Map<String, List<String>>
    createWorkerMap() {

        Map<String, List<String>> map =
                new LinkedHashMap<>();

        map.put("H0", new ArrayList<>());
        map.put("H1", new ArrayList<>());
        map.put("H2", new ArrayList<>());
        map.put("H3", new ArrayList<>());
        map.put("H4", new ArrayList<>());

        return map;
    }

    /**
     * Parsea el contrato establecido por T4.1.1.
     */
    private static List<LogEvent> parseEvents(
            List<String> lines
    ) {

        List<LogEvent> events =
                new ArrayList<>();

        for (String line : lines) {

            if (line == null) {
                throw new IllegalArgumentException(
                        "Log line cannot be null"
                );
            }

            if (line.isBlank()) {
                continue;
            }

            String[] fields =
                    line.split("\\|", -1);

            if (fields.length != 7) {

                throw new IllegalArgumentException(
                        "Invalid log line format: "
                                + line
                );
            }

            long sequence;

            try {

                sequence =
                        Long.parseLong(
                                fields[0]
                        );

            } catch (NumberFormatException exception) {

                throw new IllegalArgumentException(
                        "Invalid sequence field: "
                                + fields[0],
                        exception
                );
            }

            if (sequence <= 0) {

                throw new IllegalArgumentException(
                        "Sequence must be positive"
                );
            }

            String threadName =
                    fields[1];

            if (threadName.isBlank()) {

                throw new IllegalArgumentException(
                        "Thread field cannot be blank"
                );
            }

            String transition =
                    fields[2];

            if (!transition.matches("T\\d+")) {

                throw new IllegalArgumentException(
                        "Invalid transition field: "
                                + transition
                );
            }

            validateLongField(
                    fields[3],
                    "timestamp"
            );

            if (fields[4].isBlank()) {

                throw new IllegalArgumentException(
                        "Policy field cannot be blank"
                );
            }

            validateLongField(
                    fields[5],
                    "seed"
            );

            if (fields[6].isBlank()) {

                throw new IllegalArgumentException(
                        "Config field cannot be blank"
                );
            }

            events.add(
                    new LogEvent(
                            sequence,
                            threadName,
                            transition
                    )
            );
        }

        return events;
    }

    /**
     * Determina si la secuencia de un camino está:
     *
     * - completa;
     * - incompleta;
     * - inválida.
     */
    private static void validatePathSequence(
            String invariantName,
            String sequence,
            Pattern completePattern,
            Pattern prefixPattern,
            List<String> issues
    ) {

        if (completePattern
                .matcher(sequence)
                .matches()) {

            return;
        }

        if (prefixPattern
                .matcher(sequence)
                .matches()) {

            issues.add(
                    "Incomplete "
                            + invariantName
                            + " sequence: "
                            + sequence
            );

            return;
        }

        issues.add(
                "Invalid "
                        + invariantName
                        + " sequence: "
                        + sequence
        );
    }

    private static int countMatches(
            Pattern pattern,
            String sequence
    ) {

        int count = 0;

        Matcher matcher =
                pattern.matcher(sequence);

        while (matcher.find()) {
            count++;
        }

        return count;
    }

    private static int countTransition(
            List<LogEvent> events,
            String transition
    ) {

        int count = 0;

        for (LogEvent event : events) {

            if (event.transition.equals(transition)) {
                count++;
            }
        }

        return count;
    }

    private static String join(
            List<String> transitions
    ) {

        return String.join(
                " ",
                transitions
        );
    }

    private static void validateLongField(
            String value,
            String fieldName
    ) {

        try {

            Long.parseLong(value);

        } catch (NumberFormatException exception) {

            throw new IllegalArgumentException(
                    "Invalid "
                            + fieldName
                            + " field: "
                            + value,
                    exception
            );
        }
    }

    /**
     * Evento mínimo necesario para realizar el análisis.
     */
    private static final class LogEvent {

        private final long sequence;

        private final String threadName;

        private final String transition;

        private LogEvent(
                long sequence,
                String threadName,
                String transition
        ) {

            this.sequence = sequence;
            this.threadName = threadName;
            this.transition = transition;
        }
    }

    /**
     * Resultado inmutable del análisis.
     */
    public static final class AnalysisResult {

        private final String transitionSequence;

        private final int it1Count;

        private final int it2Count;

        private final int it3Count;

        private final int totalCompleted;

        private final int t0Count;

        private final int t9Count;

        private final boolean valid;

        private final List<String> issues;

        private AnalysisResult(
                String transitionSequence,
                int it1Count,
                int it2Count,
                int it3Count,
                int totalCompleted,
                int t0Count,
                int t9Count,
                boolean valid,
                List<String> issues
        ) {

            this.transitionSequence =
                    transitionSequence;

            this.it1Count = it1Count;
            this.it2Count = it2Count;
            this.it3Count = it3Count;
            this.totalCompleted = totalCompleted;
            this.t0Count = t0Count;
            this.t9Count = t9Count;
            this.valid = valid;

            this.issues =
                    List.copyOf(issues);
        }

        public String getTransitionSequence() {
            return transitionSequence;
        }

        public int getIt1Count() {
            return it1Count;
        }

        public int getIt2Count() {
            return it2Count;
        }

        public int getIt3Count() {
            return it3Count;
        }

        public int getTotalCompleted() {
            return totalCompleted;
        }

        public int getT0Count() {
            return t0Count;
        }

        public int getT9Count() {
            return t9Count;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getIssues() {
            return issues;
        }
    }
}