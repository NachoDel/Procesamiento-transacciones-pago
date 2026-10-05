package logging;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * [T4.1.1 - EXECUTION LOGGER]
 *
 * Registra los disparos confirmados de la Red de Petri
 * utilizando un formato estable, ordenado y parseable.
 *
 * Formato:
 *
 * SEQUENCE|THREAD|TRANSITION|TIMESTAMP|POLICY|SEED|CONFIG
 *
 * Ejemplo:
 *
 * 1|H1|T2|1791234567890|PRIORITY|2026|timing=A
 *
 * [CONCURRENCY]
 *
 * El logger es thread-safe.
 *
 * La asignación del número de secuencia y la escritura de la
 * entrada se realizan dentro de la misma sección crítica.
 * De esta manera, el orden físico de las líneas coincide con
 * el orden lógico indicado por SEQUENCE.
 */
public final class ExecutionLogger implements AutoCloseable {

    /**
     * Separador estable del formato del log.
     */
    public static final String FIELD_SEPARATOR = "|";

    private final BufferedWriter writer;

    private final String policyName;

    private final long seed;

    private final String runConfiguration;

    /**
     * Protege:
     *
     * - el contador de secuencia;
     * - la escritura sobre el archivo;
     * - el cierre del writer.
     */
    private final ReentrantLock lock =
            new ReentrantLock();

    /**
     * Próximo identificador de evento.
     *
     * Comienza en 1 para facilitar lectura humana y análisis.
     */
    private long nextSequence = 1L;

    private boolean closed = false;

    /**
     * Construye un logger para una corrida.
     *
     * Al comenzar una nueva corrida, el archivo indicado se crea
     * o se reemplaza si ya existía.
     *
     * @param logPath archivo donde se almacenará el log
     * @param policyName nombre de la política utilizada
     * @param seed seed utilizada en la corrida
     * @param runConfiguration descripción estable de la configuración
     * @throws IOException si el archivo no puede abrirse
     */
    public ExecutionLogger(
            Path logPath,
            String policyName,
            long seed,
            String runConfiguration
    ) throws IOException {

        Objects.requireNonNull(
                logPath,
                "Log path cannot be null"
        );

        this.policyName =
                validateField(
                        policyName,
                        "Policy name"
                );

        this.seed = seed;

        this.runConfiguration =
                validateField(
                        runConfiguration,
                        "Run configuration"
                );

        Path parent = logPath.getParent();

        if (parent != null) {
            Files.createDirectories(parent);
        }

        this.writer =
                Files.newBufferedWriter(
                        logPath,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.TRUNCATE_EXISTING,
                        StandardOpenOption.WRITE
                );
    }

    /**
     * Registra un disparo confirmado utilizando como identificador
     * el nombre del thread Java que realizó la llamada.
     *
     * Esta variante permite integrar el logger con el Monitor
     * sin modificar MonitorInterface.fireTransition(int).
     *
     * @param transition transición disparada
     */
    public void logTransition(int transition) {

        logTransition(
                Thread.currentThread().getName(),
                transition
        );
    }

    /**
     * Registra un disparo confirmado indicando explícitamente
     * el nombre lógico del thread.
     *
     * Esta sobrecarga facilita tests y otros consumidores.
     *
     * @param threadName thread responsable del disparo
     * @param transition transición disparada
     */
    public void logTransition(
            String threadName,
            int transition
    ) {

        String validatedThreadName =
                validateField(
                        threadName,
                        "Thread name"
                );

        if (transition < 0) {
            throw new IllegalArgumentException(
                    "Transition index cannot be negative"
            );
        }

        lock.lock();

        try {

            ensureOpen();

            /*
             * [ORDER]
             *
             * La secuencia se obtiene dentro del mismo lock
             * utilizado para escribir.
             *
             * Así no puede ocurrir:
             *
             * thread A -> sequence 1
             * thread B -> sequence 2
             * thread B escribe antes que A
             */
            long sequence =
                    nextSequence++;

            long timestamp =
                    System.currentTimeMillis();

            String line =
                    sequence
                            + FIELD_SEPARATOR
                            + validatedThreadName
                            + FIELD_SEPARATOR
                            + "T" + transition
                            + FIELD_SEPARATOR
                            + timestamp
                            + FIELD_SEPARATOR
                            + policyName
                            + FIELD_SEPARATOR
                            + seed
                            + FIELD_SEPARATOR
                            + runConfiguration;

            writer.write(line);
            writer.newLine();

            /*
             * Se fuerza el flush para que cada disparo confirmado
             * quede disponible inmediatamente en el archivo.
             *
             * Para este TP se prioriza trazabilidad por sobre
             * optimización extrema de I/O.
             */
            writer.flush();

        } catch (IOException exception) {

            /*
             * No se ocultan errores del log.
             *
             * Se transforma IOException en una excepción unchecked
             * para no modificar el contrato público del Monitor.
             */
            throw new UncheckedIOException(
                    "Could not write execution log",
                    exception
            );

        } finally {

            lock.unlock();
        }
    }

    /**
     * Cierra el archivo de log.
     *
     * Es seguro invocar close() más de una vez.
     */
    @Override
    public void close() {

        lock.lock();

        try {

            if (closed) {
                return;
            }

            writer.close();
            closed = true;

        } catch (IOException exception) {

            throw new UncheckedIOException(
                    "Could not close execution log",
                    exception
            );

        } finally {

            lock.unlock();
        }
    }

    /**
     * Verifica que el logger siga disponible.
     */
    private void ensureOpen() {

        if (closed) {
            throw new IllegalStateException(
                    "Execution logger is already closed"
            );
        }
    }

    /**
     * Valida los campos textuales que forman parte del formato.
     *
     * No se permite el separador ni saltos de línea porque
     * romperían el contrato parseable del archivo.
     */
    private static String validateField(
            String value,
            String fieldName
    ) {

        Objects.requireNonNull(
                value,
                fieldName + " cannot be null"
        );

        if (value.isBlank()) {
            throw new IllegalArgumentException(
                    fieldName + " cannot be blank"
            );
        }

        if (value.contains(FIELD_SEPARATOR)
                || value.contains("\n")
                || value.contains("\r")) {

            throw new IllegalArgumentException(
                    fieldName
                            + " contains characters reserved by the log format"
            );
        }

        return value;
    }
}
