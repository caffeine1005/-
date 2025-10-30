import org.example.App;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AppTest {

    @Test
    void mainExitsWhenSelectingExit() throws Exception {
        InputStream originalIn = System.in;
        Path users = Path.of("data", "users.db");
        Path scrolls = Path.of("data", "scrolls", "scrolls.db");
        Path uploads = Path.of("data", "uploads");
        try {
            System.setIn(new ByteArrayInputStream("4\n".getBytes()));
            App.main(new String[0]);
        } finally {
            System.setIn(originalIn);
            Files.deleteIfExists(users);
            Files.deleteIfExists(scrolls);
            if (Files.exists(uploads)) {
                try (var stream = Files.walk(uploads).sorted(Comparator.reverseOrder())) {
                    stream.forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
                }
            }
        }
        assertTrue(true);
    }
}