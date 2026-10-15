package app;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * [MAIN-ENTRY-POINT-TESTS]
 *
 * Verifica la CLI sin lanzar por accidente 200 transacciones
 * durante los tests comunes de Maven.
 */
class BootstrapTest {

    @TempDir
    Path tempDirectory;

    @Test
    void helpShouldNotStartExperimentalRun() {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        assertDoesNotThrow(() -> Main.execute(new String[]{"--help"}, output));
        assertTrue(buffer.toString(StandardCharsets.UTF_8).contains("--policy=RANDOM"));
    }

    @Test
    void shouldRejectInvalidArgumentsBeforeStartingWorkers() {
        PrintStream output = new PrintStream(new ByteArrayOutputStream());

        assertThrows(IllegalArgumentException.class,
                () -> Main.execute(new String[]{"--target=0"}, output));
        assertThrows(IllegalArgumentException.class,
                () -> Main.execute(new String[]{"--policy=UNKNOWN"}, output));
        assertThrows(IllegalArgumentException.class,
                () -> Main.execute(new String[]{"--scale=NaN"}, output));
        assertThrows(IllegalArgumentException.class,
                () -> Main.execute(new String[]{"--seed=abc"}, output));
        assertThrows(IllegalArgumentException.class,
                () -> Main.execute(new String[]{"--policy=RANDOM", "--policy=PRIORITY"}, output));
    }

    @Test
    void shouldExecuteShortRunFromEntryPoint() throws Exception {
        Path logPath = tempDirectory.resolve("main-short-run.log");
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        PrintStream output = new PrintStream(buffer, true, StandardCharsets.UTF_8);

        Main.execute(new String[]{
                "--policy=RANDOM",
                "--target=12",
                "--seed=2026",
                "--scale=0.01",
                "--timeout-seconds=10",
                "--log=" + logPath
        }, output);

        String text = buffer.toString(StandardCharsets.UTF_8);
        assertTrue(text.contains("Admisiones T0: 12"));
        assertTrue(text.contains("Finalizaciones T9: 12"));
        assertTrue(text.contains("Total T-invariantes: 12"));
        assertTrue(text.contains("Corrida exitosa: true"));
        assertTrue(Files.exists(logPath));
    }
}
