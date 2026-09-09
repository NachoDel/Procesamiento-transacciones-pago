package ar.edu.unc.concurrente;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class BootstrapTest {

    @Test
    void mainShouldStartWithoutErrors() {
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }
}
