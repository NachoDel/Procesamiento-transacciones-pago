package app;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

class BootstrapTest {

    @Test
    void mainShouldStartWithoutErrors() {
        assertDoesNotThrow(() -> Main.main(new String[0]));
    }
}
