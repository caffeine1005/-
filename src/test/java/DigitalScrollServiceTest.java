import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DigitalScrollServiceTest {

    @TempDir
    Path tempDir;

    private DigitalScrollService createService() throws Exception {
        Path storage = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        return new DigitalScrollService(repository, uploads);
    }

    @Test
    void addUpdateAndRemoveScroll() throws Exception {
        DigitalScrollService service = createService();

        Path source = tempDir.resolve("source.bin");
        Files.write(source, new byte[]{0, 1, 2});

        DigitalScroll scroll = service.addScroll("owner1", "Spell", source.toString());
        assertEquals("Spell", scroll.getName());
        assertTrue(Files.exists(Path.of(scroll.getFilePath())));
        assertEquals(1, scroll.getUploadCount());
        assertEquals(0, scroll.getDownloadCount());

        Path replacement = tempDir.resolve("replacement.bin");
        Files.write(replacement, new byte[]{3, 4, 5, 6});
        service.updateScroll("owner1", scroll.getScrollId(), "Spell Updated", replacement.toString());

        DigitalScroll updated = service.getScroll(scroll.getScrollId());
        assertEquals("Spell Updated", updated.getName());
        assertEquals(2, updated.getUploadCount());
        assertArrayEquals(Files.readAllBytes(replacement), Files.readAllBytes(Path.of(updated.getFilePath())));

        service.recordDownload(updated);
        assertEquals(1, service.getScroll(updated.getScrollId()).getDownloadCount());

        List<String> stats = service.getScrollStatistics();
        assertEquals(1, stats.size());
        assertTrue(stats.get(0).contains("uploads=2"));
        assertTrue(stats.get(0).contains("downloads=1"));

        service.removeScroll("owner1", updated.getScrollId());
        assertNull(service.getScroll(updated.getScrollId()));
        assertFalse(Files.exists(Path.of(updated.getFilePath())));
    }

    @Test
    void preventEditingOthersScrolls() throws Exception {
        DigitalScrollService service = createService();

        Path src = tempDir.resolve("data.bin");
        Files.write(src, new byte[]{7, 8});
        DigitalScroll scroll = service.addScroll("ownerA", "ScrollA", src.toString());

        assertThrows(IllegalArgumentException.class,
                () -> service.updateScroll("ownerB", scroll.getScrollId(), "Hack", null));
        assertThrows(IllegalArgumentException.class,
                () -> service.removeScroll("ownerB", scroll.getScrollId()));
    }

    @Test
    void addScrollRejectsDuplicateNames() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("dup.bin");
        Files.write(src, new byte[]{1});
        service.addScroll("owner", "UniqueName", src.toString());
        Path other = tempDir.resolve("dup2.bin");
        Files.write(other, new byte[]{2});
        assertThrows(IllegalArgumentException.class,
                () -> service.addScroll("owner", "UniqueName", other.toString()));
    }

    @Test
    void updateScrollKeepsNameWhenEmpty() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("file.bin");
        Files.write(src, new byte[]{9});
        DigitalScroll scroll = service.addScroll("owner", "BaseName", src.toString());
        service.updateScroll("owner", scroll.getScrollId(), "   ", null);
        assertEquals("BaseName", service.getScroll(scroll.getScrollId()).getName());
    }

    @Test
    void recordDownloadUpdatesRepository() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("data2.bin");
        Files.write(src, new byte[]{10});
        DigitalScroll scroll = service.addScroll("owner", "Downloadable", src.toString());
        service.recordDownload(scroll);
        service.recordDownload(scroll);
        DigitalScroll refreshed = service.getScroll(scroll.getScrollId());
        assertEquals(2, refreshed.getDownloadCount());
    }

    @Test
    void removeScrollDoesNotFailWhenFileMissing() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("toremove.bin");
        Files.write(src, new byte[]{5});
        DigitalScroll scroll = service.addScroll("owner", "Temp", src.toString());
        Files.deleteIfExists(Path.of(scroll.getFilePath()));
        service.removeScroll("owner", scroll.getScrollId());
        assertNull(service.getScroll(scroll.getScrollId()));
    }

    @Test
    void addScrollRejectsPipeCharacter() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("pipe.bin");
        Files.write(src, new byte[]{1});
        assertThrows(IllegalArgumentException.class,
                () -> service.addScroll("owner", "Bad|Name", src.toString()));
    }

    @Test
    void addScrollRejectsMissingFile() throws Exception {
        DigitalScrollService service = createService();
        assertThrows(IllegalArgumentException.class,
                () -> service.addScroll("owner", "MissingFile", tempDir.resolve("nope.bin").toString()));
    }

    @Test
    void getScrollReturnsNullForNullId() throws Exception {
        DigitalScrollService service = createService();
        assertNull(service.getScroll(null));
    }

    @Test
    void listScrollsByOwnerFiltersResults() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("ownerfile.bin");
        Files.write(src, new byte[]{9});
        service.addScroll("ownerA", "A", src.toString());
        assertEquals(1, service.listScrollsByOwner("ownerA").size());
        assertTrue(service.listScrollsByOwner("unknown").isEmpty());
    }

    @Test
    void updateScrollAllowsSameName() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("same.bin");
        Files.write(src, new byte[]{8});
        DigitalScroll scroll = service.addScroll("owner", "SameName", src.toString());
        service.updateScroll("owner", scroll.getScrollId(), "SameName", null);
        assertEquals("SameName", service.getScroll(scroll.getScrollId()).getName());
    }

    @Test
    void addScrollRejectsBlankName() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("blank.bin");
        Files.write(src, new byte[]{2});
        assertThrows(IllegalArgumentException.class,
                () -> service.addScroll("owner", "   ", src.toString()));
    }

    @Test
    void addScrollRejectsNullFilePath() throws Exception {
        DigitalScrollService service = createService();
        assertThrows(IllegalArgumentException.class,
                () -> service.addScroll("owner", "Name", null));
    }

    @Test
    void updateScrollRejectsBlankId() throws Exception {
        DigitalScrollService service = createService();
        assertThrows(IllegalArgumentException.class,
                () -> service.updateScroll("owner", "   ", null, null));
    }

    @Test
    void addScrollHandlesSourceWithoutExtension() throws Exception {
        DigitalScrollService service = createService();
        Path src = tempDir.resolve("sourcenoext");
        Files.write(src, new byte[]{1});
        DigitalScroll scroll = service.addScroll("owner", "Plain", src.toString());
        String fileName = Path.of(scroll.getFilePath()).getFileName().toString();
        assertTrue(fileName.endsWith("Plain"));
        assertFalse(fileName.contains("."));
    }
}
