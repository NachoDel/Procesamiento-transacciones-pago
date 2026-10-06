package logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [T4.1.1 - EXECUTION LOGGER TESTS]
 *
 * Verifica:
 *
 * - formato parseable;
 * - numeración secuencial;
 * - identificación del thread;
 * - seguridad ante escritura concurrente;
 * - validación de campos;
 * - comportamiento luego del cierre.
 */
class ExecutionLoggerTest {

    @TempDir
    Path tempDir;

    /**
     * [FORMAT]
     *
     * Verifica el contrato:
     *
     * SEQUENCE|THREAD|TRANSITION|TIMESTAMP|POLICY|SEED|CONFIG
     */
    @Test
    void shouldWriteParseableLogEntry()
            throws IOException {

        Path logFile =
                tempDir.resolve("execution.log");

        try (ExecutionLogger logger =
                     new ExecutionLogger(
                             logFile,
                             "PRIORITY",
                             2026L,
                             "timing=A"
                     )) {

            logger.logTransition(
                    "H1",
                    2
            );
        }

        List<String> lines =
                Files.readAllLines(logFile);

        assertEquals(
                1,
                lines.size()
        );

        String[] fields =
                lines.get(0).split("\\|", -1);

        assertEquals(
                7,
                fields.length
        );

        assertEquals("1", fields[0]);
        assertEquals("H1", fields[1]);
        assertEquals("T2", fields[2]);

        long timestamp =
                Long.parseLong(fields[3]);

        assertTrue(timestamp > 0);

        assertEquals("PRIORITY", fields[4]);
        assertEquals("2026", fields[5]);
        assertEquals("timing=A", fields[6]);
    }

    /**
     * [SEQUENCE]
     *
     * Cada registro debe obtener un identificador creciente
     * y único dentro de la corrida.
     */
    @Test
    void shouldAssignSequentialEventNumbers()
            throws IOException {

        Path logFile =
                tempDir.resolve("sequence.log");

        try (ExecutionLogger logger =
                     new ExecutionLogger(
                             logFile,
                             "RANDOM",
                             1234L,
                             "timing=B"
                     )) {

            logger.logTransition("H0", 0);
            logger.logTransition("H1", 1);
            logger.logTransition("H1", 2);
        }

        List<String> lines =
                Files.readAllLines(logFile);

        assertEquals(3, lines.size());

        assertTrue(lines.get(0).startsWith("1|"));
        assertTrue(lines.get(1).startsWith("2|"));
        assertTrue(lines.get(2).startsWith("3|"));
    }

    /**
     * [CURRENT-THREAD]
     *
     * Verifica que logTransition(int) utilice el nombre
     * del thread Java que realiza el disparo.
     */
    @Test
    void shouldUseCurrentThreadName()
            throws Exception {

        Path logFile =
                tempDir.resolve("thread-name.log");

        try (ExecutionLogger logger =
                     new ExecutionLogger(
                             logFile,
                             "RANDOM",
                             2026L,
                             "timing=A"
                     )) {

            Thread workerThread =
                    new Thread(
                            () -> logger.logTransition(7),
                            "H3"
                    );

            workerThread.start();
            workerThread.join(2000);

            assertFalse(
                    workerThread.isAlive(),
                    "Worker thread should have finished"
            );
        }

        List<String> lines =
                Files.readAllLines(logFile);

        assertEquals(1, lines.size());

        String[] fields =
                lines.get(0).split("\\|", -1);

        assertEquals("H3", fields[1]);
        assertEquals("T7", fields[2]);
    }

    /**
     * [CONCURRENT-WRITING]
     *
     * Varios threads escriben simultáneamente sobre
     * una única instancia del logger.
     *
     * El test comprueba que:
     *
     * - no se pierdan entradas;
     * - ninguna línea quede corrupta;
     * - las secuencias sean únicas;
     * - el orden físico coincida con la secuencia.
     */
    @Test
    void shouldLogConcurrentlyWithoutLosingOrCorruptingEntries()
            throws Exception {

        Path logFile =
                tempDir.resolve("concurrent.log");

        int threadCount = 6;
        int entriesPerThread = 20;

        int expectedEntries =
                threadCount * entriesPerThread;

        CountDownLatch readyLatch =
                new CountDownLatch(threadCount);

        CountDownLatch startLatch =
                new CountDownLatch(1);

        CountDownLatch finishedLatch =
                new CountDownLatch(threadCount);

        AtomicReference<Throwable> threadFailure =
                new AtomicReference<>();

        try (ExecutionLogger logger =
                     new ExecutionLogger(
                             logFile,
                             "PRIORITY",
                             2026L,
                             "timing=A"
                     )) {

            for (int threadIndex = 0;
                 threadIndex < threadCount;
                 threadIndex++) {

                final int workerIndex =
                        threadIndex;

                Thread thread =
                        new Thread(
                                () -> {

                                    readyLatch.countDown();

                                    try {

                                        startLatch.await();

                                        for (int entry = 0;
                                             entry < entriesPerThread;
                                             entry++) {

                                            logger.logTransition(
                                                    entry % 10
                                            );
                                        }

                                    } catch (Throwable exception) {

                                        threadFailure.compareAndSet(
                                                null,
                                                exception
                                        );

                                    } finally {

                                        finishedLatch.countDown();
                                    }
                                },
                                "H" + workerIndex
                        );

                thread.start();
            }

            assertTrue(
                    readyLatch.await(
                            2,
                            TimeUnit.SECONDS
                    ),
                    "Threads did not become ready"
            );

            startLatch.countDown();

            assertTrue(
                    finishedLatch.await(
                            10,
                            TimeUnit.SECONDS
                    ),
                    "Threads did not finish in time"
            );

            assertEquals(
                    null,
                    threadFailure.get(),
                    "A logging thread failed"
            );
        }

        List<String> lines =
                Files.readAllLines(logFile);

        assertEquals(
                expectedEntries,
                lines.size()
        );

        Set<Long> sequences =
                new HashSet<>();

        for (int index = 0;
             index < lines.size();
             index++) {

            String[] fields =
                    lines.get(index)
                            .split("\\|", -1);

            assertEquals(
                    7,
                    fields.length,
                    "Malformed log line: "
                            + lines.get(index)
            );

            long sequence =
                    Long.parseLong(fields[0]);

            /*
             * El orden físico del archivo debe coincidir
             * exactamente con la secuencia lógica.
             */
            assertEquals(
                    index + 1L,
                    sequence
            );

            assertTrue(
                    sequences.add(sequence),
                    "Duplicated sequence: "
                            + sequence
            );

            assertTrue(
                    fields[1].startsWith("H")
            );

            assertTrue(
                    fields[2].startsWith("T")
            );
        }

        assertEquals(
                expectedEntries,
                sequences.size()
        );
    }

    /**
     * [FORMAT-PROTECTION]
     *
     * El separador reservado no puede formar parte
     * de los campos textuales.
     */
    @Test
    void shouldRejectReservedSeparatorInsideFields()
            throws IOException {

        Path logFile =
                tempDir.resolve("invalid-field.log");

        assertThrows(
                IllegalArgumentException.class,
                () -> new ExecutionLogger(
                        logFile,
                        "PRIORITY|INVALID",
                        2026L,
                        "timing=A"
                )
        );
    }

    /**
     * [CLOSED-LOGGER]
     *
     * No debe permitirse registrar eventos después
     * de cerrar el recurso.
     */
    @Test
    void shouldRejectWritesAfterClose()
            throws IOException {

        Path logFile =
                tempDir.resolve("closed.log");

        ExecutionLogger logger =
                new ExecutionLogger(
                        logFile,
                        "RANDOM",
                        2026L,
                        "timing=A"
                );

        logger.close();

        assertThrows(
                IllegalStateException.class,
                () -> logger.logTransition(
                        "H1",
                        2
                )
        );
    }
}
