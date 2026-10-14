package experiment;

import config.PaymentConflictConfig;
import config.PaymentPetriNetConfig;
import config.PaymentWorkerConfig;

import logging.ExecutionLogger;

import monitor.Monitor;

import petri.PetriNet;

import policy.Policy;
import policy.PriorityPolicy;
import policy.RandomPolicy;

import verification.ExecutionAudit;
import verification.PlaceInvariantChecker;
import verification.TInvariantAnalyzer;

import worker.WorkerCoordinator;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * [EXPERIMENT-RUNNER]
 *
 * Orquesta una corrida experimental completa del sistema
 * concurrente de procesamiento de pagos.
 *
 * [RESPONSIBILITY]
 *
 * ExperimentRunner coordina:
 *
 * - creación aislada de PetriNet;
 * - selección de Policy;
 * - creación del logger;
 * - verificación online de P-invariantes;
 * - seguimiento de T0 y T9;
 * - creación del Monitor;
 * - lifecycle de Workers;
 * - cierre de admisión;
 * - drenaje;
 * - medición temporal;
 * - análisis post-run de T-invariantes;
 * - construcción del resultado final.
 *
 * No implementa:
 *
 * - lógica interna de PetriNet;
 * - sincronización interna del Monitor;
 * - lógica interna de Workers;
 * - reconocimiento online de IT1/IT2/IT3.
 */
public final class ExperimentRunner {

    /**
     * [ADMISSION-TRANSITION]
     *
     * T0 representa la admisión de una nueva
     * transacción al sistema.
     */
    private static final int ADMISSION_TRANSITION = 0;

    /**
     * [ADMISSION-WORKER]
     *
     * H0 es el único Worker responsable de T0.
     */
    private static final String ADMISSION_WORKER = "H0";

    /**
     * [POLL-INTERVAL]
     *
     * Intervalo utilizado por el thread coordinador
     * para observar:
     *
     * - finalización del target;
     * - errores asincrónicos;
     * - timeout de ejecución.
     *
     * Este polling no participa de la sincronización
     * de la Red de Petri.
     */
    private static final long PROGRESS_POLL_MILLIS = 5L;

    /**
     * Ejecuta una corrida experimental completa.
     *
     * @param config configuración inmutable de la corrida
     * @return resultado estructurado de la ejecución
     * @throws IOException si no puede crearse o analizarse el log
     * @throws InterruptedException si el thread coordinador
     *                              es interrumpido
     */
    public ExperimentRunResult run(
            ExperimentRunConfig config
    ) throws IOException, InterruptedException {

        Objects.requireNonNull(
                config,
                "Experiment run config cannot be null"
        );

        /*
         * [FRESH-RUN]
         *
         * Cada corrida utiliza una PetriNet nueva.
         *
         * No se comparte marcado mutable entre
         * ejecuciones experimentales.
         */
        PetriNet petriNet =
                PaymentPetriNetConfig.createPetriNet();

        Policy policy =
                createPolicy(
                        config
                );

        RunProgressTracker progressTracker =
                new RunProgressTracker(
                        config.getTarget()
                );

        /*
         * Estas variables se completan durante la fase
         * concurrente y se utilizan recién después de
         * cerrar el ExecutionLogger.
         */
        long durationNanos;

        boolean workersTerminated;

        /*
         * [LOGGER-LIFECYCLE]
         *
         * El logger permanece abierto exclusivamente
         * durante la ejecución concurrente.
         *
         * Al salir de este bloque try-with-resources:
         *
         * 1. todos los Workers ya terminaron;
         * 2. no habrá nuevos disparos;
         * 3. el archivo queda cerrado;
         * 4. recién entonces se ejecuta TInvariantAnalyzer.
         */
        try (ExecutionLogger executionLogger =
                     new ExecutionLogger(
                             config.getLogPath(),
                             config.getPolicyMode().name(),
                             config.getSeed(),
                             config.getTimingName()
                     )) {

            PlaceInvariantChecker invariantChecker =
                    new PlaceInvariantChecker();

            ExecutionAudit executionAudit =
                    new ExecutionAudit(
                            invariantChecker,
                            executionLogger
                    );

            /*
             * [POST-FIRE-COMPOSITION]
             *
             * Cada disparo confirmado actualiza:
             *
             * 1. progreso experimental;
             * 2. P-invariantes;
             * 3. log de ejecución.
             */
            ExperimentPostFireObserver postFireObserver =
                    new ExperimentPostFireObserver(
                            progressTracker,
                            executionAudit
                    );

            /*
             * [DYNAMIC-AVAILABILITY]
             *
             * T0 permanece disponible mientras todavía
             * pueden admitirse transacciones.
             *
             * Cuando se registra el T0 número TARGET:
             *
             * progressTracker.isAdmissionClosed()
             *      == true
             *
             * A partir de ese momento T0 deja de estar
             * disponible para el Monitor.
             *
             * Las restantes transiciones continúan
             * disponibles para permitir el drenaje.
             *
             * Esta separación es importante porque T0
             * es inmediata. Si siguiera apareciendo como
             * disponible podría impedir el avance de
             * transiciones temporales durante el drain.
             */
            Monitor monitor =
                    new Monitor(
                            petriNet,
                            policy,
                            PaymentConflictConfig
                                    .getConflictGroups(),
                            PaymentPetriNetConfig
                                    .createTransitionSemantics(),
                            config.getTimingConfig(),
                            postFireObserver,
                            transition ->
                                    transition
                                            != ADMISSION_TRANSITION
                                            || !progressTracker
                                            .isAdmissionClosed()
                    );

            /*
             * [SELECTIVE-STOP]
             *
             * H0 deja de iniciar ciclos cuando se alcanza
             * el target de admisiones.
             *
             * H1, H2, H3 y H4 continúan ejecutándose
             * para completar las transacciones que ya
             * fueron admitidas.
             */
            WorkerCoordinator coordinator =
                    new WorkerCoordinator(
                            PaymentWorkerConfig
                                    .getDefinitions(),
                            monitor,
                            Map.of(
                                    ADMISSION_WORKER,
                                    progressTracker
                                            ::isAdmissionClosed
                            )
                    );

            long startNanos =
                    System.nanoTime();

            long completedAtNanos;

            /*
             * [START]
             *
             * El tiempo experimental comienza inmediatamente
             * antes de iniciar los Workers.
             */
            coordinator.startAll();

            try {

                /*
                 * [RUN-UNTIL-DRAINED]
                 *
                 * Se considera completada la corrida cuando
                 * T9 alcanzó exactamente TARGET.
                 */
                completedAtNanos =
                        awaitTargetCompletion(
                                progressTracker,
                                postFireObserver,
                                config.getExecutionTimeout(),
                                startNanos
                        );

            } finally {

                /*
                 * [GLOBAL-SHUTDOWN]
                 *
                 * Luego del drenaje normal, o frente a una
                 * excepción, se solicita el cierre completo.
                 *
                 * requestStop():
                 *
                 * impide que los Workers comiencen nuevos ciclos.
                 *
                 * interruptAll():
                 *
                 * despierta threads que puedan encontrarse
                 * bloqueados esperando dentro del Monitor.
                 */
                coordinator.requestStop();

                coordinator.interruptAll();

                workersTerminated =
                        coordinator.awaitTermination(
                                config.getShutdownTimeout()
                                        .toNanos(),
                                TimeUnit.NANOSECONDS
                        );
            }

            /*
             * [SHUTDOWN-CONTRACT]
             *
             * Una corrida no se considera finalizada
             * correctamente si quedaron Workers vivos.
             */
            if (!workersTerminated) {

                throw new IllegalStateException(
                        "Worker shutdown timed out for run "
                                + config.getRunId()
                );
            }

            /*
             * [ASYNC-FAILURE-CHECK]
             *
             * Una excepción producida dentro de un Worker
             * puede no propagarse directamente al thread
             * que ejecuta ExperimentRunner.
             *
             * ExperimentPostFireObserver conserva el primer
             * fallo para que podamos recuperarlo aquí.
             */
            RuntimeException postFireFailure =
                    postFireObserver
                            .getFailure()
                            .orElse(null);

            if (postFireFailure != null) {

                throw new IllegalStateException(
                        "Post-fire failure during run "
                                + config.getRunId(),
                        postFireFailure
                );
            }

            /*
             * [ADMISSION-CONTRACT]
             *
             * Deben existir exactamente TARGET T0.
             */
            if (progressTracker.getAdmissions()
                    != config.getTarget()) {

                throw new IllegalStateException(
                        "Unexpected admission count for run "
                                + config.getRunId()
                                + ": expected="
                                + config.getTarget()
                                + ", actual="
                                + progressTracker.getAdmissions()
                );
            }

            /*
             * [DRAIN-CONTRACT]
             *
             * Deben existir exactamente TARGET T9.
             */
            if (progressTracker.getCompletions()
                    != config.getTarget()) {

                throw new IllegalStateException(
                        "Unexpected completion count for run "
                                + config.getRunId()
                                + ": expected="
                                + config.getTarget()
                                + ", actual="
                                + progressTracker.getCompletions()
                );
            }

            /*
             * [DURATION]
             *
             * La duración experimental termina cuando
             * se observa el T9 número TARGET.
             *
             * No se incluye el overhead administrativo
             * posterior utilizado para interrumpir y
             * hacer join de los Workers.
             */
            durationNanos =
                    completedAtNanos
                            - startNanos;

        }

        /*
         * [POST-RUN-ANALYSIS]
         *
         * Llegar a este punto garantiza:
         *
         * - TARGET admisiones;
         * - TARGET finalizaciones;
         * - Workers terminados;
         * - ningún error post-fire;
         * - ExecutionLogger cerrado.
         *
         * Recién ahora analizamos IT1, IT2 e IT3.
         */
        return buildResultAfterClosingLog(
                config,
                progressTracker,
                durationNanos,
                workersTerminated
        );
    }

    /**
     * [POLICY-FACTORY]
     *
     * Construye una Policy independiente para cada corrida.
     *
     * RANDOM:
     *
     * RandomPolicy(seed)
     *
     * PRIORITY:
     *
     * PriorityPolicy(
     *     HIGH_RISK_TRANSITION,
     *     RandomPolicy(seed)
     * )
     *
     * La misma seed también controla el fallback aleatorio
     * de PriorityPolicy.
     */
    private static Policy createPolicy(
            ExperimentRunConfig config
    ) {

        RandomPolicy randomPolicy =
                new RandomPolicy(
                        config.getSeed()
                );

        return switch (config.getPolicyMode()) {

            case RANDOM ->
                    randomPolicy;

            case PRIORITY ->
                    new PriorityPolicy(
                            PaymentPetriNetConfig
                                    .HIGH_RISK_TRANSITION,
                            randomPolicy
                    );
        };
    }

    /**
     * [WAIT-FOR-DRAIN]
     *
     * Espera hasta observar TARGET disparos de T9.
     *
     * Durante la espera también se controlan:
     *
     * - fallos post-fire producidos en Workers;
     * - timeout global de ejecución.
     *
     * @return instante monotónico en el que se observó
     *         el target completo
     */
    private static long awaitTargetCompletion(
            RunProgressTracker progressTracker,
            ExperimentPostFireObserver postFireObserver,
            Duration executionTimeout,
            long startNanos
    ) throws InterruptedException {

        long timeoutNanos =
                executionTimeout.toNanos();

        while (!progressTracker
                .isTargetCompleted()) {

            /*
             * [WORKER-FAILURE]
             *
             * Si un Worker falló durante la auditoría
             * o actualización de progreso, no esperamos
             * innecesariamente hasta alcanzar el timeout.
             */
            RuntimeException failure =
                    postFireObserver
                            .getFailure()
                            .orElse(null);

            if (failure != null) {

                throw new IllegalStateException(
                        "Post-fire failure before target completion",
                        failure
                );
            }

            /*
             * [EXECUTION-TIMEOUT]
             *
             * Se utiliza System.nanoTime() porque medimos
             * duración transcurrida, no tiempo calendario.
             */
            long elapsedNanos =
                    System.nanoTime()
                            - startNanos;

            if (elapsedNanos
                    >= timeoutNanos) {

                throw new IllegalStateException(
                        "Experimental run timed out before reaching target. "
                                + "target="
                                + progressTracker.getTarget()
                                + ", admissions="
                                + progressTracker.getAdmissions()
                                + ", completions="
                                + progressTracker.getCompletions()
                );
            }

            /*
             * Evita busy-waiting del thread coordinador.
             */
            Thread.sleep(
                    PROGRESS_POLL_MILLIS
            );
        }

        /*
         * [FINAL-FAILURE-CHECK]
         *
         * Un error podría publicarse prácticamente al mismo
         * tiempo que completions alcanza TARGET.
         */
        RuntimeException failure =
                postFireObserver
                        .getFailure()
                        .orElse(null);

        if (failure != null) {

            throw new IllegalStateException(
                    "Post-fire failure at target completion",
                    failure
            );
        }

        return System.nanoTime();
    }

    /**
     * [POST-RUN-ANALYSIS]
     *
     * Analiza el archivo una vez finalizada completamente
     * la etapa concurrente.
     *
     * En este punto ExecutionLogger ya se encuentra cerrado.
     */
    private static ExperimentRunResult buildResultAfterClosingLog(
            ExperimentRunConfig config,
            RunProgressTracker progressTracker,
            long durationNanos,
            boolean workersTerminated
    ) throws IOException {

        TInvariantAnalyzer analyzer =
                new TInvariantAnalyzer();

        TInvariantAnalyzer.AnalysisResult analysis =
                analyzer.analyze(
                        config.getLogPath()
                );

        return new ExperimentRunResult(
                config.getPolicyMode(),
                config.getSeed(),
                config.getTimingName(),
                config.getTarget(),
                progressTracker.getAdmissions(),
                progressTracker.getCompletions(),
                analysis.getIt1Count(),
                analysis.getIt2Count(),
                analysis.getIt3Count(),
                durationNanos,

                /*
                 * ExecutionAudit verifica los P-invariantes
                 * después de cada disparo.
                 *
                 * Cualquier violación habría producido una
                 * excepción y la ejecución no habría llegado
                 * hasta este punto.
                 */
                true,

                analysis.isValid(),

                workersTerminated,

                config.getLogPath()
        );
    }
}
