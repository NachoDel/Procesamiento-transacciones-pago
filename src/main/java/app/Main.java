package app;

import config.PaymentTimingConfig;
import experiment.CampaignPlan;
import experiment.ExperimentRunConfig;
import experiment.ExperimentRunResult;
import experiment.ExperimentRunner;
import timing.TransitionTimingConfig;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Duration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * [ENTRY-POINT]
 *
 * Punto de entrada del sistema de procesamiento concurrente de pagos.
 * Configura una corrida individual y delega toda la orquestación en
 * ExperimentRunner. No contiene lógica de PetriNet, Monitor ni Workers.
 */
public final class Main {

    private static final long DEFAULT_SEED = 2026L;
    private static final double DEFAULT_SCALE = 1.0;

    private Main() {
        // [UTILITY-CLASS]
    }

    public static void main(String[] args)
            throws IOException, InterruptedException {

        execute(args, System.out);
    }

    /**
     * [TESTABLE-ENTRY-POINT]
     *
     * Permite comprobar la interfaz de consola sin sustituir System.out.
     * La ayuda y los argumentos inválidos no crean Workers ni logs.
     */
    static void execute(String[] args, PrintStream output)
            throws IOException, InterruptedException {

        Objects.requireNonNull(args, "Arguments cannot be null");
        Objects.requireNonNull(output, "Output cannot be null");

        if (args.length == 1 && "--help".equals(args[0])) {
            printUsage(output);
            return;
        }

        Options options = parseOptions(args);

        TransitionTimingConfig timing =
                PaymentTimingConfig.createScaled(options.scale());

        String timingName = options.scale() == DEFAULT_SCALE
                ? "BASELINE"
                : "SCALED_" + Double.toString(options.scale())
                        .replace('.', 'p');

        Path logPath = options.logPath() != null
                ? options.logPath()
                : Path.of(
                        "results",
                        "logs",
                        options.policyMode().name()
                                + "_" + timingName
                                + "_" + options.seed()
                                + "_TARGET_" + options.target()
                                + ".log"
                );

        ExperimentRunConfig config =
                new ExperimentRunConfig(
                        options.policyMode(),
                        options.seed(),
                        timingName,
                        timing,
                        options.target(),
                        logPath,
                        options.executionTimeout(),
                        CampaignPlan.getShutdownTimeout()
                );

        output.println("===== CORRIDA DE PAGOS =====");
        output.println("Politica: " + config.getPolicyMode());
        output.println("Seed: " + config.getSeed());
        output.println("Configuracion temporal: " + config.getTimingName());
        output.println("Factor temporal: " + options.scale());
        output.println("Target: " + config.getTarget());
        output.println("Log: " + config.getLogPath());
        output.flush();

        ExperimentRunResult result =
                new ExperimentRunner().run(config);

        output.println();
        output.println("===== RESULTADO =====");
        output.println("Admisiones T0: " + result.getAdmissions());
        output.println("Finalizaciones T9: " + result.getCompletions());
        output.println("IT1 (tarjeta): " + result.getIt1Count());
        output.println("IT2 (alto riesgo): " + result.getIt2Count());
        output.println("IT3 (transferencia): " + result.getIt3Count());
        output.println("Total T-invariantes: " + result.getTotalTInvariants());
        output.printf(Locale.ROOT, "Duracion: %.3f s%n",
                result.getDurationNanos() / 1_000_000_000.0);
        output.println("P-invariantes validos: " + result.arePInvariantsValid());
        output.println("T-invariantes validos: " + result.areTInvariantsValid());
        output.println("Workers terminados: " + result.areWorkersTerminated());
        output.println("Corrida exitosa: " + result.isSuccessful());
        output.println("Log: " + result.getLogPath());
        output.flush();

        if (!result.isSuccessful()) {
            throw new IllegalStateException(
                    "La corrida no cumplio los criterios de aceptacion: "
                            + result.getRunId()
            );
        }
    }

    /**
     * [CLI-VALIDATION]
     *
     * Opciones en formato --clave=valor, independientes del shell.
     * Se rechazan opciones desconocidas y repetidas para evitar
     * ejecuciones con parámetros distintos de los pretendidos.
     */
    private static Options parseOptions(String[] args) {

        ExperimentRunConfig.PolicyMode policyMode =
                ExperimentRunConfig.PolicyMode.RANDOM;
        long seed = DEFAULT_SEED;
        int target = CampaignPlan.TARGET;
        double scale = DEFAULT_SCALE;
        Duration executionTimeout = CampaignPlan.getExecutionTimeout();
        Path logPath = null;
        Set<String> usedOptions = new HashSet<>();

        for (String argument : args) {
            if (argument == null || !argument.startsWith("--")) {
                throw new IllegalArgumentException(
                        "Argumento invalido: " + argument + ". Use --help."
                );
            }

            int separator = argument.indexOf('=');
            if (separator < 3 || separator == argument.length() - 1) {
                throw new IllegalArgumentException(
                        "Use el formato --opcion=valor: " + argument
                );
            }

            String key = argument.substring(0, separator);
            String value = argument.substring(separator + 1);

            if (value.isBlank()) {
                throw new IllegalArgumentException("Valor vacio para " + key);
            }
            if (!usedOptions.add(key)) {
                throw new IllegalArgumentException("Opcion repetida: " + key);
            }

            switch (key) {
                case "--policy" -> {
                    try {
                        policyMode = ExperimentRunConfig.PolicyMode.valueOf(
                                value.toUpperCase(Locale.ROOT)
                        );
                    } catch (IllegalArgumentException exception) {
                        throw new IllegalArgumentException(
                                "--policy debe ser RANDOM o PRIORITY", exception
                        );
                    }
                }
                case "--seed" -> seed = parseLong(value, key);
                case "--target" -> {
                    target = parseInt(value, key);
                    if (target <= 0) {
                        throw new IllegalArgumentException("--target debe ser positivo");
                    }
                }
                case "--scale" -> {
                    try {
                        scale = Double.parseDouble(value);
                    } catch (NumberFormatException exception) {
                        throw new IllegalArgumentException(
                                "--scale debe ser un numero decimal valido", exception
                        );
                    }
                    if (!Double.isFinite(scale) || scale <= 0) {
                        throw new IllegalArgumentException(
                                "--scale debe ser positivo y finito"
                        );
                    }
                }
                case "--timeout-seconds" -> {
                    long seconds = parseLong(value, key);
                    if (seconds <= 0) {
                        throw new IllegalArgumentException(
                                "--timeout-seconds debe ser positivo"
                        );
                    }
                    executionTimeout = Duration.ofSeconds(seconds);
                }
                case "--log" -> logPath = Path.of(value);
                default -> throw new IllegalArgumentException(
                        "Opcion desconocida: " + key + ". Use --help."
                );
            }
        }

        return new Options(
                policyMode,
                seed,
                target,
                scale,
                executionTimeout,
                logPath
        );
    }

    private static long parseLong(String value, String key) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Valor entero invalido para " + key + ": " + value,
                    exception
            );
        }
    }

    private static int parseInt(String value, String key) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(
                    "Valor entero invalido para " + key + ": " + value,
                    exception
            );
        }
    }

    private static void printUsage(PrintStream output) {
        output.println("Uso: java -cp target/classes app.Main [opciones]");
        output.println("  --help                 Mostrar esta ayuda sin ejecutar la corrida");
        output.println("  --policy=RANDOM         Politica RANDOM (default) o PRIORITY");
        output.println("  --seed=2026            Semilla pseudoaleatoria (default: 2026)");
        output.println("  --target=200           Transacciones completas (default: 200)");
        output.println("  --scale=1.0            Factor temporal (default: 1.0)");
        output.println("  --timeout-seconds=60   Timeout de ejecucion (default: 60)");
        output.println("  --log=ruta.log         Archivo de log (default: results/logs/...)");
        output.println();
        output.println("Ejemplo: java -cp target/classes app.Main --policy=PRIORITY --target=200");
        output.println("Nota: ejecutar desde la raiz del proyecto.");
    }

    private record Options(
            ExperimentRunConfig.PolicyMode policyMode,
            long seed,
            int target,
            double scale,
            Duration executionTimeout,
            Path logPath
    ) {
    }
}
