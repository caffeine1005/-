import org.example.digital_scroll_management.DigitalScroll;
import org.example.digital_scroll_management.DigitalScrollRepository;
import org.example.digital_scroll_management.DigitalScrollService;
import org.example.scroll_seeker.ScrollPreview;
import org.example.scroll_seeker.ScrollSeekerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScrollSeekerServiceTest {

    @TempDir
    Path tempDir;

    private ScrollSeekerService createService() throws Exception {
        Path storage = tempDir.resolve("scrolls.db");
        Path uploads = tempDir.resolve("uploads");
        DigitalScrollRepository repository = new DigitalScrollRepository(storage);
        DigitalScrollService scrollService = new DigitalScrollService(repository, uploads);

        Path file = tempDir.resolve("spell.bin");
        Files.write(file, new byte[]{1, 2, 3, 4});
        scrollService.addScroll("mage", "Fireball", file.toString());
        return new ScrollSeekerService(scrollService);
    }

    @Test
    void filterPreviewAndDownloadScroll() throws Exception {
        ScrollSeekerService seeker = createService();

        List<DigitalScroll> all = seeker.filterScrolls("", "", "", null);
        assertEquals(1, all.size());

        List<DigitalScroll> byOwner = seeker.filterScrolls("mag", "", "", null);
        assertEquals(1, byOwner.size());

        List<DigitalScroll> none = seeker.filterScrolls("rogue", "", "", null);
        assertTrue(none.isEmpty());

        DigitalScroll scroll = all.get(0);
        ScrollPreview preview = seeker.buildPreview(scroll);
        assertTrue(preview.getSummary().contains("Fireball"));
        assertTrue(preview.getHexSample().contains("01"));

        Path target = tempDir.resolve("download.bin");
        seeker.downloadScroll(scroll, target);
        assertArrayEquals(Files.readAllBytes(Path.of(scroll.getFilePath())), Files.readAllBytes(target));

        List<DigitalScroll> refreshed = seeker.filterScrolls("", scroll.getScrollId(), "", LocalDate.now());
        assertEquals(1, refreshed.size());
        assertEquals(1, refreshed.get(0).getDownloadCount());
    }

    @Test
    void filterByNameAndDate() throws Exception {
        ScrollSeekerService seeker = createService();
        DigitalScroll scroll = seeker.filterScrolls("", "", "", null).get(0);
        List<DigitalScroll> byName = seeker.filterScrolls("", "", "fire", null);
        assertEquals(1, byName.size());
        List<DigitalScroll> byDate = seeker.filterScrolls("", "", "", scroll.getUploadTimestamp().toLocalDate());
        assertEquals(1, byDate.size());
        List<DigitalScroll> wrongDate = seeker.filterScrolls("", "", "", LocalDate.now().minusDays(1));
        assertTrue(wrongDate.isEmpty());
    }

    @Test
    void previewHandlesMissingFile() throws Exception {
        ScrollSeekerService seeker = createService();
        DigitalScroll scroll = seeker.filterScrolls("", "", "", null).get(0);
        Files.deleteIfExists(Path.of(scroll.getFilePath()));
        ScrollPreview preview = seeker.buildPreview(scroll);
        assertTrue(preview.getHexSample().contains("file not found"));
    }

    @Test
    void downloadCreatesDirectories() throws Exception {
        ScrollSeekerService seeker = createService();
        DigitalScroll scroll = seeker.filterScrolls("", "", "", null).get(0);
        Path target = tempDir.resolve("nested").resolve("download.bin");
        seeker.downloadScroll(scroll, target);
        assertTrue(Files.exists(target));
    }

    @Test
    void filterIsCaseInsensitive() throws Exception {
        ScrollSeekerService seeker = createService();
        List<DigitalScroll> result = seeker.filterScrolls("MAGE", "SC0001", "FIRE", null);
        assertEquals(1, result.size());
    }

    @Test
    void previewShowsEmptyFileMessage() throws Exception {
        ScrollSeekerService seeker = createService();
        Path empty = tempDir.resolve("empty.bin");
        Files.write(empty, new byte[0]);
        DigitalScroll scroll = new DigitalScroll("SCX0001", "Empty", "mage", empty.toString(), LocalDateTime.now(), 0, 0);

        ScrollPreview preview = seeker.buildPreview(scroll);
        assertTrue(preview.getHexSample().contains("file is empty"));
    }

    @Test
    void previewShowsFailureMessageWhenUnreadable() throws Exception {
        ScrollSeekerService seeker = createService();
        Path dir = tempDir.resolve("unreadable");
        Files.createDirectories(dir);
        DigitalScroll scroll = new DigitalScroll("SCX0002", "Dir", "mage", dir.toString(), LocalDateTime.now(), 0, 0);

        ScrollPreview preview = seeker.buildPreview(scroll);
        assertTrue(preview.getHexSample().contains("failed to read file"));
    }

    @Test
    void filterRejectsMismatchedIdAndName() throws Exception {
        ScrollSeekerService seeker = createService();
        assertTrue(seeker.filterScrolls("", "missing", "", null).isEmpty());
        assertTrue(seeker.filterScrolls("", "", "ice", null).isEmpty());
    }

    @Test
    void previewFormatsMultipleHexLines() throws Exception {
        ScrollSeekerService seeker = createService();
        Path multi = tempDir.resolve("multi.bin");
        byte[] bytes = new byte[20];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) i;
        }
        Files.write(multi, bytes);
        DigitalScroll scroll = new DigitalScroll("SCX0003", "Multi", "mage", multi.toString(), LocalDateTime.now(), 0, 0);

        String hex = seeker.buildPreview(scroll).getHexSample();
        assertTrue(hex.contains("\n"));
    }
}
