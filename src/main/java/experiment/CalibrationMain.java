package experiment;

import config.PaymentPetriNetConfig;
import config.PaymentTimingConfig;
import timing.TransitionTimingConfig;

import java.nio.file.Path;
import java.time.Duration;

/**
 * [T4.2.2 - CALIBRATION MAIN]
 *
 * Ejecuta corridas individuales de calibración/control.
 *
 * Estas corridas NO pertenecen a la campaña formal.
 *
 * Uso:
 *
 * RANDOM BASELINE
 * RANDOM VARIANT
 * PRIORITY BASELINE
 * PRIORITY VARIANT
 */
public final class CalibrationMain {

    private static final int TARGET = 200;

    private static final long SEED = 2026L;

    private static final double VARIANT_SCALE = 1.05;

    private static final Duration EXECUTION_TIMEOUT =
            Duration.ofSeconds(60);

    private static final Duration SHUTDOWN_TIMEOUT =
            Duration.ofSeconds(5);

    private CalibrationMain() {
        /*
         * Utility class.
         */
    }

    public static void main(String[] args)
            throws Exception {

        if (args.length != 2) {

            printUsage();

            return;
        }

        String policyName =
                args[0]
                        .trim()
                        .toUpperCase();

        String timingName =
                args[1]
                        .trim()
                        .toUpperCase();

        ExperimentRunConfig.PolicyMode policyMode =
                parsePolicy(
                        policyName
                );

        TransitionTimingConfig timingConfig =
                parseTiming(
                        timingName
                );

        Path logPath =
                Path.of(
                        "results",
                        "calibration",
                        "logs",
                        policyName
                                + "_"
                                + timingName
                                + "_"
                                + SEED
                                + ".log"
                );

        ExperimentRunConfig config =
                new ExperimentRunConfig(
                        policyMode,
                        SEED,
                        timingName,
                        timingConfig,
                        TARGET,
                        logPath,
                        EXECUTION_TIMEOUT,
                        SHUTDOWN_TIMEOUT
                );

        ExperimentRunner runner =
                new ExperimentRunner();

        System.out.println();
        System.out.println(
                "===== CALIBRATION RUN ====="
        );

        System.out.println(
                "Run ID: "
                        + config.getRunId()
        );

        System.out.println(
                "Policy: "
                        + config.getPolicyMode()
        );

        System.out.println(
                "Timing: "
                        + config.getTimingName()
        );

        if (timingName.equals("VARIANT")) {

            System.out.println(
                    "Variant scale: "
                            + VARIANT_SCALE
            );
        }

        if (policyMode
                == ExperimentRunConfig.PolicyMode.PRIORITY) {

            System.out.println(
                    "Priority transition: T"
                            + PaymentPetriNetConfig.HIGH_RISK_TRANSITION
            );
        }

        System.out.println(
                "Seed: "
                        + config.getSeed()
        );

        System.out.println(
                "Target: "
                        + config.getTarget()
        );

        System.out.println();

        ExperimentRunResult result =
                runner.run(
                        config
                );

        double durationSeconds =
                result.getDurationNanos()
                        / 1_000_000_000.0;

        System.out.println();
        System.out.println(
                "===== CALIBRATION RESULT ====="
        );

        System.out.println(
                "Admissions: "
                        + result.getAdmissions()
        );

        System.out.println(
                "Completions: "
                        + result.getCompletions()
        );

        System.out.println(
                "IT1: "
                        + result.getIt1Count()
        );

        System.out.println(
                "IT2: "
                        + result.getIt2Count()
        );

        System.out.println(
                "IT3: "
                        + result.getIt3Count()
        );

        System.out.println(
                "Total T-invariants: "
                        + result.getTotalTInvariants()
        );

        System.out.printf(
                "Duration: %.3f s%n",
                durationSeconds
        );

        System.out.println(
                "P-invariants valid: "
                        + result.arePInvariantsValid()
        );

        System.out.println(
                "T-invariants valid: "
                        + result.areTInvariantsValid()
        );

        System.out.println(
                "Workers terminated: "
                        + result.areWorkersTerminated()
        );

        System.out.println(
                "Successful: "
                        + result.isSuccessful()
        );

        System.out.println(
                "Log: "
                        + result.getLogPath()
        );

        System.out.println();

        if (durationSeconds >= 20.0
                && durationSeconds <= 40.0) {

            System.out.println(
                    "CALIBRATION STATUS: WITHIN REQUIRED RANGE"
            );

        } else {

            System.out.println(
                    "CALIBRATION STATUS: OUTSIDE REQUIRED RANGE"
            );
        }
    }

    private static ExperimentRunConfig.PolicyMode parsePolicy(
            String policyName
    ) {

        return switch (policyName) {

            case "RANDOM" ->
                    ExperimentRunConfig.PolicyMode.RANDOM;

            case "PRIORITY" ->
                    ExperimentRunConfig.PolicyMode.PRIORITY;

            default ->
                    throw new IllegalArgumentException(
                            "Unknown policy: "
                                    + policyName
                    );
        };
    }

    private static TransitionTimingConfig parseTiming(
            String timingName
    ) {

        return switch (timingName) {

            case "BASELINE" ->
                    PaymentTimingConfig.createBaseline();

            case "VARIANT" ->
                    PaymentTimingConfig.createScaled(
                            VARIANT_SCALE
                    );

            default ->
                    throw new IllegalArgumentException(
                            "Unknown timing: "
                                    + timingName
                    );
        };
    }

    private static void printUsage() {

        System.out.println(
                "Usage:"
        );

        System.out.println(
                "  java -cp target/classes "
                        + "experiment.CalibrationMain "
                        + "<RANDOM|PRIORITY> "
                        + "<BASELINE|VARIANT>"
        );
    }
}
