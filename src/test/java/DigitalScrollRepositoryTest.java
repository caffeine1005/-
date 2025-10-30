import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DigitalScrollRepositoryTest {

    @TempDir
    Path tempDir;

    private DigitalScrollRepository repository() {
        return new DigitalScrollRepository(tempDir.resolve("scrolls.db"));
    }

    private DigitalScroll createScroll(String id, String name) {
        return new DigitalScroll(id, name, "owner", tempDir.resolve(id + ".bin").toString(), java.time.LocalDateTime.now(), 0, 0);
    }

    @Test
    void saveAndLoadScrolls() {
        DigitalScrollRepository repository = repository();
        repository.save(createScroll("SC0001", "Alpha"));
        repository.save(createScroll("SC0002", "Beta"));

        DigitalScrollRepository reloaded = repository();
        assertEquals(2, reloaded.getAll().size());
        assertEquals("Alpha", reloaded.findById("SC0001").getName());
    }

    @Test
    void deleteRemovesScroll() {
        DigitalScrollRepository repository = repository();
        repository.save(createScroll("SC0001", "Alpha"));
        repository.delete("SC0001");
        assertNull(repository.findById("SC0001"));
    }

    @Test
    void loadOldFormatWithoutCounts() throws IOException {
        Path storage = tempDir.resolve("scrolls.db");
        Files.writeString(storage, "SC0003|Legacy|wizard|/tmp/file|2020-01-01T10:00:00", StandardCharsets.UTF_8);
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        DigitalScroll scroll = repository.findById("SC0003");
        assertEquals("Legacy", scroll.getName());
        assertEquals(0, scroll.getUploadCount());
        assertEquals(0, scroll.getDownloadCount());
    }

    @Test
    void loadNewFormatWithCounts() throws IOException {
        Path storage = tempDir.resolve("scrolls.db");
        Files.writeString(storage, "SC0004|Modern|mage|/tmp/file|2021-02-02T02:02:02|3|5", StandardCharsets.UTF_8);
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        DigitalScroll scroll = repository.findById("SC0004");
        assertEquals(3, scroll.getUploadCount());
        assertEquals(5, scroll.getDownloadCount());
    }

    @Test
    void generateIdProducesSequentialValues() {
        DigitalScrollRepository repository = repository();
        String id1 = repository.generateId();
        String id2 = repository.generateId();
        assertEquals("SC0001", id1);
        assertEquals("SC0002", id2);
    }

    @Test
    void parseTimestampHandlesInvalidInput() throws IOException {
        Path storage = tempDir.resolve("scrolls.db");
        Files.writeString(storage, "SC0005|Broken|mage|/tmp/file|invalid-date", StandardCharsets.UTF_8);
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        assertNotNull(repository.findById("SC0005").getUploadTimestamp());
    }

    @Test
    void findByIdReturnsNullWhenMissing() {
        DigitalScrollRepository repository = repository();
        assertNull(repository.findById("UNKNOWN"));
    }
}
